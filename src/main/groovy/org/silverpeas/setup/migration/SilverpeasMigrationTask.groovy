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
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.silverpeas.setup.api.DataSourceProvider

import java.sql.SQLException


/**
 * This task is for migrating the data sources used as the backend by Silverpeas. A migration is
 * either a fresh setting-up of a data source structure or an upgrade of an existing data source
 * schema. For doing it loads a set of migration modules that gather each of them the rules to
 * migrate a given part (aka a package) of Silverpeas. These rules are described and performed by
 * SQL and Groovy scripts.
 * <p/>
 * Among the data sources used by Silverpeas, a database is used as the backbone of its persistence
 * mechanism. Currently, the  only types of database supported by Silverpeas are H2, PostgreSQL,
 * MS-SQL, and Oracle).
 * @author mmoquillon
 */
class SilverpeasMigrationTask extends DefaultTask {

  /**
   * The descriptor of the migration tool itself. It creates the database tables required to manage
   * the migration of each other modules in Silverpeas.
   */
  private static final String MIGRATION_TOOL_DESCRIPTOR = 'dbbuilder-migration.xml'

  def settings

  SilverpeasMigrationTask() {
    description = 'Migrate in version the datasource schema expected by Silverpeas'
    group = 'Build'
    dependsOn = ['configureSilverpeas']
  }

  @TaskAction
  def performMigration() {
    StringBuilder errors = new StringBuilder()
    loadMigrationModules().each { module ->
      try {
        module.migrate()
      } catch(Exception ex) {
        errors.append(ex.message).append('\n')
      }
    }
    if (errors.length() > 0) {
      throw new TaskExecutionException(this, null)
    }
  }

  private List<MigrationModule> loadMigrationModules() {
    List<MigrationModule> modules = []
    def status = loadInstalledModuleStatus()
    new File("${project.silversetup.migrationHome}/modules").listFiles().each { descriptor ->
      MigrationModule module = new MigrationModule()
          .withStatus(status)
          .withSettings(settings)
          .loadMigrationsFrom(descriptor)
      if (module.isExecutionOrdered()) {
        modules.add(module.executionOrder(), module)
      } else {
        modules << module
      }
    }
    return modules
  }

  private def loadInstalledModuleStatus() {
    def status = [:]
    Sql sql = new Sql(DataSourceProvider.dataSource)
    try {
      sql.eachRow('SELECT sr_package, sr_version FROM sr_packages') { row ->
        status[row.sr_package] = row.sr_version
      }
    } catch (SQLException ex) {
      // the database isn't set up
    }
    return status
  }

}
