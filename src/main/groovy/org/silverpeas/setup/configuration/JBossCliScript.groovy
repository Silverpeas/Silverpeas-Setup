/*
  Copyright (C) 2000 - 2022 Silverpeas

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  As a special exception to the terms and conditions of version 3.0 of
  the GPL, you may redistribute this Program in connection with Free/Libre
  Open Source Software ("FLOSS") applications as described in Silverpeas's
  FLOSS exception.  You should have received a copy of the text describing
  the FLOSS exception, and it is also available here:
  "https://www.silverpeas.org/docs/core/legal/floss_exception.html"

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.silverpeas.setup.configuration

import org.apache.commons.io.FilenameUtils
import org.silverpeas.setup.api.AbstractScript
import org.silverpeas.setup.api.JBossServer

import java.nio.file.Files
import java.nio.file.Path

/**
 * A script with JBoss CLI statements. It is used to configure JBoss/Wildfly for Silverpeas.
 * @author mmoquillon
 */
class JBossCliScript extends AbstractScript {

  /**
   * Constructs a new JBoss CLI script for a CLI file located at the specified path.
   * @param path the absolute path of a JBoss CLI script file.
   */
  JBossCliScript(String path) {
    super(path)
  }

  /**
   * Runs this script with the specified arguments.
   * @param args a Map of variables to pass to the scripts. The keys in the Map are the names of the
   * variables. Expected the following:
   * <ul>
   *  <li><em>iboss</em>: the running JBoss/Wildfly instance against which the script will be
   *  performed.</li>
   * </ul>
   * @throws RuntimeException if an error occurs during the execution of the script.
   */
  @Override
  void run(Map<String, ?> args) throws RuntimeException {
    try {
      JBossServer jboss = args.jboss
      String fileBaseName = FilenameUtils.getBaseName(script.name)
      Path cli = Files.createTempFile("${fileBaseName}-", '.cli')
      logger.info "Prepare ${script.name} into ${cli.fileName}"
      new FileReader(script).transformLine(new FileWriter(cli.toString())) { line ->
        VariableReplacement.parseExpression(line, settings)
      }
      jboss.doWhenRunning {
        jboss.processCommandFile(cli.toFile())
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex)
    }
  }
}
