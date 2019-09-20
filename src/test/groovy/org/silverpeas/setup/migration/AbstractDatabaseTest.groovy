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
import org.silverpeas.setup.api.ManagedBeanContainer
import org.silverpeas.setup.api.SilverpeasSetupService
import org.silverpeas.setup.test.DatabaseSetUp
import org.silverpeas.setup.test.TestContext
/**
 * The common class for all test cases about a database migration.
 * @author mmoquillon
 */
abstract class AbstractDatabaseTest extends GroovyTestCase {

  protected DatabaseSetUp databaseSetUp
  protected TestContext context

  DatabaseSetUp initDatabaseSetUp() {
    return DatabaseSetUp.setUp(withDatasource: true).createSrPackagesTable()
  }

  @Override
  void setUp() {
    super.setUp()
    context = TestContext.create()
    databaseSetUp = initDatabaseSetUp()
    ManagedBeanContainer.registry().register(new SilverpeasSetupService([:]))
  }

  @Override
  void tearDown() {
    super.tearDown()
    databaseSetUp.dropAll()
  }

  def prepareInitialData(String module, String version) {
    databaseSetUp.prepare { Sql sql ->
      sql.executeScript("${context.migrationHome}/db/h2/${module}/${version}/create_table.sql")
      sql.executeUpdate("INSERT INTO sr_packages (sr_package, sr_version) VALUES (:module, :version)",
          [module: module, version: version])
    }
  }

}
