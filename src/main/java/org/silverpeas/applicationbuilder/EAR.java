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

package org.silverpeas.applicationbuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class dispatches the contributions parts in the target structures and then creates the
 * archive.
 * @author Silverpeas
 * @version 1.0/B
 * @since 1.0/B
 */
public class EAR extends EARDirectory {

  /**
   * The name of the application archive to build
   * @since 1.0/B
   */
  private static final String NAME = "silverpeas.ear";
  private static final String LIB_DIRECTORY = "lib";

  private AppDescriptor theAppDescriptor = null;
  private WAR theWAR = null;

  public EAR(File directory) throws AppBuilderException {
    super(directory, NAME);
    //
    setWAR(this.earDir);
    setAppDescriptor();
    setName(NAME);
  }

  /**
   * @roseuid 3AAE4A2B024E
   */
  public void addLibrary(ApplicationBuilderItem library)
      throws AppBuilderException {
    library.setLocation(LIB_DIRECTORY);
    add(library);
    // Adds the library's name to the application descriptor
    getAppDescriptor().setClientInfos(LIB_DIRECTORY + '/' + library.getName());
  }

  /**
   * Adds a set of libraries and updates the application descriptor
   */
  public void addLibraries(ApplicationBuilderItem[] libraries)
      throws AppBuilderException {
    for (int i = 0; i < libraries.length; i++) {
      addLibrary(libraries[i]);
    }
  }

  /**
   * When all entries have been added, call this method to close the archive
   * @roseuid 3AB1EAFE02FD
   */
  public void close() throws AppBuilderException {
    // WAR
    getWAR().close();
    try {
      if (getWAR().getPath() != null && getWAR().getPath().exists()
          && !getWAR().getPath().delete()) {
        Log.add("WARNING : could not delete \"" + getWAR().getName() + "\" from temporary space");
      }
    } catch (Exception e) {
      Log.add("WARNING : could not delete \"" + getWAR().getName()
          + "\" from temporary space");
      Log.add(e);
    }
    // Application descriptor
    add(getAppDescriptor());
  }

  /**
   * Adds a set of EJBs and updates the application descriptor
   * @roseuid 3AAFC08C01E2
   */
  public void addEJBs(ApplicationBuilderItem[] srcEjbs) throws AppBuilderException {
    for (ApplicationBuilderItem srcEjb : srcEjbs) {
      add(srcEjb);
      // Adds the EJB name to the application descriptor
      getAppDescriptor().addEJBName(srcEjb.getName());
    }
  }

  /**
   * @return the WAR object
   * @since 1.0/B
   * @roseuid 3AAFC0FC0084
   */
  public WAR getWAR() {
    return theWAR;
  }

  private void setWAR(File directory) throws AppBuilderException {
    theWAR = new WAR(directory);
  }

  /**
   * @return the AppDescriptor object
   * @since 1.0
   * @roseuid 3AB10CAE03BF
   */
  public AppDescriptor getAppDescriptor() {
    return theAppDescriptor;
  }

  private void setAppDescriptor() throws AppBuilderException {
    theAppDescriptor = new AppDescriptor();
    getAppDescriptor().setWARInfos(getWAR().getName(),
        ApplicationBuilder.getApplicationRoot());
  }

  void addExternalWars(ReadOnlyArchive[] externals) throws AppBuilderException {
    for (ReadOnlyArchive externalArchive : externals) {
      ExternalWar externalWar =
          new ExternalWar(externalArchive.getHome(), externalArchive.getName());
      String warName = externalWar.getName();
      getAppDescriptor().setWARInfos(warName, warName.substring(0, warName.lastIndexOf('.')));
      add(externalWar);
    }
  }
}