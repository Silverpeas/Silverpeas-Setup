/**
 * Copyright (C) 2000 - 2012 Silverpeas
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

package org.silverpeas.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 * TODO : replace with commons-io
 * @author ehugonnet
 */
public final class FileUtil {

  private FileUtil() {
  }

  /**
   * Utility method to copy a file from one directory to another
   */
  public static void copyFile(final String fromPath, final String toPath) throws IOException {
    FileUtils.copyFileToDirectory(new File(fromPath), new File(toPath));
  }

  /**
   * Utility method to copy a file from one directory to another
   */
  public static void copyFile(final String fileName, final String fromDir, final String toDir)
      throws IOException {
    FileUtils.copyFile(new File(fromDir,fileName), new File(toDir,fileName));
  }

  /**
   * Utility method to copy a file from one directory to another
   */
  public static void copyFile(final File from, final File toDir) throws IOException {
    if (!from.canRead()) {
      throw new IOException("Cannot read file '" + from + "'.");
    }
    if (toDir.exists() && (!toDir.canWrite())) {
      throw new IOException("Cannot write to file '" + toDir + "'.");
    }
    toDir.getParentFile().mkdirs();
    FileUtils.copyFile(from, toDir);
  }

  /**
   * ---------------------------------------------------------------------
   * @param from
   * @param to
   * @throws IOException
   * @see
   */
  public static void copyDir(final String fromDir, final String toDir) throws IOException {
    FileUtils.copyDirectory(new File(fromDir), new File(toDir));
  }

  /**
   * ---------------------------------------------------------------------
   * @param from
   * @param to
   * @throws IOException if from doesn't exist
   * @see
   */
  public static void copyDir(final File fromDir, final File toDir) throws IOException {
    if (!fromDir.exists()) {
      throw new IOException("Cannot found file or directory \"" + fromDir + "\".");
    }
    if (fromDir.isFile()) {
      copyFile(fromDir, toDir);
    } else {
      if (toDir.isFile()) {
        throw new IOException("cannot copy directory \"" + fromDir
            + "\" into the file" + toDir + "\".");
      }
      toDir.mkdirs();
      FileUtils.copyDirectory(fromDir, toDir);
    }
  }



  /**
   * Utility method to get extension of a file return empty String if @file doesn't exist or if @file
   * doesn't have extension
   */
  public static String getExtension(final File file) throws IOException {
    return FilenameUtils.getExtension(file.getName());
  }

  /**
   * Utility method to get extension of a file return empty String if @file doesn't have extension
   */
  public static String getExtension(final String file) throws IOException {
    return FilenameUtils.getExtension(file);
  }

  public static void deleteFiles(final String _file) throws IOException {
    // Deleting the children recursively
    final File file = new File(_file);
    FileUtils.forceDelete(file);
  }

  public static void deleteFilesOnExit(final String _file) throws IOException {
    // Deleting the children recursively
    final File file = new File(_file);
    FileUtils.forceDeleteOnExit(file);
  }
}
