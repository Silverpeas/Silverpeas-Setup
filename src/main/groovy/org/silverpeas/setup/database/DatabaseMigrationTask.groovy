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
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import static org.silverpeas.setup.database.DatabaseType.h2

/**
 * This task is for migrating the datasources used as the backend by Silverpeas. A migration is
 * either a fresh setting-up of the database structure or an upgrade of an existing database
 * schema. For doing it loads and runs a set of migration rules for each module that made
 * Silverpeas. The migrations rules are SQL or Groovy scripts.
 * <p/>
 * Currently, the data sources supported by Silverpeas are the databases (H2, PostgreSQL, MS-SQL,
 * and Oracle), the filesystem and the JCR Jackrabbit.
 * @author mmoquillon
 */
class DatabaseMigrationTask extends DefaultTask {

  def settings

  @TaskAction
  def buildDatasources() {
    DatabaseMigration.databaseHome = project.silverconf.databaseHome
    loadSilverpeasModules().each { module ->
      module.migrate()
    }
  }

  List<MigrationModule> loadMigrationModules() {
    List<MigrationModule> modules = []
    String databaseType = settings.DB_SERVERTYPE.toLowerCase()
    def status = loadInstalledModuleStatus()
    new File("${project.silverconf.databaseHome}/data/${databaseType}").listFiles() { descriptor ->
      modules << MigrationModule.builder()
          .descriptor(descriptor)
          .settings(settings)
          .status(status)
          .build()
    }
    return modules
  }

  private def loadInstalledModuleStatus() {
    def status = [:]
    Sql sql = new Sql(DataSourceProvider.dataSource)
    sql.eachRow('SELECT sr_package, sr_version FROM sr_packages') { row ->
      status[row.sr_package] = row.sr_version
    }
    return status
  }

}
