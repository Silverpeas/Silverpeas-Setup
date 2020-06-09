package org.silverpeas.setup.migration

import org.junit.After
import org.junit.Before
import org.junit.Test
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
class JcrRepositoryTest {

  private DatabaseSetUp databaseSetUp
  private TestContext context

  @Before
  void setUp() {
    context = TestContext.create()
    databaseSetUp = DatabaseSetUp.setUp(withDatasource: true)
  }

  @After
  void tearDown() {
    databaseSetUp.dropAll()
  }

  @Test
  void testRepositoryAccess() {
    Repository repository =
        JcrRepositoryFactory.instance.createRepository([SILVERPEAS_HOME: context.resourcesDir])
    assert repository != null

    Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()))
    assert session != null
    session.logout()
  }

}
