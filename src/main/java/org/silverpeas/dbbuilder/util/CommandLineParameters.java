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
package org.silverpeas.dbbuilder.util;

import java.util.Locale;

import org.silverpeas.util.Console;
import org.silverpeas.dbbuilder.DBBuilder;

import static org.silverpeas.dbbuilder.util.Action.ACTION_ENFORCE_UNINSTALL;
import static org.silverpeas.dbbuilder.util.Action.ACTION_NONE;

/**
 * Parse the command line paramters for launching the DBBuilder.
 * @author ehugonnet
 */
public class CommandLineParameters {
  private Console console;
  private static final String USAGE =
      "DBBuilder usage: DBBuilder <action> -T <Targeted DB Server type> [-v(erbose)] [-s(imulate)]\n"
          + "where <action> == -C(onnection only) | -I(nstall only) | -U(ninstall only) | -O(ptimize only) | -A(ll) | -S(tatus) | -FU(Force Uninstall) <module> \n "
          + "      <Targeted DB Server type> == MSSQL | ORACLE | POSTGRES | H2\n";
  private Action action = null;
  private DatabaseType dbType = null;
  private boolean verbose = false;
  private boolean simulate = false;
  private String moduleName = null;

  public CommandLineParameters(Console console, String[] commandLineArgs) throws Exception {
    this.console = console;
    testParams(commandLineArgs);
  }

  /**
   * @return the action
   */
  public Action getAction() {
    return action;
  }

  /**
   * @return the dbType
   */
  public DatabaseType getDbType() {
    return dbType;
  }

  /**
   * @return the verbose
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * @return the simulate
   */
  public boolean isSimulate() {
    return simulate;
  }

  /**
   * @return the moduleName
   */
  public String getModuleName() {
    return moduleName;
  }

  protected final void testParams(String[] args) throws Exception {
    boolean getDBType = false;
    boolean getModuleName = false;
    verbose = false;
    simulate = false;
    for (int i = 0; i < args.length; i++) {
      String curArg = args[i];
      if ("-T".equals(curArg)) {
        if (getDBType || getModuleName) {
          console.printError(USAGE);
          throw new Exception();
        }
        if (null != dbType) {
          console.printError(USAGE);
          throw new Exception();
        }
        getDBType = true;
      } else if (ACTION_NONE != Action.getAction(curArg)) {
        if (getDBType || getModuleName) {
          console.printError(USAGE);
          throw new Exception();
        }
        if (null != action) {
          console.printError(USAGE);
          throw new Exception();
        }
        action = Action.getAction(curArg);
        if (ACTION_ENFORCE_UNINSTALL == action) {
          getModuleName = true;
        }
      } else if ("-v".equals(curArg)) {
        if (getDBType || getModuleName) {
          console.printError(USAGE);
          throw new Exception();
        }
        verbose = true;
      } else if ("-s".equals(curArg)) {
        if (getDBType || getModuleName) {
          console.printError(USAGE);
          throw new Exception();
        }
        simulate = true;
      } else {
        if (!getDBType && !getModuleName) {
          console.printError(USAGE);
          throw new Exception();
        }
        if (getDBType) {
          dbType = DatabaseType.valueOf(curArg.toUpperCase(Locale.FRENCH));
        } else if (getModuleName) {
          moduleName = curArg;
        }
        getDBType = false;
        getModuleName = false;
      }
    }
    if (null == dbType || null == action) {
      console.printError(USAGE);
      throw new Exception();
    }

    if (null == moduleName && getModuleName) {
      console.printError(USAGE);
      throw new Exception();
    }

  }
}
