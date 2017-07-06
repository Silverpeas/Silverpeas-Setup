/*
  Copyright (C) 2000 - 2017 Silverpeas

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
package org.silverpeas.setup.configuration

import org.gradle.api.tasks.StopExecutionException

/**
 * A replacement of any variable declarations by their value obtained from a map of key-values.
 * The environment variables and the system properties aren't taken in charge; they won't be then
 * replaced by their value. If you need to expanse also the environment variables and the system
 * properties, use then the
 * {@code org.silverpeas.setup.api.SilverpeasSetupService#expanseVariables(String)} method.
 * @author mmoquillon
 */
class VariableReplacement {

  private static final def VARIABLE_PATTERN = /\$\{(\w+)\}/

  /**
   * Parses the values of the specified parameters and for each of them, replace any variable
   * declaration by the variable value from the specified variables. If a variable, present in
   * the parameter value, isn't among the given variables, then an exception is thrown.
   * @param parameters a map of key-values whose the key is the parameter name and the value is the
   * parameter value.
   * @param variables a map of key-value whose the key is a variable identifier and the value the
   * variable value.
   * @return the specified parameters with any variable declaration in their value replaced by
   * the variable value.
   */
  static final def parseParameters(def parameters, def variables) {
    parameters.each { key, value ->
      parameters[key] = parseExpression(value, variables)
    }
    return parameters
  }

  /**
   * Parses any variable declaration in the specified expression and replace them by their value
   * from the specified variables. If a variable, present in
   * the parameter value, isn't among the given variables, then an exception is thrown.
   * @param expression the expression to parse.
   * @param variables a map of key-value whose the key is a variable identifier and the value the
   * variable value.
   * @return the specified expression with any variable declaration replaced by their value.
   */
  static final String parseExpression(String expression, def variables) {
    def matching = expression =~ VARIABLE_PATTERN
    matching.each { token ->
      if (!token[1].startsWith('env') && !token[1].startsWith('sys')) {
        if (variables.containsKey(token[1])) {
          expression = expression.replace(token[0], variables[token[1]])
        } else {
          println "Error: no such variable ${token[1]}"
          throw new StopExecutionException("Error: no such variable ${token[1]}")
        }
      }
    }
    return expression
  }

}
