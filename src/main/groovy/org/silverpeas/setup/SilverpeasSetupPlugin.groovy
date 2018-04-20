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
package org.silverpeas.setup

import org.gradle.api.*
import org.silverpeas.setup.api.DataSourceProvider
import org.silverpeas.setup.api.FileLogger
import org.silverpeas.setup.api.ManagedBeanContainer
import org.silverpeas.setup.api.SilverpeasSetupService
import org.silverpeas.setup.configuration.JBossConfigurationTask
import org.silverpeas.setup.configuration.SilverpeasConfigurationTask
import org.silverpeas.setup.configuration.VariableReplacement
import org.silverpeas.setup.construction.SilverpeasBuilder
import org.silverpeas.setup.construction.SilverpeasConstructionTask
import org.silverpeas.setup.migration.SilverpeasMigrationTask
import org.silverpeas.setup.security.Encryption
import org.silverpeas.setup.security.EncryptionFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.silverpeas.setup.api.SilverpeasSetupTaskNames.*

/**
 * This plugin aims to prepare the configuration and to setup Silverpeas.
 * For doing, it loads both the default and the customer configuration file of Silverpeas and it
 * registers two tasks, one dedicated to configure JBoss/Wildfly for Silverpeas and another to
 * configure Silverpeas.
 * <p>
 * The String class is dynamically extended with an additional method, String#asPath(),
 * that returns a Path instance from the path representation of the String. The difference between
 * this method and {@code Paths # getPath ( String )} is that the former takes into account any
 * system properties or environment variables in the path; if the path contains any system or
 * environment variables, they are then replaced by their value.
 * </p>
 */
class SilverpeasSetupPlugin implements Plugin<Project> {

  public static final String EXTENSION = 'silversetup'

  @Override
  void apply(Project project) {
    def extension = project.extensions.create(EXTENSION, SilverpeasSetupExtension, project)
    initSilverpeasSetupExtention(extension)

    SilverpeasSetupService setupService = new SilverpeasSetupService(extension.config.settings)
    ManagedBeanContainer.registry()
        .register(new DataSourceProvider(extension.config.settings))
        .register(setupService)
    String.metaClass.asPath = { Paths.get(setupService.expanseVariables(delegate.toString())) }

    project.afterEvaluate { Project currentProject, ProjectState state ->
      SilverpeasSetupExtension silverSetup =
          (SilverpeasSetupExtension) currentProject.extensions.getByName(EXTENSION)
      silverSetup.config.settings.DEV_MODE = silverSetup.developmentMode as String
      if (silverSetup.logging.useLogger) {
        initLogging(currentProject, silverSetup.logging)
      }
      silverSetup.config.settings.SILVERPEAS_VERSION = silverSetup.silverpeasVersion as String
    }

    project.tasks.create(CONFIGURE_JBOSS.name, JBossConfigurationTask) {
      it.jbossHome  = extension.jbossHome
      it.driversDir = extension.driversDir
      it.config     = extension.config
      it.logConfig  = extension.logging
    }

    project.tasks.create(CONFIGURE_SILVERPEAS.name, SilverpeasConfigurationTask) {
      it.silverpeasHome = extension.silverpeasHome
      it.config         = extension.config
    }

    project.tasks.create(MIGRATE.name, SilverpeasMigrationTask) {
      it.migrationHome = extension.migrationHome
      it.config        = extension.config
    }

    File distDir = new File (project.buildDir, 'dist')
    project.tasks.create(CONSTRUCT.name, SilverpeasConstructionTask) {
      it.silverpeasHome    = extension.silverpeasHome
      it.driversDir        = extension.driversDir
      it.developmentMode   = extension.developmentMode
      it.silverpeasBundles = extension.silverpeasBundles
      it.tiersBundles      = extension.tiersBundles
      it.destinationDir    = distDir
    }

    setUpGradleAssemblingTaskForThisPlugin(project, extension, distDir)
    setUpGradleBuildTaskForThisPlugin(project, extension, distDir)
  }

  private void setUpGradleAssemblingTaskForThisPlugin(Project project,
                                                      SilverpeasSetupExtension extension,
                                                      File distDir) {
    try {
      Task assemble = project.tasks.getByName(ASSEMBLE.name).doLast {
        if (!distDir.exists()) {
          distDir.mkdirs()
        }
        SilverpeasBuilder builder = new SilverpeasBuilder(project, FileLogger.getLogger(delegate.name))
        builder.driversDir = extension.driversDir
        builder.silverpeasHome = extension.silverpeasHome
        builder.extractSoftwareBundles(extension.silverpeasBundles.files,
            extension.tiersBundles.files, distDir)
      }
      assemble.description = 'Assemble all the software bundles that made Silverpeas'
      assemble.onlyIf { !distDir.exists() && !extension.driversDir.exists()}
      assemble.outputs.upToDateWhen {
        distDir.exists() && extension.driversDir.exists()
      }
    } catch (UnknownTaskException e) {
      // nothing to do
    }
  }

  private void setUpGradleBuildTaskForThisPlugin(Project project,
                                                 SilverpeasSetupExtension extension,
                                                 File distDir) {
    try {
      Task build = project.tasks.getByName(BUILD.name).doLast {
        SilverpeasBuilder builder = new SilverpeasBuilder(project, FileLogger.getLogger(delegate.name))
        builder.silverpeasHome = extension.silverpeasHome
        builder.developmentMode = extension.developmentMode
        builder.generateSilverpeasApplication(distDir)
      }
      build.description = 'Build the Silverpeas Collaborative Web Application'
      build.onlyIf {
        distDir.exists()
      }
      build.outputs.upToDateWhen {
        boolean ok = distDir.exists() &&
            Files.exists(Paths.get(distDir.path, 'WEB-INF', 'web.xml'))
        if (!extension.developmentMode) {
          ok = ok && Files.exists(
              Paths.get(project.buildDir.path, SilverpeasConstructionTask.SILVERPEAS_WAR))
        }
        return ok
      }
    }  catch (UnknownTaskException e) {
      // nothing to do
    }
  }

