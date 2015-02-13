package org.silverpeas.setup.configuration

import org.silverpeas.setup.api.Logger
import org.silverpeas.setup.api.Script
import org.silverpeas.setup.api.SilverpeasSetupService

/**
 * A script represented by an XML file in which are indicated the Silverpeas properties files to
 * update and for each of them the properties to add or to update.
 * @author mmoquillon
 */
class XmlSettingsScript implements Script {

  private String script
  private Logger log

  /**
   * Constructs a new XML script for an XML settings file located at the specified absolute path.
   * @param path
   */
  XmlSettingsScript(String path) {
    script = path
  }

  /**
   * Uses the specified logger to trace this script execution.
   * @param logger a logger.
   * @return itself.
   */
  @Override
  XmlSettingsScript useLogger(final Logger logger) {
    this.log = logger
    return this
  }
/**
   * Runs this script with the specified arguments.
   * @param args a Map of variables to pass to the scripts. The keys in the Map are the names of the
   * variables. Expected the configuration settings of Silverpeas under the name
   * <code>settings</code> and the logger used in the tasks under the name <code>log</code>.
   * @throws RuntimeException if an error occurs during the execution of the script.
   */
  @Override
  void run(def args) throws RuntimeException {
    def settingsStatements = new XmlSlurper().parse(script)
    settingsStatements.fileset.each { fileset ->
      String dir = VariableReplacement.parseExpression(fileset.@root.text(), args.settings)
      fileset.configfile.each { configfile ->
        String status = '[OK]'
        String properties = configfile.@name
        log.info "${properties} processing..."
        def parameters = [:]
        configfile.parameter.each {
          parameters[it.@key.text()] = it.text()
        }
        try {
          parameters = VariableReplacement.parseParameters(parameters, args.settings)
          SilverpeasSetupService.updateProperties("${dir}/${properties}", parameters)
        } catch (Exception ex) {
          status = '[FAILURE]'
          throw new RuntimeException(ex)
        } finally {
          log.info "${properties} processing: ${status}"
        }
      }
    }
  }
}
