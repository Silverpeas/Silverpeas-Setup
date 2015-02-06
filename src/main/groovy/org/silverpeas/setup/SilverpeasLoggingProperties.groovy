package org.silverpeas.setup

import org.gradle.api.logging.LogLevel

/**
 * Properties to set up the logging system used by this Gradle plugin.
 * @author mmoquillon
 */
class SilverpeasLoggingProperties {

  /**
   * The path of the directory into which the log should be generated.
   */
  String logDir

  /**
   * The default level from which the traces will be written into the log file(s).
   */
  LogLevel defaultLevel = LogLevel.INFO

  /**
   * Should the logging system of the plugin be used to output traces into log files.
   * By default, true. If false, the traces will be then output through the logging system of
   * Gradle; they will be output into the standard output (level QUIET)
   */
  boolean useLogger = true

  /**
   * Additional to its own tasks, the ones in a Gradle script the logging system of the pluging
   * should take in charge. By default, none is taken in charge; only the tasks in the plugin are
   * supported by the logging system.
   */
  List<String> scriptTasks = []
}
