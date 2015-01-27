/*
  Copyright (C) 2000 - 2015 Silverpeas

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

import static org.silverpeas.setup.migration.MigrationScriptBuilder.ScriptType.groovy
import static org.silverpeas.setup.migration.MigrationScriptBuilder.ScriptType.sql
import static org.silverpeas.setup.test.Assertion.numberOfItemsIn
import static org.silverpeas.setup.test.Assertion.versionOfModule

/**
 * Test the case of a database migration for a fresh installation of Silverpeas.
 * @author mmoquillon
 */
class DatasourceMigrationTest extends AbstractDatabaseTest {

  void testMigrationForAFreshInstallation() {
    assert versionOfModule('toto') == null

    MigrationScript script = MigrationScriptBuilder
        .fromScript("${testSetUp.migrationHome}/db/h2/toto/002/create_table.sql")
        .ofType(sql)
        .build()
    DatasourceMigration migration = DatasourceMigration.builder()
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
        .fromScript("${testSetUp.migrationHome}/db/h2/toto/up002/update.sql")
        .ofType(sql)
        .build()
    DatasourceMigration migration = DatasourceMigration.builder()
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
        .fromScript("${testSetUp.migrationHome}/scripts/toto/up003/update.groovy")
        .ofType(groovy)
        .build()
    DatasourceMigration migration = DatasourceMigration.builder()
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
        .fromScript("${testSetUp.migrationHome}/db/h2/toto/up003/create_table.sql")
        .ofType(sql)
        .build()
    MigrationScript groovyScript = MigrationScriptBuilder
        .fromScript("${testSetUp.migrationHome}/scripts/toto/up003/update.groovy")
        .ofType(groovy)
        .build()
    DatasourceMigration migration = DatasourceMigration.builder()
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
        .fromScript("${testSetUp.migrationHome}/db//h2/foo/002/create_table.sql")
        .ofType(sql)
        .build()
    DatasourceMigration migration = DatasourceMigration.builder()
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
        .fromScript("${testSetUp.migrationHome}/scripts/foo/up002/update.groovy")
        .ofType(groovy)
        .build()
    DatasourceMigration migration = DatasourceMigration.builder()
        .module('foo')
        .fromVersion('003')
        .toVersion('004')
        .scripts([script])
        .build()
    migration.migrate()

    assert versionOfModule('foo') == '003'
  }
}
