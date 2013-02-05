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
package org.silverpeas.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

public class SilverpeasHomeResolver {

  private static final String HOME_KEY = "silverpeas.home";
  private static final String ENV_KEY = "SILVERPEAS_HOME";
  private static String silverpeasHome = null;
  private static GestionVariables configuration = null;
  private static boolean abortOnError = true;
  private static final Console console = new Console(SilverpeasHomeResolver.class);

  /**
   * If set to TRUE, program is exited if Silverpeas install location cannot be found. Else, a
   * message is issued and execution goes on
   */
  public static void setAbortOnError(boolean on) {
    abortOnError = on;
  }

  /**
   * If TRUE, program is exited if Silverpeas install location cannot be found. Else, a message is
   * issued and execution goes on
   */
  public static boolean getAbortOnError() {
    return abortOnError;
  }

  /**
   * Finds Silverpeas install directory Silverpeas install location may be set by using
   * -Dsilverpeas.home=<i>location</i> on java command line
   *
   * @return the silverpeas home directory
   */
  public static String getHome() {
    if (silverpeasHome == null) {
      if (!System.getProperties().containsKey(HOME_KEY) && !System.getenv().containsKey(ENV_KEY)) {
        console.printError("### CANNOT FIND SILVERPEAS INSTALL LOCATION ###");
        console.printError("please use \"-D" + HOME_KEY
            + "=<install location>\" on the command line");
        console.printError("or define the SILVERPEAS_HOME environment variable.");
        if (getAbortOnError()) {
          console.printError("### ABORTED ###");
          System.exit(1);
        }
      }
      silverpeasHome = System.getProperty(HOME_KEY);
      if (silverpeasHome == null) {
        silverpeasHome = System.getenv(ENV_KEY);
      }
    }
    return silverpeasHome;
  }

  public static String getDataHome() throws IOException {
    if (configuration == null) {
      try {
        configuration = loadConfiguration();
      } catch (IOException ex) {
        console.printWarning("Error loading configuration", ex);
        throw ex;
      }
    }
    return configuration.resolveAndEvalString("${SILVERPEAS_DATA_HOME}");
  }

  private static GestionVariables loadConfiguration() throws IOException {
    Properties defaultConfig = new Properties();
    InputStream in = SilverpeasHomeResolver.class.getClassLoader().getResourceAsStream(
        "default_config.properties");
    try {
      defaultConfig.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
    Properties config = new Properties(defaultConfig);
    File configFile = new File(getHome() + File.separatorChar + "setup" + File.separatorChar
        + "settings", "config.properties");
    if (configFile.exists() && configFile.isFile()) {
      in = new FileInputStream(configFile);
      try {
        config.load(in);
      } finally {
        IOUtils.closeQuietly(in);
      }
    }
    return new GestionVariables(config);
  }

  private SilverpeasHomeResolver() {
  }
}
