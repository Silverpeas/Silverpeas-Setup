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
package org.silverpeas.setup.database

import groovy.transform.builder.Builder

/**
 * A module in Silverpeas. A module defines a set of functionalities that can be technical and
 * business-oriented. A module can be a Silverpeas application (previously named component or peas)
 * or a part of Silverpeas Core.
 * <p/>
 * Each module is made up of both codes, configuration files, and a set of setting-up rules. The
 * setting-up rules are on how to build the different data sources for the module to work properly.
 * This class that represents a module does take into account only of the setting-up rules.
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
   * Migrates this module. If it is an installation, then it sets up the database structure that
   * is required by this module. In the case of an upgrade, the database structure is then
   * updated.
   */
  void migrate() {
    List<DatabaseMigration> migrations = loadMigrationsFromDescriptor()
    String status = '[OK]'
    try {
      println "Migration(s) of module ${module}"
      migrations*.migrate()
    } catch (Exception ex) {
      println "An error occurred during a migration of ${module}: stop it. Cause: ${ex.message}"
      status = '[NOK]'
    }
    println "Migration(s) of module ${module}: ${status}"
  }

  private List<DatabaseMigration> loadMigrationsFromDescriptor() {
    def migrationDescription = new XmlSlurper().parse(descriptor)
    module = migrationDescription.@module.text()
    String actualVersion = status[module]
    String toVersion = migrationDescription.current.@version.text()
    List<DatabaseMigration> migrations = []
    if (actualVersion) {
      for (int version = (actualVersion as int); version < (toVersion as int); version++) {
        List<MigrationScript> scripts =
            migrationDescription.upgrade.find {it.@fromVersion == "00${version}"}?.script.collect {
              MigrationScriptBuilder
                  .fromScript(absolutePathOfScript(it.@name.text(), "up00${version}"))
                  .ofType(MigrationScriptBuilder.ScriptType.valueOf(it.@type.text()))
                  .build()
            }
        migrations << DatabaseMigration.builder()
            .module(module)
            .fromVersion("00${version}")
            .toVersion("00${version + 1}")
            .scripts(scripts)
            .build()
      }
    } else {
      List<MigrationScript> scripts = migrationDescription.currrent.script.collect {
        MigrationScriptBuilder
            .fromScript(absolutePathOfScript(it.@name.text(), toVersion))
            .ofType(MigrationScriptBuilder.ScriptType.valueOf(it.@type.text()))
            .build()
      }
      migrations << DatabaseMigration.builder()
          .module(module)
          .toVersion(toVersion)
          .scripts(scripts)
          .build()
    }
    return migrations
  }

  private String absolutePathOfScript(String path, String version) {
    if (new File(path).isAbsolute()) {
      return path
    } else {
      String databaseType = settings.DB_SERVERTYPE.toLowerCase()
      return "${settings.DATABASE_HOME}/${databaseType}/${module}/${version}/${path}"
    }
  }
}
