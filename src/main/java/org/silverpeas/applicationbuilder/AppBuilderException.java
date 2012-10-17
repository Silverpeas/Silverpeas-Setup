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

package org.silverpeas.applicationbuilder;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Makes logging available to every class in this package. The static initializer opens the log file
 * and closing is ensured by the "finalize" method of a FileOutputStream.
 * @author Silverpeas
 * @version 1.0/B
 * @since 1.0/B
 */
public class AppBuilderException extends Exception {

  private Throwable nestedException = null;

  public AppBuilderException() {
    super();
  }

  public AppBuilderException(String message) {
    super(message);
  }

  public AppBuilderException(String message, Throwable underlyingException) {
    super(message);
    nestedException = underlyingException;
  }

  public void printStackTrace() {
    if (nestedException != null) {
      System.err.println(getMessage());
      nestedException.printStackTrace();
    } else {
      super.printStackTrace();
    }
  }

  public void printStackTrace(PrintStream s) {
    if (nestedException != null) {
      s.println(getMessage());
      nestedException.printStackTrace(s);
    } else {
      super.printStackTrace(s);
    }
  }

  public void printStackTrace(PrintWriter s) {
    if (nestedException != null) {
      s.println(getMessage());
      nestedException.printStackTrace(s);
    } else {
      super.printStackTrace(s);
    }
  }

  public void printLogMessage() {
    if (getMessage() != null && !getMessage().trim().equals("")) {
      System.err.println(getMessage());
    }
    if (nestedException != null) {
      nestedException.printStackTrace();
    }
  }

  public void printLogMessage(PrintStream s) {
    if (getMessage() != null && !getMessage().trim().equals("")) {
      s.println(getMessage());
    }
    if (nestedException != null) {
      nestedException.printStackTrace(s);
    }
  }

  public void printLogMessage(PrintWriter s) {
    if (getMessage() != null && !getMessage().trim().equals("")) {
      s.println(getMessage());
    }
    if (nestedException != null) {
      nestedException.printStackTrace(s);
    }
  }
}