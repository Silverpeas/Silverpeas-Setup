/*
  Copyright (C) 2000 - 2026 Silverpeas

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  As a special exception to the terms and conditions of version 3.0 of
  the GPL, you may redistribute this Program in connection with Free/Libre
  Open Source Software ("FLOSS") applications as described in Silverpeas's
  FLOSS exception.  You should have received a copy of the text describing
  the FLOSS exception, and it is also available here:
  "https://www.silverpeas.org/legal/floss_exception.html"

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.silverpeas.setup

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.silverpeas.setup.api.FileLogger
import org.silverpeas.setup.api.JBossServer

/**
 * A build service listening to the completion of the tasks in order to customize the output of the
 * traces coming from Gradle and to indicate in this plugin's logging system to which task the
 * further traces will refer.
 * <p>
 * Because it is an {@link AutoCloseable} service, Gradle closes it once the build is done,
 * whatever its result. This is why it takes also in charge the output of a summary about the
 * Silverpeas setup. Doing so, this service replaces both the deprecated
 * {@code Gradle#useLogger(Object)} and {@code Gradle#buildFinished(Closure)} methods.
 * </p>
 * @author mmoquillon
 */
abstract class TaskEventLogging
    implements BuildService<TaskEventLogging.Parameters>, OperationCompletionListener, AutoCloseable {

  private static final String DEFAULT_LOG_NAMESPACE = 'Silverpeas Setup'

  /**
   * The parameters of this service. They are all set by the plugin once the project using it has
   * been evaluated.
   */
  static interface Parameters extends BuildServiceParameters {

    /**
     * The name of the tasks to consider in the custom output. By default, the tasks of the plugin
     * plus the ones declared in the <code>logging.scriptTasks</code> property of the plugin's
     * extension.
     */
    ListProperty<String> getTasks()

    /**
     * Has the execution of the tasks to be traced both in the log file of the plugin and in the
     * standard output? It is set with the <code>logging.useLogger</code> property of the plugin's
     * extension.
     */
    Property<Boolean> getTracing()

    Property<String> getSilverpeasVersion()

    Property<String> getSilverpeasHome()

    Property<String> getJbossHome()

    Property<String> getJcrHome()

    Property<String> getDatabase()

    Property<Boolean> getProductionMode()
  }

  private boolean buildStarted = false
  private final long startTimestamp = System.currentTimeMillis()

  @Override
  void onFinish(final FinishEvent event) {
    if (!(event instanceof TaskFinishEvent) || !parameters.tracing.get()) {
      return
    }
    TaskFinishEvent taskEvent = (TaskFinishEvent) event
    String taskName = taskNameOf(taskEvent)
    if (!parameters.tasks.get().contains(taskName)) {
      return
    }

    if (!buildStarted) {
      buildStarted = true
      outputSetupContext()
    }

    FileLogger log = FileLogger.getLogger(taskName)
    String taskTitle = unformat(taskName)
    String status = statusOf(taskEvent, log)
    log.info "${taskTitle}: [${status}]\n"
    outputTaskStatus(taskTitle, status)
  }

  /**
   * Once the build is done, whatever its result, outputs, if the tracing is enabled, a summary
   * about both the state of JBoss/Wildfly and the duration of the whole setup.
   */
  @Override
  void close() {
    if (!parameters.tracing.get()) {
      return
    }
    try {
      JBossServer jboss = new JBossServer(parameters.jbossHome.get())
      String status = "JBoss is ${jboss.status()}"
      String buildDuration =
          "The whole tasks took ${(long) ((System.currentTimeMillis() - startTimestamp) / 1000)}s"
      if (buildStarted) {
        FileLogger.getLogger(DEFAULT_LOG_NAMESPACE).formatInfo('\n%s\n%s\n', status, buildDuration)
        println "${buildDuration}"
        buildStarted = false
      }
      println()
      println "INFO: ${status}"
    } catch (Exception e) {
      // the summary is just informative: it mustn't mask the result of the build itself
      FileLogger.getLogger(DEFAULT_LOG_NAMESPACE)
          .warn("Cannot output the summary of the setup: ${e.message}")
    }
  }

  private void outputSetupContext() {
    String javaHome = System.getenv('JAVA_HOME')
    FileLogger.getLogger(DEFAULT_LOG_NAMESPACE).formatInfo('%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n',
        "SILVERPEAS SETUP: ${parameters.silverpeasVersion.get()}",
        "SILVERPEAS HOME:  ${parameters.silverpeasHome.get()}",
        "JBOSS HOME:       ${parameters.jbossHome.get()}",
        "JCR HOME:         ${parameters.jcrHome.get()}",
        "JAVA HOME:        ${javaHome != null ? javaHome : 'not set'}",
        "DATABASE:         ${parameters.database.get()}",
        "OPERATING SYSTEM: ${System.getProperty('os.name')}",
        "PRODUCTION MODE:  ${parameters.productionMode.get()}")
  }

  private static String taskNameOf(final TaskFinishEvent event) {
    String taskPath = event.descriptor.taskPath
    return taskPath.substring(taskPath.lastIndexOf(':') + 1)
  }

  private static String statusOf(final TaskFinishEvent event, final FileLogger log) {
    String status = 'OK'
    if (event.result instanceof TaskFailureResult) {
      status = 'FAILURE'
      ((TaskFailureResult) event.result).failures.each {
        log.error("${it.message}\n${it.description}")
      }
    } else if (event.result instanceof TaskSkippedResult) {
      status = 'SKIPPED'
    }
    return status
  }

  private static String unformat(String name) {
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

  private static void outputTaskStatus(String taskTitle, String status) {
    StringBuilder result = new StringBuilder("${taskTitle}... ")
    int charToAdd = 20 - result.length()
    for (int i = 0; i < charToAdd; i++) {
      result.append(' ')
    }
    println "${result.toString()}       ${status}"
  }
}
