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

    project.silversetup.logging.logDir = "${project.buildDir.path.replaceAll("[\\\\]", "/")}/log"
    project.silversetup.logging.useLogger = false
    project.silversetup.silverpeasVersion = project.version
  }

  void testSilverpeasConfiguration() {
    TestContext context = new TestContext().before()

    project.tasks.findByPath('configureSilverpeas').configureSilverpeas()

    context.after()

    assertThePropertiesFileAreCorrectlyConfigured(context)
    assertTheCustomerWorkflowIsCorrectlyConfigured(context)
    assertTheWorkflowEngineIsCorrectlyConfiguredByGroovyScript(context)
  }

  void assertTheCustomerWorkflowIsCorrectlyConfigured(TestContext context) {
    def before = context.xmlconf.before
    def after = context.xmlconf.after
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

  void assertThePropertiesFileAreCorrectlyConfigured(TestContext context) {
    def before = context.settings.before
    def after = context.settings.after

    assert after.autDomainSQL['fallbackType'] == 'always' &&
        after.autDomainSQL['fallbackType'] == before.autDomainSQL['fallbackType']
    assert after.autDomainSQL['autServer0.SQLJDBCUrl'] ==
        "jdbc:h2:file:${testSetUp.resourcesDir}/h2/test;MV_STORE=FALSE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" &&
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

  void assertTheWorkflowEngineIsCorrectlyConfiguredByGroovyScript(TestContext context) {
    // the JDO Castor doesn't support H2, so the engine is set up by default to postgresql
    def before = context.xmlconf.before
    def after = context.xmlconf.after

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

}
