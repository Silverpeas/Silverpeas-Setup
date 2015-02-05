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
import org.gradle.api.tasks.TaskExecutionException
import org.silverpeas.setup.api.Logger

import java.util.regex.Matcher

/**
 * This task is to configure JBoss/Wildfly in order to be ready to migrate Silverpeas.
 * @author mmoquillon
 */
class JBossConfigurationTask extends DefaultTask {
  def settings
  Logger log = Logger.getLogger(this.name)
  def jboss = new JBossServer("${project.silversetup.jbossHome}")
      .redirectOutputTo(new File("${project.silversetup.logDir}/output.log"))
      .useLogger(log)

  JBossConfigurationTask() {
    description = 'Configure JBoss/Wildfly for Silverpeas'
    group = 'Build'
    dependsOn = ['assemble']
  }

  @TaskAction
  def configureJBoss() {
    setUpJVMOptions()
    installAdditionalModules()
    if (!jboss.isRunning()) {
      log.info 'JBoss not started, so start it'
      jboss.start()
    }

    String jbossConfFilesDir = "${project.buildDir}/cli"
    setUpJDBCDriver()
    generateConfigurationFilesInto(jbossConfFilesDir)
    processConfigurationFiles(jbossConfFilesDir)

  }

  private def setUpJVMOptions() {
    log.info 'JVM options setting'
    if (jboss.isRunning()) {
      jboss.stop()
    }
    new File("${project.silversetup.jbossHome}/bin").listFiles(new FilenameFilter() {
      @Override
      boolean accept(final File dir, final String name) {
        return name.endsWith('.conf') || name.endsWith('.conf.bat')
      }
    }).each { conf ->
      conf.withReader {
        it.transformLine(new FileWriter("${conf.path}.tmp")) { line ->
          Matcher matcher = line =~ /\s*JAVA_OPTS="-Xm.+/
          if (matcher.matches()) {
            line = "JAVA_OPTS=\"-Xmx${settings.JVM_RAM_MAX} -Djava.net.preferIPv4Stack=true ${settings.JVM_OPTS}\""
          }
          line
        }
      }
      File modifiedConf = new File("${conf.path}.tmp")
      modifiedConf.setReadable(conf.canRead())
      modifiedConf.setWritable(conf.canWrite())
      modifiedConf.setExecutable(conf.canExecute())
      conf.delete()
      modifiedConf.renameTo(conf)
    }
  }

  private def installAdditionalModules() {
    log.info 'Additional modules installation'
    project.copy {
      from "${project.silversetup.configurationHome}/jboss/modules"
      into "${project.silversetup.jbossHome}/modules"
    }
  }

  private def generateConfigurationFilesInto(String jbossConfDir) {
    new File("${project.silversetup.configurationHome}/jboss").listFiles(new FileFilter() {
      @Override
      boolean accept(final File aFile) {
        return aFile.isFile()
      }
    }).each { cli ->
      String[] resource = cli.name.split('\\.')
      ResourceType type = ResourceType.valueOf(resource[1])
      log.info "Prepare configuration of ${type} ${resource[0]} for Silverpeas"
      project.copy {
        from(cli) {
          filter({ line ->
            return VariableReplacement.parseExpression(line, settings)

          })
        }
        into jbossConfDir
      }
    }
  }

  private def setUpJDBCDriver() {
    log.info "Install database driver for ${settings.DB_SERVERTYPE}"
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
      try {
        jboss.deploy("${project.silversetup.driversDir}/${settings.DB_DRIVER_NAME}")
      } catch (Exception ex) {
        log.error("Error: cannot deploy ${settings.DB_DRIVER_NAME}", ex)
        throw new TaskExecutionException(this, ex.message)
      }
    }
  }

  private void processConfigurationFiles(jbossConfFilesDir) {
    new File(jbossConfFilesDir).listFiles().each { cli ->
      String[] resource = cli.name.split('\\.')
      ResourceType type = ResourceType.valueOf(resource[1])
      log.info " -> Configure ${type} ${resource[0]} for Silverpeas"
      jboss.processCommandFile(new File("${jbossConfFilesDir}/${cli.name}"),
          new File("${project.silversetup.logDir}/jboss-cli-output.log"))
      logger.debug(new File("${project.silversetup.logDir}/jboss-cli-output.log").text)
      log.info('\n')
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
