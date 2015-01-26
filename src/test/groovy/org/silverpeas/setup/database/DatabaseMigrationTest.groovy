package org.silverpeas.setup.database

import static org.silverpeas.setup.database.MigrationScriptBuilder.ScriptType.groovy
import static org.silverpeas.setup.database.MigrationScriptBuilder.ScriptType.sql

/**
 * Test the case of a database migration for a fresh installation of Silverpeas.
 * @author mmoquillon
 */
class DatabaseMigrationTest extends AbstractDatabaseTest {

  void testMigrationForAFreshInstallation() {
    assert versionOfModule('toto') == null

    MigrationScript script = MigrationScriptBuilder
        .fromScript("${testSetUp.databaseMigrationHome}/h2/toto/002/create_table.sql")
        .ofType(sql)
        .build()
    DatabaseMigration migration = DatabaseMigration.builder()
        .module('toto')
        .toVersion('002')
        .scripts([script])
        .build()
    migration.migrate()

    assert versionOfModule('toto') == '002'
  }

  void testUpgradeWithOnlySQLScripts() {
    prepareInitialData('toto', '002')
    assert versionOfModule('toto') == '002'

    MigrationScript script = MigrationScriptBuilder
        .fromScript("${testSetUp.databaseMigrationHome}/h2/toto/up002/update.sql")
        .ofType(sql)
        .build()
    DatabaseMigration migration = DatabaseMigration.builder()
        .module('toto')
        .fromVersion('002')
        .toVersion('003')
        .scripts([script])
        .build()
    migration.migrate()

    assert versionOfModule('toto') == '003'
  }

  void testUpgradeWithOnlyGroovyScripts() {
    prepareInitialData('toto', '003')
    assert versionOfModule('toto') == '003'
    assert numberOfItemsIn('Person') == 0

    MigrationScript script = MigrationScriptBuilder
        .fromScript("${testSetUp.databaseMigrationHome}/h2/toto/up003/update.groovy")
        .ofType(groovy)
        .build()
    DatabaseMigration migration = DatabaseMigration.builder()
        .module('toto')
        .fromVersion('003')
        .toVersion('004')
        .scripts([script])
        .build()
    migration.migrate()

    assert versionOfModule('toto') == '004'
    assert numberOfItemsIn('Person') == 1
  }

  void testMigrationForAnUpgrade() {
    prepareInitialData('toto', '003')
    assert versionOfModule('toto') == '003'
    assert numberOfItemsIn('Person') == 0

    MigrationScript sqlScript = MigrationScriptBuilder
        .fromScript("${testSetUp.databaseMigrationHome}/h2/toto/up003/create_table.sql")
        .ofType(sql)
        .build()
    MigrationScript groovyScript = MigrationScriptBuilder
        .fromScript("${testSetUp.databaseMigrationHome}/h2/toto/up003/update.groovy")
        .ofType(groovy)
        .build()
    DatabaseMigration migration = DatabaseMigration.builder()
        .module('toto')
        .fromVersion('003')
        .toVersion('004')
        .scripts([sqlScript, groovyScript])
        .build()
    migration.migrate()

    assert versionOfModule('toto') == '004'
    assert numberOfItemsIn('Person') == 1
  }

  void testAnInstallationFailure() {
    assert versionOfModule('foo') == null

    MigrationScript script = MigrationScriptBuilder
        .fromScript("${testSetUp.databaseMigrationHome}/h2/foo/002/create_table.sql")
        .ofType(sql)
        .build()
    DatabaseMigration migration = DatabaseMigration.builder()
        .module('foo')
        .toVersion('002')
        .scripts([script])
        .build()
    migration.migrate()

    assert versionOfModule('foo') == null
  }

  void testAnUpgradeFailure() {
    prepareInitialData('foo', '003')
    assert versionOfModule('foo') == '003'

    MigrationScript script = MigrationScriptBuilder
        .fromScript("${testSetUp.databaseMigrationHome}/h2/foo/up002/update.groovy")
        .ofType(groovy)
        .build()
    DatabaseMigration migration = DatabaseMigration.builder()
        .module('foo')
        .fromVersion('003')
        .toVersion('004')
        .scripts([script])
        .build()
    migration.migrate()

    assert versionOfModule('foo') == '003'
  }
}
