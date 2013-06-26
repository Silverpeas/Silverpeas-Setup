/*
 * Copyright (C) 2000 - 2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection withWriter Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.migration.jcr.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeTypeExistsException;

import org.silverpeas.util.SilverpeasHomeResolver;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.RepositoryFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ehugonnet
 */
public class RepositoryManager {

  private static final Logger logger = LoggerFactory.getLogger(RepositoryManager.class);
  private JackrabbitRepository repository;

  public RepositoryManager() {
    try {
      String conf = SilverpeasHomeResolver.getHome() + File.separatorChar + "setup"
          + File.separatorChar + "jackrabbit" + File.separatorChar + "repository.xml";
      String repositoryHome = SilverpeasHomeResolver.getDataHome() + File.separatorChar
          + "jackrabbit";
      initRepository(repositoryHome, conf);
    } catch (RepositoryException ex) {
      logger.error("Error during JCR repository initalisation", ex);
    } catch (IOException ex) {
      logger.error("Error during JCR repository initalisation", ex);
    }
  }

  public RepositoryManager(String repositoryHome, String repositoryXml) {
    try {
      initRepository(repositoryHome, repositoryXml);
    } catch (RepositoryException ex) {
      logger.error("Error during JCR repository initalisation", ex);
    }
  }

  private final void initRepository(String repositoryHome, String conf) throws RepositoryException {
    Map<String, String> parameters = new HashMap<String, String>(2);
    parameters.put(RepositoryFactoryImpl.REPOSITORY_HOME, repositoryHome);
    parameters.put(RepositoryFactoryImpl.REPOSITORY_CONF, conf);
    repository = (JackrabbitRepository) JcrUtils.getRepository(parameters);
    Reader reader = new InputStreamReader(
        this.getClass().getClassLoader().getResourceAsStream("silverpeas-jcr.txt"), Charsets.UTF_8);
    try {
      Session session = getSession();
      CndImporter.registerNodeTypes(reader, session);
      session.save();
      session.logout();
    } catch (InvalidNodeTypeDefinitionException ex) {
      logger.error("Error during JCR repository initalisation", ex);
    } catch (NodeTypeExistsException ex) {
      logger.error("Error during JCR repository initalisation", ex);
    } catch (UnsupportedRepositoryOperationException ex) {
      logger.error("Error during JCR repository initalisation", ex);
    } catch (ParseException ex) {
      logger.error("Error during JCR repository initalisation", ex);
    } catch (IOException ex) {
      logger.error("Error during JCR repository initalisation", ex);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  public Session getSession() throws RepositoryException {
    return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
  }

  public void logout(Session session) {
    if (session != null) {
      session.logout();
    }
  }

  public void shutdown() {
    this.repository.shutdown();
  }
}
