/*
 * Copyright (C) 2000 - 2012 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection withWriter Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.util;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.silverpeas.dbbuilder.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Console into which messages are displayed. It wraps the source into which messages are printed
 * out.
 */
public class Console {
  private static final String newline = System.getProperty("line.separator");
  private final Logger logger;
  private File logFile;
  private PrintWriter logBuffer;
  private boolean echoAsDotEnabled = true;

  /**
   * Creates and open a console upon the specified file. All messages will be printed into the file.
   * The file will be created in the directory provided by the Configuration.getLogDir() method.
   *
   * @param fileName the name of the file into which the messages will be printed.
   * @param builderClass the class of the builder.
   * @throws IOException if an error occurs while creating the console.
   */
  public Console(final String fileName, final Class builderClass) throws IOException {
    logger = LoggerFactory.getLogger(builderClass);
    logFile = new File(Configuration.getLogDir(), fileName);
    FileUtils.forceMkdir(logFile.getParentFile());
    logBuffer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
        logFile.getAbsolutePath(), true), Charsets.UTF_8)));
  }

  public Console(final Class builderClass) {
    logger = LoggerFactory.getLogger(builderClass);
  }

  /**
   * Creates and open a console upon the standard system output.
   */
  public Console() {
    logger = LoggerFactory.getLogger(Console.class);
  }

  public synchronized void printError(String errMsg, Exception ex) {
    printMessage(errMsg, true);
    if (null != logBuffer) {
      ex.printStackTrace(logBuffer);
    }
    logger.error(errMsg, ex);
  }

  public synchronized void printError(String errMsg) {
    printMessage(errMsg, true);
    logger.error(errMsg);
  }

  public synchronized void printWarning(String errMsg) {
    printMessage(errMsg, true);
    logger.warn(errMsg);
  }

  public synchronized void printWarning(String errMsg, Exception ex) {
    printMessage(errMsg, true);
    logger.warn(errMsg, ex);
  }

  public synchronized void printMessage(String msg) {
    printMessage(msg, false);
  }

  public synchronized void printTrace(String msg) {
    logger.debug(msg);
  }

  private synchronized void printMessage(String msg, boolean isError) {
    String message = "Thread " + Thread.currentThread().getId() + " - " + (msg != null ? msg : "");
    if (null != logBuffer) {
      logBuffer.print(message);
    }
    if (echoAsDotEnabled) {
      System.out.print(".");
    } else {
      System.out.println(newline + message + newline);
    }
    if (!isError) {
      logger.info(message);
    }
  }

  public synchronized void close() {
    if (null != logBuffer) {
      logBuffer.close();
    }
  }

  public synchronized void setEchoAsDotEnabled(boolean on) {
    echoAsDotEnabled = on;
  }
}
