/*
  Copyright (C) 2000 - 2015 Silverpeas

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

import groovy.transform.builder.Builder

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
@Builder
class MigrationModule {

  private String module
  File descriptor
  def status = [:]
  def settings

  private MigrationModule() {

  }

  /**
   * Migrates the package represented by this module. If it is an installation, then it sets up the
   * persistence schema required by the package in the concerned data source structure. Otherwhise,
   * the existing persistence schema is just upgraded to the latest state.
   * </p>
   * In the case of an upgrade, it will occur in different steps of migration, each of them
   * representing the passing from one version to the next one up to the available latest version.
   * Each of these migration will be performed within their own SQL transaction.
   */
  void migrate() {
    List<DatasourceMigration> migrations = loadMigrationsFromDescriptor()
    String status = '[OK]'
    try {
      println "Migration(s) of module ${module}"
      migrations*.migrate(settings)
    } catch (Exception ex) {
      println "An error occurred during a migration of ${module}: stop it. Cause: ${ex.message}"
      status = '[NOK]'
    }
    println "Migration(s) of module ${module}: ${status}"
  }

  private List<DatasourceMigration> loadMigrationsFromDescriptor() {
    def migrationDescription = new XmlSlurper().parse(descriptor)
    module = migrationDescription.@module.text()
    String actualVersion = status[module]
    String toVersion = migrationDescription.current.@version.text()
    List<DatasourceMigration> migrations = []
    if (actualVersion) {
      for (int version = (actualVersion as int); version < (toVersion as int); version++) {
        List<MigrationScript> scripts =
            migrationDescription.upgrade.find {it.@fromVersion == "00${version}"}?.script.collect {
              MigrationScriptBuilder
                  .fromScript(absolutePathOfScript(it.@name.text(), it.@type.text(), "up00${version}"))
                  .ofType(MigrationScriptBuilder.ScriptType.valueOf(it.@type.text()))
                  .build()
            }
        migrations << DatasourceMigration.builder()
            .module(module)
            .fromVersion("00${version}")
            .toVersion("00${version + 1}")
            .scripts(scripts)
            .build()
      }
    } else {
      List<MigrationScript> scripts = migrationDescription.current.script.collect {
        MigrationScriptBuilder
            .fromScript(absolutePathOfScript(it.@name.text(), it.@type.text(), toVersion))
            .ofType(MigrationScriptBuilder.ScriptType.valueOf(it.@type.text()))
            .build()
      }
      migrations << DatasourceMigration.builder()
          .module(module)
          .toVersion(toVersion)
          .scripts(scripts)
          .build()
    }
    return migrations
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
}
