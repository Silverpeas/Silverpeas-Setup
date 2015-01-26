package org.silverpeas.setup.database

import static org.silverpeas.setup.database.DatabaseType.h2

/**
 * Test the case of a migration of a Silverpeas component from its migration descriptor.
 * @author mmoquillon
 */
class MigrationModuleTest extends AbstractDatabaseTest{

  void testAFreshInstallation() {
    assert versionOfModule('toto') == null

    MigrationModule module = MigrationModule.builder()
        .descriptor(new File("${testSetUp.databaseMigrationHome}/data/h2/toto-migration.xml"))
        .settings([DATABASE_HOME: testSetUp.databaseMigrationHome, DB_SERVERTYPE: 'H2'])
        .status([:])
        .build()
    module.migrate()

    assert versionOfModule('toto') == '004'
  }

  void testAnUpgradeFrom002To004() {
    prepareInitialData('toto', '002')
    assert versionOfModule('toto') == '002'
    assert numberOfItemsIn('Person') == 0

    MigrationModule module = MigrationModule.builder()
        .descriptor(new File("${testSetUp.databaseMigrationHome}/data/h2/toto-migration.xml"))
        .settings([DATABASE_HOME: testSetUp.databaseMigrationHome, DB_SERVERTYPE: 'H2'])
        .status(['toto':'002'])
        .build()
    module.migrate()

    assert versionOfModule('toto') == '004'
    assert numberOfItemsIn('Person') == 1
  }

  void testAnUpgradeFrom003To004() {
    prepareInitialData('toto', '003')
    assert versionOfModule('toto') == '003'
    assert numberOfItemsIn('Person') == 0

    MigrationModule module = MigrationModule.builder()
        .descriptor(new File("${testSetUp.databaseMigrationHome}/data/h2/toto-migration.xml"))
        .settings([DATABASE_HOME:testSetUp.databaseMigrationHome, DB_SERVERTYPE: 'H2'])
        .status(['toto':'003'])
        .build()
    module.migrate()

    assert versionOfModule('toto') == '004'
    assert numberOfItemsIn('Person') == 1
  }

}
