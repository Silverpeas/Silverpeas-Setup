package org.silverpeas.setup

import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.silverpeas.setup.api.Logger

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

  public TaskEventLogging withTasks(tasks) {
    this.tasks.addAll(tasks)
    return this
  }

  public void beforeExecute(Task task) {
    if (tasks.contains(task.name)) {
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
      if (c.isUpperCase()) {
        str.append(' ')
      }
      str.append(c)
    }
    return str.toString()
  }
}