  private void initSilverpeasSetupExtention(SilverpeasSetupExtension silverSetup) {
    silverSetup.config.settings = loadConfigurationProperties(silverSetup.config.configurationHome)
    completeSettings(silverSetup.config.settings, silverSetup)
    encryptAdminPassword(silverSetup.config.settings)
  }

  private Map loadConfigurationProperties(File configurationHome) {
    Properties properties = new Properties()
    properties.load(getClass().getResourceAsStream('/default_config.properties'))
    def customConfiguration = new File(configurationHome, 'config.properties')
    // the custom configuration overrides the default configuration
    if (customConfiguration.exists()) {
      Properties customProperties = new Properties()
      customProperties.load(customConfiguration.newReader())
      customProperties.propertyNames().each {
        properties[it] = customProperties[it]
      }
    }
    // replace the variables by their values in the properties and return the properties
    return VariableReplacement.parseParameters(properties, properties)
  }

  private void completeSettings(Map settings, SilverpeasSetupExtension silverSetup) {
    settings.SILVERPEAS_HOME = normalizePath(silverSetup.silverpeasHome.path)
    settings.MIGRATION_HOME = normalizePath(silverSetup.migrationHome.path)
    settings.CONFIGURATION_HOME = normalizePath(silverSetup.config.configurationHome.path)
    settings.DB_DATASOURCE_JNDI = 'java:/datasources/silverpeas'
    settings.SILVERPEAS_DATA_HOME = normalizePath(settings.SILVERPEAS_DATA_HOME)
    settings.SILVERPEAS_DATA_WEB = normalizePath(settings.SILVERPEAS_DATA_WEB)
    switch (settings.DB_SERVERTYPE) {
      case 'MSSQL':
        settings.DB_URL = "jdbc:jtds:sqlserver://${settings.DB_SERVER}:${settings.DB_PORT_MSSQL}/${settings.DB_NAME}"
        settings.JCR_URL = "jdbc:jtds:sqlserver://${settings.DB_SERVER}:${settings.DB_PORT_MSSQL}/${settings.JCR_NAME}"
        settings.DB_DRIVER = 'net.sourceforge.jtds.jdbc.Driver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.MSSqlPersistenceManager'
        break
      case 'ORACLE':
        settings.DB_URL = "jdbc:oracle:thin:@${settings.DB_SERVER}:${settings.DB_PORT_ORACLE}:${settings.DB_NAME}"
        settings.JCR_URL = "jdbc:oracle:thin:@${settings.DB_SERVER}:${settings.DB_PORT_ORACLE}:${settings.JCR_NAME}"
        settings.DB_DRIVER = 'oracle.jdbc.driver.OracleDriver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.OraclePersistenceManager'
        break
      case 'POSTGRESQL':
        settings.DB_URL = "jdbc:postgresql://${settings.DB_SERVER}:${settings.DB_PORT_POSTGRESQL}/${settings.DB_NAME}"
        settings.JCR_URL = "jdbc:postgresql://${settings.DB_SERVER}:${settings.DB_PORT_POSTGRESQL}/${settings.JCR_NAME}"
        settings.DB_DRIVER = 'org.postgresql.Driver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.PostgreSQLPersistenceManager'
        break
      case 'H2':
        if (settings.DB_SERVER == ':file:') {
          Path databaseDirPath = Paths.get(settings.SILVERPEAS_HOME, 'h2')
          if (!Files.exists(databaseDirPath))
            Files.createDirectory(databaseDirPath)
          settings.DB_URL = "jdbc:h2:file:${settings.SILVERPEAS_HOME}/h2/${settings.DB_NAME};MV_STORE=FALSE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
          settings.JCR_URL = "jdbc:h2:file:${settings.SILVERPEAS_HOME}/h2/${settings.JCR_NAME};MV_STORE=FALSE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        } else {
          settings.DB_URL = "jdbc:h2:tcp://${settings.DB_SERVER}:${settings.DB_PORT_H2}/${settings.DB_NAME}"
          settings.JCR_URL = "jdbc:h2:tcp://${settings.DB_SERVER}:${settings.DB_PORT_H2}/${settings.JCR_NAME}"
        }
        settings.DB_DRIVER = 'org.h2.Driver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.H2PersistenceManager'
        break
      default:
        throw new IllegalArgumentException("Unsupported database system: ${settings.DB_SERVERTYPE}")
    }
    settings.DB_SCHEMA = settings.DB_SERVERTYPE.toLowerCase()
  }

  private void encryptAdminPassword(Map settings) {
    Encryption encryption = EncryptionFactory.instance.createDefaultEncryption()
    settings.SILVERPEAS_ADMIN_PASSWORD = encryption.encrypt(settings.SILVERPEAS_ADMIN_PASSWORD)
  }

  private void initLogging(Project project, SilverpeasLoggingProperties loggingProperties) {
    String timestamp = new Date().format('yyyyMMdd_HHmmss')
    if (!loggingProperties.logDir.exists()) {
      loggingProperties.logDir.mkdirs()
    }
    File logFile = new File(loggingProperties.logDir, "build-${timestamp}.log")
    FileLogger.init(logFile, loggingProperties.defaultLevel)

    /* customize the traces writing both on the standard output and on the log file. */
    project.gradle.useLogger(new TaskEventLogging()
        .withTasks(loggingProperties.scriptTasks))
  }

  private static String normalizePath(String path) {
    return path.replaceAll('\\\\', '/')
  }
}
