package org.silverpeas.setup

import org.gradle.BuildAdapter
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import org.silverpeas.setup.api.Logger
import org.silverpeas.setup.api.SilverpeasSetupService

/**
 * A logger hooking to events from the task execution in order to customize the output of the
 * traces coming from Gradle and to indicates in this plugin's logging system at which tasks the
 * further traces will refer.
 * @author mmoquillon
 */
class TaskEventLogging implements TaskExecutionListener {

  /**
   * The name of the tasks to consider in the custom output. By default, the tasks configureJBoss,
   * configureSilverpeas, and migration are supported.
   */
  List<String> tasks = ['configureJBoss', 'configureSilverpeas', 'migration']

  private buildStarted = false

  public TaskEventLogging withTasks(tasks) {
    this.tasks.addAll(tasks)
    return this
  }

  public void beforeExecute(Task task) {
    if (tasks.contains(task.name)) {
      if (!buildStarted) {
        buildStarted = true
        Logger.getLogger('Silverpeas Setup').formatInfo('%s\n%s\n%s\n%s\n%s\n%s\n',
            "SILVERPEAS SETUP: ${task.project.version}",
            "SILVERPEAS HOME:  ${task.project.silversetup.silverpeasHome}",
            "JBOSS HOME:       ${task.project.silversetup.jbossHome}",
            "JCR HOME:         ${SilverpeasSetupService.getPath(SilverpeasSetupService.currentSettings.JCR_HOME).toString()}",
            "JAVA HOME:        ${System.getenv('JAVA_HOME')}",
            "OPERATING SYSTEM: ${System.getProperty('os.name')}")
      }
      Logger log = Logger.getLogger(task.name)
      String taskTitle = unformat(task.name)
      log.info "${taskTitle}...\n"
      println "${taskTitle}..."
    }
  }

  public void afterExecute(Task task, TaskState state) {
    if (tasks.contains(task.name)) {
      Logger log = Logger.getLogger(task.name)
      String taskTitle = unformat(task.name)
      String status = '[OK]'
      if (state.failure != null) {
        status = '[FAILURE]'
        log.error state.failure
      }
      log.info "${taskTitle}: ${status}\n"
      println "${taskTitle}: ${status}"
    }
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
}
