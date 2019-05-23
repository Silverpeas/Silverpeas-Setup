/*
  Copyright (C) 2000 - 2018 Silverpeas

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  As a special exception to the terms and conditions of version 3.0 of
  the GPL, you may redistribute this Program in connection with Free/Libre
  Open Source Software ("FLOSS") applications as described in Silverpeas's
  FLOSS exception.  You should have recieved a copy of the text describing
  the FLOSS exception, and it is also available here:
  "http://www.silverpeas.org/docs/core/legal/floss_exception.html"

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.setup.api

import org.gradle.api.tasks.StopExecutionException

import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher

/**
 * A service providing high-level or added-value functions for both the tasks defined by this
 * plugin and the Groovy scripts invoked within the execution of those tasks.
 *
 * @author mmoquillon
 */
class SilverpeasSetupService {

  private static final def VAR_PATTERN = /\$\{(env\.|sys\.)?(\w+)\}/
  private static final def SCRIPT_PATTERN = /\$\{eval:([:.\w \{\}\$]+)\}/
  // pattern defining the grammar of a property in a properties file and in which we capture the key
  private static final def PROPERTY_PATTERN = /^\s*([\w\d._-]+)\s*=\s*[\S+\s*]*$/

  final Map settings

  /**
   * Create a new Silverpeas setup service with the specified Silverpeas settings.
   * @param settings a Map instance with all the global settings for the installation and
   * configuration of Silverpeas
   */
  SilverpeasSetupService(Map settings) {
    Objects.requireNonNull(settings)
    this.settings = settings
  }

  /**
   * Updates the specified properties file by replacing each property value by those specified in
   * the given properties and by adding those not defined in the properties file.
   * @param propertiesFilePath the path of the properties file.
   * @param properties the properties to put into the file.
   */
  void updateProperties(String propertiesFilePath, properties) {
    def existingProperties = []
    FileWriter updatedPropertiesFile = new FileWriter(propertiesFilePath + '.tmp')
    new FileReader(propertiesFilePath).transformLine(updatedPropertiesFile) { line ->
      Matcher m = (line =~ PROPERTY_PATTERN)
      if (m.matches()) {
        // it is a property definition, then capture the key
        String currentKey = m.group(1)
        def property = properties.find { key, value -> key == currentKey }
        if (property != null) {
          existingProperties << property.key
          String value = normalizePropsValue(property.value)
          line = line.replaceFirst('=.*', "=  ${value}")
        }
      }
      line
    }
    new FileWriter(propertiesFilePath + '.tmp', true).withWriter { writer ->
      writer.println()
      properties.findAll({ key, value -> !existingProperties.contains(key) }).each { key, value ->
        writer.println("${key} = ${value.trim().replaceAll('\\\\', '\\\\\\\\')}")
      }
    }
    def template = new File(propertiesFilePath)
    def propertiesFile = new File(propertiesFilePath + '.tmp')
    propertiesFile.setReadable(template.canRead())
    propertiesFile.setWritable(template.canWrite())
    propertiesFile.setExecutable(template.canExecute())
    template.delete()
    propertiesFile.renameTo(template)
  }

  /**
   * Expanses any variable declaration by their value in the specified expression.
   *
   * <p>
   * A variable is expected to be declared in the expression in the following way:
   * </p>
   * <pre><code>${VARIABLE}</code></pre>
   * <p>with VARIABLE as a variable declaration. According to a well-defined prefix, the replacement
   * computation can be customized:</p>
   * <ul>
   *   <li><em>No prefix</em>: the variable declaration will be replaced by its value from the
   *   Silverpeas settings; these settings come from both the default and the customer configuration
   *   properties.</li>
   *   <li><em>Prefixed by <code>env.</code></em>: the variable is expected to be an environment
   *   variable.</li>
   *   <li><em>Prefixed by <code>sys.</code></em>: the variable is expected to be a system property.
   *   </li>
   * </ul>
   * <p>If no value exists for the variable or if a script evaluation fails, then an exception is
   * thrown.</p>
   * @param expression the expression in which any variable declarations should be replaced by their
   * value.
   * @return the new expression as the result of the variable replacement.
   */
  String expanseVariables(String expression) {
    def matching = expression =~ VAR_PATTERN
    matching.each { token ->
      switch (token[1]) {
        case 'sys.':
          if (!SystemWrapper.getProperty(token[2])) {
            println "Error: no such system property ${token[2]}"
            throw new StopExecutionException("Error: no such system property ${token[2]}")
          }
          expression = expression.replace(token[0], normalizePath(SystemWrapper.getProperty(token[2])))
          break
        case 'env.':
          if (!SystemWrapper.getenv(token[2])) {
            println "Error: no such environment variable ${token[2]}"
            throw new StopExecutionException("Error: no such environment variable ${token[2]}")
          }
          expression = expression.replace(token[0], normalizePath(SystemWrapper.getenv(token[2])))
          break
        default:
          if (settings[token[2]] == null) {
            println "Error: no such variable ${token[2]}"
            throw new StopExecutionException("Error: no such variable ${token[2]}")
          }
          expression = expanseVariables(expression.replace(token[0], settings[token[2]]))
          break
      }
    }
    return expression
  }

  /**
   * Creates the directory at the specified path if it doesn't already exists. If its parent doesn't
   * exist they are then created.
   * It does nothing if the directory already exist.
   * @param path the path of the directory to create.
   * @param attributes a map defining the attributes of the directory. Supported keys (attributes):
   * readable, writable, executable, and hidden. The hidden attribute is only supported under
   * Windows; in other operating systems any files prefixed by a point is marked as hidden.
   * @return the path of the created directory.
   */
  Path createDirectory(Path path, def attributes) {
    Path dirPath = path
    if (!Files.exists(dirPath)) {
      dirPath = Files.createDirectories(path)
      File dir = dirPath.toFile()
      if (attributes?.containsKey('readable')) {
        dir.setReadable(attributes.readable)
      }
      if (attributes?.containsKey('writable')) {
        dir.setWritable(attributes.writable)
      }
      if (attributes?.containsKey('executable')) {
        dir.setExecutable(attributes.executable)
      }
      if (attributes?.containsKey('hidden')) {
        if (System.properties['os.name'].toLowerCase().contains('windows')) {
          Files.setAttribute(dirPath, 'dos:hidden', attributes.hidden)
        }
      }
    }
    return dirPath
  }

  /**
   * Gets a logger for the specified namespace. This logger will output any traces into a log file
   * @param namespace the namespace under which any traces will be written.
   * @return the logger.
   */
  FileLogger getLogger(String namespace) {
    return FileLogger.getLogger(namespace)
  }

  private static String normalizePath(String path) {
    return path.replace('\\', '/');
  }

  private static String normalizePropsValue(String value) {
    StringBuilder replacement = new StringBuilder()
    int i = 0
    while(i < value.length()) {
      char entry = value.charAt(i)
      if (entry == '\\' && i < value.length() && value.charAt(i + 1).toLowerCase() != 'u') {
        i++
        replacement.append('\\').append(entry)
      } else if (entry == '\r') {
        replacement.append('\\ \r\n')
        i += 2
      } else if (entry == '\n') {
        replacement.append('\\ \n')
        i++
      } else {
        replacement.append(entry)
        i++
      }
    }
    return Matcher.quoteReplacement(replacement.toString()).trim()
  }

}
