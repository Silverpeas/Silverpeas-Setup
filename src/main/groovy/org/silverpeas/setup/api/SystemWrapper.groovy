package org.silverpeas.setup.api

/**
 * The system wrapper wraps the actual System class in order to perform additional treatments on
 * some of the System's methods.
 * @author mmoquillon
 */
abstract class SystemWrapper {

  /**
   * Gets the value of the specified environment variable. As the Silverpeas Setup tool provides a
   * way to override an environment variable with the system property of the same name, it first
   * checks if the variable exists as a system property before looking for among the environment
   * variables of the OS.
   * @param env the environment variable.
   * @return the value of the specified environment variable.
   */
  static String getenv(String env) {
    String value = System.getProperty(env)
    if (!value) {
      value = System.getenv(env)
    }
    return value
  }

  /**
   * @see System#getProperty(java.lang.String)
   */
  static String getProperty(String property) {
    return System.getProperty(property)
  }

  /**
   * @see System#getProperty(java.lang.String, java.lang.String)
   */
  static String getProperty(String property, String defaultValue) {
    return System.getProperty(property, defaultValue)
  }

  /**
   * @see System#setProperty(java.lang.String, java.lang.String)
   */
  static String setProperty(String property, String value) {
    return System.setProperty(property, value)
  }

}
