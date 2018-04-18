package org.silverpeas.setup.test

import groovy.sql.Sql

import java.sql.SQLException
/**
 *
 * @author mmoquillon
 */
abstract class Assertion {

  private static
      final String VERSION_QUERY = 'SELECT sr_version from sr_packages WHERE sr_package = :module'

  static def versionOfModule(Sql sql, String module) {
    try {
      return sql.firstRow(VERSION_QUERY, [module: module])?.get('sr_version')
    } catch (SQLException ex) {
      return null
    }
  }

  static def numberOfItems(Sql sql, String table) {
    try {
      return sql.firstRow('SELECT count(id) AS count FROM ' + table)?.get('count')
    } catch (SQLException ex) {
      return null
    }
  }
}
