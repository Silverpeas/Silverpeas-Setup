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

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher

/**
 * A service providing high-level or added-value functions for both the tasks defined by this
 * plugin and the Groovy scripts invoked within the execution of those tasks.
 * @author mmoquillon
 */
class SilverpeasSetupService {

  private static final def ENV_VAR_PATTERN = /\$\{(env|sys)\.(\w+)\}/

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
    def matching = path =~ ENV_VAR_PATTERN
    matching.each { token ->
      if (token[1] == 'sys') {
        path = path.replace(token[0], SystemWrapper.getProperty(token[2]))
      } else if (token[1] == 'env') {
        path = path.replace(token[0], SystemWrapper.getenv(token[2]))
      }
    }
    return Paths.get(path)
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
          line = line.replaceFirst('=.*',"=  ${Matcher.quoteReplacement(value)}")
        }
      }
      line
    }
    new FileWriter(propertiesFilePath + '.tmp', true).withWriter { writer ->
      writer.println()
      properties.findAll({ key, value -> !existingProperties.contains(key) }).each { key, value ->
        writer.println("${key} = ${value}")
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

}
