package org.silverpeas.setup.migration

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.silverpeas.setup.test.DatabaseSetUp
import org.silverpeas.setup.test.TestSetUp

import static org.silverpeas.setup.test.Assertion.versionOfModule

/**
 * Test the case of the migration of the data sources performed by a dedicated Gradle task.
 * @author mmoquillon
 */
class SilverpeasMigrationTaskTest extends AbstractDatabaseTest {

  private Project project

  @Override
  void initDatabase() {
    databaseSetUp = DatabaseSetUp.setUp()
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
    assert versionOfModule('toto') == null

    project.tasks.findByPath('migrate').performMigration()

    assert versionOfModule('toto') == '004'
  }

  void testSilverpeasUpgrade() {
    databaseSetUp.createSrPackagesTable()
    prepareInitialData('toto', '002')

    assert versionOfModule('toto') == '002'

    project.tasks.findByPath('migrate').performMigration()

    assert versionOfModule('toto') == '004'
  }
}
