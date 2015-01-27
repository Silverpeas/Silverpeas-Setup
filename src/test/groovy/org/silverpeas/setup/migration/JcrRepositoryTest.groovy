package org.silverpeas.setup.migration

import org.silverpeas.setup.api.JcrRepositoryFactory
import org.silverpeas.setup.test.DatabaseSetUp
import org.silverpeas.setup.test.TestSetUp

import javax.jcr.Repository
import javax.jcr.Session
import javax.jcr.SimpleCredentials
import javax.naming.InitialContext
import javax.naming.NameNotFoundException

/**
 * Test case on the JCR repository fetching.
 * @author mmoquillon
 */
class JcrRepositoryTest extends GroovyTestCase {

  private DatabaseSetUp databaseSetUp
  private TestSetUp testSetUp

  @Override
  void setUp() {
    super.setUp()
    testSetUp = TestSetUp.setUp()
    databaseSetUp = DatabaseSetUp.setUp(withDatasource: true)
  }

  void testRepositoryAccess() {
    Repository repository =
        JcrRepositoryFactory.instance.createRepository([SILVERPEAS_HOME: testSetUp.resourcesDir])
    assert repository != null

    Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()))
    assert session != null
    session.logout()
  }

}
