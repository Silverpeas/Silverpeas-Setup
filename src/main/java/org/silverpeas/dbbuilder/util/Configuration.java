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
package org.silverpeas.dbbuilder.util;


import org.silverpeas.util.ConfigurationHolder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author ehugonnet
 */
public class Configuration {
  private static String dbbuilderData = null;
  private static final String DATA_KEY = "dbbuilder.data";
  private static final String DBREPOSITORY_SUBDIR = "dbRepository";
  private static final String CONTRIB_FILES_SUBDIR = "data";
  private static final String LOG_FILES_SUBDIR = "log";
  private static final String TEMP_FILES_SUBDIR = "temp";
  private static final String DIR_CONTRIBUTIONFILESROOT = ConfigurationHolder.getHome()
      + File.separatorChar + DBREPOSITORY_SUBDIR + File.separatorChar + CONTRIB_FILES_SUBDIR;
  // Répertoire racine des DB Pieces Contribution File
  private static final String DIR_DBPIECESFILESROOT = ConfigurationHolder.getHome()
      + File.separatorChar + DBREPOSITORY_SUBDIR;
  // Répertoire temp
  private static final String DIR_TEMP = ConfigurationHolder.getHome()
      + File.separatorChar + TEMP_FILES_SUBDIR;

 

  public static String getContributionFilesDir() {
    return DIR_CONTRIBUTIONFILESROOT;
  }

  public static String getSilverpeasData() {
    try {
      return ConfigurationHolder.getDataHome();
    } catch (IOException e) {
      return null;
    }
  }

  public static String getSilverpeasDataMigrationSaves() {
    try {
      return ConfigurationHolder.getDataHome() + File.separatorChar + "migrationSaves" +
          new SimpleDateFormat("_yyyy-MM-dd_HH.mm").format(new Date());
    } catch (IOException e) {
      return null;
    }
  }

  public static String getPiecesFilesDir() {
    return DIR_DBPIECESFILESROOT;
  }

  // Récupère le répertoire data d'installation
  public static String getData() {
    if (null == dbbuilderData) {
      if (System.getProperties().containsKey(DATA_KEY)) {
        dbbuilderData = System.getProperty(DATA_KEY);
      }
    }
    return dbbuilderData;
  }

  /**
   * Return the temporary directory path.
   * @return the temporary directory path.
   */
  public static String getTemp() {
    return DIR_TEMP;
  }

  public static String getLogDir() {
    return ConfigurationHolder.getHome() + File.separatorChar + LOG_FILES_SUBDIR;
  }

  private static String getPropertyPath(String propertyPath) {
    String path = propertyPath.replace('/', File.separatorChar);
    path = path.replace('\\', File.separatorChar);
    if (!path.startsWith(File.separator)) {
      path = File.separatorChar + path;
    }
    return ConfigurationHolder.getHome() + File.separatorChar + "properties" + path;
  }

  private Configuration() {
  }
}
