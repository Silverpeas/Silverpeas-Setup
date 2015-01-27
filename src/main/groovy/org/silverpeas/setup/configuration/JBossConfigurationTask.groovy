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
package org.silverpeas.setup.configuration

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * This task is to configure JBoss/Wildfly in order to be ready to migrate Silverpeas.
 * @author mmoquillon
 */
class JBossConfigurationTask extends DefaultTask {
  def settings
  def jboss = new JBossServer("${project.silversetup.jbossHome}")
      .redirectOutputTo(new File("${project.silversetup.logDir}/output.log"))

  JBossConfigurationTask() {
    description = 'Configure JBoss/Wildfly for Silverpeas'
    group = 'Build'
    dependsOn = ['assemble']
    outputs.upToDateWhen {
      return jboss.isAlreadyConfigured()
    }
  }

  @TaskAction
  def configureJBoss() {
    if (!jboss.isRunning()) {
      println 'JBoss not started, so start it'
      jboss.start()
    }

    String jbossConfFilesDir = "${project.buildDir}/cli"
    setUpJDBCDriver()
    installAdditionalModules()
    generateConfigurationFilesInto(jbossConfFilesDir)
    processConfigurationFiles(jbossConfFilesDir)
    println 'Stop JBoss now'
    jboss.stop()
  }

  private def setUpDriver() {

  }

  private def installAdditionalModules() {
    println 'Additional modules installation'
    project.copy {
      from "${project.silversetup.modulesDir}/jboss"
      into "${project.silversetup.jbossHome}/modules"
    }
  }

  private def generateConfigurationFilesInto(String jbossConfDir) {
    new File("${project.silversetup.configurationHome}/jboss").listFiles().each { cli ->
      String[] resource = cli.name.split('\\.')
      ResourceType type = ResourceType.valueOf(resource[1])
      println "Prepare configuration of ${type} ${resource[0]} for Silverpeas"
      project.copy {
        from(cli) {
          filter({ line ->
            return VariableReplacement.parseValue(line, settings)

          })
        }
        into jbossConfDir
      }
    }
  }

  private def setUpJDBCDriver() {
    println "Install database driver for ${settings.DB_SERVERTYPE}"
    new File(project.silversetup.driversDir).listFiles().each { driver ->
      if ((driver.name.startsWith('postgresql') && settings.DB_SERVERTYPE == 'POSTGRESQL') ||
          (driver.name.startsWith('jtds') && settings.DB_SERVERTYPE == 'MSSQL') ||
          (driver.name.startsWith('ojdbc') && settings.DB_SERVERTYPE == 'ORACLE') ||
          (driver.name.startsWith('h2') && settings.DB_SERVERTYPE == 'H2')) {
        settings.DB_DRIVER_NAME = driver.name
      }
    }
    // H2 is already available by default in JBoss/Wildfly
    if (settings.DB_SERVERTYPE != 'H2') {
      project.copy {
        from "${project.silversetup.driversDir}/${settings.DB_DRIVER_NAME}"
        into "${project.silversetup.jbossHome}/standalone/deployments"
      }
    }
  }

  private void processConfigurationFiles(jbossConfFilesDir) {
    new File(jbossConfFilesDir).listFiles().each { cli ->
      String[] resource = cli.name.split('\\.')
      ResourceType type = ResourceType.valueOf(resource[1])
      println "Configure ${type} ${resource[0]} for Silverpeas"
      jboss.processCommandFile(new File("${jbossConfFilesDir}/${cli.name}"),
          new File("${project.silversetup.logDir}/configuration-jboss.log"))
      println()
    }
  }

  private enum ResourceType {
    ra('resource adapter'),
    ds('database'),
    dl('deployment location'),
    sys('subsystem')

    private String type;

    protected ResourceType(String type) {
      this.type = type;
    }

    @Override
    String toString() {
      return type;
    }
  }
}
