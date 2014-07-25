/*
 * Copyright (C) 2000-2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Writer Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.migration.sqldomain;

import org.apache.commons.io.IOUtils;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.silverpeas.passwordencryption.Sha512Crypt;
import org.silverpeas.test.Database;
import org.silverpeas.test.SpringContext;
import org.silverpeas.test.SystemInitializationForTests;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.Assert.fail;

/**
 * Unit tests on the migration of the password column in the user tables.
 *
 * @author mmoquillon
 */
public class PasswordSizeIncreaseTest {

  @Rule
  public SpringContext springContext = new SpringContext(this, "/spring-domain-datasource.xml");
  @Rule
  public Database database = new Database(springContext.getBean(DataSource.class), getDataSet());

  public PasswordSizeIncreaseTest() {
  }

  @BeforeClass
  public static void setUp() throws IOException {
    SystemInitializationForTests.initialize();
  }

  /**
   * Test of migrate method, of class PasswordSizeIncrease.
   */
  @Test
  public void testMigrate() throws Exception {
    PasswordSizeIncrease migration = new PasswordSizeIncrease();
    migration.setConnection(database.getConnection());
    migration.setSharedConnection(database.getConnection());
    migration.migrate();

    assertPasswordTypeFor("Toto");
    assertPasswordTypeFor("Titi");

  }

  protected static IDataSet getDataSet() {
    InputStream in = PasswordSizeIncreaseTest.class.getResourceAsStream("domain-dataset.xml");
    try {
      return new FlatXmlDataSetBuilder().build(in);
    } catch (Exception ex) {
      return null;
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /*
   * Cannot get the column data type from the table metadata :-(
   * So check the size of the varchar by setting a password with the maximum size.
   */
  protected void assertPasswordTypeFor(String domain) throws Exception {
    String tableName = "Domain" + domain + "_User";
    Statement statement = null;
    Connection connection = null;
    try {
      connection = database.openConnection();
      statement = connection.createStatement();
      statement.executeUpdate("update " + tableName + " set password='" + longPassword()
          + "' where id=1");
    } catch (Exception ex) {
      fail(ex.getMessage());
    } finally {
      if (statement != null) {
        statement.close();
      }
      if (connection != null) {
        database.closeConnection(connection);
      }
    }
  }

  /*
   * The password should be now a varchar(123). Computes a password with a such size.
   */
  protected String longPassword() {
    Sha512Crypt crypt = new Sha512Crypt();
    String password = crypt.encrypt("password");
    StringBuilder passwordBuilder = new StringBuilder(password);
    for (int i = password.length(); i < 123; i++) {
      passwordBuilder.append("A");
    }
    return passwordBuilder.toString();
  }
}
