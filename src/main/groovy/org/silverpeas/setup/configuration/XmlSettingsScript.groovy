package org.silverpeas.setup.configuration

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import org.silverpeas.setup.api.AbstractScript
import org.silverpeas.setup.api.ManagedBeanContainer
import org.silverpeas.setup.api.SilverpeasSetupService
import org.w3c.dom.Document
import org.w3c.dom.Node

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
/**
 * A script represented by an XML file in which are indicated the Silverpeas properties and XML
 * files to update and for each of them the properties to add or to update.
 * @author mmoquillon
 */
class XmlSettingsScript extends AbstractScript {

  /**
   * Constructs a new XML script for an XML settings file located at the specified absolute path.
   * @param path
   */
  XmlSettingsScript(String path) {
    super(path)
  }

  /**
   * Runs this script with the specified arguments.
   * @param args a Map of variables to pass to the scripts. The keys in the Map are the names of the
   * variables.
   * @throws RuntimeException if an error occurs during the execution of the script.
   */
  @Override
  void run(Map<String, ?> args) throws RuntimeException {
    SilverpeasSetupService service = ManagedBeanContainer.get(SilverpeasSetupService)
    def settingsStatements = new XmlSlurper().parse(script)
    logger.info "${script.name} scanning..."
    settingsStatements.test.each { GPathResult test ->
      test.parameter.each { GPathResult parameter ->
        String settingName = parameter.@key.text()
        if (settings[settingName] == null || settings[settingName].trim().isEmpty()) {
          throw new RuntimeException(
              "The parameter '${settingName}' is not defined or not valued in config.properties")
        }
      }
    }

    settingsStatements.fileset.each { GPathResult fileset ->
      String dir = service.expanseVariables(fileset.@root.text())
      fileset.children().each { GPathResult file ->
        String status = '[OK]'
        String filename = file.@name
        String settingFile = "${pathToLog(dir, settings.SILVERPEAS_HOME)}/${filename}"
        logger.info "${settingFile} processing..."
        try {
          switch (file.name()) {
            case 'configfile':
              updateConfigurationFile(service, "${dir}/${filename}", file.parameter)
              break
            case 'xmlfile':
              updateXmlFile(service, "${dir}/${filename}", file.parameter)
          }
        } catch (Exception ex) {
          status = '[FAILURE]'
          throw new RuntimeException(ex)
        } finally {
          logger.info "${settingFile} processing: ${status}"
        }
      }
    }

    logger.info "${script.name} scanning done."
  }

  private void updateConfigurationFile(SilverpeasSetupService service, String configurationFilePath,
                                       GPathResult parameters) {
    Map<String, String> properties = [:]
    parameters.each { GPathResult parameter ->
      properties[parameter.@key.text()] = service.expanseVariables(parameter.text())
    }
    service.updateProperties(configurationFilePath, properties)
  }

  private void updateXmlFile(SilverpeasSetupService service, String xmlFilePath,
                             GPathResult parameters) {
    Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(xmlFilePath))
    XPath xpath = XPathFactory.newInstance().newXPath()
    parameters.each { GPathResult parameter ->
      def nodes = xpath.evaluate(parameter.@key.text(), xml, XPathConstants.NODESET)
      nodes.each { Node node ->
        if (node.nodeType == Node.ATTRIBUTE_NODE) {
          node.nodeValue = service.expanseVariables(parameter.text())
        } else if (node.nodeType == Node.ELEMENT_NODE) {
          node.textContent = service.expanseVariables(parameter.text())
        }
      }
    }

    Transformer transformer = TransformerFactory.newInstance().newTransformer()
    transformer.transform(new DOMSource(xml), new StreamResult(new File(xmlFilePath)))
  }

  private String pathToLog(String filePath, String silverpeasHomePath) {
    String relativeFilePath = filePath
    if (relativeFilePath.startsWith(silverpeasHomePath)) {
      relativeFilePath = filePath.substring(silverpeasHomePath.length() + 1)
    }
    return relativeFilePath
  }

}
