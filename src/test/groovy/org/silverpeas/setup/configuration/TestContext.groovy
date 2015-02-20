package org.silverpeas.setup.configuration

import groovy.util.slurpersupport.GPathResult

/**
 * A context of a unit test on the Silverpeas configuration.
 * @author mmoquillon
 */
class TestContext {

  def settings = [
      before: [
          autDomainSQL: null,
          system: null,
          scheduler: null
      ],
      after : [
          autDomainSQL: null,
          system: null,
          scheduler: null
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

  TestContext() {
  }

  TestContext before() {
    settings.before.autDomainSQL = loadPropertiesFrom('authentication/autDomainSQL.properties')
    settings.before.system = loadPropertiesFrom('systemSettings.properties')
    settings.before.scheduler = loadPropertiesFrom('workflow/engine/schedulerSettings.properties')

    xmlconf.before.adefCreateSupplier = loadXmlFileFrom('/data/workflowRepository/ADEFCreateSupplier.xml')
    xmlconf.before.adefCreateProduct = loadXmlFileFrom('/data/workflowRepository/ADEFCreateProduct.xml')
    xmlconf.before.workflowDatabaseConf = loadXmlFileFrom('/resources/instanceManager/database.xml')
    xmlconf.before.workflowFastDatabaseConf = loadXmlFileFrom('/resources/instanceManager/fast_database.xml')
    return this
  }

  TestContext after() {
    settings.after.autDomainSQL = loadPropertiesFrom('authentication/autDomainSQL.properties')
    settings.after.system = loadPropertiesFrom('systemSettings.properties')
    settings.after.scheduler = loadPropertiesFrom('workflow/engine/schedulerSettings.properties')

    xmlconf.after.adefCreateSupplier = loadXmlFileFrom('/data/workflowRepository/ADEFCreateSupplier.xml')
    xmlconf.after.adefCreateProduct = loadXmlFileFrom('/data/workflowRepository/ADEFCreateProduct.xml')
    xmlconf.after.workflowDatabaseConf = loadXmlFileFrom('/resources/instanceManager/database.xml')
    xmlconf.after.workflowFastDatabaseConf = loadXmlFileFrom('/resources/instanceManager/fast_database.xml')
    return this
  }

  private Properties loadPropertiesFrom(String path) {
    Properties properties = new Properties()
    properties.load(getClass().getResourceAsStream("/properties/org/silverpeas/${path}"))
    return properties
  }

  private GPathResult loadXmlFileFrom(String path) {
    return new XmlSlurper().parse(getClass().getResourceAsStream(path))
  }
}
