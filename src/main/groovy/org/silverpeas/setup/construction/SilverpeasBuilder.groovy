/*
    Copyright (C) 2000 - 2018 Silverpeas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    As a special exception to the terms and conditions of version 3.0 of
    the GPL, you may redistribute this Program in connection with Free/Libre
    Open Source Software ("FLOSS") applications as described in Silverpeas's
    FLOSS exception.  You should have recieved a copy of the text describing
    the FLOSS exception, and it is also available here:
    "http://www.silverpeas.org/docs/core/legal/floss_exception.html"

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.setup.construction

import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil
import org.gradle.api.Project
import org.gradle.util.GFileUtils
import org.silverpeas.setup.api.FileLogger
import org.silverpeas.setup.api.ManagedBeanContainer
import org.silverpeas.setup.api.SilverpeasSetupService

import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
/**
 * A builder of the Silverpeas Collaborative Web Application from all the software bundles that made
 * it.
 * @author mmoquillon
 */
class SilverpeasBuilder {

  private static final String CORE_WAR_BUNDLE_ID = 'silverpeas-core-war'
  private static final String WEB_XML_PREFIX = 'web'
  private static final Pattern BUNDLE_NAME_PATTERN = ~/([a-zA-Z_\-]+)(-[0-9.]+[a-zA-Z0-9_\-.]*)(\.\w+)/

  private final Project project
  private FileLogger logger
  File silverpeasHome
  File driversDir
  Map settings
  boolean developmentMode

  SilverpeasBuilder(final Project project) {
    this(project, FileLogger.getLogger('builder'))
  }

  SilverpeasBuilder(final Project project, final FileLogger logger) {
    this.project = project
    this.logger = logger
  }

  /**
   * Extracts all the specified software bundles into the specified destination directory. The
   * way the bundles are extracted follows the guide rules of the Silverpeas Portal Application
   * construction.
   * @param silverpeasBundles a collection of the software bundles that makes Silverpeas.
   * @param tiersBundles a collection of tiers bundles to add to Silverpeas. This bundles are
   * processed differently than the Silverpeas ones.
   * @param destinationDir the destination directory into which the bundles will be extracted
   */
  void extractSoftwareBundles(
      final Collection<File> silverpeasBundles,
      final Collection<File> tiersBundles, final File destinationDir) {
    Objects.requireNonNull(silverpeasHome)
    Objects.requireNonNull(driversDir)
    def isAWar = { File f ->
      f.name.endsWith('.war') && !f.name.startsWith(CORE_WAR_BUNDLE_ID)
    }
    def isAConf = { File f ->
      f.name.matches(/^.*-configuration-.*.jar$/)
    }
    def isAJdbcDriver = { File f ->
      f.name.startsWith('postgresql') || f.name.startsWith('jtds') || f.name.startsWith('ojdbc')
    }
    def isARar = { File f ->
      f.name.endsWith('.rar')
    }
    def isALib = { File f ->
      f.name.endsWith('.jar')
    }

    // the silverpeas core war bundle is first extracted so that others war bundles have a chance
    // to overwrite some of its files (for customizing purpose)
    File silverpeasWar = findSilverpeasCoreWarBundle(silverpeasBundles)
    extractWarBundle(silverpeasWar, destinationDir)

    // now we extract the other Silverpeas bundles by their type
    silverpeasBundles.each { bundle ->
      if (isAWar(bundle)) {
        extractWarBundle(bundle, destinationDir)
      } else if (isAConf(bundle)) {
        extractConfigurationBundle(bundle, silverpeasHome)
      } else if (isAJdbcDriver(bundle)) {
        extractJdbcDriver(bundle, driversDir)
      } else if (isARar(bundle)) {
        extractRarBundle(bundle, project.buildDir)
      }
    }

    // extract now the tiers bundles to add to Silverpeas
    tiersBundles.each { bundle ->
      if (isAJdbcDriver(bundle)) {
        extractJdbcDriver(bundle, driversDir)
      } else if (isALib(bundle)) {
        extractLibBundle(bundle, Paths.get(destinationDir.path, 'WEB-INF', 'lib').toFile())
      }
    }

    moveSilverpeasDataDirectoriesContent()
  }

