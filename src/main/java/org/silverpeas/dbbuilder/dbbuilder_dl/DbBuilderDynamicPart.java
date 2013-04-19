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
package org.silverpeas.dbbuilder.dbbuilder_dl;


import java.sql.Connection;

import org.silverpeas.util.Console;

/**
 * Titre : Description : Copyright : Copyright (c) 2002 Société :
 * @author
 * @version 1.0
 */
public abstract class DbBuilderDynamicPart {

  private String SILVERPEAS_HOME = null;
  private String SILVERPEAS_DATA = null;
  private Console console;
  private Connection con = null;

  public DbBuilderDynamicPart() {
  }

  public void setSILVERPEAS_HOME(String sh) throws Exception {
    if (SILVERPEAS_HOME != null) {
      throw new Exception(
          "DbBuilderDynamicPart.setSILVERPEAS_HOME() fatal error : SILVERPEAS_HOME is already set.");
    }
    SILVERPEAS_HOME = sh;
  }

  public void setSILVERPEAS_DATA(String sh) throws Exception {
    if (SILVERPEAS_DATA != null) {
      throw new Exception(
          "DbBuilderDynamicPart.setSILVERPEAS_DATA() fatal error : SILVERPEAS_DATA is already set.");
    }
    SILVERPEAS_DATA = sh;
  }

  public void setConnection(Connection con) {
    this.con = con;
  }

  public void setConsole(final Console console) {
    this.console = console;
  }

  public String getSILVERPEAS_HOME() {
    return SILVERPEAS_HOME;
  }

  public String getSILVERPEAS_DATA() {
    return SILVERPEAS_DATA;
  }

  public Connection getConnection() {
    return con;
  }

  public Console getConsole() {
    return console;
  }
}