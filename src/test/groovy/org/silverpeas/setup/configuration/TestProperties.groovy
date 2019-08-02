package org.silverpeas.setup.configuration

import groovy.util.slurpersupport.GPathResult

/**
 * A set of properties to use in the tests on the Silverpeas configuration.
 * @author mmoquillon
 */
class TestProperties {

  def settings = [
      before: [
          autDomainSQL: null,
          system: null,
          scheduler: null,
          castorSettings: null
      ],
      after : [
          autDomainSQL: null,
          system: null,
          scheduler: null,
          castorSettings: null
      ]
  ]

  def xmlconf = [
      before: [
          workflowDatabaseConf: null,
          workflowFastDatabaseConf: null,
          adefCreateSupplier: null,
          adefCreateProduct : null
      ],
      after : [
          workflowDatabaseConf: null,
          workflowFastDatabaseConf: null,
          adefCreateSupplier: null,
          adefCreateProduct : null
      ]
  ]

  def configContext = [
      before: [:],
      after: [:]
  ]

  TestProperties() {
  }

  TestProperties before() {
    settings.before.autDomainSQL = loadPropertiesFrom('authentication/autDomainSQL.properties')
    settings.before.system = loadPropertiesFrom('systemSettings.properties')
    settings.before.scheduler = loadPropertiesFrom('workflow/engine/schedulerSettings.properties')
    settings.before.castorSettings = loadPropertiesFrom('workflow/castorSettings.properties')

    xmlconf.before.adefCreateSupplier = loadXmlFileFrom('/data/workflowRepository/ADEFCreateSupplier.xml')
    xmlconf.before.adefCreateProduct = loadXmlFileFrom('/data/workflowRepository/ADEFCreateProduct.xml')
    xmlconf.before.workflowDatabaseConf = loadXmlFileFrom('/resources/instanceManager/database.xml')
    xmlconf.before.workflowFastDatabaseConf = loadXmlFileFrom('/resources/instanceManager/fast_database.xml')

    configContext.before = loadKeyValuesFileFrom('/configuration/.context')
    return this
  }

  TestProperties after() {
    settings.after.autDomainSQL = loadPropertiesFrom('authentication/autDomainSQL.properties')
    settings.after.system = loadPropertiesFrom('systemSettings.properties')
    settings.after.scheduler = loadPropertiesFrom('workflow/engine/schedulerSettings.properties')
    settings.after.castorSettings = loadPropertiesFrom('workflow/castorSettings.properties')

    xmlconf.after.adefCreateSupplier = loadXmlFileFrom('/data/workflowRepository/ADEFCreateSupplier.xml')
    xmlconf.after.adefCreateProduct = loadXmlFileFrom('/data/workflowRepository/ADEFCreateProduct.xml')
    xmlconf.after.workflowDatabaseConf = loadXmlFileFrom('/resources/instanceManager/database.xml')
    xmlconf.after.workflowFastDatabaseConf = loadXmlFileFrom('/resources/instanceManager/fast_database.xml')

    configContext.after = loadKeyValuesFileFrom('/configuration/.context')
    return this
  }

  private Properties loadPropertiesFrom(String path) {
    Properties properties = new Properties()
    InputStream is = getClass().getResourceAsStream("/properties/org/silverpeas/${path}")
    properties.load(is)
    is.close()
    return properties
  }

  private GPathResult loadXmlFileFrom(String path) {
    InputStream is = getClass().getResourceAsStream(path)
    GPathResult result = new XmlSlurper().parse(is)
    is.close()
    return result
  }

  private Map loadKeyValuesFileFrom(String path) {
    final Map keyValuePairs = [:]
    getClass().getResourceAsStream(path).text.eachLine { line ->
      String[] keyValue = line.trim().split(':')
      keyValuePairs[keyValue[0].trim()] = keyValue[1].trim()
    }
    return keyValuePairs
  }
}
