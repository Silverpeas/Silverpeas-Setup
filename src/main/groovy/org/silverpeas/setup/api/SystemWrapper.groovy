/*
  Copyright (C) 2000 - 2017 Silverpeas

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

/**
 * The system wrapper wraps the actual System class in order to perform additional treatments on
 * some of the System's methods. This class should be used in the project instead of System.
 * @author mmoquillon
 */
abstract class SystemWrapper {

  /**
   * Gets the value of the specified environment variable. As the Silverpeas Setup tool provides a
   * way to override an environment variable with the system property of the same name, it first
   * checks if the variable exists as a system property before looking for among the environment
   * variables of the OS.
   * @param env the environment variable.
   * @return the value of the specified environment variable.
   */
  static String getenv(String env) {
    String value = System.getProperty(env)
    if (!value) {
      value = System.getenv(env)
    }
    return value
  }

  /**
   * @see System#getProperty(java.lang.String)
   */
  static String getProperty(String property) {
    return System.getProperty(property)
  }

  /**
   * @see System#getProperty(java.lang.String, java.lang.String)
   */
  static String getProperty(String property, String defaultValue) {
    return System.getProperty(property, defaultValue)
  }

  /**
   * @see System#setProperty(java.lang.String, java.lang.String)
   */
  static String setProperty(String property, String value) {
    return System.setProperty(property, value)
  }

}
