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
package org.silverpeas.setup.test

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.silverpeas.setup.SilverpeasSetupExtension
import org.silverpeas.setup.SilverpeasSetupPlugin
import org.silverpeas.setup.api.ManagedBeanContainer
import org.silverpeas.setup.api.SilverpeasSetupService

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Set up the test environment.
 * @author mmoquillon
 */
class TestContext {

  String resourcesDir
  String migrationHome

  private TestContext() {
    Properties properties = new Properties()
    properties.load(getClass().getResourceAsStream('/test.properties'))
    this.migrationHome = properties.migrationHome
    this.resourcesDir = properties.resourcesDir
  }

  /**
   * Creates a new test context.
   * @return a new TestContext instance
   */
  static TestContext create() {
    return new TestContext()
  }

  /**
   * Sets up the environment variables required by the plugin to work.
   * @return itself.
   */
  TestContext setUpSystemEnv() {
    System.setProperty('SILVERPEAS_HOME',resourcesDir)
    System.setProperty('JBOSS_HOME', resourcesDir)
    return this
  }

  /**
   * Cleans up any resources that were allocated for the tests to run.
   */
  void cleanUp() {
    Files.deleteIfExists(Paths.get(resourcesDir, 'build.gradle'))
    Files.deleteIfExists(Paths.get(resourcesDir, 'settings.gradle'))
    FileUtils.deleteDirectory(Paths.get(resourcesDir, 'build').toFile())
  }

  /**
   * Initializes in the filesystem a Gradle project that uses the plugin. The Gradle project
   * can then be ran by using the Gradle runner returned by the TestContext#getGradleRunner method.
   * @return itself.
   */
  TestContext initGradleProject() {
    Files.createDirectories(Paths.get(resourcesDir, 'build', 'drivers'))
    Files.createDirectories(Paths.get(resourcesDir, 'build', 'dist'))
    Files.createFile(Paths.get(resourcesDir, 'settings.gradle'))
        .toFile()
        .text = """
rootProject.name = 'silverpeas-installer'
"""
    Files.createFile(Paths.get(resourcesDir, 'build.gradle'))
        .toFile()
        .text = """
plugins {
  id 'silversetup'
}

silversetup {
  logging {
    logDir = file("\${project.buildDir}/log")
    useLogger = false
  }
}
"""
    return this
  }

  /**
   * Creates in memory a Gradle project that uses the plugin and returns it. The different tasks
   * provided by the plugin can be then get from the returned project in order to be executed
   * directly without using a Gradle runner.
   * @return a Project instance.
   */
  Project createGradleProject() {
    Project project = ProjectBuilder.builder().withName('silverpeas-installer').build()
    project.apply plugin: 'silversetup'
    project.silversetup.logging.logDir = new File(project.buildDir, 'log')
    project.silversetup.logging.useLogger = false

    SilverpeasSetupExtension extension =
        (SilverpeasSetupExtension) project.extensions.getByName(SilverpeasSetupPlugin.EXTENSION)
    ManagedBeanContainer.registry().register(new SilverpeasSetupService(extension.settings))
    return project
  }

  /**
   * Gets a Gradle runner configured to execute a task (with its dependencies) of a Gradle project
   * that was previously initialized with the TestContext#initGradleProject method.
   * @param debug a boolean indicating whether the project'tasks have to be executed in debug
   * @return a GradleRunner instance.
   */
  GradleRunner getGradleRunner(boolean debug = false) {
    GradleRunner.create()
        .withProjectDir(new File(resourcesDir))
        .withPluginClasspath()
        .withDebug(debug)
  }
}
