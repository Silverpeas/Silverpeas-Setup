package org.silverpeas.setup.configuration

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.silverpeas.setup.test.TestContext

import java.time.LocalDate

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Test the case of the configuration of Silverpeas performed by a dedicated Gradle task.
 * @author mmoquillon
 */
class SilverpeasConfigurationTaskTest {

  private TestContext context

  @Before
  void setUp() {
    context = TestContext.create().setUpSystemEnv().initGradleProject(['construct'])
  }

  @After
  void tearDown() throws Exception {
    context.cleanUp()
  }

  @Test
  void testSilverpeasConfiguration() {
    TestProperties testProperties = new TestProperties().before()

    def result = context.getGradleRunner(true)
        .withArguments('configure_silverpeas')
        .build()
    assert result.task(':configure_silverpeas').outcome == SUCCESS

    testProperties.after()

    assertThePropertiesFileAreCorrectlyConfigured(testProperties)
    assertTheCustomerWorkflowIsCorrectlyConfigured(testProperties)
    assertTheWorkflowEngineIsCorrectlyConfiguredByGroovyScript(testProperties)
    assertTheConfigContextIsCorrectlySaved(testProperties)
  }

  void assertTheCustomerWorkflowIsCorrectlyConfigured(TestProperties props) {
    def before = props.xmlconf.before
    def after = props.xmlconf.after
    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'achats'}
          .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value !=
        before.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'achats'}
          .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
        .consequences.consequence.find { it.@value == 'achats'}
        .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value == 'kmelia42'

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'achats'}
          .triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value !=
        before.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'achats'}
          .triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'achats'}
          .triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value == '100'

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'MO'}
          .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value !=
        before.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'MO'}
          .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'MO'}
          .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value == 'kmelia42'

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'MO'}
          .triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value !=
        before.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
        .consequences.consequence.find { it.@value == 'MO'}
        .triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'MO'}
          .triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value == '100'

    assert after.adefCreateProduct.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value !=
        before.adefCreateProduct.actions.action.find { it.@name == 'Archiver'}.consequences.consequence
          .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value

    assert after.adefCreateProduct.actions.action.find { it.@name == 'Archiver'}.consequences.consequence
          .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value == 'kmelia42'

    assert after.adefCreateProduct.actions.action.find { it.@name == 'Archiver'}.consequences.consequence
          .triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value !=
        before.adefCreateProduct.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value

    assert after.adefCreateProduct.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value == '100'
  }

  void assertThePropertiesFileAreCorrectlyConfigured(TestProperties props) {
    def before = props.settings.before
    def after = props.settings.after

    assert after.autDomainSQL['fallbackType'] == 'always' &&
        after.autDomainSQL['fallbackType'] == before.autDomainSQL['fallbackType']
    assert after.autDomainSQL['autServer0.SQLJDBCUrl'] ==
        "jdbc:h2:file:${context.resourcesDir}/h2/test;MV_STORE=FALSE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" &&
        after.autDomainSQL['autServer0.SQLJDBCUrl'] != before.autDomainSQL['autServer0.SQLJDBCUrl']
    assert after.autDomainSQL['autServer0.SQLAccessLogin'] == 'sa' &&
        after.autDomainSQL['autServer0.SQLAccessLogin'] != before.autDomainSQL['autServer0.SQLAccessLogin']
    assert after.autDomainSQL['autServer0.SQLAccessPasswd'] == '' &&
        after.autDomainSQL['autServer0.SQLAccessPasswd'] != before.autDomainSQL['autServer0.SQLAccessPasswd']
    assert after.autDomainSQL['autServer0.SQLDriverClass'] == 'org.h2.Driver' &&
        after.autDomainSQL['autServer0.SQLDriverClass'] != before.autDomainSQL['autServer0.SQLDriverClass']

    assert after.scheduler['timeoutSchedule'] == '* 0,4,8,12,16,20 * * *' &&
        after.scheduler['timeoutSchedule'] != before.scheduler['timeoutSchedule']

    assert after.castorSettings['CastorJDODatabaseFileURL'] != before.castorSettings['CastorJDODatabaseFileURL'] &&
        !after.castorSettings['CastorJDODatabaseFileURL'].empty
  }

  void assertTheWorkflowEngineIsCorrectlyConfiguredByGroovyScript(TestProperties props) {
    // the JDO Castor doesn't support H2, so the engine is set up by default to postgresql
    def before = props.xmlconf.before
    def after = props.xmlconf.after

    assert after.workflowDatabaseConf.@engine == 'postgresql' &&
        after.workflowDatabaseConf.@engine.text() != before.workflowDatabaseConf.@engine.text()
    assert after.workflowDatabaseConf.database.@engine == 'postgresql' &&
        after.workflowDatabaseConf.database.@engine.text() != before.workflowDatabaseConf.database.@engine.text()
    assert after.workflowDatabaseConf.database.mapping.@href.text() ==
        "file:///${System.getProperty('SILVERPEAS_HOME')}/resources/instanceManager/mapping.xml"

    assert after.workflowFastDatabaseConf.@engine == 'postgresql' &&
        after.workflowFastDatabaseConf.@engine.text() != before.workflowFastDatabaseConf.@engine.text()
    assert after.workflowFastDatabaseConf.mapping.@href.text() ==
        "file:///${System.getProperty('SILVERPEAS_HOME')}/resources/instanceManager/fast_mapping.xml"
  }

  void assertTheConfigContextIsCorrectlySaved(TestProperties props) {
    assert props.configContext.before['status is'] == props.configContext.after['status is']
    assert props.configContext.before['installed at'] == props.configContext.after['installed at']
    assert props.configContext.before['updated at'] == props.configContext.after['updated at']
    assert props.configContext.after['context handled at'] == LocalDate.now().toString()
  }

}
