/*
  Copyright (C) 2000 - 2019 Silverpeas

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
package org.silverpeas.setup.migration

import com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.gradle.api.tasks.TaskExecutionException
import org.silverpeas.setup.api.FileLogger
import org.silverpeas.setup.api.Script

import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import java.nio.file.Path
import java.nio.file.Paths
/**
 * A migration module. It is described by an XML descriptor located in the directory
 * <code>SILVERPEAS_HOME/migration/modules/</code> and it defines all the migration processes
 * to apply to the data sources in order to keep up to date the persistence schema of a given part
 * of Silverpeas (aka a package). Each migration processes does then a change on the structure of
 * the data sources. A package, referred by a migration module, can be either a component of
 * Silverpeas Core or a Silverpeas application.
 * @author mmoquillon
 */
@Builder(builderStrategy=SimpleStrategy, prefix='with')
class MigrationModule {

  private static final int UNORDERED = Integer.MAX_VALUE

  private String module
  private Integer order
  private List<DatasourceMigration> migrations = []
  Map<String, String> status
  Map settings
  FileLogger logger

  /**
   * The name of this migration module.
   * @return the migration module name.
   */
  String name() {
    return module
  }

  /**
   * The order of this migration module among others modules. It indicates at which order it must
   * be executed. The special value UNORDERED means no order is specified and it can then be
   * executed in any order.
   * @return the order this migration module should be executed. UNORDERED means no order so it can
   * be executed in any order.
   */
  int executionOrder() {
    return order
  }

  /**
   * Is this migration module execution is ordered?
   * @return true if the migration of this module should be executed at a specific order, false
   * otherwise.
   */
  boolean isExecutionOrdered() {
    return order != UNORDERED
  }

  /**
   * Migrates the package represented by this module. If it is an installation, then it sets up the
   * persistence schema required by the package in the concerned data source structure. Otherwhise,
   * the existing persistence schema is just upgraded to the latest state.
   * </p>
   * In the case of an upgrade, it will occur in different steps of migration, each of them
   * representing the passing from one version to the next one up to the available latest version.
   * Each of these migration will be performed within their own SQL transaction.
   * @throws TaskExecutionException if an error occurs while migrating the datasource for this
   * module.
   */
  void migrate() throws Exception {
    String result = '[OK]'
    try {
      logger.info "Migration(s) of module ${module}"
      migrations*.migrate()
    } catch (Exception ex) {
      result = '[FAILURE]'
      throw ex
    } finally {
      logger.info "Migration(s) of module ${module}: ${result}"
    }
  }

  /**
   * Loads all the migration rules from the specified XML descriptor.
   * @param descriptor the migration descriptor in which are described the different rules to apply.
   * @return itself, initialized by the specified descriptor.
   */
  MigrationModule loadMigrationsFrom(File descriptor) {
    logger.info "Load migration descriptor ${descriptor.name}"
    validateDescriptor(descriptor)
    def migrationDescriptor = new XmlSlurper().parse(descriptor)
    this.module =  migrationDescriptor.@module.text()
    this.order = migrationDescriptor.@order?.text() ? migrationDescriptor.@order.text() as int : UNORDERED
    String actualVersion = status[module]
    String toVersion = migrationDescriptor.current.@version.text()
    logger.debug "  Module ${module}: actual version is '${actualVersion}'."
    logger.debug "  Module ${module}: expected version is '${toVersion}'"
    if (actualVersion) {
      for (int version = (actualVersion as int); version < (toVersion as int); version++) {
        String versionIn3Digits = String.format("%03d", version)
        List<Script> scripts =
            migrationDescriptor.upgrade.find {it.@fromVersion == versionIn3Digits}?.script.collect {
              MigrationScriptBuilder
                  .fromScript(absolutePathOfScript(it.@name.text(), it.@type.text(), "up${versionIn3Digits}"))
                  .ofType(MigrationScriptBuilder.ScriptType.valueOf(it.@type.text()))
                  .build()
            }
        if (scripts.empty) {
          throw new RuntimeException('Missing upgrade statements from version ' + versionIn3Digits)
        }
        migrations << DatasourceMigration.builder()
            .module(module)
            .fromVersion(versionIn3Digits)
            .toVersion(String.format("%03d", version + 1))
            .scripts(scripts)
            .settings(settings)
            .logger(logger)
            .build()
      }
    } else {
      List<Script> scripts = migrationDescriptor.current.script.collect {
        MigrationScriptBuilder
            .fromScript(absolutePathOfScript(it.@name.text(), it.@type.text(), toVersion))
            .ofType(MigrationScriptBuilder.ScriptType.valueOf(it.@type.text()))
            .build()
      }
      migrations << DatasourceMigration.builder()
          .module(module)
          .toVersion(toVersion)
          .scripts(scripts)
          .settings(settings)
          .logger(logger)
          .build()
    }
    return this
  }

  private String absolutePathOfScript(String path, String type, String version) {
    Path scriptPath = Paths.get(path)
    if (!scriptPath.absolute) {
      switch (type.toLowerCase()) {
        case 'sql':
          String databaseType = settings.DB_SERVERTYPE.toLowerCase()
          scriptPath = Paths.get("${settings.MIGRATION_HOME}/db/${databaseType}/${module}/${version}/${path}")
          break
        case 'groovy':
          scriptPath = Paths.get("${settings.MIGRATION_HOME}/scripts/${module}/${version}/${path}")
          break
        default:
          throw new IllegalFormatException("The script type ${type} isn't supported by the migration tool!")
      }
    }
    if (!scriptPath.toFile().exists()) {
      throw new FileNotFoundException("The script at ${scriptPath.toString()} doesn't exist!")
    }
    return scriptPath.toString()
  }

  private void validateDescriptor(File descriptor) {
    def xsd = new StreamSource(getClass().getResourceAsStream('/migration.xsd'))
    def factory = XMLSchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
    def schema = factory.newSchema([xsd] as Source[])
    def validator = schema.newValidator()
    validator.validate(new StreamSource(new FileReader(descriptor)))
  }
}
