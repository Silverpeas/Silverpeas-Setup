package org.silverpeas.setup.api

import org.silverpeas.setup.api.JcrRepositoryFactory.DisposableRepository

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
   * @return a closeable session. Once closed, the user is disconnected from the repository and the
   * access to it is freed.
   */
  @SuppressWarnings('GrMethodMayBeStatic')
  CloseableSession openSession(Map settings) {
    DisposableRepository repository = JcrRepositoryFactory.instance.createRepository(settings)
    Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()))
    return new CloseableSession(repository: repository, session: session)
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
    @Delegate Session session
    private DisposableRepository repository

    @Override
    void close() throws IOException {
      session.logout()
      repository.dispose()
    }
  }
}
