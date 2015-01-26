package org.silverpeas.setup.test

/**
 * Set up the test environment.
 * @author mmoquillon
 */
class TestSetUp {

  String resourcesDir
  String databaseMigrationHome

  private TestSetUp() {

  }

  static TestSetUp setUp() {
    Properties properties = new Properties()
    properties.load(getClass().getResourceAsStream('/test.properties'))
    TestSetUp testSetUp = new TestSetUp()
    testSetUp.databaseMigrationHome = properties.databaseHome
    testSetUp.resourcesDir = properties.resourcesDir
    return testSetUp
  }
}
