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

import org.silverpeas.setup.api.FileLogger
import org.xml.sax.SAXParseException

import static org.silverpeas.setup.test.Assertion.numberOfItems
import static org.silverpeas.setup.test.Assertion.versionOfModule

/**
 * Test the case of a migration of a Silverpeas component from its migration descriptor.
 * @author mmoquillon
 */
class MigrationModuleTest extends AbstractDatabaseTest{

  void testAFreshInstallation() {
    assert versionOfModule(databaseSetUp.sql, 'toto') == null

    MigrationModule module = new MigrationModule()
        .withSettings([MIGRATION_HOME: context.migrationHome, DB_SERVERTYPE: 'H2'])
        .withStatus([:])
        .withLogger(FileLogger.getLogger(getClass().getSimpleName()))
        .loadMigrationsFrom(new File("${context.migrationHome}/modules/toto-migration.xml"))
    module.migrate()

    assert versionOfModule(databaseSetUp.sql, 'toto') == '004'
  }

  void testAnUpgradeFrom002To004() {
    prepareInitialData('toto', '002')
    assert versionOfModule(databaseSetUp.sql, 'toto') == '002'
    assert numberOfItems(databaseSetUp.sql, 'Person') == 0

    MigrationModule module = new MigrationModule()
        .withSettings([MIGRATION_HOME: context.migrationHome, DB_SERVERTYPE: 'H2'])
        .withStatus(['toto':'002'])
        .withLogger(FileLogger.getLogger(getClass().getSimpleName()))
        .loadMigrationsFrom(new File("${context.migrationHome}/modules/toto-migration.xml"))
    module.migrate()

    assert versionOfModule(databaseSetUp.sql, 'toto') == '004'
    assert numberOfItems(databaseSetUp.sql, 'Person') == 1
  }

  void testAnUpgradeFrom003To004() {
    prepareInitialData('toto', '003')
    assert versionOfModule(databaseSetUp.sql, 'toto') == '003'
    assert numberOfItems(databaseSetUp.sql, 'Person') == 0

    MigrationModule module = new MigrationModule()
        .withSettings([MIGRATION_HOME:context.migrationHome, DB_SERVERTYPE: 'H2'])
        .withStatus(['toto':'003'])
        .withLogger(FileLogger.getLogger(getClass().getSimpleName()))
        .loadMigrationsFrom(new File("${context.migrationHome}/modules/toto-migration.xml"))
    module.migrate()

    assert versionOfModule(databaseSetUp.sql, 'toto') == '004'
    assert numberOfItems(databaseSetUp.sql, 'Person') == 1
  }

  void testAMalformedMigrationDescriptor() {
    shouldFail(SAXParseException) {
      new MigrationModule()
          .withSettings([MIGRATION_HOME:context.migrationHome, DB_SERVERTYPE: 'H2'])
          .withStatus(['toto':'003'])
          .withLogger(FileLogger.getLogger(getClass().getSimpleName()))
          .loadMigrationsFrom(new File("${context.migrationHome}/malformed-migration.xml"))
    }
  }

}
