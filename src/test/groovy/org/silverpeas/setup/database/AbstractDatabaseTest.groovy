package org.silverpeas.setup.database

import groovy.sql.Sql
import org.silverpeas.setup.test.DatabaseSetUp
import org.silverpeas.setup.test.TestSetUp

/**
 * The common class for all test cases about a database migration.
 * @author mmoquillon
 */
abstract class AbstractDatabaseTest extends GroovyTestCase {

  private static
    final String VERSION_QUERY = 'SELECT sr_version from sr_packages WHERE sr_package = :module'

  protected DatabaseSetUp databaseSetUp
  protected TestSetUp testSetUp

  @Override
  void setUp() {
    super.setUp()
    testSetUp = TestSetUp.setUp()
    databaseSetUp = DatabaseSetUp.setUp().createSrPackagesTable()
  }

  @Override
  void tearDown() {
    super.tearDown()
    databaseSetUp.dropAll()
  }

  def versionOfModule(String module) {
    Sql sql = new Sql(DataSourceProvider.dataSource)
    return sql.firstRow(VERSION_QUERY, [module: module])?.get('sr_version')
  }

  def numberOfItemsIn(String table) {
    Sql sql = new Sql(DataSourceProvider.dataSource)
    return sql.firstRow('SELECT count(id) AS count FROM ' + table)?.get('count')
  }

  def prepareInitialData(String module, String version) {
    databaseSetUp.prepare { Sql sql ->
      sql.executeScript("${testSetUp.databaseMigrationHome}/h2/${module}/${version}/create_table.sql")
      sql.executeUpdate("INSERT INTO sr_packages (sr_package, sr_version) VALUES (:module, :version)",
          [module: module, version: version])
    }
  }

}
