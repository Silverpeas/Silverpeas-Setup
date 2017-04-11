package org.silverpeas.setup.test

import groovy.sql.Sql
import org.silverpeas.setup.api.SilverpeasSetupService

import java.sql.SQLException
/**
 *
 * @author mmoquillon
 */
abstract class Assertion {

  private static
      final String VERSION_QUERY = 'SELECT sr_version from sr_packages WHERE sr_package = :module'

  static def versionOfModule(String module) {
    Sql sql = SilverpeasSetupService.sql
    try {
      return sql.firstRow(VERSION_QUERY, [module: module])?.get('sr_version')
    } catch (SQLException ex) {
      return null
    }
  }

  static def numberOfItemsIn(String table) {
    Sql sql = SilverpeasSetupService.sql
    try {
      return sql.firstRow('SELECT count(id) AS count FROM ' + table)?.get('count')
    } catch (SQLException ex) {
      return null
    }
  }
}
