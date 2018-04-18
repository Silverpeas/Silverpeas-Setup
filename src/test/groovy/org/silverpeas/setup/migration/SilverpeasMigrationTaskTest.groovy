package org.silverpeas.setup.migration

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.silverpeas.setup.test.DatabaseSetUp

import static org.silverpeas.setup.test.Assertion.versionOfModule

/**
 * Test the case of the migration of the data sources performed by a dedicated Gradle task.
 * @author mmoquillon
 */
class SilverpeasMigrationTaskTest extends AbstractDatabaseTest {

  private Project project

  @Override
  DatabaseSetUp initDatabaseSetUp() {
    return DatabaseSetUp.setUp(withDatasource: true)
  }

  @Override
  void setUp() {
    super.setUp()
    System.setProperty('SILVERPEAS_HOME', testSetUp.resourcesDir)
    System.setProperty('JBOSS_HOME', testSetUp.resourcesDir)

    project = ProjectBuilder.builder().build()
    project.apply plugin: 'silversetup'
  }

  void testSilverpeasInstallation() {
    assert versionOfModule(databaseSetUp.sql, 'toto') == null
    assert versionOfModule(databaseSetUp.sql, 'busCore') == null

    project.tasks.findByPath('migration').performMigration()

    assert versionOfModule(databaseSetUp.sql, 'toto') == '004'
    assert versionOfModule(databaseSetUp.sql, 'busCore') == '032'
  }

  void testSilverpeasUpgrade() {
    databaseSetUp.createSrPackagesTable()
    prepareInitialData('toto', '002')

    assert versionOfModule(databaseSetUp.sql, 'toto') == '002'
    assert versionOfModule(databaseSetUp.sql, 'busCore') == null

    project.tasks.findByPath('migration').performMigration()

    assert versionOfModule(databaseSetUp.sql, 'toto') == '004'
    assert versionOfModule(databaseSetUp.sql, 'busCore') == '032'
  }
}
