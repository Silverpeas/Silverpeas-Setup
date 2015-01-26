/*
    Copyright (C) 2000 - 2015 Silverpeas

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
package org.silverpeas.setup

import org.silverpeas.setup.database.DatabaseType

/**
 * Extension of the plugin in order to provide to the usual Silverpeas setting up properties.
 * @author mmoquillon
 */
class SilverpeasSetupExtension {

  /**
   * The path of the Silverpeas home directory.
   */
  String silverpeasHome = System.getenv('SILVERPEAS_HOME')
  /**
   * The path of the JBoss home directory.
   */
  String jbossHome = System.getenv('JBOSS_HOME')
  /**
   * The path of the Silverpeas and JBoss configuration home directory. It is expected to contain
   * two subdirectories:
   * <ul>
   *   <li><code>jboss</code> with the configuration files for JBoss/Wildfly;</li>
   *   <li><code>silverpeas</code> with the configuration files for Silverpeas itself</li>
   * </ul>
   * By default, the configuration home directory is set to be placed in
   * <code>SILVERPEAS_HOME</code> under the name <code>configuration</code>
   */
  String configurationHome = "${silverpeasHome}/configuration"
  /**
   * The path of the home directory of the database structure building scripts. It is expected to
   * contain two kinds of subdirectories:
   * <ul>
   *   <li><code>data</code> containing a folder for each supported database system and in which an
   *   XML setting file provide information on the SQL scripts to migrate for building the database;</li>
   *   <li>a directory per supported database system into which a folder per Silverpeas components
   *   gathers the SQL scripts tp build the database; these scripts are located in subdirectories
   *   representing a given version of the database structure for the belonged component.</li>
   * </ul>
   */
  String databaseHome = "${silverpeasHome}/dbRepository"
  /**
   * The path of the directory containing the JDBC drivers.
   */
  String driversDir
  /**
   * The path of the directory containing the application servers-dedicated modules required by
   * Silverpeas. The modules are gathered into a folder per supported application server.
   */
  String modulesDir
  /**
   * The path of the directory into which the log should be generated.
   */
  String logDir = "${silverpeasHome}/log"

  /**
   * Constructs a new silverpeas configuration extension. It checks the environment variables
   * SILVERPEAS_HOME and JBOSS_HOME are correctly set.
   */
  SilverpeasSetupExtension() {
    if (!silverpeasHome || !jbossHome) {
      println 'The environment variables SILVERPEAS_HOME or JBOSS_HOME aren\'t set!'
      throw new IllegalStateException()
    }
  }
}
