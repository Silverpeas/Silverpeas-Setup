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
import org.silverpeas.setup.SilverpeasConfigurationProperties
import org.silverpeas.setup.SilverpeasLoggingProperties
import org.silverpeas.setup.api.FileLogger
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
  File jbossHome
  SilverpeasConfigurationProperties config
  SilverpeasLoggingProperties logConfig
  JBossServer jboss
  final FileLogger log = FileLogger.getLogger(this.name)

  JBossConfigurationTask() {
    description = 'Configure JBoss/Wildfly for Silverpeas'
    group = 'Build'
    onlyIf {
      precondition()
    }

    project.afterEvaluate { Project currentProject, ProjectState state ->
      if (state.executed) {
        initializeJBossServer()
      }
    }
  }

  boolean precondition() {
    project.buildDir.exists() && Files.exists(this.driversDir.toPath())
  }

  void initializeJBossServer() {
    jboss = new JBossServer(jbossHome.path)
    if (logConfig.useLogger) {
      jboss.redirectOutputTo(new File(logConfig.logDir, 'jboss_output.log'))
          .useLogger(log)
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
    new File(jbossHome, 'bin').listFiles(new FilenameFilter() {
      @Override
      boolean accept(final File dir, final String name) {
        return name.endsWith('.conf') || name.endsWith('.conf.bat')
      }
    }).each { conf ->
      String jvmOpts; def regexp
      if (conf.name.endsWith('.bat')) {
        jvmOpts = "set \"JAVA_OPTS=-Xmx${config.settings.JVM_RAM_MAX} ${config.settings.JVM_OPTS}"
        regexp = /\s*set\s+"JAVA_OPTS=-Xm.+/
      } else {
        jvmOpts =
            "JAVA_OPTS=\"-Xmx${config.settings.JVM_RAM_MAX} -Djava.net.preferIPv4Stack=true ${config.settings.JVM_OPTS}"
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
      it.from config.jbossModulesDir.toPath()
      it.into Paths.get(jbossHome.path, 'modules')
    }
  }

  private def setUpJDBCDriver() throws Exception {
    log.info "Install database driver for ${config.settings.DB_SERVERTYPE}"
    if (config.settings.DB_SERVERTYPE == 'H2') {
      // H2 is already available by default in JBoss/Wildfly
      config.settings.DB_DRIVER_NAME = 'h2'
    } else {
      // install the required driver other than H2
      driversDir.listFiles().each { driver ->
        if ((driver.name.startsWith('postgresql') && config.settings.DB_SERVERTYPE == 'POSTGRESQL') ||
            (driver.name.startsWith('jtds') && config.settings.DB_SERVERTYPE == 'MSSQL') ||
            (driver.name.startsWith('ojdbc') && config.settings.DB_SERVERTYPE == 'ORACLE')) {
          config.settings.DB_DRIVER_NAME = driver.name
          try {
            jboss.add(Paths.get(driversDir.path, config.settings.DB_DRIVER_NAME).toString())
            jboss.deploy(config.settings.DB_DRIVER_NAME)
          } catch (Exception ex) {
            log.error("Error: cannot deploy ${config.settings.DB_DRIVER_NAME}", ex)
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
    config.jbossConfigurationDir.listFiles(new FileFilter() {
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
            .useSettings(config.settings)
            .run(jboss: jboss)
      }
    } catch(Exception ex) {
      log.error("Error while running cli script: ${ex.message}", ex)
      throw ex
    }
  }
}
