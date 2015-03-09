/*
  Copyright (C) 2000 - 2015 Silverpeas

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
import java.nio.file.Paths
import java.util.regex.Matcher

/**
 * A service providing high-level or added-value functions for both the tasks defined by this
 * plugin and the Groovy scripts invoked within the execution of those tasks.
 * @author mmoquillon
 */
class SilverpeasSetupService {

  private static final def VAR_PATTERN = /\$\{(env\.|sys\.)?(\w+)\}/
  private static final def SCRIPT_PATTERN = /\$\{eval:([:.\w \{\}\$]+)\}/

  /**
   * The current Silverpeas settings from both the customer configuration properties and the
   * default configuration properties.
   */
  static def currentSettings = [:]

  /**
   * Gets a Path object from the specified file or directory path. The difference with the
   * {@code Paths#getPath(String)} method is that it supports the system properties or environment
   * variables in the specified path. If the given path contains system or environment variables,
   * then they are replaced by their value.
   * @param path the path of a file or a directory.
   * @return the Path instance representing the specified file/directory path.
   */
  static final Path getPath(String path) {
    return Paths.get(expanseVariables(path))
  }

  /**
   * Updates the specified properties file by replacing each property value by those specified in
   * the given properties and by adding those not defined in the properties file.
   * @param propertiesFilePath the path of the properties file.
   * @param properties the properties to put into the file.
   */
  static final void updateProperties(propertiesFilePath, properties) {
    def existingProperties = []
    FileWriter updatedPropertiesFile = new FileWriter(propertiesFilePath + '.tmp')
    new FileReader(propertiesFilePath).transformLine(updatedPropertiesFile) { line ->
      properties.each() { key, value ->
        if (line.contains(key)) {
          existingProperties << key
          line = line.replaceFirst('=.*',"=  ${Matcher.quoteReplacement(value).trim()}")
        }
      }
      line
    }
    new FileWriter(propertiesFilePath + '.tmp', true).withWriter { writer ->
      writer.println()
      properties.findAll({ key, value -> !existingProperties.contains(key) }).each { key, value ->
        writer.println("${key} = ${value.trim()}")
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
  static final String expanseVariables(String expression) {
    def matching = expression =~ VAR_PATTERN
    matching.each { token ->
      switch (token[1]) {
        case 'sys.':
          if (!SystemWrapper.getProperty(token[2])) {
            println "Error: no such system property ${token[2]}"
            throw new StopExecutionException("Error: no such system property ${token[2]}")
          }
          expression = expression.replace(token[0], SystemWrapper.getProperty(token[2]))
          break
        case 'env.':
          if (!SystemWrapper.getenv(token[2])) {
            println "Error: no such environment variable ${token[2]}"
            throw new StopExecutionException("Error: no such environment variable ${token[2]}")
          }
          expression = expression.replace(token[0], SystemWrapper.getenv(token[2]))
          break
        default:
          if (currentSettings[token[2]] ==  null) {
            println "Error: no such variable ${token[2]}"
            throw new StopExecutionException("Error: no such variable ${token[2]}")
          }
          expression = expanseVariables(expression.replace(token[0], currentSettings[token[2]]))
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
  static final Path createDirectory(Path path, def attributes) {
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
  static final Logger getLogger(String namespace) {
    return Logger.getLogger(namespace)
  }

}
