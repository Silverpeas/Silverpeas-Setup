/*
    Copyright (C) 2000 - 2024 Silverpeas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    As a special exception to the terms and conditions of version 3.0 of
    the GPL, you may redistribute this Program in connection with Free/Libre
    Open Source Software ("FLOSS") applications as described in Silverpeas's
    FLOSS exception.  You should have received a copy of the text describing
    the FLOSS exception, and it is also available here:
    "https://www.silverpeas.org/legal/floss_exception.html"

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.silverpeas.setup.construction

import org.gradle.api.tasks.*
import org.silverpeas.setup.SilverpeasInstallationProperties
import org.silverpeas.setup.api.FileLogger
import org.silverpeas.setup.api.SilverpeasSetupTask

import java.nio.file.Files
import java.nio.file.Path
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
      preconditionSatisfied()
    }
    outputs.upToDateWhen {
      allIsGenerated()
    }
  }

  def preconditionSatisfied() {
    !areBundlesAssembled() || !isWebDescriptorGenerated()
  }

  def allIsGenerated() {
    boolean ok = areBundlesAssembled() && isWebDescriptorGenerated()
    if (!installation.developmentMode.get()) {
      ok = ok && Files.exists(Paths.get(project.buildDir.path, SILVERPEAS_WAR))
    }
    return ok
  }

  private boolean isWebDescriptorGenerated() {
    Path webDescriptor = Paths.get(installation.distDir.get().path, 'WEB-INF', 'web.xml')
    return Files.exists(webDescriptor)
  }

  private boolean areBundlesAssembled() {
    return installation.distDir.get().exists() && installation.dsDriversDir.get().exists()
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
