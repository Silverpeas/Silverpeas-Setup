/*
  Copyright (C) 2000 - 2024 Silverpeas

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  As a special exception to the terms and conditions of version 3.0 of
  the GPL, you may redistribute this Program in connection with Free/Libre
  Open Source Software ("FLOSS") applications as described in Silverpeas's
  FLOSS exception.  You should have recieved a copy of the text describing
  the FLOSS exception, and it is also available here:
  "https://www.silverpeas.org/legal/floss_exception.html"

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
          cliScript.toFile() << new File(scriptPath).text + '\n'
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
