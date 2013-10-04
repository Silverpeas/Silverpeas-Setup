/**
 * Copyright (C) 2000 - 2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.migration.jcr.service;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.silverpeas.dbbuilder.sql.ConnectionFactory;
import org.silverpeas.util.DateUtil;
import org.silverpeas.util.StringUtil;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for path and date conversions between JCR and Slverpeas Pojo.
 *
 * @author Emmanuel Hugonnet
 * @version $revision$
 */
public class ConverterUtil {

  private static final Logger logger = LoggerFactory.getLogger(ConverterUtil.class);
  public static final String defaultLanguage = "fr";
  private static final Pattern WysiwygLangPattern = Pattern
      .compile("[0-9]+wysiwyg_([a-z][a-z])\\.txt");
  private static final Pattern WysiwygBaseNamePattern = Pattern.compile(
      "([0-9]+wysiwyg).*\\.txt");

  public static String extractLanguage(String filename) {
    Matcher matcher = WysiwygLangPattern.matcher(filename);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return null;
  }

  public static String extractBaseName(String filename) {
    Matcher matcher = WysiwygBaseNamePattern.matcher(filename);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return null;
  }

  public static String checkLanguage(String language) {
    String lang = language;
    if (!StringUtil.isDefined(language)) {
      lang = defaultLanguage;
    }
    return lang;
  }

  /**
   * Token used to replace space in names.
   */
  public static final String SPACE_TOKEN = "__";
  /**
   * Token used in path.
   */
  public static final String PATH_SEPARATOR = "/";
  private static final String OPENING_BRACKET = "[";
  private static final String CLOSING_BRACKET = "]";

  /**
   * Encodes the JCR path to a Xpath compatible path.
   *
   * @param path the JCR path to be encoded for Xpath.
   * @return the corresponding xpath.
   */
  public static final String encodeJcrPath(String path) {
    return ISO9075.encodePath(convertToJcrPath(path));
  }

