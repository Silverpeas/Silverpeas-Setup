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
package org.silverpeas.setup.migration

import groovy.sql.Sql
import org.silverpeas.setup.api.SilverpeasSetupService

import java.sql.SQLException

/**
 * A programming script written in Groovy.
 * @author mmoquillon
 */
class GroovyScript implements MigrationScript {

  private List<File> scripts = []

  GroovyScript(String... scriptPath) {
    for (String groovyScript : scriptPath) {
      scripts << new File(groovyScript)
    }

  }

  /**
   * Runs this script.
   * @param the Sql instance to use to perform operations against the database.
   * @throws SQLException if an error occurs during the execution of this script.
   */
  @Override
  void run(Sql sql) throws SQLException {
    GroovyScriptEngine engine =
        new GroovyScriptEngine(scripts.collect { it.parent }.toArray() as String[])
    Binding scriptEnv = new Binding()
    scriptEnv.setVariable('sql', sql)
    scriptEnv.setVariable('settings', SilverpeasSetupService.currentSettings)
    scriptEnv.setVariable('Service', SilverpeasSetupService)
    try {
      scripts.each { script ->
        engine.run(script.path, scriptEnv)
      }
    } catch(Exception ex) {
      throw new SQLException(ex)
    }
  }
}
