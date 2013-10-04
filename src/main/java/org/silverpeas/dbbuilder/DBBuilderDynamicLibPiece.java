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

import java.sql.Connection;

import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;
import org.silverpeas.dbbuilder.util.Configuration;
import org.silverpeas.dbbuilder.util.DynamicLoader;
import org.silverpeas.util.Console;
import org.silverpeas.util.ConfigurationHolder;

public class DBBuilderDynamicLibPiece extends DBBuilderPiece {

  private String method;
  private final DynamicLoader loader;
  private DbBuilderDynamicPart dynamicPart = null;

  // contructeurs non utilisés
  private DBBuilderDynamicLibPiece(Console console, String pieceName, String actionName,
      boolean traceMode) throws Exception {
    super(console, pieceName, actionName, traceMode);
    loader = new DynamicLoader(console);
  }

  private DBBuilderDynamicLibPiece(Console console, String pieceName, String actionName,
      String content, boolean traceMode) throws Exception {
    super(console, pieceName, actionName, content, traceMode);
    loader = new DynamicLoader(console);
  }

  // Contructeur utilisé pour une pièce de type fichier
  public DBBuilderDynamicLibPiece(Console console, String pieceName, String actionName,
      boolean traceMode, String className, String methodName) throws Exception {
    super(console, pieceName, actionName, traceMode);
    loader = new DynamicLoader(console);
    moreInitialize(className, methodName);
  }

  // Contructeur utilisé pour une pièce de type chaîne en mémoire
  public DBBuilderDynamicLibPiece(Console console, String pieceName, String actionName,
      String content, boolean traceMode, String className, String methodName) throws Exception {
    super(console, pieceName, actionName, content, traceMode);
    loader = new DynamicLoader(console);
    moreInitialize(className, methodName);
  }

  private void moreInitialize(String className, String methodName) throws Exception {
    if (className == null) {
      throw new Exception("Missing <classname> tag for \"pieceName\" item.");
    }
    if (methodName == null) {
      throw new Exception("Missing <methodname> tag for \"pieceName\" item.");
    }

    try {
      dynamicPart = loader.loadDynamicPart(className);
      method = methodName;
    } catch (IllegalAccessException e) {
      throw new Exception("Unable to load \"" + className + "\" class.");
    } catch (ClassNotFoundException e) {
      throw new Exception("Unable to load \"" + className + "\" class.");
    } catch (InstantiationException e) {
      throw new Exception("Unable to load \"" + className + "\" class.");
    }
    dynamicPart.setSILVERPEAS_HOME(ConfigurationHolder.getHome());
    dynamicPart.setSILVERPEAS_DATA(Configuration.getData());
    dynamicPart.setConsole(getConsole());
    setInstructions();
  }

  @Override
  public void setInstructions() {
    instructions = new Instruction[1];
    instructions[0] = new Instruction(Instruction.IN_INVOKEJAVA, method, dynamicPart);
  }

  @Override
  public void cacheIntoDB(Connection connection, String _package, int _itemOrder) throws Exception {
    // rien à cacher pour une proc dynamique
  }
  
  @Override  
  public void setConnection(Connection connection) {
    super.setConnection(connection);
    if(dynamicPart != null) {
      dynamicPart.setConnection(connection);
    }
  }
}