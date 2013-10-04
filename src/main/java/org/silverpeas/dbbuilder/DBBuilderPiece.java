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
package org.silverpeas.dbbuilder;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;
import org.silverpeas.dbbuilder.sql.ConnectionFactory;
import org.silverpeas.dbbuilder.sql.DbProcParameter;
import org.silverpeas.dbbuilder.sql.QueryExecutor;
import org.silverpeas.util.Console;
import org.silverpeas.util.StringUtil;

public abstract class DBBuilderPiece {

  // identifiant unique pour toute la session
  private static AtomicInteger increment = new AtomicInteger(0);
  // identifiant de la pièce si elle est stockée en base
  private String actionInternalID = null;
  // nom de la pièce ou du fichier
  private String pieceName = null;
  // contenu initial de la pièce
  private String content = null;
  // nom de l'action
  private String actionName = null;
  // oui ou non fonctionnement en mode trace
  private boolean traceMode = false;
  // contenu interprété en séquence d'instructions
  protected Instruction[] instructions = null;
  protected Connection connection;
  protected Console console = null;

  // Contructeur utilisé pour une pièce de type fichier
  public DBBuilderPiece(Console console, String pieceName, String actionName, boolean traceMode)
      throws Exception {
    this.console = console;
    this.traceMode = traceMode;
    this.actionName = actionName;
    this.pieceName = pieceName;
    if (pieceName.endsWith(".jar")) {
      content = "";
    } else {
      // charge son contenu sauf pour un jar qui doit être dans le classpath
      File myFile = new File(pieceName);
      if (!myFile.exists() || !myFile.isFile() || !myFile.canRead()) {
        console.printMessage("\t\t***Unable to load : " + pieceName);
        throw new Exception("Unable to find or load : " + pieceName);
      }
      int fileSize = (int) myFile.length();
      byte[] data = new byte[fileSize];
      DataInputStream in = new DataInputStream(new FileInputStream(pieceName));
      try {
        in.readFully(data);
      } finally {
        IOUtils.closeQuietly(in);
      }
      content = new String(data, Charsets.UTF_8);
    }
    Properties res = DBBuilder.getdbBuilderResources();
    if (res != null) {
      for (Enumeration e = res.keys(); e.hasMoreElements();) {
        String key = (String) e.nextElement();
        String value = res.getProperty(key);
        content = StringUtil.sReplace("${" + key + '}', value, content);
      }
    }
  }

  // Contructeur utilisé pour une pièce de type chaîne en mémoire
  public DBBuilderPiece(Console console, String pieceName, String actionName, String content,
      boolean traceMode) throws Exception {
    this.console = console;
    this.traceMode = traceMode;
    this.actionName = actionName;
    this.pieceName = pieceName;
    this.content = content;
  }

  // Contructeur utilisé pour une pièce stockée en base de données
  public DBBuilderPiece(Console console, String actionInternalID, String pieceName,
      String actionName, int itemOrder, boolean traceMode) throws Exception {
    this.console = console;
    this.traceMode = traceMode;
    this.actionName = actionName;
    this.pieceName = pieceName;
    this.actionInternalID = actionInternalID;
    this.content = getContentFromDB(actionInternalID);
  }

  public String getActionInternalID() {
    return actionInternalID;
  }

  public String getPieceName() {
    return pieceName;
  }

  public String getActionName() {
    return actionName;
  }

  /*
   * retourne le contenu du fichier
   */
  public String getContent() {
    return content;
  }

  public Console getConsole() {
    return this.console;
  }

  /*
   * retourne si oui/non mode trace
   */
  public boolean isTraceMode() {
    return traceMode;
  }

  public abstract void setInstructions();

  public abstract void cacheIntoDB(Connection connection, String _package, int _itemOrder)
      throws Exception;

  public Instruction[] getInstructions() {
    return instructions;
  }

  public void traceInstructions() {
    for (Instruction instruction : instructions) {
      System.out.println(instruction.getInstructionText());
    }
  }

  /**
   * Execute via JDBC la séquence d'instructions élémentaires conservées sur instructions[]
   *
   * @param connection
   */
  public void executeInstructions(Connection connection) throws Exception {
    setConnection(connection);
    // try {
    for (Instruction instruction : instructions) {
      String currentInstruction = instruction.getInstructionText();
      switch (instruction.getInstructionType()) {
        case Instruction.IN_INVOKEJAVA:
          executeJavaInvoke(currentInstruction, instruction.getInstructionDetail());
          break;
        case Instruction.IN_CALLDBPROC:
          executeSingleProcedure(currentInstruction, (DbProcParameter[]) instruction.
              getInstructionDetail());
          break;
        case Instruction.IN_UPDATE:
        default:
          executeSingleUpdate(currentInstruction);
          break;
      }
    }
  }

