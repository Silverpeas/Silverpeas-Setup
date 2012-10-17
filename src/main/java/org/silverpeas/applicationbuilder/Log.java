/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//Source file: R:\\StraProduct\\Pkg1.0\\Dev\\SrcJava\\Java\\ApplicationBuilder\\JBuilderEnv\\src\\com\\silverpeas\\applicationbuilder\\Log.java

package org.silverpeas.applicationbuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.GregorianCalendar;

import org.silverpeas.installedtree.DirectoryLocator;

/**
 * Makes logging available to every class in this package. The static initializer opens the log file
 * and closing is ensured by the "finalize" method of a FileOutputStream.
 * @author Silverpeas
 * @version 1.0/B
 * @since 1.0/B
 */
public class Log {

  /**
   * The name of the log file to generate
   * @since 1.0
   */
  private static final String name = "applicationBuilder.log";

  /**
   * The stream to write to. It is obtained from a FileOutputStream based on the log file. The
   * absolute path to the log file is builded from the name and the target home asked to the
   * ApplicationBuilder class.
   * @since 1.0
   */
  private static PrintWriter out = null;

  private static boolean logOutputIsScreen = false;
  private static boolean echoAsDotEnabled = false;

  static {
    try {
      File logFile = new File(DirectoryLocator.getLogHome(), name);
      logFile.getParentFile().mkdirs();
      out = new PrintWriter(new FileWriter(logFile));
    } catch (Exception e) {
      System.err.println("Logging to file is disabled");
      e.printStackTrace();
      logOutputIsScreen = true;
    }
    if (!logOutputIsScreen) {
      out.println("Application Builder Log File - BEGIN "
          + GregorianCalendar.getInstance().getTime());
      out.println("------------------------------");
    }
  }

  public static String getName() {
    return name;
  }

  /**
   * If enabled, any message added to log file is displayed on screen as a dot ('.') If disabled,
   * messages are only added to log file disabled by default
   */
  public static void setEchoAsDotEnabled(boolean on) {
    if (echoAsDotEnabled && !on) {
      System.out.println();
    }
    echoAsDotEnabled = on;
  }

  /**
   * Adds a message in the log file with echo to standard output
   */
  public static void echo(String message) {
    echo(message, System.out);
  }

  /**
   * Adds a message in the log file with echo to given output
   */
  public static void echo(String message, PrintStream output) {
    add(message);
    if (!logOutputIsScreen) {
      output.println(message);
    }
  }

  /**
   * Adds an AppBuilderException message in the log file with echo to standard error output
   */
  public static void echo(AppBuilderException abe) {
    add(abe);
    if (!logOutputIsScreen) {
      abe.printLogMessage();
    }
  }

  /**
   * Adds any Throwable message in the log file with echo to standard error output
   */
  public static void echo(Throwable t) {
    add(t);
    if (!logOutputIsScreen) {
      t.printStackTrace();
    }
  }

  /**
   * Adds a line in the log file.
   * @since 1.0/B
   * @roseuid 3AB0A0200026
   */
  public static void add(String message) {
    if (logOutputIsScreen) {
      System.out.println(message);
    } else {
      out.println(message);
      if (echoAsDotEnabled) {
        System.out.print('.');
      }
    }
  }

  /**
   * Adds the exception message in the log file
   * @since 1.0/B
   * @roseuid 3AB0A0200026
   */
  public static void add(Throwable exception) {
    if (logOutputIsScreen) {
      exception.printStackTrace();
    } else {
      exception.printStackTrace(out);
      if (echoAsDotEnabled) {
        System.out.print('.');
      }
    }
  }

  /**
   * Adds the exception message in the log file
   * @since 1.0/B
   * @roseuid 3AB0A0200026
   */
  public static void add(AppBuilderException abe) {
    if (logOutputIsScreen) {
      abe.printLogMessage();
    } else {
      abe.printLogMessage(out);
      if (echoAsDotEnabled) {
        System.out.print('.');
      }
    }
  }

  public static void close() {
    if (!logOutputIsScreen) {
      out.println("------------------------------");
      out.println("Application Builder Log File - END "
          + GregorianCalendar.getInstance().getTime());
      out.close();
    }
  }
}
