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


import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.silverpeas.setup.SilverpeasConfigurationProperties
import org.silverpeas.setup.api.FileLogger
import org.silverpeas.setup.api.Script
import org.silverpeas.setup.api.SilverpeasSetupTask

import java.nio.file.Files
import java.nio.file.Paths

/**
 * This task aims to configure Silverpeas from the Silverpeas configuration file, from some XML
 * configuration rules and from Groovy scripts.
 * @author mmoquillon
 */
class SilverpeasConfigurationTask extends SilverpeasSetupTask {

  @Internal
  File silverpeasHome
  @Nested
  SilverpeasConfigurationProperties config
  @Internal
  final FileLogger log = FileLogger.getLogger(this.name)

  SilverpeasConfigurationTask() {
    description = 'Configure Silverpeas'
    group = 'Build'
    onlyIf {
      precondition()
    }
  }

  boolean precondition() {
    project.buildDir.exists() &&
        Files.exists(Paths.get(silverpeasHome.path, 'properties'))
  }

  @TaskAction
  void configureSilverpeas() {
    config.silverpeasConfigurationDir.get().listFiles(new FileFilter() {
      @Override
      boolean accept(final File child) {
        return child.isFile()
      }
    }).sort {
      it.name
    }.each { configurationFile ->
      try {
        Script script = ConfigurationScriptBuilder.fromScript(configurationFile.path).build()
        script
            .useLogger(log)
            .useSettings(settings)
            .run([:])
      } catch (Exception ex) {
        log.error("Error while processing the configuration file ${configurationFile.path}", ex)
        throw new TaskExecutionException(this, ex)
      }
    }
  }
}
