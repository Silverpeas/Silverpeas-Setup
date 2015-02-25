import groovy.xml.XmlUtil

/**
 * This script configures the different datasource XML configuration.
 * @author mmoquillon
 */

println 'Configure the workflow persistence engine'

def workflowSettingsDir = "${settings.SILVERPEAS_HOME}/resources/instanceManager"
def xmlSettingFiles = ['database.xml', 'fast_database.xml']

def engine
switch (settings.DB_SERVERTYPE) {
  case 'MSSQL':
    engine = 'sql-server'
    break
  case 'ORACLE':
    engine = 'oracle'
    break
  default:
    engine = 'postgresql'
    break
}

xmlSettingFiles.each { aXmlSettingFile ->
  def jdoConf = new XmlSlurper(false, false).parse(new File("${workflowSettingsDir}/${aXmlSettingFile}"))
  jdoConf.@engine = engine
  if (aXmlSettingFile.contains('fast')) {
    jdoConf.mapping.@href = "file:///${settings.SILVERPEAS_HOME}/resources/instanceManager/fast_mapping.xml" as String
  } else {
    jdoConf.database.@engine = engine
    jdoConf.database.mapping.@href = "file:///${settings.SILVERPEAS_HOME}/resources/instanceManager/mapping.xml" as String
  }

  XmlUtil.serialize(jdoConf, new FileWriter("${workflowSettingsDir}/${aXmlSettingFile}"))
}
