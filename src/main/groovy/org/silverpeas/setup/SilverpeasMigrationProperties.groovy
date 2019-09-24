package org.silverpeas.setup

import org.gradle.api.Project
import org.gradle.api.provider.Property

import javax.inject.Inject

/**
 * Properties for the migration of the data source used by Silverpeas when installing it or
 * upgrading it to a new version.
 * @author mmoquillon
 */
class SilverpeasMigrationProperties {

  /**
   * The directory in which are all located both the data source migration descriptors
   * and the scripts to create or to update the schema of the database to be used by Silverpeas.
   * It is defaulted to SILVERPEAS_HOME/migrations.
   * It is expected to contain two kinds of subdirectories:
   * <ul>
   *   <li><em><code>modules</code></em> in which are provided the XML descriptor of each migration
   *   module. These descriptors refers the scripts to use to create or to update the
   *   database schema for a given Silverpeas module;</li>
   *   <li><em><code>db</code></em> in which are located per database type and per module the
   *   different SQL scripts to create or to upgrade the schema of the database;</li>
   *   <li><em><code>scripts</code></em> in which are located per module the different programming
   *   scripts (currently, only Groovy is supported) to perform complex tasks on the database or
   *   any other data sources used by Silverpeas (like the JCR for example).
   *   </li>
   * </ul>
   */
  final Property<File> homeDir

  @Inject
  SilverpeasMigrationProperties(Project project, File silverpeasHome) {
    this.homeDir = project.objects.property(File)
    this.homeDir.set(new File(silverpeasHome, 'migrations'))
  }
}
