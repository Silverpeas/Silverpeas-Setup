package org.silverpeas.setup.api

/**
 * A script to be executed by a Gradle task.
 * @author mmoquillon
 */
interface Script {

  /**
   * Runs this script with the specified arguments.
   * @param args the arguments to pass to the script. Either an array or a Map of variables in
   * which the key is the variable name and the value its value.
   * @throws RuntimeException if an error occurs during the execution of the script.
   */
  public void run(def args) throws RuntimeException

  /**
   * Uses the specified logger to trace this script execution.
   * @param logger a logger.
   * @return itself.
   */
  public <T extends Script> T useLogger(Logger logger)

}
