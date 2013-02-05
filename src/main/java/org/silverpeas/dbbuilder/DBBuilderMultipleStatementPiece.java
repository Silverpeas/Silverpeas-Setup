/**
 * Copyright (C) 2000 - 2012 Silverpeas
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

import org.silverpeas.util.Console;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.silverpeas.util.StringUtil;

public class DBBuilderMultipleStatementPiece extends DBBuilderPiece {

  private String delimiter = null;
  private boolean keepDelimiter = false;

  // contructeurs non utilisés
  private DBBuilderMultipleStatementPiece(Console console, String pieceName, String actionName,
      boolean traceMode) throws Exception {
    super(console, pieceName, actionName, traceMode);
  }

  private DBBuilderMultipleStatementPiece(Console console, String pieceName, String actionName,
      String content, boolean traceMode) throws Exception {
    super(console, pieceName, actionName, content, traceMode);
  }

  private DBBuilderMultipleStatementPiece(Console console, String actionInternalID, String pieceName,
      String actionName, int itemOrder, boolean traceMode) throws Exception {
    super(console, actionInternalID, pieceName, actionName, itemOrder, traceMode);
  }

  // Contructeur utilisé pour une pièce de type fichier
  public DBBuilderMultipleStatementPiece(Console console, String pieceName, String actionName,
      boolean traceMode, String delimiter, boolean keepDelimiter) throws Exception {
    super(console, pieceName, actionName, traceMode);
    moreInitialize(delimiter, keepDelimiter);
  }

  // Contructeur utilisé pour une pièce de type chaîne en mémoire
  public DBBuilderMultipleStatementPiece(Console console, String pieceName, String actionName,
      String content, boolean traceMode, String delimiter, boolean keepDelimiter) throws Exception {
    super(console, pieceName, actionName, content, traceMode);
    moreInitialize(delimiter, keepDelimiter);
  }

  // Contructeur utilisé pour une pièce stockée en base de données
  public DBBuilderMultipleStatementPiece(Console console, String actionInternalID, String pieceName,
      String actionName, int itemOrder, boolean traceMode, String delimiter, boolean keepDelimiter)
      throws Exception {
    super(console, actionInternalID, pieceName, actionName, itemOrder, traceMode);
    moreInitialize(delimiter, keepDelimiter);
  }

  private void moreInitialize(String delimiter, boolean keepDelimiter) throws Exception {
    if (delimiter == null) {
      throw new Exception("Missing <delimiter> tag for \"pieceName\" item.");
    }
    String d = StringUtil.sReplace("\\n", "\n", delimiter);
    d = StringUtil.sReplace("\\t", "\t", d);
    this.delimiter = d;
    this.keepDelimiter = keepDelimiter;
    setInstructions();
  }

  @Override
  public void setInstructions() {
    if (getContent() != null && delimiter != null) {
      List<String> tokens = tokenizeAll(getContent(), delimiter, keepDelimiter);
      instructions = new Instruction[tokens.size()];
      for (int i = 0; i < tokens.size(); i++) {
        instructions[i] = new Instruction(Instruction.IN_UPDATE, tokens.get(i), null);
      }
    }
  }

  @Override
  public void cacheIntoDB(Connection connection, String _package, int _itemOrder) throws Exception {
    Integer kd;
    if (keepDelimiter) {
      kd = 1;
    } else {
      kd = 0;
    }
    cacheIntoDB(connection, _package, _itemOrder, DBBuilderFileItem.FILEATTRIBSEQUENCE_VALUE,
        delimiter, kd, null);
  }

  private List<String> tokenizeAll(String str, String delimiter, boolean keepDelimiter) {
    List<String> tokens = new ArrayList<String>();
    int curi = 0;
    while (curi < str.length() && curi >= 0) {
      int previ = curi;
      curi = str.indexOf(delimiter, curi);
      if (curi < str.length() && curi >= 0) {
        int endIndex = curi;
        if (keepDelimiter) {
          endIndex = curi + delimiter.length();
        }
        String instruction = str.substring(previ, endIndex).trim();
        if (!" ".equals(instruction) && instruction != null && !instruction.isEmpty()) {
          tokens.add(instruction);
        }
        curi += delimiter.length();
      } else if (str.length() - previ > delimiter.length()) {
        String instruction = str.substring(previ, str.length()).trim();
        if (!" ".equals(instruction) && instruction != null && !instruction.isEmpty()) {
          if (keepDelimiter) {
            instruction = instruction + delimiter;
          }
          tokens.add(instruction);
        }
      }
    }
    return tokens;
  }
}