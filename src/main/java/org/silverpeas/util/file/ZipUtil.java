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
package org.silverpeas.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public final class ZipUtil {

  /**
   * ---------------------------------------------------------------------
   * @param fromFile
   * @param toDir
   * @throws IOException
   * @throws ZipException
   * @see
   */
  public static void unzip(final String fromFile, final String toDir) throws IOException {
    unzip(new File(fromFile), new File(toDir));
  }

  /**
   * ---------------------------------------------------------------------
   * @param from
   * @param to
   * @throws IOException
   * @throws IllegalArgumentException
   * @see
   */
  public static void unzip(final File fromZipFile, final File toDir) throws IOException {
    if (!toDir.exists()) {
      toDir.mkdirs();
    }
    final ZipFile zipFile = new ZipFile(fromZipFile);
    ZipInputStream zipInput = null;
    try {
      zipInput = new ZipInputStream(new FileInputStream(fromZipFile));
      ZipEntry entry;
      while ((entry = zipInput.getNextEntry()) != null) {
        extractFile(zipFile, entry, toDir);
        zipInput.closeEntry();
      }
    } finally {
      if (zipInput != null) {
        IOUtils.closeQuietly(zipInput);
      }
      zipFile.close();
    }
  }

  /**
   * ---------------------------------------------------------------------
   * @param zipFile
   * @param zipEntry
   * @param toDir
   * @throws Exception
   * @see
   */
  private static void extractFile(final ZipFile zipFile, final ZipEntry zipEntry, final File toDir)
      throws IOException {
    final File toFile = new File(toDir.getAbsolutePath(), zipEntry.getName());
    if (zipEntry.isDirectory()) {
      FileUtils.forceMkdir(toFile);
    } else if (!toFile.exists()) {
      toFile.getParentFile().mkdirs();
      final OutputStream out = FileUtils.openOutputStream(toFile);
      IOUtils.copy(zipFile.getInputStream(zipEntry), out);
      out.close();
    }
  }
}
