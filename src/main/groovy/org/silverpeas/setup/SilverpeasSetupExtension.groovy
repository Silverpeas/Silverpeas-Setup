/*
    Copyright (C) 2000 - 2018 Silverpeas

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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
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
   * The path of the Silverpeas home directory. Defaulted with the SILVERPEAS_HOME environment
   * variable.
   */
  final File silverpeasHome

  /**
   * The path of the JBoss home directory. Defaulted with the JBOSS_HOME environment variable.
   */
  final File jbossHome

  /**
   * The properties to access the configuration if Silverpeas in order to apply it to the
   * current Silverpeas distribution.
   */
  final SilverpeasConfigurationProperties config

  /**
   * The directory in which are all located both the data source migration descriptors
   * and the scripts to create or to update the schema of the database to be used by Silverpeas.
   * It is expected to contain two kinds of subdirectories:
   * <ul>
   *   <li><em><code>modules</code></em> in which are provided the XML descriptor of each migration
   *   module. These descriptors refers the scripts to use to create or to update the
   *   database schema for a given Silverpeas module;</li>
   *   <li><em><code>db</code></em> in which are located per database type and per module the
   *   different SQL scripts to create or to upgrade the schema of the database;</li>
   *   <li><em><code>scripts</code></em> in which are located per module the different programming
   *   scripts (currently, only Groovy is supported) to perform complex tasks on the database or
   *   any other data sources used by Silverpeas (like the JCR for example).
   *   </li>
   * </ul>
   */
  final File migrationHome

  /**
   * The properties to configure the logging. They define the location of the logging file, the
   * logging level and so on.
   */
  final SilverpeasLoggingProperties logging

  /**
   * The path of the directory containing the JDBC drivers for the different data source supported
   * by Silverpeas. Currently only PostgreSQL, MS-SQLServer, and Oracle database systems are
   * supported.
   */
  final File driversDir

  /**
   * All the software bundles that made Silverpeas. Those bundles are usually downloaded from our
   * own Software Repository by the Silverpeas installer. They are required to assemble and build
   * the final Silverpeas Web Application. The Jar libraries other than the supported JDBC drivers
   * aren't taken in charge.
   */
  final ConfigurableFileCollection silverpeasBundles

  /**
   * Any tiers bundles to add into the Silverpeas Application being built. The tiers bundles are
   * processed differently by the plugin: only the JAR libraries are taken in charge.
   */
  final ConfigurableFileCollection tiersBundles

  /**
   * Is in development mode ? (In this case, some peculiar configuration are applied to support the
   * dev mode in the application server.) This is a property and hence can be set by the user input
   * from the build script.
   */
  final Property<Boolean> developmentMode

  /**
   * The actual version of Silverpeas to set up and run.
   */
  String silverpeasVersion

  /**
   * Constructs a new silverpeas configuration extension. It checks the environment variables
   * SILVERPEAS_HOME and JBOSS_HOME are correctly set.
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
    migrationHome = project.file("${silverpeasHome.path}/migrations")
    driversDir = project.file("${project.buildDir.path}/drivers")
    config  = project.objects.newInstance(SilverpeasConfigurationProperties, project, silverpeasHome)
    logging = project.objects.newInstance(SilverpeasLoggingProperties)
    silverpeasBundles = project.files()
    tiersBundles = project.files();
    developmentMode = project.objects.property(Boolean)
    developmentMode.set(false)
  }

  void setSilverpeasBundles(FileCollection bundles) {
    this.silverpeasBundles.setFrom(bundles)
  }

  void setTiersBundles(FileCollection bundles) {
    this.tiersBundles.setFrom(bundles)
  }

  void logging(Action<? extends SilverpeasLoggingProperties> action) {
    action.execute(logging)
  }

  void config(Action<? extends SilverpeasConfigurationProperties> action) {
    action.execute(config)
  }
}