  /**
   * Replace all whitespace to SPACE_TOKEN.
   *
   * @param name the String o be converted.
   * @return the resulting String.
   */
  public static String convertToJcrPath(String name) {
    String coolName = name.replaceAll(" ", SPACE_TOKEN);
    StringBuilder buffer = new StringBuilder(coolName.length() + 10);
    StringTokenizer tokenizer = new StringTokenizer(coolName, PATH_SEPARATOR, true);
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      if (PATH_SEPARATOR.equals(token)) {
        buffer.append(token);
      } else {
        buffer.append(Text.escapeIllegalJcrChars(token));
      }
    }
    return buffer.toString();
  }

  public static String escapeIllegalJcrChars(String name) {
    return StringUtil.escapeQuote(name).replace(OPENING_BRACKET, " ").replace(CLOSING_BRACKET, " ");
  }

  /**
   * Replace all %39 with the char'
   *
   * @param text
   * @return a String with all %39 replaced by quotes
   */
  public static String unescapeQuote(String text) {
    return text.replaceAll("%" + ((int) ('\'')), "'");
  }

  /**
   * Replace all SPACE_TOKEN to whitespace.
   *
   * @param name the String o be converted.
   * @return the resulting String.
   */
  public static String convertFromJcrPath(String name) {
    return Text.unescapeIllegalJcrChars(name.replaceAll(SPACE_TOKEN, " "));
  }

  /**
   * Parse a String of format yyyy/MM/dd and return the corresponding Date.
   *
   * @param date the String to be parsed.
   * @return the corresponding date.
   * @throws ParseException
   */
  public static Date parseDate(String date) throws ParseException {
    return DateUtil.parse(date);
  }

  /**
   * Format a Date to a String of format yyyy/MM/dd.
   *
   * @param date the date to be formatted.
   * @return the formatted String.
   */
  public static String formatDate(Date date) {
    return DateUtil.formatDate(date);
  }

  /**
   * Format a Calendar to a String of format yyyy/MM/dd.
   *
   * @param calend the date to be formatted.
   * @return the formatted String.
   */
  public static String formatDate(Calendar calend) {
    return DateUtil.formatDate(calend);
  }

  /**
   * Parse a String of format HH:mm and set the corresponding hours and minutes to the specified
   * Calendar.
   *
   * @param time the String to be parsed.
   * @param calend the calendar to be updated.
   */
  public static void setTime(Calendar calend, String time) {
    DateUtil.setTime(calend, time);
  }

  /**
   * Format a Date to a String of format HH:mm.
   *
   * @param date the date to be formatted.
   * @return the formatted String.
   */
  public static String formatTime(Date date) {
    return DateUtil.formatTime(date);
  }

  /**
   * Format a Calendar to a String of format HH:mm.
   *
   * @param calend the date to be formatted.
   * @return the formatted String.
   */
  public static String formatTime(Calendar calend) {
    return DateUtil.formatTime(calend);
  }

  public static String formatDateForXpath(Date date) {
    return "xs:dateTime('" + getXpathFormattedDate(date) + 'T'
        + getXpathFormattedTime(date) + getTimeZone(date) + "')";
  }

  public static String formatCalendarForXpath(Calendar date) {
    return "xs:dateTime('" + getXpathFormattedDate(date.getTime()) + 'T'
        + getXpathFormattedTime(date.getTime()) + getTimeZone(date.getTime()) + "')";
  }

  protected static String getTimeZone(Date date) {
    DateFormat xpathTimezoneFormat = new SimpleDateFormat("Z");
    String timeZone = xpathTimezoneFormat.format(date);
    return timeZone.substring(0, timeZone.length() - 2) + ':'
        + timeZone.substring(timeZone.length() - 2);
  }

  protected static String getXpathFormattedTime(Date date) {
    DateFormat xpathTimeFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
    return xpathTimeFormatter.format(date);
  }

  protected static String getXpathFormattedDate(Date date) {
    DateFormat xpathDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    return xpathDateFormatter.format(date);
  }

  public static int getNextId(String tableName, String idName) {
    Connection connection = null;
    try {
      connection = ConnectionFactory.getConnection();
      connection.setAutoCommit(false);
      return getMaxId(connection, tableName, idName);
    } catch (SQLException ex) {
      logger.debug("Impossible de recupérer le prochain id", ex);
      throw new AttachmentException("Impossible de recupérer le prochain id", ex);
    } finally {
      DbUtils.closeQuietly(connection);
    }
  }

  protected static int getMaxId(Connection connection, String tableName, String idName)
      throws SQLException {
    // tentative d'update
    logger.debug("DBUtil.getNextId with dBName = " + tableName);
    try {
      int max = updateMaxFromTable(connection, tableName);
      connection.commit();
      return max;
    } catch (Exception e) {
      // l'update n'a rien fait, il faut recuperer une valeur par defaut.
      // on recupere le max (depuis la table existante du composant)
      logger.info("Impossible d'updater, if faut recuperer la valeur initiale", e);
    }
    int max = getMaxFromTable(connection, tableName, idName);
    PreparedStatement createStmt = null;
    try {
      // on enregistre le max
      String createStatement = "INSERT INTO UniqueId (maxId, tableName) VALUES (?, ?)";
      createStmt = connection.prepareStatement(createStatement);
      createStmt.setInt(1, max);
      createStmt.setString(2, tableName.toLowerCase(Locale.getDefault()));
      createStmt.executeUpdate();
      connection.commit();
      return max;
    } catch (Exception e) {
      // impossible de creer, on est en concurence, on reessaye l'update.
      logger.info("Impossible de creer, if faut reessayer l'update", e);
      DbUtils.rollback(connection);
    } finally {
      DbUtils.closeQuietly(createStmt);
    }
    max = updateMaxFromTable(connection, tableName);
    connection.commit();
    return max;
  }

  private static int updateMaxFromTable(Connection connection, String tableName)
      throws SQLException {
    String table = tableName.toLowerCase(Locale.ROOT);
    int max = 0;
    PreparedStatement prepStmt = null;
    int count = 0;
    try {
      prepStmt = connection.prepareStatement(
          "UPDATE UniqueId SET maxId = maxId + 1 WHERE tableName = ?");
      prepStmt.setString(1, table);
      count = prepStmt.executeUpdate();
      connection.commit();
    } catch (SQLException sqlex) {
      DbUtils.rollback(connection);
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(prepStmt);
    }

    if (count == 1) {
      PreparedStatement selectStmt = null;
      ResultSet rs = null;
      try {
        // l'update c'est bien passe, on recupere la valeur
        selectStmt = connection.prepareStatement("SELECT maxId FROM UniqueId WHERE tableName = ?");
        selectStmt.setString(1, table);
        rs = selectStmt.executeQuery();
        if (!rs.next()) {
          logger.error("No row for " + table + " found.");
          throw new RuntimeException("Erreur Interne DBUtil.getNextId()");
        }
        max = rs.getInt(1);
      } finally {
        DbUtils.closeQuietly(rs);
        DbUtils.closeQuietly(selectStmt);
      }
      return max;
    }
    throw new SQLException("Update impossible : Ligne non existante");
  }

  private static int getMaxFromTable(Connection con, String tableName, String idName)
      throws SQLException {
    if (!StringUtil.isDefined(tableName) || !StringUtil.isDefined(idName)) {
      return 1;
    }
    Statement prepStmt = con.createStatement();
    ResultSet rs = null;
    try {
      int maxFromTable = 0;
      String nextPKStatement = "SELECT MAX(" + idName + ") " + "FROM " + tableName;
      rs = prepStmt.executeQuery(nextPKStatement);
      if (rs.next()) {
        maxFromTable = rs.getInt(1);
      }
      return maxFromTable + 1;
    } catch (SQLException ex) {
      DbUtils.rollback(con);
      return 1;
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(prepStmt);
    }
  }

  public static void deleteFile(File file) {
    File parent = file.getParentFile();
    FileUtils.deleteQuietly(file);
    if (parent.isDirectory() && parent.list().length == 0) {
      FileUtils.deleteQuietly(parent);
    }
  }

  private ConverterUtil() {
  }
}
