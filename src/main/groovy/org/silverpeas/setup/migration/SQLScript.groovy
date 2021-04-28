/*
  Copyright (C) 2000 - 2020 Silverpeas

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
import org.silverpeas.setup.api.AbstractScript
import org.silverpeas.setup.api.ManagedBeanContainer
import org.silverpeas.setup.api.SilverpeasSetupService

import java.sql.SQLException

/**
 * A SQL script.
 * @author mmoquillon
 */
class SQLScript extends AbstractScript {

  SQLScript(String scriptPath) {
    super(scriptPath)
  }

  /**
   * Runs this script.
   * @param args a Map of variables to pass to the scripts. The keys in the Map are the names of the
   * variables. Expected the following:
   * <ul>
   *   <li><em>sql</em>: the Sql instance to use to perform operations against the database.</li>
   * </ul>
   * @throws SQLException if an error occurs during the execution of this script.
   */
  @Override
  void run(Map<String, ?> args) throws SQLException {
    Sql sql = args.sql
    List<String> statements = loadSqlStatements()
    sql.withBatch { batch ->
      statements.each {
        batch.addBatch(it)
      }
    }
  }

  private List<String> loadSqlStatements() {
    SilverpeasSetupService service = ManagedBeanContainer.get(SilverpeasSetupService)
    List<String> statements = []
    String scriptContent = script.getText('UTF-8')
    scriptContent.split(';').each { String aStatement ->
      if (!aStatement.trim().isEmpty()) {
        statements << service.expanseVariables(aStatement.trim())
      }
    }
    return statements
  }
}
