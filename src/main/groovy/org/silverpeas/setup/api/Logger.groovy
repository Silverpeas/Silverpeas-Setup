package org.silverpeas.setup.api

import org.gradle.api.logging.LogLevel

/**
 * The logger to be used in the plugin execution context to trace processing information into a
 * log file.
 * </p>
 * This logger is defined because Gradle vampires the logging system to redirect any traces into
 * the standard outputs and doesn't provide no configuration to define new appenders or to override
 * this logging behaviour.
 * </p>
 * If this logger isn't initialized by invoking the {@code Logger#init(File, LogLevel)} static
 * method, then by default the traces will be written into the standard output with as level INFO.
 * @author mmoquillon
 */
class Logger {

  private static def file = System.out
  private static LogLevel defaultLevel = LogLevel.INFO
  private LogLevel level = defaultLevel
  private static Logger currentLogger
  private String namespace

  /**
   * Initialises the logging system of the plugin by specifying the default log file into which
   * the traces will be written and the default log level from which the traces will be really
   * output.
   * @param logFile the default log file to use to write the traces.
   * @param level the default level from which any traces will be output.
   */
  static void init(File logFile, LogLevel level) {
    file = logFile
    defaultLevel = level
  }

  /**
   * Gets a logger for the specified namespace or scopes of traces.
   * @param namespace the namespace within which the traces to output has to belong.
   * @return the logger matching the specified namespace.
   */
  static Logger getLogger(String namespace) {
    return new Logger(namespace)
  }

  /**
   * Gets the current logging level.
   * @return the logging level.
   */
  LogLevel level() {
    return this.level
  }

  /**
   * Sets a new logging level to this logger.
   * @param newLevel the new logging level to set.
   * @return itself.
   */
  Logger level(LogLevel newLevel) {
    this.level = newLevel
    return this
  }

  /**
   * Writes out the specified message as an INFORMATION level.
   * @param msg the message to output.
   * @return itself.
   */
  Logger info(String msg) {
    if (this.level <= LogLevel.INFO) {
      file << "[${namespace}] ${msg}\n"
    }
    return this
  }

  /**
   * Writes out the specified message as an DEBUG level.
   * @param msg the message to output.
   * @return itself.
   */
  Logger debug(String msg) {
    if (this.level <= LogLevel.DEBUG) {
      file << "[${namespace}] ${msg}\n"
    }
    return this
  }

  /**
   * Writes out the specified message as an WARNING level.
   * @param msg the message to output.
   * @return itself.
   */
  Logger warn(String msg) {
    if (this.level <= LogLevel.WARN) {
      file << "[${namespace}] ${msg}\n"
    }
    return this
  }

  /**
   * Writes out the specified message as an ERROR level.
   * @param msg the message to output.
   * @return itself.
   */
  Logger error(String msg) {
    if (this.level <= LogLevel.ERROR) {
      file << "[${namespace}] ${msg}\n"
    }
    return this
  }

  /**
   * Writes out the specified cause as an ERROR level.
   * @param cause the exception as the cause of the error.
   * @return itself.
   */
  Logger error(Throwable cause) {
    if (this.level <= LogLevel.ERROR) {
      StringWriter stackTrace = new StringWriter()
      cause.printStackTrace(new PrintWriter(stackTrace))
      file << "[${namespace}] ${cause.getMessage()}\n${stackTrace}\n"
    }
    return this
  }

  /**
   * Writes out the specified message as an ERROR level.
   * @param msg the message to output.
   * @param cause the exception as the cause of the error.
   * @return itself.
   */
  Logger error(String msg, Throwable cause) {
    if (this.level <= LogLevel.ERROR) {
      StringWriter stackTrace = new StringWriter()
      cause.printStackTrace(new PrintWriter(stackTrace))
      file << "[${namespace}] ${msq}\n${cause.getMessage()}\n${stackTrace}\n"
    }
    return this
  }

  private Logger(String namespace) {
    this.namespace = namespace
  }
}
