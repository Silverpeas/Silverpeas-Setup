package org.silverpeas.setup

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.silverpeas.setup.api.Logger
import org.silverpeas.setup.api.SilverpeasSetupService
import org.silverpeas.setup.configuration.JBossServer
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
  List<String> tasks = ['configureJBoss', 'configureSilverpeas', 'migration']

  private buildStarted = false
  private List<String> executedTasks = []

  TaskEventLogging withTasks(tasks) {
    this.tasks.addAll(tasks)
    return this
  }

  @Override
  void beforeExecute(Task task) {
    if (tasks.contains(task.name)) {
      if (!buildStarted) {
        buildStarted = true
        Logger.getLogger(DEFAULT_LOG_NAMESPACE).formatInfo('%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n',
            "SILVERPEAS SETUP: ${task.project.version}",
            "SILVERPEAS HOME:  ${task.project.silversetup.silverpeasHome}",
            "JBOSS HOME:       ${task.project.silversetup.jbossHome}",
            "JCR HOME:         ${SilverpeasSetupService.currentSettings.JCR_HOME.asPath().toString()}",
            "JAVA HOME:        ${System.getenv('JAVA_HOME')}",
            "DATABASE:         ${SilverpeasSetupService.currentSettings.DB_SERVERTYPE.toLowerCase()}",
            "OPERATING SYSTEM: ${System.getProperty('os.name')}",
            "PRODUCTION MODE:  ${!task.project.silversetup.developmentMode}")
      }
      Logger log = Logger.getLogger(task.name)
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
      Logger log = Logger.getLogger(task.name)
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
    JBossServer jboss = new JBossServer(result.gradle.rootProject.extensions.silversetup.jbossHome)
    String status = "JBoss is ${jboss.status()}"
    if (buildStarted) {
      Logger.getLogger(DEFAULT_LOG_NAMESPACE).formatInfo('\n%s\n', status)
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
