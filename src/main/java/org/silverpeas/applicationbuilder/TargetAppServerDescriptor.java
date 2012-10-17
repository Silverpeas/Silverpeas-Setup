/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//Source file: R:\\StraProduct\\Pkg1.0\\Dev\\SrcJava\\Java\\ApplicationBuilder\\JBuilderEnv\\src\\com\\silverpeas\\applicationbuilder\\TargetAppServerDescriptor.java

package org.silverpeas.applicationbuilder;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

/**
 * This file is updated before launching the ApplicationBuilder tool to provide the root directory
 * of the target application server. It contains also the name of the class that provides
 * application specific behavior or datas.
 * @author Silverpeas
 * @version 1.0/B
 * @since 1.0/B
 */
public class TargetAppServerDescriptor extends XmlDocument {

  /**
   * The name of the XML file that describes one or more application server.
   * @since 1.0/B
   */
  private static final String NAME = "targetAppServer.xml";

  /** one for each application server */
  private static final String SERVER_TAG = "app-server";
  /** children */
  private static final String SVRNAME_TAG = "name";
  private static final String SVRHOME_TAG = "home";
  private static final String SVRCLASS_TAG = "deploy-class";
  private static final String SVRSUBDIR_TAG = "repository-subdir";

  private static String repositorySpecificSubdir = null;

  public static void setRepositorySpecificSubdir(String subdir) {
    repositorySpecificSubdir = subdir;
  }

  public static String getRepositorySpecificSubdir() {
    return repositorySpecificSubdir;
  }

  public TargetAppServerDescriptor(File directory) throws AppBuilderException {
    super(directory, NAME);
    load();
  }

  /**
   * @roseuid 3AAF3EC603CC
   */
  public String[] getTargetAppServers() throws AppBuilderException {
    List serverElements = getDocument().getRootElement()
        .getChildren(SERVER_TAG);
    if (serverElements.size() == 0) {
      String message = getPath().getAbsolutePath()
          + "\" does not contain any application server description";
      message += "\n\tit needs at least one \"" + SERVER_TAG + "\" element";
      throw new AppBuilderException(message);
    }
    String[] result = new String[serverElements.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = ((Element) serverElements.get(i)).getChildText(SVRNAME_TAG);
      if (result[i] == null) {
        String message = "a \"" + SERVER_TAG + "\" element in "
            + getPath().getAbsolutePath() + "\" does not have a name";
        message += "\n\ta \"" + SERVER_TAG + "\" element needs one \""
            + SVRNAME_TAG + "\" child";
        throw new AppBuilderException(message);
      }
    }
    return result;
  }

  public String getRepositorySpecificSubdir(String appServer)
      throws AppBuilderException {
    return getAppServerInfo(appServer, SVRSUBDIR_TAG);
  }

  private String getAppServerInfo(String appServer, String tag)
      throws AppBuilderException {
    List serverElements = getDocument().getRootElement()
        .getChildren(SERVER_TAG);
    Iterator iServer = serverElements.iterator();
    Element server = null;
    String theText = null;
    while (iServer.hasNext()) {
      theText = null;
      server = (Element) iServer.next();
      if (server.getChildText(SVRNAME_TAG).equals(appServer)) {
        theText = server.getChildText(tag);
        if (theText == null) {
          throw new AppBuilderException(getName() + " :could not find \"" + tag
              + "\" element for \"" + appServer + "\"");
        }
        return theText;
      }
    }
    return null;
  }

}
