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
package org.silverpeas.setup.api

/**
 * The name of all the tasks the plugin defines.
 * @author mmoquillon
 */
enum SilverpeasSetupTaskNames {
  /**
   * Extracts all the software bundles making up Silverpeas and assembles their content into a single location
   */
  ASSEMBLE('assemble'),
  /**
   * Builds the Silverpeas Web application by generating the JEE required descriptors (web.xml, persistence.xml, ...)
   */
  BUILD('build'),
  /**
   * Assembles from different software bundles and builds the Silverpeas Web application.
   */
  CONSTRUCT('construct'),
  /**
   * Configures the JBoss/Wildfly JEE server for running Silverpeas.
   */
  CONFIGURE_JBOSS('configure_jboss'),
  /**
   * Configure Silverpeas.
   */
  CONFIGURE_SILVERPEAS('configure_silverpeas'),
  /**
   * Configure both JBoss/Wildfly and Silverpeas.
   */
  CONFIGURE('configure'),
  /**
   * Migrates the data source structure from the current version to the latest one.
   */
  MIGRATE('migrate'),
  /**
   * Installs Silverpeas into JBoss/Wildfly
   */
  INSTALL('install'),
  /**
   * Upgrade Silverpeas from the current version to the one specified in its installation descriptor.
   */
  UPGRADE('upgrade')

  final String name

  SilverpeasSetupTaskNames(final String taskName) {
    this.name = taskName
  }


}