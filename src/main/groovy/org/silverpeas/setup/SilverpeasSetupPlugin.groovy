/*
    Copyright (C) 2000 - 2015 Silverpeas

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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.silverpeas.setup.api.API
import org.silverpeas.setup.configuration.JBossConfigurationTask
import org.silverpeas.setup.configuration.SilverpeasConfigurationTask
import org.silverpeas.setup.configuration.VariableReplacement
import org.silverpeas.setup.migration.DataSourceProvider
import org.silverpeas.setup.migration.SilverpeasMigrationTask

import javax.naming.InitialContext
import javax.naming.NameNotFoundException

/**
 * This plugin aims to prepare the configuration and to setup Silverpeas.
 * For doing, it loads both the default and the customer configuration file of Silverpeas and it
 * registers two tasks, one dedicated to configure JBoss/Wildfly for Silverpeas and another to
 * configure Silverpeas..
 */
class SilverpeasSetupPlugin implements Plugin<Project> {

  private def settings

  @Override
  void apply(Project project) {
    project.extensions.create('silversetup', SilverpeasSetupExtension)

    this.settings = loadConfiguration(project.silversetup.configurationHome)
    completeSettingsForProject(project)
    DataSourceProvider.init(settings)
    API.currentSettings = settings

    project.task('configureJBoss', type: JBossConfigurationTask) {
      settings = this.settings
    }

    project.task('configureSilverpeas', type: SilverpeasConfigurationTask) {
      settings = this.settings
    }

    project.task('migration', type:SilverpeasMigrationTask) {
      settings = this.settings
    }
  }

  private def loadConfiguration(String configurationHome) {
    Properties properties = new Properties()
    properties.load(getClass().getResourceAsStream('/default_config.properties'))
    def customConfiguration = new File("${configurationHome}/config.properties")
    // the custom configuration overrides the default configuration
    if (customConfiguration.exists()) {
      Properties customProperties = new Properties()
      customProperties.load(customConfiguration.newReader())
      customProperties.propertyNames().each {
        properties[it] = customProperties[it]
      }
    }
    // replace the variables by their values in the properties and return the properties
    return VariableReplacement.parseParameters(properties, properties)
  }

  private def completeSettingsForProject(Project project) {
    settings.SILVERPEAS_HOME = project.silversetup.silverpeasHome
    settings.MIGRATION_HOME = project.silversetup.migrationHome
    switch (settings.DB_SERVERTYPE) {
      case 'MSSQL':
        settings.DB_URL = "jdbc:jtds:sqlserver://${settings.DB_SERVER}:${settings.DB_PORT_MSSQL}/${settings.DB_NAME}"
        settings.DB_DRIVER = 'net.sourceforge.jtds.jdbc.Driver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.MSSqlPersistenceManager'
        break
      case 'ORACLE':
        settings.DB_URL = "jdbc:oracle:thin:@${settings.DB_SERVER}:${settings.DB_PORT_ORACLE}:${settings.DB_NAME}"
        settings.DB_DRIVER = 'oracle.jdbc.driver.OracleDriver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.OraclePersistenceManager'
        break
      case 'POSTGRESQL':
        settings.DB_URL = "jdbc:postgresql://${settings.DB_SERVER}:${settings.DB_PORT_POSTGRESQL}/${settings.DB_NAME}"
        settings.DB_DRIVER = 'org.postgresql.Driver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.PostgreSQLPersistenceManager'
        break
      case 'H2':
        if (settings.DB_SERVER == ':mem:') {
          settings.DB_URL = "jdbc:h2:mem:${settings.DB_NAME}"
        } else {
          settings.DB_URL = "jdbc:h2:tcp://${settings.DB_SERVER}:${settings.DB_PORT_H2}/${settings.DB_NAME}"
        }
        settings.DB_DRIVER = 'org.h2.Driver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.H2PersistenceManager'
        break
      default:
        throw new IllegalArgumentException("Unsupported database system: ${settings.DB_SERVERTYPE}")
    }
    settings.DB_SCHEMA = settings.DB_SERVERTYPE.toLowerCase()
  }

}
