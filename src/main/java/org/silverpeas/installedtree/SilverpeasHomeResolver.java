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

package org.silverpeas.installedtree;

public class SilverpeasHomeResolver {

  private static final String HOME_KEY = "silverpeas.home";
  private static final String ENV_KEY = "SILVERPEAS_HOME";
  private static String silverpeasHome = null;
  private static boolean abortOnError = true;

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
   * @return the silverpeas home directory
   */
  public static String getHome() {
    if (silverpeasHome == null) {
      if (!System.getProperties().containsKey(HOME_KEY)
          && !System.getenv().containsKey(ENV_KEY)) {
        System.err.println("### CANNOT FIND SILVERPEAS INSTALL LOCATION ###");
        System.err.println("please use \"-D" + HOME_KEY
            + "=<install location>\" on the command line");
        System.err
            .println("or define the SILVERPEAS_HOME environment variable.");
        if (getAbortOnError()) {
          System.err.println("### ABORTED ###");
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
}
