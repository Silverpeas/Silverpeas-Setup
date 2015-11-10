package org.silverpeas.setup.api

/**
 * A script to be executed by a Gradle task.
 * @author mmoquillon
 */
interface Script {

  /**
   * Runs this script with the specified arguments.
   * @param args the arguments to pass to the script. It is a Map of variables in
   * which the key is the variable name and the value its value.
   * @throws RuntimeException if an error occurs during the execution of the script.
   */
  public void run(Map args) throws RuntimeException

  /**
   * Uses the specified logger to trace the execution of this script.
   * @param logger a logger.
   * @return itself.
   */
  public <T extends Script> T useLogger(Logger logger)

  /**
   * Uses the specified settings to parameterize the execution of this script.
   * @param settings a collection of key-value pairs defining all the settings.
   * @return itself.
   */
  public <T extends Script> T useSettings(Map settings)

}