  // Cache en BD via JDBC une séquence de désinstallation
  // le paramètre est la liste des valeurs à insérer dans la table
  // SR_UNINSTITEMS
  public void cacheIntoDB(Connection connexion, String _package, int _itemOrder, String _pieceType,
      String _delimiter, Integer _keepDelimiter, String _dbProcName)
      throws Exception {
    setConnection(connexion);
    PreparedStatement pstmt = null;
    // insertion SR_UNINSTITEMS
    long theLong = System.currentTimeMillis();
    String itemID = String.valueOf(theLong) + '-' + increment.incrementAndGet();
    try {
      pstmt = connexion.prepareStatement("insert into SR_UNINSTITEMS(SR_ITEM_ID, "
          + "SR_PACKAGE, SR_ACTION_TAG, SR_ITEM_ORDER, SR_FILE_NAME, SR_FILE_TYPE, SR_DELIMITER, "
          + "SR_KEEP_DELIMITER, SR_DBPROC_NAME) values ( ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      pstmt.setString(1, itemID);
      pstmt.setString(2, _package);
      pstmt.setString(3, actionName);
      pstmt.setInt(4, _itemOrder);
      pstmt.setString(5, pieceName);
      pstmt.setString(6, _pieceType);
      pstmt.setString(7, _delimiter);
      pstmt.setInt(8, _keepDelimiter);
      pstmt.setString(9, _dbProcName);
      pstmt.executeUpdate();
    } catch (Exception ex) {
      throw new Exception("\n\t\t***ERROR RETURNED BY THE RDBMS : " + ex.getMessage() + '\n', ex);
    } finally {
      DbUtils.closeQuietly(pstmt);
    }
    try {
      // insertion SR_SCRIPTS
      final String[] subS = getSubStrings(content);
      pstmt = connexion.prepareStatement("insert into SR_SCRIPTS(SR_ITEM_ID, SR_SEQ_NUM, SR_TEXT) "
          + "values (?, ?, ? )");
      for (int i = 0; i < subS.length; i++) {
        pstmt.setString(1, itemID);
        pstmt.setInt(2, i);
        pstmt.setString(3, subS[i]);
        pstmt.executeUpdate();
      }
    } catch (Exception ex) {
      throw new Exception("\n\t\t***ERROR RETURNED BY THE RDBMS : " + ex.getMessage() + '\n', ex);
    } finally {
      DbUtils.closeQuietly(pstmt);
    }
  }

  public void executeSingleUpdate(String currentInstruction) throws Exception {
    if (traceMode) {
      String printableInstruction = StringUtil.sReplace("\r\n", " ", currentInstruction);
      printableInstruction = StringUtil.sReplace("\t", " ", printableInstruction);
      if (printableInstruction.length() > 147) {
        printableInstruction = printableInstruction.substring(0, 146) + "...";
      }
      console.printMessage("\t\t>" + printableInstruction);
    }
    Statement stmt = connection.createStatement();
    try {
      stmt.executeUpdate(currentInstruction);
    } catch (Exception e) {
      throw new Exception("\r\n***ERROR RETURNED BY THE RDBMS : " + e.getMessage()
          + "\r\n***STATEMENT ON ERROR IS : " + currentInstruction + " " + pieceName, e);
    } finally {
      DbUtils.closeQuietly(stmt);
    }
  }

  public void executeSingleProcedure(String currentInstruction, DbProcParameter[] params)
      throws Exception {
    if (traceMode) {
      String printableInstruction = StringUtil.sReplace("\n", " ", currentInstruction);
      printableInstruction = StringUtil.sReplace("\t", " ",
          printableInstruction);
      if (printableInstruction.length() > 147) {
        printableInstruction = printableInstruction.substring(0, 146) + "...";
      }
      console.printMessage("\t\t>" + printableInstruction);
    }
    try {
      QueryExecutor.executeProcedure(connection, currentInstruction, params);
    } catch (Exception e) {
      throw new Exception("\r\n***ERROR RETURNED BY THE RDBMS : " + e.getMessage()
          + "\r\n***STATEMENT ON ERROR IS : " + currentInstruction, e);
    }
  }

  public void executeJavaInvoke(String currentInstruction, Object myClass) throws Exception {
    if (traceMode) {
      console.printMessage("\t\t>" + myClass.getClass().getName() + '.' + currentInstruction
          + "()");
    }
    ((DbBuilderDynamicPart) myClass).setConnection(connection);
    Method methode;
    try {
      methode = myClass.getClass().getMethod(currentInstruction);
    } catch (NoSuchMethodException e) {
      throw new Exception("No method \"" + currentInstruction
          + "\" defined for \"" + myClass.getClass().getName() + "\" class.", e);
    } catch (SecurityException e) {
      throw new Exception("No method \"" + currentInstruction
          + "\" defined for \"" + myClass.getClass().getName() + "\" class.", e);
    }
    try {
      methode.invoke(myClass);
    } catch (Exception e) {
      throw new Exception("\n\t\t***ERROR RETURNED BY THE JVM : " + e.getMessage(), e);
    }
  }

  private String[] getSubStrings(String str) {
    int maxl = 1100;
    int nbS = str.length() / maxl;
    if ((str.length() - nbS * maxl) > 0) {
      nbS++;
    }
    String tmpS = str;
    String[] retS = new String[nbS];
    for (int i = 0; i < nbS; i++) {
      if (i == nbS - 1) {
        retS[i] = tmpS;
      } else {
        retS[i] = tmpS.substring(0, maxl - 1);
        tmpS = tmpS.substring(maxl - 1);
      }
    }
    return retS;
  }

  private String getContentFromDB(String itemID) throws Exception {
    StringBuilder dbContent = new StringBuilder("");
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      Connection connexion = ConnectionFactory.getConnection();
      pstmt = connexion.prepareStatement(
          "select SR_SEQ_NUM, SR_TEXT from SR_SCRIPTS where SR_ITEM_ID = ? order by 1");
      pstmt.setString(1, itemID);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        dbContent = dbContent.append(rs.getString("SR_TEXT"));
      }

    } catch (Exception e) {
      throw new Exception("\r\n***ERROR RETURNED BY THE JVM : " + e.getMessage() 
          + "\r\n(select SR_SEQ_NUM, SR_TEXT from SR_SCRIPTS where SR_ITEM_ID = '" + itemID
          + "'  order by 1)");
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(pstmt);
    }
    return dbContent.toString();
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }
}
