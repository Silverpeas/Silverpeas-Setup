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

import groovy.sql.Sql
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.silverpeas.setup.SilverpeasConfigurationProperties
import org.silverpeas.setup.api.DataSourceProvider
import org.silverpeas.setup.api.FileLogger
import org.silverpeas.setup.api.ManagedBeanContainer

import java.nio.file.Path
import java.nio.file.Paths
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException
/**
 * This task is for migrating the data sources used as the backend by Silverpeas. A migration is
 * either a fresh setting-up of a data source structure or an upgrade of an existing data source
 * schema. For doing it loads a set of migration modules that gather each of them the rules to
 * migrate a given part (aka a package) of Silverpeas. These rules are described and performed by
 * SQL and Groovy scripts.
 * <p/>
 * Both the settings and the data of a data source migration process are persisted into the default
 * data source used as the backbone of the Silverpeas persistence. As such, they can also be
 * subject to a migration task. Hence, the migration tool is also represented by a migration
 * module that is loaded and migrated before any others migration modules.
 * </p>
 * Among the data sources used by Silverpeas, a database is used as the backbone of its persistence
 * mechanism. Currently, the  only types of database supported by Silverpeas are H2, PostgreSQL,
 * MS-SQL, and Oracle).
 * @author mmoquillon
 */
class SilverpeasMigrationTask extends DefaultTask {

  static final String MIGRATION_SETTING_MODULE = 'dbbuilder-migration.xml'

  File migrationHome
  SilverpeasConfigurationProperties config
  final FileLogger log = FileLogger.getLogger(this.name)

  SilverpeasMigrationTask() {
    description = 'Migrate in version the data source structure expected by Silverpeas'
    group = 'Build'
  }

  @TaskAction
  void performMigration() {
    initMigrationTask()
    loadMigrationModules().each { module ->
      try {
        module.migrate()
      } catch (Exception ex) {
        log.error(ex)
        throw new TaskExecutionException(this, ex)
      }
    }
  }

  /**
   * Initializes the migration task first by loading the settings of the migration process, then by
   * performing any migration of these settings if it is required.
   * </p>
   * The migration settings are also persisted in a data source and they could be subject to a
   * migration before doing anything.
   */
  private void initMigrationTask() {
    log.info 'Migration initialization...'
    def status = loadInstalledModuleStatus()
    Path descriptor = Paths.get(migrationHome.path, 'modules', MIGRATION_SETTING_MODULE)
    MigrationModule module = new MigrationModule()
        .withStatus(status)
        .withSettings(config.settings)
        .withLogger(log)
        .loadMigrationsFrom(descriptor.toFile())
    module.migrate()
    log.info 'Migration initialization done'
  }

  /**
   * Loads all the available migration modules in Silverpeas.
   * @return a list of the available migration modules.
   */
  private List<MigrationModule> loadMigrationModules() {
    def status = loadInstalledModuleStatus()
    List<MigrationModule> modules = []
    new File(migrationHome, 'modules').listFiles().each { descriptor ->
      if (descriptor.name != MIGRATION_SETTING_MODULE) {
        MigrationModule module = new MigrationModule()
            .withStatus(status)
            .withSettings(config.settings)
            .withLogger(log)
            .loadMigrationsFrom(descriptor)
        modules << module
      }
    }
    return modules.sort { module1, module2 ->
      module1.executionOrder() <=> module2.executionOrder()
    }
  }

  private def loadInstalledModuleStatus() {
    def level = logging.level
    logging.captureStandardOutput(LogLevel.ERROR)
    def status = [:]
    DataSourceProvider dataSourceProvider = ManagedBeanContainer.get(DataSourceProvider.class)
    Sql sql = new Sql(dataSourceProvider.dataSource)
    try {
      DatabaseMetaData metaData = sql.getDataSource().getConnection().getMetaData()
      ResultSet tables = metaData.getTables(null, null, 'sr_packages', ['TABLE'] as String[])
      if (!tables.next()) {
        tables = metaData.getTables(null, null, 'SR_PACKAGES', ['TABLE'] as String[])
        if (tables.next()) {
          fetchModuleStatusFromDb(sql, status)
        } else {
          log.info 'This is a fresh installation'
        }
      } else {
        fetchModuleStatusFromDb(sql, status)
      }
    } catch (SQLException ex) {
      // the database isn't set up
    }
    logging.captureStandardOutput(level)
    return status
  }

  private void fetchModuleStatusFromDb(Sql sql, def status) {
    log.info 'This is an upgrade'
    sql.eachRow('SELECT sr_package, sr_version FROM sr_packages') { row ->
      status[row.sr_package] = row.sr_version
    }
  }

}
