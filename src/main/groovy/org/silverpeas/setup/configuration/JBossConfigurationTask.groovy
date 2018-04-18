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
package org.silverpeas.setup.configuration

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.ProjectState
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.silverpeas.setup.SilverpeasSetupExtension
import org.silverpeas.setup.SilverpeasSetupPlugin
import org.silverpeas.setup.api.Logger
import org.silverpeas.setup.api.Script

import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Matcher
/**
 * A Gradle task to configure a JBoss/Wildfly instance from some CLI scripts to be ready to run
 * Silverpeas.
 * @author mmoquillon
 */
class JBossConfigurationTask extends DefaultTask {
  File driversDir
  final JBossServer jboss
  final SilverpeasSetupExtension silverSetup
  final Logger log = Logger.getLogger(this.name)

  JBossConfigurationTask() {
    description = 'Configure JBoss/Wildfly for Silverpeas'
    group = 'Build'
    onlyIf {
      Files.exists(this.driversDir.path)
    }
    silverSetup =
        (SilverpeasSetupExtension) project.extensions.getByName(SilverpeasSetupPlugin.EXTENSION)
    jboss = new JBossServer(silverSetup.jbossHome.path)
    project.afterEvaluate { Project currentProject, ProjectState state ->
      if (currentProject.silversetup.logging.useLogger) {
        jboss.redirectOutputTo(new File(currentProject.silversetup.logging.logDir, '/jboss_output.log'))
            .useLogger(log)
      }
    }
  }

  @TaskAction
  def configureJBoss() {
    try {
      if (jboss.isStartingOrRunning()) {
        jboss.stop()
      }
      setUpJVMOptions()
      installAdditionalModules()
      jboss.start(adminOnly: true) // start in admin only to perform only configuration tasks
      setUpJDBCDriver()
      processConfigurationFiles()
    } catch(Exception ex) {
      log.error 'Error while configuring JBoss/Wildfly', ex
      throw new TaskExecutionException(this, ex)
    } finally {
      jboss.stop() // stop the admin mode
    }
  }

  private def setUpJVMOptions() {
    log.info 'JVM options setting'
    new File(silverSetup.jbossHome, 'bin').listFiles(new FilenameFilter() {
      @Override
      boolean accept(final File dir, final String name) {
        return name.endsWith('.conf') || name.endsWith('.conf.bat')
      }
    }).each { conf ->
      String jvmOpts; def regexp
      if (conf.name.endsWith('.bat')) {
        jvmOpts = "set \"JAVA_OPTS=-Xmx${silverSetup.config.JVM_RAM_MAX} ${silverSetup.config.JVM_OPTS}"
        regexp = /\s*set\s+"JAVA_OPTS=-Xm.+/
      } else {
        jvmOpts = "JAVA_OPTS=\"-Xmx${silverSetup.config.JVM_RAM_MAX} -Djava.net.preferIPv4Stack=true ${silverSetup.config.JVM_OPTS}"
        regexp = /\s*JAVA_OPTS="-Xm.+/
      }
      jvmOpts += '"'
      conf.withReader {
        it.transformLine(new FileWriter("${conf.path}.tmp")) { line ->
          Matcher matcher = line =~ regexp
          if (matcher.matches()) {
            line = jvmOpts
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
      from silverSetup.jbossModulesDir.toPath()
      into Paths.get(silverSetup.jbossHome.path, 'modules')
    }
  }

  private def setUpJDBCDriver() throws Exception {
    log.info "Install database driver for ${silverSetup.config.DB_SERVERTYPE}"
    if (silverSetup.config.DB_SERVERTYPE == 'H2') {
      // H2 is already available by default in JBoss/Wildfly
      silverSetup.config.DB_DRIVER_NAME = 'h2'
    } else {
      // install the required driver other than H2
      driversDir.listFiles().each { driver ->
        if ((driver.name.startsWith('postgresql') && silverSetup.config.DB_SERVERTYPE == 'POSTGRESQL') ||
            (driver.name.startsWith('jtds') && silverSetup.config.DB_SERVERTYPE == 'MSSQL') ||
            (driver.name.startsWith('ojdbc') && silverSetup.config.DB_SERVERTYPE == 'ORACLE')) {
          silverSetup.config.DB_DRIVER_NAME = driver.name
          try {
            jboss.add(Paths.get(driversDir.path, silverSetup.config.DB_DRIVER_NAME).toString())
            jboss.deploy(silverSetup.config.DB_DRIVER_NAME)
          } catch (Exception ex) {
            log.error("Error: cannot deploy ${silverSetup.config.DB_DRIVER_NAME}", ex)
            throw ex
          }
        }
      }
    }
  }

  private void processConfigurationFiles() throws Exception {
    JBossCliScript cliScript = new JBossCliScript(
        Files.createTempFile('jboss-configuration-', '.cli').toString())
    log.info "CLI configuration scripts will be merged into ${cliScript.toFile().name}"
    Set<Script> scripts = new HashSet<>()
    silverSetup.jbossConfigurationDir.listFiles(new FileFilter() {
      @Override
      boolean accept(final File child) {
        return child.isFile()
      }
    }).each { File confFile ->
      log.info "Load configuration file ${confFile.name}"
      scripts.add(ConfigurationScriptBuilder.fromScript(confFile.path)
          .mergeOnlyIfCLIInto(cliScript)
          .build())
    }
    try {
      scripts.each { aScript ->
        aScript
            .useLogger(log)
            .useSettings(silverSetup.config)
            .run(jboss: jboss)
      }
    } catch(Exception ex) {
      log.error("Error while running cli script: ${ex.message}", ex)
      throw ex
    }
  }
}
