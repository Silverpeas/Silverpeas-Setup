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

import groovy.sql.Sql
import groovy.transform.builder.Builder

import java.sql.SQLException

/**
 * The migration of the structure of a data source for a given package in Silverpeas. It consists,
 * in each data source concerned by the migration, to change the persistence schema of the package
 * from one version to a next one. The change are taken in charge by a set of scripts that define
 * the rules of the migration itself. The migration is performed within a single transaction.
 * @author mmoquillon
 */
@Builder
class DatasourceMigration {

  private static String VERSION_UPDATE =
      'UPDATE sr_packages SET sr_version = :version WHERE sr_package = :module'
  private static String MODULE_INSTALL =
      'INSERT INTO sr_packages (sr_package, sr_version) VALUES (:module, :version)'

  String module
  String fromVersion
  String toVersion
  def scripts = []

  private DatasourceMigration() {

  }

  /**
   * Migrates the structure of the data sources for the module referred by this instance and by
   * applying the registered scripts. The scripts, whatever their type, are executed within a
   * single SQL transaction. If an error occurs, then all is rolled back (be caution on the changes
   * caused by programming scripts as they won't be rolled back automatically by the migration
   * process).
   * @param settings the settings applied in the migration of Silverpeas.
   */
  def migrate(settings) {
    Sql sql = new Sql(DataSourceProvider.dataSource)
    def settingsToApply = (settings ? settings:[:])
    if (isAnInstallation()) {
      performInstallation(sql, settingsToApply)
    } else if (isAnUpgrade()) {
      performUpgrade(sql, settingsToApply)
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

  private void performInstallation(Sql sql, def settings) {
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

  private void performUpgrade(Sql sql, def settings) {
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
