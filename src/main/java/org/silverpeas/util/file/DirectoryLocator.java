/**
 * Copyright (C) 2000 - 2013 Silverpeas
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
package org.silverpeas.util.file;

import java.io.File;

import org.silverpeas.util.ConfigurationHolder;

public class DirectoryLocator {
  // Sublevel 1

  private static final String PROPERTIES_HOME_SUBDIR = "properties";
  private static final String HELP_HOME_SUBDIR = "help" + File.separatorChar + "fr";
  private static final String LOG_SUBDIR = "log";
  private static final String LIB_SUBDIR = "jar";
  private static final String REPOSITORY_SUBDIR = "repository";
  private static final String VERSION_SUBDIR = "version";
  private static final String TEMP_SUBDIR = "temp";
  // Contributed pieces Sublevels (repository sublevels)
  private static final String WAR_CONTRIB_SUBDIR = "war";
  private static final String CLIENT_CONTRIB_SUBDIR = "client";
  private static final String LIB_CONTRIB_SUBDIR = "java";
  private static final String CONTRIB_FILES_SUBDIR = "data";
  private static final String EJB_CONTRIB_SUBDIR = "ejb";
  private static final String EXTERNAL_CONTRIB_SUBDIR = "external";

  /* MEMBERS */
  // Install location
  private static String silverpeasHome = null;
  // Application level
  private static String applicationHome = null;
  // Sublevel 1
  private static String propertiesHome = null;
  private static String helpHome = null;
  private static String logHome = null;
  private static String libHome = null;
  private static String repositoryHome = null;
  private static String versionHome = null;
  private static String tempHome = null;
  // Contributed pieces locations
  private static String warContribHome = null;
  private static String clientContribHome = null;
  private static String libContribHome = null;
  private static String ejbContribHome = null;
  private static String contribFilesHome = null;
  private static String externalFilesHome = null;

  /**
   * @return the Silverpeas install location
   */
  public static String getSilverpeasHome() {
    if (silverpeasHome == null) {
      silverpeasHome = ConfigurationHolder.getHome();
    }
    return silverpeasHome;
  }

  /**
   * @return the root directory of Silverpeas installed tree. the parent of 'bin', 'properties',
   * etc.
   */
  static public String getApplicationHome() {
    if (applicationHome == null) {
      applicationHome = getSilverpeasHome();
    }
    return applicationHome;
  }

  /**
   * @return the root directory of the properties tree
   */
  public static String getPropertiesHome() {
    if (propertiesHome == null) {
      propertiesHome = getApplicationHome() + File.separatorChar + PROPERTIES_HOME_SUBDIR;
    }
    return propertiesHome;
  }

  /**
   * @return a map of the help paths (String) indexed by the locales("fr", "en", "de", ...)
   */
  public static String getHelpHome() {
    if (helpHome == null) {
      helpHome = getApplicationHome() + File.separatorChar + HELP_HOME_SUBDIR;
    }
    return helpHome;
  }

  /**
   * @return the log directory
   */
  public static String getLogHome() {
    if (logHome == null) {
      logHome = getApplicationHome() + File.separatorChar + LOG_SUBDIR;
    }
    return logHome;
  }

  /**
   * @return the version directory
   */
  public static String getVersionHome() {
    if (versionHome == null) {
      versionHome = getApplicationHome() + File.separatorChar + VERSION_SUBDIR;
    }
    return versionHome;
  }

  /**
   * @return the jar directory
   */
  public static String getLibraryHome() {
    if (libHome == null) {
      libHome = getApplicationHome() + File.separatorChar + LIB_SUBDIR;
    }
    return libHome;
  }

  /**
   * @return the temp directory
   */
  public static String getTempHome() {
    if (tempHome == null) {
      tempHome = getApplicationHome() + File.separatorChar + TEMP_SUBDIR;
    }
    return tempHome;
  }

  /**
   * @param repository
   * @return the repository directory
   */
  public static void setRepositoryHome(String repository) {
    repositoryHome = getApplicationHome() + File.separatorChar + repository;
    warContribHome = getRepositoryHome() + File.separatorChar + WAR_CONTRIB_SUBDIR;
    clientContribHome = getRepositoryHome() + File.separatorChar + CLIENT_CONTRIB_SUBDIR;
    libContribHome = getRepositoryHome() + File.separatorChar + LIB_CONTRIB_SUBDIR;
    ejbContribHome = getRepositoryHome() + File.separatorChar + EJB_CONTRIB_SUBDIR;
    contribFilesHome = getRepositoryHome() + File.separatorChar + CONTRIB_FILES_SUBDIR;
  }

  /**
   * @return the repository directory
   */
  public static String getRepositoryHome() {
    if (repositoryHome == null) {
      repositoryHome = getApplicationHome() + File.separatorChar + REPOSITORY_SUBDIR;
    }
    return repositoryHome;
  }

  /**
   * @return the war contributions directory
   */
  public static String getWarContribHome() {
    if (warContribHome == null) {
      warContribHome = getRepositoryHome() + File.separatorChar + WAR_CONTRIB_SUBDIR;
    }
    return warContribHome;
  }

  /**
   * @return the client contributions directory
   */
  public static String getClientContribHome() {
    if (clientContribHome == null) {
      clientContribHome = getRepositoryHome() + File.separatorChar + CLIENT_CONTRIB_SUBDIR;
    }
    return clientContribHome;
  }

  /**
   * @return the library contibutions directory
   */
  public static String getLibContribHome() {
    if (libContribHome == null) {
      libContribHome = getRepositoryHome() + File.separatorChar + LIB_CONTRIB_SUBDIR;
    }
    return libContribHome;
  }

  /**
   * @return the EJB contributions directory
   */
  public static String getEjbContribHome() {
    if (ejbContribHome == null) {
      ejbContribHome = getRepositoryHome() + File.separatorChar + EJB_CONTRIB_SUBDIR;
    }
    return ejbContribHome;
  }

  /**
   * @return the contribution XML files directory
   */
  public static String getContribFilesHome() {
    if (contribFilesHome == null) {
      contribFilesHome = getRepositoryHome() + File.separatorChar + CONTRIB_FILES_SUBDIR;
    }
    return contribFilesHome;
  }

  public static String getExternalFilesHome() {
    if (externalFilesHome == null) {
      externalFilesHome = getRepositoryHome() + File.separatorChar + EXTERNAL_CONTRIB_SUBDIR;
    }
    return externalFilesHome;
  }
}
