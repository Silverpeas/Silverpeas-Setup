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

import java.io.File;
import java.io.FileFilter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.silverpeas.dbbuilder.Console;
import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;
import org.silverpeas.dbbuilder.util.Configuration;

/**
 * DB migration to change the type size (a varchar) of the password field upto 123 characters in the
 * for the table of users in the customer's domains.
 * @author mmoquillon
 */
public class PasswordSizeIncrease extends DbBuilderDynamicPart {

  // request to fetch all the SQL domains of the customer
  static final String DOMAINS_SQL =
      "SELECT * FROM ST_DOMAIN WHERE className='com.stratelia.silverpeas.domains.sqldriver.SQLDriver'";
  // the domain property that gives the user table in the domain database
  static final String TABLE_OF_USERS = "database.SQLUserTableName";
  // the domain property that gives the password field in the user table
  static final String PASSWORD_FIELD = "database.SQLUserPasswordColumnName";
  // the domain property that gives the JDBC driver to use for connecting to the database
  static final String SQL_DRIVER = "database.SQLClassName";
  // the domain property that gives the URL of the database
  static final String DATABASE_URL = "database.SQLJDBCUrl";
  // the domain property that gives the login to open a connection with the database
  static final String DATABASE_LOGIN = "database.SQLAccessLogin";
  // the domain property that gives the password associated with the login
  static final String DATABASE_PASSWORD = "database.SQLAccessPasswd";
  // the H2 database defined by its JDBC driver
  static final String H2_DATABASE = "org.h2.Driver";
  // the PostgreSQL database defined by its JDBC driver
  static final String POSTGRESQL_DATABASE = "org.postgresql.Driver";
  // the MS-SQLServer database defined by the its open-source JDBC driver
  static final String MSSQL_DATABASE = "net.sourceforge.jtds.jdbc.Driver";
  // the Oracle database defined by its proprietary JDBC driver
  static final String ORACLE_DATABASE = "oracle.jdbc.driver.OracleDriver";
  // the H2 SQL instruction to change the type of the password field in the user table
  static final String H2_SQL = "ALTER TABLE {0} ALTER COLUMN {1} varchar(123)";
  // the PostgreSQL SQL instruction to change the type of the password field in the user table
  static final String POSTGRESQL_SQL = "ALTER TABLE {0} ALTER COLUMN {1} TYPE varchar(123)";
  // the MS-SQLServer SQL instruction to change the type of the password field in the user table
  static final String MSSQL_SQL = "ALTER TABLE {0} ALTER COLUMN {1} varchar(123)";
  // the Oracle SQL instruction to change the type of the password field in the user table
  static final String ORACLE_SQL = "ALTER TABLE {0} MODIFY ({1} varchar(123))";
  private Connection sharedConnection;
  private Console console;

  public void migrate() throws Exception {
    getConsole().printMessageln("Migration of the user password length to 123 characters");
    List<String> sqlDomains = getAllSQLCustomerDomains();
    for (String resourcePath : sqlDomains) {
      String name = resourcePath.replace('.', File.separatorChar) + ".properties";
      Properties resource = Configuration.loadResource(name);
      if (resource.isEmpty()) {
        // the resource isn't located at com/stratelia/silverpeas/domains but at the new
        // properties location org/silverpeas/domains
        name = name.replace("com" + File.separatorChar + "stratelia" + File.separatorChar,
            "org" + File.separatorChar);
        resource = Configuration.loadResource(name);
      }
      if (!resource.isEmpty()) {
        updatePasswordTypeInDBs(name, resource);
      }
    }
  }

  private List<String> getAllSQLCustomerDomains() throws Exception {
    List<String> sqlDomains = new ArrayList<String>();
    Connection connection = getConnection();
    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = connection.createStatement();
      rs = statement.executeQuery(DOMAINS_SQL);
      while (rs.next()) {
        sqlDomains.add(rs.getString("propFileName"));
      }
    } finally {
      closeResultSet(rs);
      closeStatement(statement);
    }
    return sqlDomains;
  }

  private void updatePasswordTypeInDBs(String domainName, Properties domainProps) {
    Console out = getConsole();
    Connection connection = null;
    Statement statement = null;
    try {
      if (isSQLDomain(domainProps)) {
        connection = openConnection(domainProps);
        statement = connection.createStatement();
        statement.execute(sqlInstructionFor(domainProps));
      }
    } catch (Exception ex) {
      out.printError("Error while processing the domain in " + domainName, ex);
    } finally {
      closeStatement(statement);
      closeConnection(connection);
    }
  }

  private static boolean isSQLDomain(Properties domain) {
    return domain.containsKey(SQL_DRIVER);
  }

  private static String sqlInstructionFor(Properties domain) {
    String sql = null;
    String database = domain.getProperty(SQL_DRIVER);
    String table = domain.getProperty(TABLE_OF_USERS);
    String password = domain.getProperty(PASSWORD_FIELD);
    if (POSTGRESQL_DATABASE.equals(database)) {
      sql = MessageFormat.format(POSTGRESQL_SQL, table, password);
    } else if (MSSQL_DATABASE.equals(database)) {
      sql = MessageFormat.format(MSSQL_SQL, table, password);
    } else if (ORACLE_DATABASE.equals(database)) {
      MessageFormat.format(ORACLE_SQL, table, password);
    } else if (H2_DATABASE.equals(database)) {
      sql = MessageFormat.format(H2_SQL, table, password);
    } else {
      throw new IllegalArgumentException("The database '" + database + "' isn't supported by"
          + "Silverpeas");
    }
    return sql;
  }

  private Connection openConnection(Properties domain) throws Exception {
    Connection connection = getSharedConnection();
    if (connection == null) {
      Class.forName(domain.getProperty(SQL_DRIVER)).newInstance();
      String password = domain.getProperty(DATABASE_PASSWORD);
      if (password == null) {
        password = "";
      }
      connection = DriverManager.getConnection(domain.getProperty(DATABASE_URL),
          domain.getProperty(DATABASE_LOGIN),
          password);
    }
    return connection;
  }

  private void closeConnection(Connection connection) {
    if (connection != null && connection != getSharedConnection()) {
      try {
        connection.close();
      } catch (SQLException e) {
      }
    }
  }

  private static void closeStatement(Statement statement) {
    if (statement != null) {
      try {
        statement.close();
      } catch (SQLException e) {
      }
    }
  }

  private static void closeResultSet(ResultSet resultSet) {
    if (resultSet != null) {
      try {
        resultSet.close();
      } catch (SQLException e) {
      }
    }
  }

  private Connection getSharedConnection() {
    return sharedConnection;
  }

  /**
   * Sets a connection to be shared by all of the domains to be updated. This is for SQL domains
   * defined within the same data source.
   * @param connection a shared connection to SQL data source.
   */
  protected void setSharedConnection(Connection connection) {
    sharedConnection = connection;
  }

  @Override
  public Console getConsole() {
    if (console == null) {
      Console theConsole = super.getConsole();
      if (theConsole == null) {
        console = new Console();
      } else {
        console = theConsole;
      }
    }
    return console;
  }
}