  /**
   * Generates the Silverpeas Application from all the bundles that were extracts in the specified
   * directory.
   * @param sourceDir the directory that contains all the extracts software bundles that made
   * Silverpeas.
   */
  void generateSilverpeasApplication(File sourceDir) {
    Objects.requireNonNull(silverpeasHome)
    if (!sourceDir.exists() &&
        Files.exists(Paths.get(sourceDir.path, 'WEB-INF', 'web.xml'))) {
      throw new IllegalArgumentException("The directory ${sourceDir.path} doesn't contain any extracted Silverpeas software bundles!")
    }

    compileWebDescriptor(sourceDir)
    compilePersistenceDescriptor(sourceDir)
  }

  /**
   * Finds among all the specified software bundles the Silverpeas Core War bundle. This bundle is
   * the main archive of the Silverpeas web application; all other war bundles enrich it with
   * additional features.
   * @param bundles all the software bundles that made Silverpeas.
   * @return the Silverpeas Core War bundle.
   */
  File findSilverpeasCoreWarBundle(Collection<File> bundles) {
    bundles.find { it.name.startsWith(CORE_WAR_BUNDLE_ID) }
  }

  /**
   * Extracts the specified war bundle into the specified directory.
   * @param war a war bundle.
   * @param destinationDir the directory into which the bundle's content has to be extracted.
   */
  void extractWarBundle(File war, File destinationDir) {
    logger.info "Extract ${war.name} into ${destinationDir.path}"
    project.copy {
      it.from(project.zipTree(war)) {
        it.rename 'web.xml', "${WEB_XML_PREFIX}-${war.name}.xml"
        if (!war.name.startsWith(CORE_WAR_BUNDLE_ID)) {
          it.exclude 'WEB-INF/classes/META-INF/MANIFEST.MF'
        }
      }
      it.into destinationDir
    }
  }

  private void extractConfigurationBundle(File conf, File destinationDir) {
    logger.info "Extract ${conf.name} into ${destinationDir}"
    project.copy {
      it.from(project.zipTree(conf))
      it.exclude '**/META-INF/**'
      it.into destinationDir
    }
  }

  private void extractRarBundle(File rar, File destinationDir) {
    logger.info "JCA found: ${rar.name}"
    project.copy {
      it.from(rar) {
        String nameWithoutVersion = rar.name
        Matcher matching = BUNDLE_NAME_PATTERN.matcher(rar.name)
        matching.each {
          nameWithoutVersion = it[1] + it[3]
        }
        it.rename rar.name, nameWithoutVersion
      }
      it.into destinationDir
    }
  }

  private void extractJdbcDriver(File driver, File desinationDir) {
    logger.info "JDBC driver found: ${driver.name}"
    // h2 is already provided by JBoss >= 8
    project.copy {
      it.from(driver) {
        String nameWithoutVersion = driver.name
        Matcher matching = BUNDLE_NAME_PATTERN.matcher(driver.name)
        matching.each {
          nameWithoutVersion = it[1] + it[3]
        }
        it.rename driver.name, nameWithoutVersion
      }
      it.into desinationDir
    }
  }

  private void extractLibBundle(File lib, File destinationDir) {
    String jarType = lib.name.toLowerCase().indexOf('silverpeas') >= 0 ? 'Silverpeas' : 'Tiers'
    logger.info "${jarType} Library found: ${lib.name}"
    project.copy {
      it.from lib
      it.into destinationDir
    }
  }

