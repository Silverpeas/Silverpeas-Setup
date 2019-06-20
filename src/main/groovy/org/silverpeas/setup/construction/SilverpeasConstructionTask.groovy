/*
    Copyright (C) 2000 - 2019 Silverpeas

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

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.silverpeas.setup.api.FileLogger

import java.nio.file.Files
import java.nio.file.Paths

/**
 * A Gradle task to construct the collaborative web portal from all the software bundles that made
 * Silverpeas. It gathers both the assembling and the build tasks.
 * @author mmoquillon
 */
class SilverpeasConstructionTask extends DefaultTask {

  public static final String SILVERPEAS_WAR = 'silverpeas.war'

  File silverpeasHome
  File driversDir
  Map settings
  final Property<File> destinationDir = project.objects.property(File)
  final ConfigurableFileCollection silverpeasBundles = project.files()
  final ConfigurableFileCollection tiersBundles = project.files()
  final Property<Boolean> developmentMode = project.objects.property(Boolean)
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

  boolean precondition() {
    !destinationDir.get().exists() && !driversDir.exists()
  }

  boolean isUpToDate() {
    boolean ok = destinationDir.get().exists() && driversDir.exists()
    if (!developmentMode.get()) {
      ok = ok && Files.exists(Paths.get(project.buildDir.path, SILVERPEAS_WAR))
    }
    return ok
  }

  void setSilverpeasBundles(FileCollection bundles) {
    this.silverpeasBundles.setFrom(bundles)
  }

  void setTiersBundles(FileCollection bundles) {
    this.tiersBundles.setFrom(bundles)
  }

  @TaskAction
  void construct() {
    if (!destinationDir.get().exists()) {
      destinationDir.get().mkdirs()
    }
    if (!driversDir.exists()) {
      driversDir.mkdirs()
    }
    SilverpeasBuilder builder = new SilverpeasBuilder(project, log)
    builder.driversDir = driversDir
    builder.silverpeasHome = silverpeasHome
    builder.developmentMode = developmentMode.get()
    builder.settings = settings
    builder.extractSoftwareBundles(silverpeasBundles.files, tiersBundles.files, destinationDir.get())
    builder.generateSilverpeasApplication(destinationDir.get())
  }

}
