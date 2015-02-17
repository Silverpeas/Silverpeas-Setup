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
import org.silverpeas.setup.api.Logger
import org.silverpeas.setup.api.Script

import java.sql.SQLException

import static org.silverpeas.setup.api.SilverpeasSetupService.replaceVariables

/**
 * A SQL script.
 * @author mmoquillon
 */
class SQLScript implements Script {

  private List<String> statements = []

  SQLScript(String scriptPath) {
    String script = new File(scriptPath).getText('UTF-8')
      script.split(';').each { String aStatement ->
        if (!aStatement.trim().isEmpty()) {
          statements << replaceVariables(aStatement.trim())
        }
      }
  }

  /**
   * Uses the specified logger to trace this script execution.
   * @param logger a logger.
   * @return itself.
   */
  @Override
  SQLScript useLogger(final Logger logger) {
    return this
  }
/**
   * Runs this script.
 * @param args the Sql instance to use to perform operations against the database.
   * @throws SQLException if an error occurs during the execution of this script.
   */
  @Override
  void run(def args) throws SQLException {
    Sql sql = args.sql
    sql.withBatch { batch ->
      statements.each {
        batch.addBatch(it)
      }
    }
  }
}