  private void compileWebDescriptor(File sourceDir) {
    // merge all of the web.xml from the different WARs into a single one
    logger.info "Compile the web.xml from all the web descriptors of the WARs"
    List<File> mainWebXmlFiles = new File(sourceDir, 'WEB-INF').listFiles(new FilenameFilter() {
      boolean accept(File f, String fileName) {
        return fileName.startsWith("${WEB_XML_PREFIX}-${CORE_WAR_BUNDLE_ID}") &&
            fileName.endsWith('war.xml')
      }
    })
    if (mainWebXmlFiles.size() != 1) {
      throw new IllegalStateException('No main web descriptor found!')
    }

    File mainWebXmlFile = mainWebXmlFiles.get(0)
    GPathResult mainWebXml = new XmlSlurper(false, false).parse(mainWebXmlFile)
    new File(sourceDir, 'WEB-INF').listFiles(new FilenameFilter() {
      boolean accept(File f, String fileName) {
        return fileName.startsWith(WEB_XML_PREFIX) && fileName.endsWith('war.xml') &&
            fileName != mainWebXmlFile.name
      }
    }).each {
      // the sub-elements of the web-app elements can be in the arbitrary order.
      GPathResult aWebXml = new XmlSlurper(false, false).parse(it)
      aWebXml.'context-param'.each { elt -> mainWebXml.appendNode(elt) }
      aWebXml.'filter'.each { elt -> mainWebXml.appendNode(elt) }
      aWebXml.'filter-mapping'.each { elt -> mainWebXml.appendNode(elt) }
      aWebXml.'listener'.each { elt -> mainWebXml.appendNode(elt) }
      aWebXml.'servlet'.each { elt -> mainWebXml.appendNode(elt) }
      aWebXml.'servlet-mapping'.each { elt -> mainWebXml.appendNode(elt) }
      aWebXml.'resource-env-ref'.each { elt -> mainWebXml.appendNode(elt) }
      GFileUtils.forceDelete(it)
    }
    XmlUtil.serialize(mainWebXml,
        new FileWriter(Paths.get(sourceDir.path, 'WEB-INF', 'web.xml').toFile()))
    GFileUtils.forceDelete(mainWebXmlFile)
  }

  private void compilePersistenceDescriptor(File sourceDir) {
    // generate the final persistence.xml in which each Silverpeas components are referenced as a
    // JPA entities provider
    logger.info "Compile the persistence.xml from all the providers of JPA entities"
    File xmlPersistenceFile =
        Paths.get(sourceDir.path, 'WEB-INF', 'classes', 'META-INF', 'persistence.xml').toFile()
    GPathResult persistence = new XmlSlurper(false, false).parse(xmlPersistenceFile)
    Paths.get(sourceDir.path, 'WEB-INF', 'lib').toFile().list(new FilenameFilter() {
      @Override
      boolean accept(final File dir, final String name) {
        return name.endsWith("''${project.version}.jar")
      }
    }).each { jpaComponent ->
      persistence.'persistence-unit'.'jta-data-source' + {
        'jar-file'("lib/${jpaComponent}")
      }
    }
    XmlUtil.serialize(persistence, new FileWriter(xmlPersistenceFile))

    if (!developmentMode) {
      logger.info "Generate silverpeas.war in ${project.buildDir.path}"
      project.ant.zip(destfile: "${project.buildDir.path}/silverpeas.war", baseDir: sourceDir.path)
    } else {
      logger.info "Silverpeas Application generation done in ${sourceDir}"
    }
  }

  private void moveSilverpeasDataDirectoriesContent() {
    // now move data and web data directories if necessary
    SilverpeasSetupService service = ManagedBeanContainer.get(SilverpeasSetupService)
    
    final File silverpeasDataHome = new File(silverpeasHome, 'data')
    final File silverpeasWebDataHome = new File(silverpeasDataHome, 'web')

    final File dataDestinationDir = new File(service.expanseVariables(settings.SILVERPEAS_DATA_HOME))
    final File webDataDestinationDir = new File(service.expanseVariables(settings.SILVERPEAS_DATA_WEB))

    if (webDataDestinationDir.path != silverpeasWebDataHome.path) {
      logger.info "Move content of ${silverpeasWebDataHome.path} into ${webDataDestinationDir.path}"
      if (!webDataDestinationDir.exists()) {
        webDataDestinationDir.mkdirs()
      }
      project.ant.move(todir: webDataDestinationDir.path) {
        fileset(dir: silverpeasWebDataHome.path) {
          include(name: '**/*')
        }
      }
    }
    if (dataDestinationDir.path != silverpeasDataHome.path) {
      logger.info "Move content of ${silverpeasDataHome.path} into ${dataDestinationDir.path}"
      if (!dataDestinationDir.exists()) {
        dataDestinationDir.mkdirs()
      }
      project.ant.move(todir: dataDestinationDir.path) {
        fileset(dir: silverpeasDataHome.path) {
          include(name: '**/*')
          exclude(name: 'web/**')
        }
      }
    }
  }
}