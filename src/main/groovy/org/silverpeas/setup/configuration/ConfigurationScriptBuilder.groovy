package org.silverpeas.setup.configuration

import org.silverpeas.setup.api.GroovyScript
import org.silverpeas.setup.api.Script
/**
 * A build of configuration scripts.
 * @author mmoquillon
 */
class ConfigurationScriptBuilder {

  private String scriptPath
  private JBossCliScript cliScript

  /**
   * Creates a script builder from the specified absolute path of a script.
   * @param scriptPath the absolute path of a script.
   * @return a configuration script builder.
   */
  static ConfigurationScriptBuilder fromScript(String scriptPath) {
    if (!new File(scriptPath).exists()) {
      throw new FileNotFoundException("The script at ${scriptPath} doesn't exist!")
    }
    ConfigurationScriptBuilder builder = new ConfigurationScriptBuilder()
    builder.scriptPath = scriptPath
    return builder
  }

  /**
   * Instead to build a Script object from the given CLI script path, merges finally the content of
   * latter into the specified CLI script <code>destination</code> and returns it.
   * @param destination a JBoss CLI script into which will be merge the content of the script given
   * by its path at this ConfigurationScriptBuilder construction.
   * @return itself.
   */
  ConfigurationScriptBuilder mergeOnlyIfCLIInto(JBossCliScript destination) {
    File scriptFile = destination.toFile()
    if (!scriptFile.exists()) {
      scriptFile.createNewFile()
    }
    this.cliScript = destination
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
        if (cliScript) {
          cliScript.toFile() << new File(scriptPath).text
          script = cliScript
        } else {
          script = new JBossCliScript(scriptPath)
        }
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
    return script
  }
}
