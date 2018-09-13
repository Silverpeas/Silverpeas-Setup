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

import org.silverpeas.setup.SilverpeasSetupPlugin

/**
 * A Groovy script. It wraps the actual referred script file and it manages its execution.
 * @author mmoquillon
 */
class GroovyScript extends AbstractScript {

  private static GroovyScriptEngine engine = new GroovyScriptEngine('',
      SilverpeasSetupPlugin.getClassLoader())

  /**
   * Constructs a new GroovyScript instance that refers the script located at the specified path.
   * @param path the absolute path of a Groovy script.
   */
  GroovyScript(String path) {
    super(path)
  }

  /**
   * Runs this script with the specified arguments.
   * @param args a Map of variables to pass to the scripts. The keys in the Map are the names of the
   * variables.
   * @throws RuntimeException if an error occurs during the execution of the script.
  */
  @Override
  void run(Map args) throws RuntimeException {
    logger.info "${script.name} processing..."
    Binding parameters = new Binding()
    parameters.setVariable('settings', settings)
    parameters.setVariable('log', logger)
    parameters.setVariable('service', ManagedBeanContainer.get(SilverpeasSetupService))
    args.each { key, value ->
      parameters.setVariable(key, value)
    }
    String status = '[OK]'
    try {
      engine.run(script.toURI().toString(), parameters)
    } catch (Exception ex) {
      status = '[FAILURE]'
      throw ex
    } finally {
      logger.info "${script.name} processing: ${status}"
    }
  }


}
