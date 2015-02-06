package org.silverpeas.setup.configuration

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.silverpeas.setup.test.TestSetUp

/**
 * Test the case of the configuration of Silverpeas performed by a dedicated Gradle task.
 * @author mmoquillon
 */
class SilverpeasConfigurationTaskTest extends GroovyTestCase {

  private TestSetUp testSetUp
  private Project project

  @Override
  void setUp() {
    super.setUp()
    testSetUp = TestSetUp.setUp()

    System.setProperty('SILVERPEAS_HOME', testSetUp.resourcesDir)
    System.setProperty('JBOSS_HOME', testSetUp.resourcesDir)

    project = ProjectBuilder.builder().build()
    project.apply plugin: 'silversetup'

    project.silversetup.logging.logDir = "${project.buildDir}/log"
    project.silversetup.logging.useLogger = false
  }

  void testSilverpeasConfiguration() {
    thePropertiesFileAreCorrectlyConfigured()
    theWorkflowEngineIsCorrectlyConfiguredByGroovyScript()
  }

  void thePropertiesFileAreCorrectlyConfigured() {
    Properties autDomainSQLBefore = loadPropertiesFrom('authentication/autDomainSQL.properties')
    Properties systemSettingsBefore = loadPropertiesFrom('systemSettings.properties')
    Properties schedulerSettingsBefore = loadPropertiesFrom('workflow/engine/schedulerSettings.properties')

    project.tasks.findByPath('configureSilverpeas').configureSilverpeas()

    Properties autDomainSQLAfter = loadPropertiesFrom('authentication/autDomainSQL.properties')
    Properties systemSettingsAfter = loadPropertiesFrom('systemSettings.properties')
    Properties schedulerSettingsAfter = loadPropertiesFrom('workflow/engine/schedulerSettings.properties')

    assert autDomainSQLAfter['fallbackType'] == 'always' &&
        autDomainSQLAfter['fallbackType'] == autDomainSQLBefore['fallbackType']
    assert autDomainSQLAfter['autServer0.SQLJDBCUrl'] == 'jdbc:h2:mem:test' &&
        autDomainSQLAfter['autServer0.SQLJDBCUrl'] != autDomainSQLBefore['autServer0.SQLJDBCUrl']
    assert autDomainSQLAfter['autServer0.SQLAccessLogin'] == 'sa' &&
        autDomainSQLAfter['autServer0.SQLAccessLogin'] != autDomainSQLBefore['autServer0.SQLAccessLogin']
    assert autDomainSQLAfter['autServer0.SQLAccessPasswd'] == '' &&
        autDomainSQLAfter['autServer0.SQLAccessPasswd'] != autDomainSQLBefore['autServer0.SQLAccessPasswd']
    assert autDomainSQLAfter['autServer0.SQLDriverClass'] == 'org.h2.Driver' &&
        autDomainSQLAfter['autServer0.SQLDriverClass'] != autDomainSQLBefore['autServer0.SQLDriverClass']

    assert systemSettingsAfter['http.proxyHost'] == 'tartempion.net' &&
        systemSettingsAfter['http.proxyHost'] != systemSettingsBefore['http.proxyHost']
    assert systemSettingsAfter['http.proxyPort'] == '1234' &&
        systemSettingsAfter['http.proxyPort'] != systemSettingsBefore['http.proxyPort']
    assert systemSettingsAfter['http.nonProxyHosts'] == '127.0.0.1|localhost' &&
        systemSettingsAfter['http.nonProxyHosts'] != systemSettingsBefore['http.nonProxyHosts']

    assert schedulerSettingsAfter['timeoutSchedule'] == '* 0,4,8,12,16,20 * * *' &&
        systemSettingsAfter['timeoutSchedule'] != schedulerSettingsBefore['timeoutSchedule']
  }

  void theWorkflowEngineIsCorrectlyConfiguredByGroovyScript() {

    project.tasks.findByPath('configureSilverpeas').configureSilverpeas()

    def databaseConf = new XmlSlurper(false, false)
        .parse(getClass().getResourceAsStream('/resources/instanceManager/database.xml'))
    databaseConf.@engine == 'h2'
    databaseConf.database.@engine == 'h2'
    databaseConf.database.mapping.@href.text() ==
        "file:///${System.getProperty('SILVERPEAS_HOME')}/resources/instanceManager/mapping.xml"

    def fastDatabaseConf = new XmlSlurper(false, false)
        .parse(getClass().getResourceAsStream('/resources/instanceManager/fast_database.xml'))
    fastDatabaseConf.@engine == 'h2'
    fastDatabaseConf.database.@engine == 'h2'
    fastDatabaseConf.database.mapping.@href.text() ==
        "file:///${System.getProperty('SILVERPEAS_HOME')}/resources/instanceManager/fast_mapping.xml"
  }

  private Properties loadPropertiesFrom(String relativePath) {
    Properties properties = new Properties()
    properties.load(getClass().getResourceAsStream("/properties/org/silverpeas/${relativePath}"))
    return properties
  }

}
