/*
  Copyright (C) 2000 - 2021 Silverpeas

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  As a special exception to the terms and conditions of version 3.0 of
  the GPL, you may redistribute this Program in connection with Free/Libre
  Open Source Software ("FLOSS") applications as described in Silverpeas's
  FLOSS exception.  You should have recieved a copy of the text describing
  the FLOSS exception, and it is also available here:
  "http://www.silverpeas.org/docs/core/legal/floss_exception.html"

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.setup.api

import groovy.xml.XmlUtil
import org.apache.jackrabbit.core.RepositoryImpl
import org.apache.jackrabbit.core.config.RepositoryConfig
import org.gradle.api.Project

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
  private Project project

  private synchronized File getRepositoryConfiguration(Map settings) {
    if (!repositoryConf) {
      initJNDIContext()
      File destination = File.createTempFile('repository', 'xml')

      SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance()
      factory.validating = false
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
          false)
      def jcrRepositoryConf = new XmlSlurper(
          factory.newSAXParser()).parse(getClass().getResourceAsStream(JCR_CONFIG_FILE))
      jcrRepositoryConf.Workspace.PersistenceManager.@class =
          settings.JACKRABBIT_PERSISTENCE_MANAGER
      jcrRepositoryConf.Workspace.PersistenceManager.param.find {
        it.@name == 'schema'
      }.@value = settings.DB_SCHEMA
      jcrRepositoryConf.Versioning.PersistenceManager.@class =
          settings.JACKRABBIT_PERSISTENCE_MANAGER

      XmlUtil.serialize(jcrRepositoryConf, new FileWriter(destination))
      repositoryConf = destination
    }
    return repositoryConf
  }

  private void initJNDIContext() {
    DataSourceProvider dataSourceProvider = ManagedBeanContainer.get(DataSourceProvider)
    InitialContext ic = new InitialContext();
    try {
      ic.lookup('java:/datasources/DocumentStore')
    } catch(NameNotFoundException ex) {
      ic.createSubcontext('java:/datasources')
      ic.bind('java:/datasources/DocumentStore', dataSourceProvider.dataSource);
    }
  }

  /**
   * Creates an instance mapping to the JCR repository used by Silverpeas.
   * @return a JCR repository instance.
   */
  Repository createRepository(Map settings) {
    try {
      File repositoryConf = getRepositoryConfiguration(settings)
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
