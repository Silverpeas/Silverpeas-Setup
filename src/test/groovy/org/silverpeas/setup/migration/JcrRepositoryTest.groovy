package org.silverpeas.setup.migration

import org.silverpeas.setup.api.JcrRepositoryFactory
import org.silverpeas.setup.test.DatabaseSetUp
import org.silverpeas.setup.test.TestContext

import javax.jcr.Repository
import javax.jcr.Session
import javax.jcr.SimpleCredentials

/**
 * Test case on the JCR repository fetching.
 * @author mmoquillon
 */
class JcrRepositoryTest extends GroovyTestCase {

  private DatabaseSetUp databaseSetUp
  private TestContext context

  @Override
  void setUp() {
    super.setUp()
    context = TestContext.create()
    databaseSetUp = DatabaseSetUp.setUp(withDatasource: true)
  }

  @Override
  void tearDown() {
    databaseSetUp.dropAll()
  }

  void testRepositoryAccess() {
    Repository repository =
        JcrRepositoryFactory.instance.createRepository([SILVERPEAS_HOME: context.resourcesDir])
    assert repository != null

    Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()))
    assert session != null
    session.logout()
  }

}
