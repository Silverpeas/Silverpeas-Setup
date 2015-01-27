package org.silverpeas.setup.api

import groovy.xml.XmlUtil
import org.apache.jackrabbit.core.RepositoryImpl
import org.apache.jackrabbit.core.config.RepositoryConfig
import org.silverpeas.setup.migration.DataSourceProvider

import javax.jcr.Repository
import javax.jcr.RepositoryException
import javax.naming.InitialContext
import javax.naming.NameNotFoundException
import javax.xml.parsers.SAXParserFactory

/**
 * A factory to create instances of JCR Repository from configuration properties.
 * @author mmoquillon
 */
@Singleton(lazy = true)
class JcrRepositoryFactory {

  private static final String JCR_HOME = 'jcr.home.dir'
  private static final String JCR_CONFIG_FILE = '/repository.xml'

  private File repositoryConf

  private synchronized File getRepositoryConfiguration() {
    if (!repositoryConf) {
      initJNDIContext()
      File destination = File.createTempFile('repository', 'xml')

      SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance()
      factory.validating = false
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
          false)
      def jcrRepositoryConf = new XmlSlurper(
          factory.newSAXParser()).parse(getClass().getResourceAsStream(JCR_CONFIG_FILE))
      jcrRepositoryConf.Workspace.PersistenceManager.@class = API.currentSettings.JACKRABBIT_PERSISTENCE_MANAGER
      jcrRepositoryConf.Workspace.PersistenceManager.param.find {
        it.@name == 'schema'
      }.@value = API.currentSettings.DB_SCHEMA
      jcrRepositoryConf.Versioning.PersistenceManager.@class = API.currentSettings.JACKRABBIT_PERSISTENCE_MANAGER

      XmlUtil.serialize(jcrRepositoryConf, new FileWriter(destination))
      repositoryConf = destination
    }
  }

  private void initJNDIContext() {
    InitialContext ic = new InitialContext();
    try {
      ic.lookup('java:/datasources/DocumentStore')
    } catch(NameNotFoundException ex) {
      ic.createSubcontext('java:/datasources')
      ic.bind('java:/datasources/DocumentStore', DataSourceProvider.dataSource);
    }
  }

  /**
   * Creates an instance mapping to the JCR repository used by Silverpeas.
   * @return a JCR repository instance.
   */
  Repository createRepository(def settings) {
    try {
      File repositoryConf = getRepositoryConfiguration()
      Properties jcrProperties = new Properties();
      jcrProperties.load(new FileInputStream(
          "${settings.SILVERPEAS_HOME}/properties/org/silverpeas/util/jcr.properties"));
      String jcrHomePath = jcrProperties[JCR_HOME];

      RepositoryConfig config = RepositoryConfig.create(repositoryConf.path, jcrHomePath);
      return RepositoryImpl.create(config);
    } catch (IOException | RepositoryException ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }
}
