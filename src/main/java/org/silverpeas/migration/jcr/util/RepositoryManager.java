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
package org.silverpeas.migration.jcr.util;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.RepositoryFactoryImpl;
import org.silverpeas.util.SilverpeasHomeResolver;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ehugonnet
 */
public class RepositoryManager {

  private JackrabbitRepository repository;

  public RepositoryManager() {
    try {
      String conf = getClass().getClassLoader().getResource("repository.xml").toURI().toURL().
          toString();
      String repositoryHome = SilverpeasHomeResolver.getHome() + File.separatorChar + "data"
          + File.separatorChar + "jackrabbit";
      initRepository(repositoryHome, conf);
    } catch (URISyntaxException ex) {
      Logger.getLogger(RepositoryManager.class.getName()).log(Level.SEVERE, null, ex);
    } catch (MalformedURLException ex) {
      Logger.getLogger(RepositoryManager.class.getName()).log(Level.SEVERE, null, ex);
    } catch (RepositoryException ex) {
      Logger.getLogger(RepositoryManager.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public RepositoryManager(String repositoryHome, String conf) {
    try {
      initRepository(repositoryHome, conf);
    } catch (RepositoryException ex) {
      Logger.getLogger(RepositoryManager.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private final void initRepository(String repositoryHome, String conf) throws RepositoryException {
    Map<String, String> parameters = new HashMap<String, String>(2);
    parameters.put(RepositoryFactoryImpl.REPOSITORY_HOME, repositoryHome);
    parameters.put(RepositoryFactoryImpl.REPOSITORY_CONF, conf);
    repository = (JackrabbitRepository) JcrUtils.getRepository(parameters);
  }

  public Session getSession() throws RepositoryException {
    return repository.login(/* new SilverpeasSystemCredentials() */);
  }

  public void logout(Session session) {
    if (session != null) {
      session.logout();
    }
  }

  public void close() {
    this.repository.shutdown();
  }
}
