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
package org.silverpeas.setup.installation

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.ProjectState
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.silverpeas.setup.api.FileLogger
import org.silverpeas.setup.api.JBossServer

import static org.silverpeas.setup.construction.SilverpeasConstructionTask.SILVERPEAS_WAR

/**
 * A Gradle task to install the Web archive of the Silverpeas application into the JEE application
 * server.
 * @author mmoquillon
 */
class SilverpeasInstallationTask extends DefaultTask {

  Property<JBossServer> jboss = project.objects.property(JBossServer)
  File deploymentDir
  File distDir
  final Property<Boolean> developmentMode = project.objects.property(Boolean)
  final FileLogger log = FileLogger.getLogger(this.name)

  SilverpeasInstallationTask() {
    description = 'Installs Silverpeas into JBoss/Wildfly'
    group = 'Build'

    project.afterEvaluate { Project currentProject, ProjectState state ->
      if (state.executed) {
        jboss.get().useLogger(log)
      }
    }
  }

  @TaskAction
  void install() {
    JBossServer server = jboss.get()
    if (server.isStartingOrRunning()) {
      server.stop()
    }
    server.start(adminOnly: true)

    if (!deploymentDir.exists()) {
      deploymentDir.mkdirs()
    }
    project.copy {
      it.from(project.fileTree(project.buildDir))
      it.include '*.rar'
      it.include '*.jar'
      it.include '*.war'
      it.into deploymentDir
    }
    try {
      deploymentDir.listFiles().sort().each { artifact ->
        log.info "(Re)Installation of ${artifact.name}"
        server.remove(artifact.name)
        server.add(artifact.path)
        server.deploy(artifact.name)
        log.info "(Re)Installation of ${artifact.name}: [OK]"
      }
      if (developmentMode.get()) {
        log.info '(Re)Installation of silverpeas.war as exploded (dev mode)'
        server.remove(SILVERPEAS_WAR)
        server.add(distDir.path, SILVERPEAS_WAR)
        server.deploy(SILVERPEAS_WAR)
        log.info '(Re)Installation of silverpeas.war as exploded (dev mode): [OK]'
      }
    } finally {
      server.stop()
    }
  }
}
