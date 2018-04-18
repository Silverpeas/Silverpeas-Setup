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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.silverpeas.setup.SilverpeasSetupExtension
import org.silverpeas.setup.SilverpeasSetupPlugin
import org.silverpeas.setup.api.Logger
import org.silverpeas.setup.api.Script

import java.nio.file.Files
import java.nio.file.Paths
/**
 * This task aims to configure Silverpeas from the Silverpeas configuration file, from some XML
 * configuration rules and from Groovy scripts.
 * @author mmoquillon
 */
class SilverpeasConfigurationTask extends DefaultTask {
  final Logger log = Logger.getLogger(this.name)
  final SilverpeasSetupExtension silverSetup

  SilverpeasConfigurationTask() {
    description = 'Configure Silverpeas'
    group = 'Build'
    onlyIf {
      project.buildDir.exists() &&
          Files.exists(Paths.get(project.silversetup.silverpeasHome.path, 'properties'))
    }
    silverSetup =
        (SilverpeasSetupExtension) project.extensions.getByName(SilverpeasSetupPlugin.EXTENSION)
  }

  @TaskAction
  def configureSilverpeas() {
    silverSetup.silverpeasConfigurationDir.listFiles(new FileFilter() {
      @Override
      boolean accept(final File child) {
        return child.isFile()
      }
    }).sort{
      it.name
    }.each { configurationFile ->
      try {
        Script script = ConfigurationScriptBuilder.fromScript(configurationFile.path).build()
        script
            .useLogger(log)
            .useSettings(silverSetup.config)
            .run()
      } catch (Exception ex) {
        log.error("Error while processing the configuration file ${configurationFile.path}", ex)
        throw new TaskExecutionException(this, ex)
      }
    }
  }
}
