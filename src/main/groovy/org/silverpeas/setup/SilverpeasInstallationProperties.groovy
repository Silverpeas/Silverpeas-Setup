package org.silverpeas.setup

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property

import javax.inject.Inject
/**
 * Properties for the installation and deployment of Silverpeas in a JBoss server.
 * @author mmoquillon
 */
class SilverpeasInstallationProperties {

  /**
   * The distribution directory. It is the directory that contains all the content of the
   * constructed Silverpeas collaborative application. Defaulted into the build directory. Set a
   * different location is pertinent only for development mode as in this mode the distribution
   * directory is deployed as such in the JBoss/Wildfly application server.
   * environment variable.
   */
  final Property<File> distDir

  /**
   * Directory that have to contain all the application or resource archives to deploy into
   * JBoss/Wildfly. Defaulted in the SILVERPEAS_HOME/deployments directory.
   */
  final Property<File> deploymentDir

  /**
   * Directory that have to contain all the drivers required by Silverpeas and the Silverpeas Setup
   * plugin to access the data source of Silverpeas.
   */
  final Property<File> dsDriversDir

  /**
   * Is in development mode? (In this case, some peculiar configuration are applied to support the
   * dev mode in the application server.) This is a property and hence can be set by the user input
   * from the build script.
   */
  final Property<Boolean> developmentMode

  /**
   * Collections of software bundles required to construct the Silverpeas application.These bundles
   * will be downloaded from our software repository server (provided by our Nexus service) and then
   * unpacked to a given directory in order to generate the final application.
   */
  final SoftwareBundles bundles

  @Inject
  SilverpeasInstallationProperties(Project project, File silverpeasHome) {
    distDir = project.objects.property(File)
    distDir.set(new File(project.buildDir, "dist"))
    deploymentDir = project.objects.property(File)
    deploymentDir.set(new File(silverpeasHome, 'deployments'))
    dsDriversDir = project.objects.property(File)
    dsDriversDir.set(new File(project.buildDir, "drivers"))
    developmentMode = project.objects.property(Boolean)
    developmentMode.set(false)
    bundles = project.objects.newInstance(SoftwareBundles, project)
  }

  void bundles(Action<? extends SoftwareBundles> action) {
    action.execute(bundles)
  }
}
