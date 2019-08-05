package org.silverpeas.setup.installation

import groovy.mock.interceptor.MockFor
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.silverpeas.setup.api.JBossServer
import org.silverpeas.setup.test.TestContext

import static org.silverpeas.setup.api.SilverpeasSetupTaskNames.INSTALL
/**
 * Test the case of the installation of Silverpeas performed by a dedicated Gradle task.
 * @author mmoquillon
 */
class SilverpeasInstallationTaskTest extends GroovyTestCase {

  private Project project
  protected TestContext context

  @Override
  void setUp() {
    super.setUp()

    context = TestContext.create().setUpSystemEnv()

    project = ProjectBuilder.builder().build()
    project.apply plugin: 'silversetup'

    project.silversetup.logging.logDir = new File(project.buildDir, 'log')
    project.silversetup.logging.useLogger = false
  }

  void testInstallJustSilverpeas() {
    SilverpeasInstallationTask task = project.tasks.findByPath(INSTALL.name)
    def mock = new MockFor(JBossServer)

    mock.demand.with {
      isStartingOrRunning { false }
      start { }
      remove { a -> a == 'silverpeas.war' }
      add { a, b, c -> b == 'silverpeas.war' }
      deploy { a -> a == 'silverpeas.war' }
      stop { }
    }

    def jboss = mock.proxyInstance()
    task.jboss.set(jboss)
    task.install()

    mock.verify(jboss)
  }

  void testInstall() {
    SilverpeasInstallationTask task = project.tasks.findByPath(INSTALL.name)

    File jackrabbit = new File(task.installation.deploymentDir.get(), 'jackrabbit-jca.rar')
    jackrabbit.createNewFile()

    def mock = new MockFor(JBossServer)
    mock.demand.with {
      isStartingOrRunning { false }
      start { }

      remove { a -> a == jackrabbit.name }
      add { a -> a == jackrabbit.path }
      deploy { a -> a == jackrabbit.name }

      remove { a -> a == 'silverpeas.war' }
      add { a, b, c -> b == 'silverpeas.war' }
      deploy { a -> a == 'silverpeas.war' }

      stop { }
    }

    def jboss = mock.proxyInstance()
    task.jboss.set(jboss)
    task.install()

    mock.verify(jboss)
  }
}
