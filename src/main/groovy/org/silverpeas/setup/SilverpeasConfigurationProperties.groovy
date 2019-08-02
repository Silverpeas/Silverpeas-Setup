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

import org.gradle.api.Project

import javax.inject.Inject
import java.nio.file.Files

/**
 * Properties for the configuration of Silverpeas. Such properties include the location of the
 * configuration directories, the location of the Silverpeas data, and so on. Among those properties
 * some of them are defined by the <code>config.properties</code> global configuration file.
 * @author mmoquillon
 */
class SilverpeasConfigurationProperties {

  /**
   * The home configuration directory of Silverpeas. It should contain both the global
   * configuration properties, the Silverpeas and the JBoss configuration directory. It is expected
   * to contain two subdirectories:
   * <ul>
   *   <li><code>jboss</code> with the scripts to configure JBoss/Wildfly for Silverpeas;</li>
   *   <li><code>silverpeas</code> with the scripts to configure Silverpeas itself.</li>
   * </ul>
   * By default, the configuration home directory is expected to be
   * <code>SILVERPEAS_HOME/configuration</code> where <code>SILVERPEAS_HOME</code> is the
   * Silverpeas installation directory.
   */
  File configurationHome

  /**
   * The directory that contains all the configuration scripts to configure JBoss/Wildfly for
   * Silverpeas.
   */
  final File jbossConfigurationDir

  /**
   * The directory that contains all the configuration scripts to configure specifically the
   * Silverpeas web portal and components.
   */
  final File silverpeasConfigurationDir

  /**
   * The directory that contains the additional JBoss/Wildfly modules to install in JBoss/Wildfy
   * for Silverpeas
   */
  final File jbossModulesDir

  /**
   * The Silverpeas settings as defined in the <code>config.properties</code> file.
   * Some of them are computed from the properties in the file <code>config.properties</code>.
   */
  final Map settings = [:]

  /**
   * Context of a configuration process of Silverpeas: it is a set of context properties left to the
   * discretion of the different steps executed in the configuration. The context is serialized so
   * that it can be retrieved in the next configuration process by the different steps so that they
   * can adapt their behaviour according to the properties they have set.
   */
  final Context context

  @Inject
  SilverpeasConfigurationProperties(Project project, File silverpeasHome) {
    configurationHome = project.file("${silverpeasHome.path}/configuration")
    jbossConfigurationDir = project.file("${silverpeasHome.path}/configuration/jboss")
    silverpeasConfigurationDir = project.file("${silverpeasHome.path}/configuration/silverpeas")
    jbossModulesDir = project.file("${jbossConfigurationDir.path}/modules")
    context = new Context(configurationHome, settings)
  }

  void setSettings(final Map configProperties) {
    this.settings.putAll(configProperties)
  }

  /**
   * Context of a configuration process. It sets in the settings variable (that is shared by all
   * the steps implied within a configuration of Silverpeas) the peculiar <code>context</code>
   * attribute that is a dictionary of all the context properties the steps can set for their usage.
   */
  static class Context {
    final private File file
    final private Map props = [:]

    /**
     * Constructs a new configuration context.
     * @param storageDir the directory into which the context will be saved.
     * @param settings the dictionary to use to store the context properties. A peculiar
     * <code>context</code> key will be put with as values a Map object.
     */
    private Context(File storageDir, final Map settings) {
      file = new File(storageDir, '.context')
      settings.context = props
      if (Files.exists(file.toPath())) {
        file.text.eachLine { line ->
          String[] keyValue = line.trim().split(':')
          props[keyValue[0].trim()] = keyValue[1].trim()
        }
      }
    }

    void save() {
      if (!Files.exists(file.toPath())) {
        Files.createFile(file.toPath())
      }
      file.withWriter('UTF-8') { w ->
        props.each { k, v ->
          w.println("${k}: ${v}")
        }
      }
    }
  }
}
