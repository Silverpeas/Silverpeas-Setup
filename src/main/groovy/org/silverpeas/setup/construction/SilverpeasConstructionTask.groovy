/*
    Copyright (C) 2000 - 2022 Silverpeas

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
package org.silverpeas.setup.construction

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.silverpeas.setup.SilverpeasInstallationProperties
import org.silverpeas.setup.api.FileLogger
import org.silverpeas.setup.api.SilverpeasSetupTask

import java.nio.file.Files
import java.nio.file.Paths

/**
 * A Gradle task to construct the collaborative web portal from all the software bundles that made
 * Silverpeas. It gathers both the assembling and the build tasks.
 * @author mmoquillon
 */
class SilverpeasConstructionTask extends SilverpeasSetupTask {

  public static final String SILVERPEAS_WAR = 'silverpeas.war'

  @Internal
  File silverpeasHome
  @Nested
  SilverpeasInstallationProperties installation
  @Internal
  final FileLogger log = FileLogger.getLogger(this.name)

  SilverpeasConstructionTask() {
    description = 'Assemble and build the Silverpeas Collaborative Web Application'
    group = 'Build'
    onlyIf {
      precondition()
    }
    outputs.upToDateWhen {
      isUpToDate()
    }
  }

  def precondition() {
    !installation.distDir.get().exists() && !installation.dsDriversDir.get().exists()
  }

  def isUpToDate() {
    boolean ok = installation.distDir.get().exists() && installation.dsDriversDir.get().exists()
    if (!installation.developmentMode.get()) {
      ok = ok && Files.exists(Paths.get(project.buildDir.path, SILVERPEAS_WAR))
    }
    return ok
  }

  @TaskAction
  void construct() {
    if (!installation.distDir.get().exists()) {
      installation.distDir.get().mkdirs()
    }
    if (!installation.dsDriversDir.get().exists()) {
      installation.dsDriversDir.get().mkdirs()
    }
    SilverpeasBuilder builder = new SilverpeasBuilder(project, log)
    builder.driversDir = installation.dsDriversDir.get()
    builder.silverpeasHome = silverpeasHome
    builder.developmentMode = installation.developmentMode.get()
    builder.settings = settings
    builder.extractSoftwareBundles(installation.bundles, installation.distDir.get())
    builder.generateSilverpeasApplication(installation.distDir.get())
  }

}
