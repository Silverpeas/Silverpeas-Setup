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

  /**
   * The assertions below are only about the expected value of each configured parameter. Don't add
   * any assertion checking a value has changed: the configuration is applied in place, on the
   * resources of the build directory, and hence a subsequent execution of this test would find them
   * already configured and would fail whereas the configuration did work.
   */
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
    assertTheConfigContextIsCorrectlySaved(testProperties)
  }

  static void assertTheCustomerWorkflowIsCorrectlyConfigured(TestProperties props) {
    def after = props.xmlconf.after

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
        .consequences.consequence.find { it.@value == 'achats'}
        .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value == 'kmelia42'

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'achats'}
          .triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value == '100'

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'MO'}
          .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value == 'kmelia42'

    assert after.adefCreateSupplier.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.find { it.@value == 'MO'}
          .triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value == '100'

    assert after.adefCreateProduct.actions.action.find { it.@name == 'Archiver'}.consequences.consequence
          .triggers.trigger.param.find { it.@name == 'targetComponentId'}.@value == 'kmelia42'

    assert after.adefCreateProduct.actions.action.find { it.@name == 'Archiver'}
          .consequences.consequence.triggers.trigger.param.find { it.@name == 'targetTopicId'}.@value == '100'
  }

  void assertThePropertiesFileAreCorrectlyConfigured(TestProperties props) {
    def before = props.settings.before
    def after = props.settings.after

    assert after.autDomainSQL['fallbackType'] == 'always' &&
        after.autDomainSQL['fallbackType'] == before.autDomainSQL['fallbackType']
    assert after.autDomainSQL['autServer0.SQLJDBCUrl'] ==
        "jdbc:h2:file:${context.resourcesDir}/h2/test;MV_STORE=FALSE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    assert after.autDomainSQL['autServer0.SQLAccessLogin'] == 'sa'
    assert after.autDomainSQL['autServer0.SQLAccessPasswd'] == ''
    assert after.autDomainSQL['autServer0.SQLDriverClass'] == 'org.h2.Driver'

    assert after.scheduler['timeoutSchedule'] == '* 0,4,8,12,16,20 * * *'
  }

  static void assertTheConfigContextIsCorrectlySaved(TestProperties props) {
    assert props.configContext.before['status is'] == props.configContext.after['status is']
    assert props.configContext.before['installed at'] == props.configContext.after['installed at']
    assert props.configContext.before['updated at'] == props.configContext.after['updated at']
    assert props.configContext.after['context handled at'] == LocalDate.now().toString()
  }

}
