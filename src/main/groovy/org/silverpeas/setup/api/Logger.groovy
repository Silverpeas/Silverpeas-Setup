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
   * Writes out in a new line the specified message as an INFORMATION level.
   * @param msg the message to output.
   * @return itself.
   */
  Logger info(String msg) {
    return formatInfo("\n${msgHeading()} %s", msg)
  }

  /**
   * Writes out as an INFORMATION level a raw message in the specified format and with the given
   * arguments.
   * @param format the format of the message to output.
   * @param args the different arguments to put in the message format.
   * @return itself.
   */
  Logger formatInfo(String format, Object... args) {
    if (this.level <= LogLevel.INFO) {
      formatMsg(format, args)
    }
    return this
  }

  /**
   * Writes out in a new line the specified message as an DEBUG level.
   * @param msg the message to output.
   * @return itself.
   */
  Logger debug(String msg) {
    return formatDebug("\n${msgHeading()} %s", msg)
  }

  /**
   * Writes out as a DEBUG level a raw message in the specified format and with the given arguments.
   * @param format the format of the message to output.
   * @param args the different arguments to put in the message format.
   * @return itself.
   */
  Logger formatDebug(String format, Object... args) {
    if (this.level <= LogLevel.DEBUG) {
      formatMsg(format, args)
    }
    return this
  }

  /**
   * Writes out in a new line the specified message as a WARNING level.
   * @param msg the message to output.
   * @return itself.
   */
  Logger warn(String msg) {
    return formatWarn("\n${msgHeading()} %s", msg)
  }

  /**
   * Writes out as a WARNING level a raw message in the specified format and with the given
   * arguments.
   * @param format the format of the message to output.
   * @param args the different arguments to put in the message format.
   * @return itself.
   */
  Logger formatWarn(String format, Object... args) {
    if (this.level <= LogLevel.WARN) {
      formatMsg(format, args)
    }
    return this
  }

  /**
   * Writes out in a new line the specified message as an ERROR level.
   * @param msg the message to output.
   * @return itself.
   */
  Logger error(String msg) {
    return formatError("\n${msgHeading()} %s", msg)
  }

  /**
   * Writes out in new lines the specified cause as an ERROR level.
   * @param cause the exception as the cause of the error.
   * @return itself.
   */
  Logger error(Throwable cause) {
    StringWriter stackTrace = new StringWriter()
    cause.printStackTrace(new PrintWriter(stackTrace))
    return formatError("\n${msgHeading()} %s\n%s", cause.getMessage(), stackTrace.toString())
  }

  /**
   * Writes out in new lines the specified message as an ERROR level.
   * @param msg the message to output.
   * @param cause the exception as the cause of the error.
   * @return itself.
   */
  Logger error(String msg, Throwable cause) {
    StringWriter stackTrace = new StringWriter()
    cause.printStackTrace(new PrintWriter(stackTrace))
    return formatError("\n${msgHeading()} %s\n%s\n%s", msg, cause.getMessage(), stackTrace.toString())
  }

  /**
   * WWrites out as an ERROR level a raw message in the specified format and with the given
   * arguments.
   * @param format the format of the message to output.
   * @param args the different arguments to put in the message format.
   * @return itself.
   */
  Logger formatError(String format, Object... args) {
    if (this.level <= LogLevel.ERROR) {
      formatMsg(format, args)
    }
    return this
  }

  private String msgHeading() {
    return "[${namespace}]"
  }

  private void formatMsg(String format, Object... args) {
    Formatter formatter = new Formatter()
    file << formatter.format(format, args).toString()
  }

  private Logger(String namespace) {
    this.namespace = namespace
  }
}
