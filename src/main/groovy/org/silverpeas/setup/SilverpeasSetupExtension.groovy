/*
    Copyright (C) 2000 - 2016 Silverpeas

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

import org.gradle.api.logging.LogLevel
import org.silverpeas.setup.api.SystemWrapper

/**
 * Extension of the plugin in order to provide to the usual Silverpeas setting up properties.
 * @author mmoquillon
 */
class SilverpeasSetupExtension {

  /**
   * The path of the Silverpeas home directory.
   */
  String silverpeasHome = SystemWrapper.getenv('SILVERPEAS_HOME')

  /**
   * The path of the JBoss home directory.
   */
  String jbossHome = SystemWrapper.getenv('JBOSS_HOME')

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
   *   <li><em><code>modules</code></em> in which are provided the XML descriptor of each migration module
   *   to build or to upgrade the datasources used Silverpeas;</li>
   *   <li><em><code>db</code></em> in which are located per database type and per module the
   *   different SQL scripts to create or to upgrade the schema of the main database;</li>
   *   <li><em><code>scripts</code></em> in which are located per module the different programming
   *   scripts to work on the migration both the main database or of any of the other datasources
   *   used by Silverpeas (like JCR for example). Currently, only the Groovy scripts are supported.
   *   </li>
   * </ul>
   */
  String migrationHome = "${silverpeasHome}/migrations"

  /**
   * The path of the directory containing the JDBC drivers supported by Silverpeas.
   */
  String driversDir

  /**
   * Is in development mode (in this case, some peculiar configuration are applied to support the
   * dev mode in the application server.
   */
  boolean developmentMode = false


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
