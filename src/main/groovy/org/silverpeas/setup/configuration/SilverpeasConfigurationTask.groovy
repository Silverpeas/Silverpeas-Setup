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
package org.silverpeas.setup.configuration

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.silverpeas.setup.api.API


/**
 * This task aims to configure Silverpeas from the Silverpeas configuration file, from some XML
 * configuration rules and from Groovy scripts.
 * @author mmoquillon
 */
class SilverpeasConfigurationTask extends DefaultTask {

  def settings
  private def scriptEngine

  SilverpeasConfigurationTask() {
    description = 'Configure Silverpeas'
    group = 'Build'
    dependsOn = ['assemble']
  }

  @TaskAction
  def configureSilverpeas() {
    scriptEngine = new GroovyScriptEngine(["${project.silversetup.configurationHome}/silverpeas"]
        as String[])

    new File("${project.silversetup.configurationHome}/silverpeas").listFiles().each {
      try {
        if (it.name.endsWith('.xml')) {
          processXmlSettingsFile(it)
        } else if (it.name.endsWith('.groovy')) {
          processScriptFile(it)
        } else {
          throw new UnsupportedOperationException('Configuration file not supported')
        }
      } catch (Exception ex) {
        println "An error occured while processing the configuration file ${it.path}: ${ex.message}"
      }
    }
  }

  def processXmlSettingsFile(settingsFile) {
    def settingsStatements = new XmlSlurper().parse(settingsFile)
    settingsStatements.fileset.each { fileset ->
      String dir = VariableReplacement.parseValue(fileset.@root.text(), settings)
      fileset.configfile.each { configfile ->
        String properties = configfile.@name
        def parameters = [:]
        configfile.parameter.each {
          parameters[it.@key.text()] = it.text()
        }
        parameters = VariableReplacement.parseParameters(parameters, settings)
        processPropertiesFile("${dir}/${properties}", parameters)
      }
    }
  }

  def processPropertiesFile(propertiesFilePath, parameters) {
    API.updateProperties(propertiesFilePath, parameters)
  }

  def processScriptFile(scriptFile) {
    def scriptEnv = new Binding()
    scriptEnv.setVariable('settings', settings)
    scriptEnv.setVariable('API', API)
    scriptEngine.run(scriptFile.path, scriptEnv)
  }
}
