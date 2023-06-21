package org.silverpeas.setup.api


import org.apache.jackrabbit.oak.spi.commit.CommitInfo
import org.apache.jackrabbit.oak.spi.commit.EmptyHook
import org.apache.jackrabbit.oak.spi.state.NodeBuilder
import org.apache.jackrabbit.oak.spi.state.NodeState
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.silverpeas.setup.api.JcrRepositoryFactory.CloseableRepository

import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.jcr.SimpleCredentials
import javax.jcr.Value

/**
 * The service to handle the Silverpeas JCR content.
 * @author mmoquillon
 */
@Singleton(lazy = true)
class JcrService {

  /**
   * Opens a new session to the JCR repository.
   * @param settings the parameters required to access the repository. Expected the property
   * JCR_HOME with the absolute path of the JCR home directory into which the repository is located.
   * @return a closeable session. It should be closed once the any treatments with the JCR is done. Once closed, the
   * user is disconnected from the repository and the
   * access to it is freed.
   */
  @SuppressWarnings('GrMethodMayBeStatic')
  CloseableSession openSession(Map settings) {
    CloseableRepository repository = JcrRepositoryFactory.instance.createRepository(settings)
    Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()))
    return new CloseableSession(repository: repository, session: session)
  }

  /**
   * Performs administrative task against the JCR referred by the specified settings and using the Oak API.
   * The task accepts one argument: the root of the JCR tree as a NodeBuilder instance. The method takes in charge
   * the opening and the disposing of the repository as well as to commit with the JCR any changes registered with the
   * NodeBuilder.
   * @param settings the parameters required to access the repository. Expected the property
   * JCR_HOME with the absolute path of the JCR home directory into which the repository is located.
   * @param adminTask a closure accepting as argument the NodeBuilder of the JCR root node.
   */
  @SuppressWarnings(['GrMethodMayBeStatic', 'unused'])
  void performOnNodeRootState(Map settings, Closure<NodeBuilder> adminTask) {
    CloseableRepository repository = JcrRepositoryFactory.instance.createRepository(settings)
    repository.withCloseable {
        NodeStore nodeStore = it.nodeStore
        NodeState rootState = nodeStore.getRoot()
        NodeBuilder root = rootState.builder()
        adminTask(root)

        nodeStore.merge(root, new EmptyHook(), CommitInfo.EMPTY)
    }
  }

  /**
   * Creates a JCR valid value from the specified Java datetime.
   * @param session a user session with the repository.
   * @param date the datetime to convert
   * @return the datetime as a JCR value.
   * @throws RepositoryException if an error occurs while converting the datetime.
   */
  @SuppressWarnings('GrMethodMayBeStatic')
  Value convertToJcrValue(final Session session, final Date date)
      throws RepositoryException {
    Calendar calendar = Calendar.getInstance()
    calendar.setTime(date)
    return session.getValueFactory().createValue(calendar)
  }

  static class CloseableSession implements Session, Closeable {
    @Delegate
    Session session
    private CloseableRepository repository

    @Override
    void close() throws IOException {
      session.logout()
      repository.close()
    }
  }
}
