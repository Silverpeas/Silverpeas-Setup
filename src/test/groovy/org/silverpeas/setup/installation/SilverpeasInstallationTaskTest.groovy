package org.silverpeas.setup.installation

import groovy.mock.interceptor.MockFor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.junit.Before
import org.junit.Test
import org.silverpeas.setup.api.JBossServer
import org.silverpeas.setup.test.TestContext

import static org.silverpeas.setup.api.SilverpeasSetupTaskNames.INSTALL

/**
 * Test the case of the installation of Silverpeas performed by a dedicated Gradle task.
 * @author mmoquillon
 */
class SilverpeasInstallationTaskTest {

  private Project project
  private TestContext context

  @Before
  void setUp() {
    context = TestContext.create().setUpSystemEnv()
    project = context.createGradleProject()
  }

  @Test
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
    task.jboss = jboss
    task.install()

    mock.verify(jboss)
  }

  @Test
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
    task.jboss = jboss
    task.install()

    FileUtils.deleteQuietly(jackrabbit)

    mock.verify(jboss)
  }
}
