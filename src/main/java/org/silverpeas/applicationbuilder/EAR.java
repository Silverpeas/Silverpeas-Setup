/**
 * Copyright (C) 2000 - 2012 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.applicationbuilder;

import java.io.File;

import org.silverpeas.util.Console;

/**
 * This class dispatches the contributions parts in the target structures and then creates the
 * archive.
 *
 * @author Silverpeas
 * @version 1.0/B
 * @since 1.0/B
 */
public class EAR extends EARDirectory {

  /**
   * The name of the application archive to build
   *
   * @since 1.0/B
   */
  private static final String NAME = "silverpeas.ear";
  private static final String LIB_DIRECTORY = "lib";
  private AppDescriptor theAppDescriptor = null;
  private WAR theWAR = null;

  public EAR(File directory, Console console) throws AppBuilderException {
    super(directory, NAME, console);
    //
    setWAR(this.earDir);
    setAppDescriptor();
    setName(NAME);
  }

  public void addLibrary(ApplicationBuilderItem library) throws AppBuilderException {
    library.setLocation(LIB_DIRECTORY);
    add(library);
    getAppDescriptor().setClientInfos(LIB_DIRECTORY + '/' + library.getName());
  }

  /**
   * Adds a set of libraries and updates the application descriptor
   *
   * @param libraries
   * @throws AppBuilderException
   */
  public void addLibraries(ApplicationBuilderItem[] libraries) throws AppBuilderException {
    for (int i = 0; i < libraries.length; i++) {
      addLibrary(libraries[i]);
    }
  }

  /**
   * When all entries have been added, call this method to close the archive
   *
   * @throws AppBuilderException
   */
  public void close() throws AppBuilderException {
    getWAR().close();
    try {
      if (getWAR().getPath() != null && getWAR().getPath().exists() && !getWAR().getPath().delete()) {
        console.printMessage("WARNING : could not delete \"" + getWAR().getName()
            + "\" from temporary space");
      }
    } catch (Exception e) {
      console.printError("WARNING : could not delete \"" + getWAR().getName()
          + "\" from temporary space", e);
    }
    add(getAppDescriptor());
  }

  /**
   * Adds a set of EJBs and updates the application descriptor.
   *
   * @param srcEjbs
   * @throws AppBuilderException
   */
  public void addEJBs(ApplicationBuilderItem[] srcEjbs) throws AppBuilderException {
    for (ApplicationBuilderItem srcEjb : srcEjbs) {
      add(srcEjb);
      getAppDescriptor().addEJBName(srcEjb.getName());
    }
  }

  /**
   * @return the WAR object
   */
  public WAR getWAR() {
    return theWAR;
  }

  private void setWAR(File directory) throws AppBuilderException {
    theWAR = new WAR(directory, console);
  }

  /**
   * @return the AppDescriptor object
   */
  public AppDescriptor getAppDescriptor() {
    return theAppDescriptor;
  }

  private void setAppDescriptor() throws AppBuilderException {
    theAppDescriptor = new AppDescriptor();
    getAppDescriptor().setWARInfos(getWAR().getName(), ApplicationBuilder.getApplicationRoot());
  }

  void addExternalWars(ReadOnlyArchive[] externals) throws AppBuilderException {
    for (ReadOnlyArchive externalArchive : externals) {
      ExternalWar externalWar = new ExternalWar(externalArchive.getHome(),externalArchive.getName(),
          console);
      String warName = externalWar.getName();
      getAppDescriptor().setWARInfos(warName, warName.substring(0, warName.lastIndexOf('.')));
      add(externalWar);
    }
  }
}