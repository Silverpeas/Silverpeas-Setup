/*
  Copyright (C) 2000 - 2017 Silverpeas

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
package org.silverpeas.setup.test

import groovy.sql.Sql
import org.silverpeas.setup.api.DataSourceProvider
import org.silverpeas.setup.api.ManagedBeanContainer

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

  private Sql requester

  private DatabaseSetUp() {
  }

  /**
   * Sets up the database context to use in the tests.
   * @param args a map of key-values to configure the setting up:
   * <ul>
   *   <li><code>withDatasource</code>: a boolean indicating if the datasource must be configured
   *   by the DatasourceProvider class for the tests. By default false whether omitted.</li>
   * </ul>
   * @return
   */
  static DatabaseSetUp setUp(args) {
    DataSourceProvider dataSourceProvider
    if (args?.withDatasource) {
      def settings = [
          'DB_URL'     : 'jdbc:h2:mem:test',
          'DB_DRIVER'  : 'org.h2.Driver',
          'DB_USER'    : 'sa',
          'DB_PASSWORD': ''
      ]
      dataSourceProvider = new DataSourceProvider(settings)
      ManagedBeanContainer.registry().register(dataSourceProvider)
    }
    return new DatabaseSetUp()
  }

  /**
   * Creates the database structure used by the migration tool for doing its management of
   * installed migration modules.
   * @return itself.
   */
  DatabaseSetUp createSrPackagesTable() {
    String script = new File(getClass()
        .getResource('/migrations/db/h2/dbbuilder/002/create_table.sql').toURI()).text
    try {
      Sql sql = getSql()
      sql.withTransaction {
        script.split(';').each { statement ->
          sql.execute(statement.trim())
        }
        sql.executeUpdate('INSERT INTO sr_packages(sr_package, sr_version) VALUES (:module, :version)',
            [module: 'dbbuilder', version: '002'])
      }
    } catch (SQLException ex) {
      println "Error while creating the table sr_packages: ${ex.message}"
      throw ex
    }
    return this
  }

  /**
   * Runs the specified closure by passing it a Sql instance in order to perform additional
   * SQL operations.
   * @param closure the closure to run for preparing the database for some tests.
   * @return itself.
   * @throws Exception if an error occurs during the closure execution.
   */
  DatabaseSetUp prepare(Closure closure) throws Exception {
    Sql sql = getSql()
    sql.withTransaction {
      closure.call(sql)
    }
    return this
  }

  /**
   * Drops the complete schema of the database used in the tests.
   * @return itself.
   */
  DatabaseSetUp dropAll() {
    try {
      Sql sql = getSql()
      sql.execute('DROP ALL OBJECTS')
    } catch (SQLException ex) {
      println "Error while dropping all the tables: ${ex.message}"
      throw ex
    }
    return this
  }

  private Sql getSql() {
    if (requester == null) {
      DataSourceProvider dataSourceProvider = ManagedBeanContainer.get(DataSourceProvider.class)
      requester = new Sql(dataSourceProvider.dataSource)
    }
    return requester
  }

}
