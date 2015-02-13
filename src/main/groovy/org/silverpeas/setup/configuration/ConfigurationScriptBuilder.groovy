package org.silverpeas.setup.configuration

import org.silverpeas.setup.api.GroovyScript
import org.silverpeas.setup.api.Logger
import org.silverpeas.setup.api.Script

/**
 * A build of configuration scripts.
 * @author mmoquillon
 */
class ConfigurationScriptBuilder {

  private String scriptPath
  private Logger logger

  /**
   * Creates a script builder from the specified absolute path of a script.
   * @param scriptPath the absolute path of a script.
   * @return a migration script builder.
   */
  static ConfigurationScriptBuilder fromScript(String scriptPath) {
    if (!new File(scriptPath).exists()) {
      throw new FileNotFoundException("The script at ${scriptPath} doesn't exist!")
    }
    ConfigurationScriptBuilder builder = new ConfigurationScriptBuilder()
    builder.scriptPath = scriptPath
    return builder
  }

  ConfigurationScriptBuilder withLogger(Logger logger) {
    this.logger = logger
    return this
  }

  /**
   * Builds a script object.
   * @return an object representing a script in the migration process.
   */
  Script build() {
    String type = scriptPath.substring(scriptPath.lastIndexOf('.') + 1)
    Script script
    switch(type.toLowerCase()) {
      case 'cli':
        script = new JBossCliScript(scriptPath)
        break
      case 'xml':
        script = new XmlSettingsScript(scriptPath)
        break
      case 'groovy':
        script = new GroovyScript(scriptPath)
        break
      default:
        throw new IllegalArgumentException("Unknow script type: ${type}")
    }
    return script.useLogger(logger)
  }
}
