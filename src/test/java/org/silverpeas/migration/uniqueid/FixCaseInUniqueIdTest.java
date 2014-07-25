/*
 * Copyright (C) 2000 - 2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection withWriter Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.migration.uniqueid;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.silverpeas.test.Database;
import org.silverpeas.test.SpringContext;
import org.silverpeas.test.SystemInitializationForTests;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;

import static org.dbunit.Assertion.assertEquals;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests on the migration of the sb_contentmanager_content table.
 */
public class FixCaseInUniqueIdTest {

  public static final String UNIQUEID_TABLE = "uniqueId";

  @Rule
  public SpringContext context = new SpringContext(this, "/spring-uniqueid-datasource.xml");
  @Rule
  public Database database = new Database(context.getBean(DataSource.class), getDataSet());

  public FixCaseInUniqueIdTest() {
  }

  @BeforeClass
  public static void setUp() throws IOException {
    SystemInitializationForTests.initialize();
  }

  /**
   * Test the migration of the sb_contentmanager_content table.
   */
  @Test
  public void testMigration() throws Exception {
    FixCaseInUniqueId fixCaseInUniqueId = new FixCaseInUniqueId();
    fixCaseInUniqueId.setConnection(database.getConnection());
    fixCaseInUniqueId.migrate();
    ITable actualContentTable = getActualTable(UNIQUEID_TABLE);
    ITable expectedContentTable = getExpectedTable(UNIQUEID_TABLE);
    assertThat(actualContentTable.getRowCount(), is(4));
    assertEquals(expectedContentTable, actualContentTable);
  }

  protected static IDataSet getDataSet() {
    try {
      return new FlatXmlDataSetBuilder()
          .build(FixCaseInUniqueIdTest.class.getResourceAsStream("uniqueId-dataset.xml"));
    } catch (DataSetException e) {
      return null;
    }
  }

  protected IDataSet getExpectedDataSet() throws Exception {
    return new FlatXmlDataSetBuilder().build(getClass().getResourceAsStream(
        "expected-uniqueId-dataset.xml"));
  }

  protected ITable getActualTable(String tableName) throws Exception {
    Connection connection = database.openConnection();
    IDatabaseConnection databaseConnection = new DatabaseConnection(connection);
    IDataSet dataSet = databaseConnection.createDataSet();
    ITable table = dataSet.getTable(tableName);
    database.closeConnection(connection);
    return table;
  }

  protected ITable getExpectedTable(String tableName) throws Exception {
    IDataSet dataSet = getExpectedDataSet();
    return dataSet.getTable(tableName);
  }
}
