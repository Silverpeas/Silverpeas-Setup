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

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.silverpeas.setup.api.FileLogger
import org.silverpeas.setup.api.JBossServer
import org.silverpeas.setup.api.SilverpeasSetupTaskNames

/**
 * A logger hooking to events from the task execution in order to customize the output of the
 * traces coming from Gradle and to indicates in this plugin's logging system at which tasks the
 * further traces will refer.
 * @author mmoquillon
 */
class TaskEventLogging extends BuildAdapter implements TaskExecutionListener {

  private static final String DEFAULT_LOG_NAMESPACE = 'Silverpeas Setup'

  /**
   * The name of the tasks to consider in the custom output. By default, the tasks configureJBoss,
   * configureSilverpeas, and migration are supported.
   */
  List<String> tasks = SilverpeasSetupTaskNames.values().collect { it.name }

  private buildStarted = false
  private List<String> executedTasks = []
  private long startTimestamp;

  TaskEventLogging() {
    startTimestamp = System.currentTimeMillis()
  }

  TaskEventLogging withTasks(tasks) {
    this.tasks.addAll(tasks)
    return this
  }

  @Override
  void beforeExecute(Task task) {
    if (tasks.contains(task.name)) {
      if (!buildStarted) {
        buildStarted = true
        SilverpeasSetupExtension silverSetup =
            (SilverpeasSetupExtension) task.project.extensions.getByName(SilverpeasSetupPlugin.EXTENSION)
        FileLogger.getLogger(DEFAULT_LOG_NAMESPACE).formatInfo('%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n',
            "SILVERPEAS SETUP: ${task.project.version}",
            "SILVERPEAS HOME:  ${silverSetup.silverpeasHome.path}",
            "JBOSS HOME:       ${silverSetup.jbossHome.path}",
            "JCR HOME:         ${silverSetup.settings.JCR_HOME.asPath().toString()}",
            "JAVA HOME:        ${System.getenv('JAVA_HOME')}",
            "DATABASE:         ${silverSetup.settings.DB_SERVERTYPE.toLowerCase()}",
            "OPERATING SYSTEM: ${System.getProperty('os.name')}",
            "PRODUCTION MODE:  ${!silverSetup.installation.developmentMode}")
      }
      FileLogger log = FileLogger.getLogger(task.name)
      String taskTitle = unformat(task.name)
      if (!task.didWork) {
        log.info "${taskTitle}..."
        outputTask taskTitle
      } else {
        executedTasks << task.name
      }
    }
  }

  @Override
  void afterExecute(Task task, TaskState state) {
    if (tasks.contains(task.name) && !executedTasks.contains(task.name)) {
      FileLogger log = FileLogger.getLogger(task.name)
      String taskTitle = unformat(task.name)
      String status = 'OK'
      if (state.failure != null) {
        status = 'FAILURE'
        log.error state.failure
      }
      log.info "${taskTitle}: [${status}]\n"
      outputStatus status
    }
  }

  @Override
  void buildFinished(final BuildResult result) {
    JBossServer jboss = new JBossServer(result.gradle.rootProject.extensions.silversetup.jbossHome.path)
    String status = "JBoss is ${jboss.status()}"
    String buildDuration = "The whole tasks took ${(long)((System.currentTimeMillis() - startTimestamp) / 1000)}s"
    if (buildStarted) {
      FileLogger.getLogger(DEFAULT_LOG_NAMESPACE).formatInfo('\n%s\n%s\n', status, buildDuration)
      println "${buildDuration}"
      buildStarted = false
    }
    println()
    println "INFO: ${status}"
    result.rethrowFailure()
  }

  private String unformat(String name) {
    StringBuilder str = new StringBuilder()
    str.append(name.charAt(0).toUpperCase())
    for (int i = 1; i < name.length(); i++) {
      char c = name.charAt(i)
      if (c.isUpperCase() && name.charAt(i - 1).isLowerCase()) {
        str.append(' ')
      }
      str.append(c)
    }
    return str.toString()
  }

  private void outputTask(String taskTitle) {
    StringBuilder result = new StringBuilder("${taskTitle}... ")
    int charToAdd = 20 - result.length()
    for (int i = 0; i < charToAdd; i++) {
      result.append(' ')
    }
    print result.toString()
  }

  private void outputStatus(String status) {
    println "       ${status}"
  }
}
