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

import groovy.sql.Sql
import groovy.transform.builder.Builder

import java.sql.SQLException

import static org.silverpeas.setup.database.MigrationScriptBuilder.ScriptType.sql

/**
 * The migration of the structure (aka schema) of a given database. For a first installation,
 * the migration consists of setting-up the database structure, whereas for each further
 * Silverpeas upgrade it is on the update of the database schema. Currently, only SQL and
 * Groovy scripts are supported.
 * @author mmoquillon
 */
@Builder
class DatabaseMigration {

  private static String VERSION_UPDATE =
      'UPDATE sr_packages SET sr_version = :version WHERE sr_package = :module'
  private static String MODULE_INSTALL =
      'INSERT INTO sr_packages (sr_package, sr_version) VALUES (:module, :version)'

  String module
  String fromVersion
  def scripts = []
  String toVersion

  private DatabaseMigration() {

  }

  def migrate() {
    Sql sql = new Sql(DataSourceProvider.dataSource)
    if (isAnInstallation()) {
      performInstallation(sql)
    } else if (isAnUpgrade()) {
      performUpgrade(sql)
    } else {
      println "Error. I cannot downgrade module ${module} from ${toVersion} downto ${fromVersion}"
    }
  }

  boolean isAnInstallation() {
    return fromVersion == null
  }

  boolean isAnUpgrade() {
    return fromVersion != null && (fromVersion as int) < (toVersion as int)
  }

  private void performInstallation(Sql sql) {
    println "Installation of the module ${module} to version ${toVersion}"
    String status = 'OK'
    try {
      sql.withTransaction {
        scripts*.run(sql)
        int count = sql.executeUpdate(MODULE_INSTALL, [module: this.module, version: this.toVersion])
        if (count != 1) {
          throw new SQLException("Setting up of module to version ${toVersion} not done!")
        }
      }
    } catch (Exception ex) {
      status = 'NOK'
      println "Installation failure! Cause: ${ex.message}"
    }
    println "Installation of the module ${module} to version ${toVersion}: [${status}]"
  }

  private void performUpgrade(Sql sql) {
    println "Upgrade of the module ${module} to version ${toVersion}"
    String status = 'OK'
    try {
      sql.withTransaction {
        scripts*.run(sql)
        int count = sql.executeUpdate(VERSION_UPDATE, [module: this.module, version: this.toVersion])
        if (count != 1) {
          throw new SQLException("Upgrade of module to version ${toVersion} not done!")
        }
      }
    } catch (Exception ex) {
      status = 'NOK'
      println "Upgrade failure! Cause: ${ex.message}"
    }
    println "Upgrade of the module ${module} to version ${toVersion}: [${status}]"
  }
}
