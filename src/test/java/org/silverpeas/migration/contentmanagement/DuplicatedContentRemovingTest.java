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
package org.silverpeas.migration.contentmanagement;

import org.apache.commons.io.IOUtils;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
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
import java.io.InputStream;
import java.sql.Connection;

import static org.dbunit.Assertion.assertEquals;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests on the migration of the sb_contentmanager_content table.
 */
public class DuplicatedContentRemovingTest {

  public static final String CONTENT_TABLE = "sb_contentmanager_content";
  public static final String CONTENT_CLASSIFICATION_TABLE = "sb_classifyengine_classify";
  public static final int CONTENT_COUNT = 12;
  public static final int DUPLICATE_CONTENT_COUNT = 5;
  public static final int DUPLICATE_CONTENT_CLASSIFICATION_COUNT = 1;
  public static final int CONTENT_CLASSIFICATION_COUNT = 4;

  @Rule
  public SpringContext context = new SpringContext(this, "/spring-content-datasource.xml");
  @Rule
  public Database database = new Database(context.getBean(DataSource.class), getDataSet());

  @BeforeClass
  public static void setUp() throws IOException {
    SystemInitializationForTests.initialize();
  }

  public DuplicatedContentRemovingTest() {
  }


  /**
   * Test the migration of the sb_contentmanager_content table.
   */
  @Test
  public void testMigration() throws Exception {
    DuplicateContentRemoving duplicateContentRemoving = new DuplicateContentRemoving();
    duplicateContentRemoving.setConnection(database.getConnection());

    duplicateContentRemoving.migrate();

    ITable actualContentTable = getActualTable(CONTENT_TABLE);
    ITable expectedContentTable = getExpectedTable(CONTENT_TABLE);
    assertThat(actualContentTable.getRowCount(), is(CONTENT_COUNT - DUPLICATE_CONTENT_COUNT));
    assertEquals(expectedContentTable, actualContentTable);

    ITable actualClassificationTable = getActualTable(CONTENT_CLASSIFICATION_TABLE);
    ITable expectedClassificationTable = getExpectedTable(CONTENT_CLASSIFICATION_TABLE);
    assertThat(actualClassificationTable.getRowCount(), is(CONTENT_CLASSIFICATION_COUNT
        - DUPLICATE_CONTENT_CLASSIFICATION_COUNT));
    assertEquals(expectedClassificationTable, actualClassificationTable);
  }

  protected IDataSet getDataSet() {
    InputStream in = getClass().getResourceAsStream("pdc-dataset.xml");
    try {
      return new FlatXmlDataSetBuilder().build(in);
    } catch (Exception e) {
      return null;
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  protected IDataSet getExpectedDataSet() throws Exception {
    InputStream in = getClass().getResourceAsStream("expected-pdc-dataset.xml");
    try {
      return new FlatXmlDataSetBuilder().build(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
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
