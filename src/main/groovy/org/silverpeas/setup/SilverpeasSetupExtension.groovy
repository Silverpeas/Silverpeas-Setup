/*
    Copyright (C) 2000 - 2019 Silverpeas

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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.silverpeas.setup.api.JBossServer
import org.silverpeas.setup.api.SystemWrapper
/**
 * Extension of the plugin in which are defined the different properties required by the plugin to
 * work. Some of the properties are already valued whereas others have to be set by the Gradle
 * build.
 * <p>
 * Some other properties are defined directly in the bootstrapping of this plugin and those are only
 * for readonly use by the Gradle script. Such properties are:
 * </p>
 * <ul>
 *   <li><code>logging</code>: an object to set some logging properties to be used by this plugin.
 *   It has the following attributes:
 *   <ul>
 *     <li><code>logDir</code> to value with the path of the directory in which the logging files
 *     will be generated.</li>
 *     <li><code>defaultLevel</code> to set the default logging level when running the Gradle build.
 *     </li>
 *     <li><code>scriptTasks</code> an array with the tasks specific the Gradle build.
 *   </ul>
 *   <li>silverpeas
 * @author mmoquillon
 */
class SilverpeasSetupExtension {

  /**
   * The Silverpeas home directory. Defaulted with the SILVERPEAS_HOME environment
   * variable.
   */
  final File silverpeasHome

  /**
   * The JBoss home directory. Defaulted with the JBOSS_HOME environment variable.
   */
  final File jbossHome

  /**
   * The properties required by the configuration execution of a Silverpeas distribution.
   */
  final SilverpeasConfigurationProperties config

  /**
   * The properties required by the build of a given version of Silverpeas and its deployment
   * in a JBoss server.
   */
  final SilverpeasInstallationProperties installation

  /**
   * The properties required to perform a data source migration when upgrading Silverpeas to a newer
   * version or when installing a fresh Silverpeas version.
   */
  final SilverpeasMigrationProperties migration

  /**
   * The properties to configure the logging. They define the location of the logging file, the
   * logging level and so on.
   */
  final SilverpeasLoggingProperties logging

  /**
   * The time out when waiting JBoss answering to our requests. Defaulted to 5mn.
   */
  final Property<Long> timeout

  /**
   * The Silverpeas settings as defined in the <code>config.properties</code> file.
   * Some of them are computed from the properties in the file <code>config.properties</code>.
   * Tasks can overwrite some of the settings as well as add their own properties.
   */
  final Map settings = [:]

  /**
   * Constructs a new silverpeas configuration extension. It checks the environment variables
   * SILVERPEAS_HOME and JBOSS_HOME are correctly set and it sets in the settings variable
   * (that is shared by all the steps implied within a configuration of Silverpeas) the peculiar
   * <code>context</code> attribute that is a dictionary of all the context properties the steps
   * in a configuration process can set for their usage.
   */
  SilverpeasSetupExtension(Project project) {
    String silverpeasHomePath = SystemWrapper.getenv('SILVERPEAS_HOME')
    String jbossHomePath = SystemWrapper.getenv('JBOSS_HOME')
    if (!silverpeasHomePath || !jbossHomePath) {
      println 'The environment variables SILVERPEAS_HOME or JBOSS_HOME aren\'t set!'
      throw new IllegalStateException()
    }

    silverpeasHome = project.file(silverpeasHomePath)
    jbossHome = project.file(jbossHomePath)
    if (!(silverpeasHome.exists() && silverpeasHome.isDirectory()) ||
        !(jbossHome.exists() && jbossHome.isDirectory())) {
      println 'The path referred by SILVERPEAS_HOME or by JBOSS_HOME doesn\'t exist or isn\'t a directory!'
      throw new IllegalStateException()
    }
    config  = project.objects.newInstance(SilverpeasConfigurationProperties, project, silverpeasHome)
    installation = project.objects.newInstance(SilverpeasInstallationProperties, project, silverpeasHome)
    migration = project.objects.newInstance(SilverpeasMigrationProperties, project, silverpeasHome)
    logging = project.objects.newInstance(SilverpeasLoggingProperties)
    timeout = project.objects.property(Long)
    timeout.set(JBossServer.DEFAULT_TIMEOUT)
    settings.context = config.context.properties()
  }

  void setSettings(final Map configProperties) {
    this.settings.putAll(configProperties)
  }

  void logging(Action<? extends SilverpeasLoggingProperties> action) {
    action.execute(logging)
  }

  void config(Action<? extends SilverpeasConfigurationProperties> action) {
    action.execute(config)
  }

  void installation(Action<? extends SilverpeasInstallationProperties> action) {
    action.execute(installation)
  }

  void migration(Action<? extends SilverpeasMigrationProperties> action) {
    action.execute(migration)
  }
}
