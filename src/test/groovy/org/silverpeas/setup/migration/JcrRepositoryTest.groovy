package org.silverpeas.setup.migration

import org.junit.Before
import org.junit.Test
import org.silverpeas.setup.api.JcrService
import org.silverpeas.setup.api.JcrService.CloseableSession
import org.silverpeas.setup.test.TestContext

import javax.jcr.Node

/**
 * Test case on the JCR repository fetching.
 * @author mmoquillon
 */
class JcrRepositoryTest {

  private TestContext context

  @Before
  void setUp() {
    context = TestContext.create()
  }

  @Test
  void test1RepositoryAccess() {
    try(CloseableSession session = openSession()) {
      assert session != null
    }
  }

  @Test
  void test2NodeAccessInRepository() {
    try(CloseableSession session = openSession()) {
      Node doc = session.getNode('/kmelia1/attachments/simpledoc_1')
      assert doc != null
      assert doc.primaryNodeType.isNodeType('slv:simpleDocument')
      assert doc.getProperty('jcr:uuid').string == '99c222b7-3323-4d56-9b7e-7f5c2197170e'
      assert doc.getProperty('slv:foreignKey').string == '1'
      assert doc.getProperty('slv:instanceId').string == 'kmelia1'
      assert !doc.getProperty('slv:versioned').boolean

    }
  }

  @Test
  void test3NodeModificationRepository() {
    Date now = new Date()

    try(CloseableSession session = openSession()) {
      Node doc = session.getNode('/kmelia1/attachments/simpledoc_1')
      assert doc != null

      doc.setProperty('slv:reservationDate', JcrService.instance.convertToJcrValue(session, now))
      doc.setProperty('slv:owner', 'toto')

      session.save()
    }

    try(CloseableSession session = openSession()) {
      Node doc = session.getNode('/kmelia1/attachments/simpledoc_1')
      assert doc != null

      assert doc.getProperty('slv:owner').string == 'toto'
      assert doc.getProperty('slv:reservationDate').date.getTime() == now
    }
  }

  private CloseableSession openSession() {
    return JcrService.instance.openSession([JCR_HOME: "${context.resourcesDir}/jcr"])
  }
}
