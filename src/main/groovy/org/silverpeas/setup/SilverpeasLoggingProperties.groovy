/*
  Copyright (C) 2000 - 2020 Silverpeas

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
package org.silverpeas.setup

import org.gradle.api.logging.LogLevel

/**
 * Properties to set up the logging system used by this Gradle plugin.
 * @author mmoquillon
 */
class SilverpeasLoggingProperties {

  /**
   * The directory into which the logging files should be generated.
   */
  File logDir

  /**
   * The default level from which the traces will be written into the log file(s).
   */
  LogLevel defaultLevel = LogLevel.INFO

  /**
   * Should the logging system of the plugin be used to output traces into log files.
   * By default, true. If false, the traces will be then output through the logging system of
   * Gradle; they will be output into the standard output (level QUIET)
   */
  boolean useLogger = true

  /**
   * Additional to its own tasks, the ones in a Gradle script the logging system of the pluging
   * should take in charge. By default, none is taken in charge; only the tasks in the plugin are
   * supported by the logging system.
   */
  List<String> scriptTasks = []
}
