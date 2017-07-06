/*
    Copyright (C) 2000 - 2017 Silverpeas

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
import org.gradle.api.ProjectState
import org.silverpeas.setup.api.DataSourceProvider
import org.silverpeas.setup.api.Logger
import org.silverpeas.setup.api.SilverpeasSetupService
import org.silverpeas.setup.configuration.JBossConfigurationTask
import org.silverpeas.setup.configuration.SilverpeasConfigurationTask
import org.silverpeas.setup.configuration.VariableReplacement
import org.silverpeas.setup.migration.SilverpeasMigrationTask
import org.silverpeas.setup.security.Encryption
import org.silverpeas.setup.security.EncryptionFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * This plugin aims to prepare the configuration and to setup Silverpeas.
 * For doing, it loads both the default and the customer configuration file of Silverpeas and it
 * registers two tasks, one dedicated to configure JBoss/Wildfly for Silverpeas and another to
 * configure Silverpeas..
 */
class SilverpeasSetupPlugin implements Plugin<Project> {

  private Map settings

  @Override
  void apply(Project project) {
    project.extensions.create('silversetup', SilverpeasSetupExtension)
    project.silversetup.extensions.create('logging', SilverpeasLoggingProperties)

    this.settings = loadConfiguration(project.silversetup.configurationHome)
    completeSettingsForProject(project)
    encryptAdminPassword()
    DataSourceProvider.init(settings)
    SilverpeasSetupService.currentSettings = settings

    project.afterEvaluate { Project currentProject, ProjectState state ->
      settings.DEV_MODE = currentProject.silversetup.developmentMode as String
      if (currentProject.silversetup.logging.useLogger) {
        initLogging(currentProject)
      }
    }

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

  private Map loadConfiguration(String configurationHome) {
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

  private void completeSettingsForProject(Project project) {
    settings.SILVERPEAS_HOME = normalizePath(project.silversetup.silverpeasHome)
    settings.MIGRATION_HOME = normalizePath(project.silversetup.migrationHome)
    settings.CONFIGURATION_HOME = normalizePath(project.silversetup.configurationHome)
    settings.DB_DATASOURCE_JNDI = 'java:/datasources/silverpeas'
    switch (settings.DB_SERVERTYPE) {
      case 'MSSQL':
        settings.DB_URL = "jdbc:jtds:sqlserver://${settings.DB_SERVER}:${settings.DB_PORT_MSSQL}/${settings.DB_NAME}"
        settings.JCR_URL = "jdbc:jtds:sqlserver://${settings.DB_SERVER}:${settings.DB_PORT_MSSQL}/${settings.JCR_NAME}"
        settings.DB_DRIVER = 'net.sourceforge.jtds.jdbc.Driver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.MSSqlPersistenceManager'
        break
      case 'ORACLE':
        settings.DB_URL = "jdbc:oracle:thin:@${settings.DB_SERVER}:${settings.DB_PORT_ORACLE}:${settings.DB_NAME}"
        settings.JCR_URL = "jdbc:oracle:thin:@${settings.DB_SERVER}:${settings.DB_PORT_ORACLE}:${settings.JCR_NAME}"
        settings.DB_DRIVER = 'oracle.jdbc.driver.OracleDriver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.OraclePersistenceManager'
        break
      case 'POSTGRESQL':
        settings.DB_URL = "jdbc:postgresql://${settings.DB_SERVER}:${settings.DB_PORT_POSTGRESQL}/${settings.DB_NAME}"
        settings.JCR_URL = "jdbc:postgresql://${settings.DB_SERVER}:${settings.DB_PORT_POSTGRESQL}/${settings.JCR_NAME}"
        settings.DB_DRIVER = 'org.postgresql.Driver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.PostgreSQLPersistenceManager'
        break
      case 'H2':
        if (settings.DB_SERVER == ':file:') {
          Path databaseDirPath = Paths.get(settings.SILVERPEAS_HOME, 'h2')
          if (!Files.exists(databaseDirPath))
            Files.createDirectory(databaseDirPath)
          settings.DB_URL = "jdbc:h2:file:${settings.SILVERPEAS_HOME}/h2/${settings.DB_NAME};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" //USER=${settings.DB_USER};PASSWORD=${settings.DB_PASSWORD}"
          settings.JCR_URL = "jdbc:h2:file:${settings.SILVERPEAS_HOME}/h2/${settings.JCR_NAME};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" //USER=${settings.DB_USER};PASSWORD=${settings.DB_PASSWORD}"
        } else {
          settings.DB_URL = "jdbc:h2:tcp://${settings.DB_SERVER}:${settings.DB_PORT_H2}/${settings.DB_NAME}"
          settings.JCR_URL = "jdbc:h2:tcp://${settings.DB_SERVER}:${settings.DB_PORT_H2}/${settings.JCR_NAME}"
        }
        settings.DB_DRIVER = 'org.h2.Driver'
        settings.JACKRABBIT_PERSISTENCE_MANAGER = 'org.apache.jackrabbit.core.persistence.pool.H2PersistenceManager'
        break
      default:
        throw new IllegalArgumentException("Unsupported database system: ${settings.DB_SERVERTYPE}")
    }
    settings.DB_SCHEMA = settings.DB_SERVERTYPE.toLowerCase()
  }

  private void encryptAdminPassword() {
    Encryption encryption = EncryptionFactory.instance.createDefaultEncryption()
    settings.SILVERPEAS_ADMIN_PASSWORD = encryption.encrypt(settings.SILVERPEAS_ADMIN_PASSWORD)
  }

  private void initLogging(Project project) {
    String timestamp = new Date().format('yyyyMMdd_HHmmss')
    File logDir = new File(project.silversetup.logging.logDir)
    if (!logDir.exists()) {
      logDir.mkdirs()
    }
    File logFile = new File("build-${timestamp}.log", logDir)
    Logger.init(logFile, project.silversetup.logging.defaultLevel)

    /* customize the traces writing both on the standard output and on the log file. */
    project.gradle.useLogger(new TaskEventLogging()
        .withTasks(project.silversetup.logging.scriptTasks))
  }

  private String normalizePath(String path) {
    return path.replaceAll('\\\\', '/')
  }
}
