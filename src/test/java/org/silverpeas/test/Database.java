package org.silverpeas.test;

import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.junit.rules.ExternalResource;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author mmoquillon
 */
public class Database extends ExternalResource {

  private final IDataSet dataSet;
  private DataSource dataSource;
  private DataSourceDatabaseTester databaseTester;

  /**
   * Creates a new database environment for testing purpose with the specified data source and
   * initialized from the specified data set.
   * @param dataSource the data source with which connections are established.
   * @param dataSet the data set to use to populate the database.
   */
  public Database(final DataSource dataSource, IDataSet dataSet) {
    if (dataSet != null && dataSource != null) {
      this.dataSource = dataSource;
      this.dataSet = dataSet;
    } else {
      throw new NullPointerException("Either the data source or the data set is null");
    }
  }

  @Override
  protected void before() throws Throwable {
    databaseTester = new DataSourceDatabaseTester(dataSource);
    databaseTester.setDataSet(dataSet);
    databaseTester.onSetup();
  }

  @Override
  protected void after() {
    try {
      databaseTester.onTearDown();
    } catch (Exception e) {
      Logger.getLogger(getClass().getSimpleName()).log(Level.SEVERE, e.getMessage(), e);
    }
  }

  /**
   * Gets a connection to the initial data set.
   * @return a SQL connection to the data set.
   * @throws Exception if an error occurs while getting the connection to the data set.
   */
  public Connection getConnection() throws Exception {
    return databaseTester.getConnection().getConnection();
  }

  /**
   * Opens a new connection to the underlying data source.
   * @return a SQL connection to the data source.
   * @throws Exception if an error occurs while opening a new SQL connection.
   */
  public Connection openConnection() throws Exception {
    return DataSourceUtils.getConnection(dataSource);
  }

  /**
   * Closes an previously opened connection to the underlying data source.
   * @param connection the SQL connection to close.
   */
  public void closeConnection(final Connection connection) {
    DataSourceUtils.releaseConnection(connection, dataSource);
  }
}
