package org.silverpeas.setup.test

import groovy.sql.Sql
import org.silverpeas.setup.database.DataSourceProvider

import java.sql.SQLException

/**
 * Sets up an in-memory database for testing purpose.
 * @author mmoquillon
 */
class DatabaseSetUp {

  /**
   * Adds a new method to the Sql instances: executeScript to run a SQL script specified by its
   * path passed as argument.
   */
  static {
    Sql.metaClass.executeScript << { String scriptPath ->
      String script = new File(scriptPath).getText('UTF-8')
      script.split(';').each { statement ->
        delegate.execute(statement.trim())
      }
    }
  }

  private Sql sql

  private DatabaseSetUp() {
    sql = new Sql(DataSourceProvider.dataSource)
  }

  static DatabaseSetUp setUp() {
    def settings = [
        'DB_URL'     : 'jdbc:h2:mem:test',
        'DB_DRIVER'  : 'org.h2.Driver',
        'DB_USER'    : 'sa',
        'DB_PASSWORD': ''
    ]
    DataSourceProvider.init(settings)
    return new DatabaseSetUp()
  }

  DatabaseSetUp createSrPackagesTable() {
    String script = new File(getClass()
        .getResource('/dbRepository/h2/dbbuilder/002/create_table.sql').toURI()).text
    try {
      sql.withTransaction {
        script.split(';').each { statement ->
          sql.execute(statement.trim())
        }
      }
    } catch (SQLException ex) {
      println "Error while creating the table sr_packages: ${ex.message}"
      throw ex
    }
    return this
  }

  DatabaseSetUp prepare(Closure closure) throws Exception {
    sql.withTransaction {
      closure.call(sql)
    }
    return this
  }

  DatabaseSetUp dropAll() {
    try {
      sql.execute('DROP ALL OBJECTS')
    } catch (SQLException ex) {
      println "Error while dropping all the tables: ${ex.message}"
      throw ex
    }
    return this
  }

}
