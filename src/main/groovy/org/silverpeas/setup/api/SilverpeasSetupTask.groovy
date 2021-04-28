package org.silverpeas.setup.api

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal

/**
 * Common definition of a task in the Silverpeas Setup plugin.
 * @author mmoquillon
 */
abstract class SilverpeasSetupTask extends DefaultTask {

  /**
   * The settings is a dictionary of all configuration properties initially loaded from the
   * <code>config.properties</code> file and computed by the plugin. Any task can also set
   * additional properties in order to share information with other tasks. Usually, the settings
   * should be injected in the scripts that are executed by a task.
   */
  @Internal
  Map<String, String> settings
}
