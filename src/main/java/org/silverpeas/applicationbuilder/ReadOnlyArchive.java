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
package org.silverpeas.applicationbuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;

import org.silverpeas.util.Console;

/**
 * Convenient class for archive reading in conjunction with WriteOnlyArchive.
 *
 * @author Silverpeas
 * @version 1.0/B
 * @since 1.0/B
 */
public class ReadOnlyArchive extends ApplicationBuilderItem {

  private JarFile myJar = null;
  protected Console console;

  /**
   * Builder for a read only archive.
   *
   * @param directory The absolute path to the directory hosting the archive
   * @param fileName The name of the archive in the file system
   * @param console
   * @throws AppBuilderException
   * @since 1.0
   */
  public ReadOnlyArchive(File directory, String fileName, Console console) throws
      AppBuilderException {
    super(directory, fileName);
    this.console = console;
    setJar();
  }

  /**
   * Gets the Archive file
   *
   * @return the JarFile object denoting this archive
   */
  private JarFile getJar() {
    return myJar;
  }

  /**
   * Sets the Archive file
   */
  private void setJar() throws AppBuilderException {
    if (!getPath().exists()) {
      throw new AppBuilderException(getPath().getAbsolutePath() + " not found");
    }
    if (!getPath().isFile()) {
      throw new AppBuilderException(getPath().getAbsolutePath() + " is not a file");
    }
    if (!getPath().canRead()) {
      throw new AppBuilderException(getPath().getAbsolutePath() + " is not readable");
    }
    try {
      myJar = new JarFile(getPath(), false);
    } catch (IOException ioe) {
      throw new AppBuilderException(getName() + " : could not instantiate JarFile", ioe);
    }
  }

  /**
   * Gets the available entries in the archive. It also opens the archive for reading.
   *
   * @return the entries of this archive
   * @since 1.0
   * @roseuid 3AAFB0770391
   */
  public ApplicationBuilderItem[] getEntries() {
    if (getJar() == null) {
      return null;
    }
    List<ApplicationBuilderItem> entries = new ArrayList<ApplicationBuilderItem>(getJar().size());
    for (Enumeration<JarEntry> e = getJar().entries(); e.hasMoreElements();) {
      JarEntry jarEntry = e.nextElement();
      if (!jarEntry.isDirectory()) {
        File oneFile = new File(jarEntry.getName());
        ApplicationBuilderItem item = new ApplicationBuilderItem(oneFile.getParent(), oneFile.
            getName());
        item.setSize(jarEntry.getSize());
        entries.add(item);
      }
    }
    return entries.toArray(new ApplicationBuilderItem[entries.size()]);
  }

  /**
   * @param entry the entry to read
   * @return the stream for reading the contents of the entry.
   */
  public InputStream getEntry(ApplicationBuilderItem entry) {
    InputStream in = null;
    try {
      JarEntry jarEntry = getJarEntry(entry);
      if (jarEntry == null) {
        return null;
      }
      in = getJar().getInputStream(jarEntry);
    } catch (IOException ioe) {
      console.printError("Could not get input stream from item \"" + entry.getName() + '"', ioe);
    }
    return in;
  }

  /**
   * @param entry the entry to read
   * @return the stream for reading the contents of the entry
   * @since 1.0/B
   * @roseuid 3AB080F602D2
   */
  public long getEntrySize(ApplicationBuilderItem entry) {
    long size = 0L;
    JarEntry jarEntry = getJarEntry(entry);
    if (jarEntry != null) {
      size = jarEntry.getSize();
    } 
    return size;
  }

  /**
   * When no more entries have to be read, call this method to close the archive.
   * @throws AppBuilderException 
   */
  public void close() throws AppBuilderException {
    if (getJar() != null) {
      try {
        getJar().close();
      } catch (IOException ioe) {
        throw new AppBuilderException(getName() + " : could not close JarFile object", ioe);
      }
    }
  }

  /**
   * @param entry the entry to read
   * @return the jarEntry of the entry (null if not found or directory)
   * @since 1.0/B
   */
  private JarEntry getJarEntry(ApplicationBuilderItem entry) {
    if (getJar() == null) {
      return null;
    }
    Enumeration<JarEntry> e = getJar().entries();
    while (e.hasMoreElements()) {
      JarEntry jarEntry = e.nextElement();
      File entryFile = new File(jarEntry.getName());
      if (entryFile.getPath().equals(entry.getArchivePath())) {
        return jarEntry;
      }
    }
    return null;
  }
}
