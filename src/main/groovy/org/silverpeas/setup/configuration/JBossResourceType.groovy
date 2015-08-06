package org.silverpeas.setup.configuration

/**
 * The different types of resources that can be managed by the JBoss Management API.
 * @author mmoquillon
 */
enum JBossResourceType {
  ra('resource adapter'),
  ds('database'),
  dl('deployment location'),
  jms('JMS resources'),
  sys('subsystem')

  private String type;

  protected JBossResourceType(String type) {
    this.type = type;
  }

  @Override
  String toString() {
    return type;
  }
}