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
package org.silverpeas.applicationbuilder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import org.silverpeas.util.Console;

/**
 * Convenient class for building archive.
 */
public class EARDirectory extends ApplicationBuilderItem {

  protected static final String MANIFEST_PATH = "META-INF" + File.separator + "MANIFEST.MF";
  protected Console console;
  File earDir = null;
  private Map<String, String> alreadyAddedFiles = new HashMap<String, String>();

  /**
   * Builder for a write only archive.
   *
   * @param directory The absolute path to the directory hosting the archive
   * @param fileName The name of the archive in the file system
   * @param console
   * @throws AppBuilderException
   * @since 1.0
   */
  public EARDirectory(File directory, String fileName, Console console)
      throws AppBuilderException {
    earDir = new File(directory, fileName);
    this.console = console;
  }

  /**
   * Adds an XML file in the archive by the means of streams.
   *
   * @param xmlDoc the XML document to add in the archive
   * @throws AppBuilderException
   */
  public void add(XmlDocument xmlDoc) throws AppBuilderException {
    OutputStream out = null;
    try {
      File entry = getNormalizedEntry(xmlDoc.getLocation());
      out = new BufferedOutputStream(new FileOutputStream(new File(entry, xmlDoc.getName())));
      xmlDoc.saveTo(out);
      out.flush();
    } catch (Exception e) {
      throw new AppBuilderException(getName() + " : impossible to add the document \"" + xmlDoc.
          getArchivePath() + '"', e);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * Adds an entry to the archive. The entry added is fetched from the file system
   *
   * @param entry the file to add
   * @throws AppBuilderException
   */
  public void add(ApplicationBuilderItem entry) throws AppBuilderException {
    InputStream contents = null;
    try {
      contents = entry.getPath().toURI().toURL().openStream();
      add(entry, contents);
    } catch (MalformedURLException mue) {
      throw new AppBuilderException(getName() + " : could not add \"" + entry.getName() + '"', mue);
    } catch (IOException ioe) {
      throw new AppBuilderException(getName() + " : could not add \"" + entry.getName() + '"', ioe);
    } finally {
      IOUtils.closeQuietly(contents);
    }
  }

  /**
   * Merges an archive with this archive.
   *
   * @param archive the archive to merge
   * @since 1.0
   */
  public void mergeWith(ReadOnlyArchive archive) throws AppBuilderException {
    mergeWith(archive, (Set) null);
  }

  /**
   * Merges an archive with this archive filtering the entries to exclude one of them.
   *
   * @param archive the archive to merge
   * @param entryToExclude the entry to exclude from merge. Contains the archive path of the entry
   * to exclude.
   * @since 1.0
   */
  public void mergeWith(ReadOnlyArchive archive, String entryToExclude) throws AppBuilderException {
    Set<String> excludeSet = new HashSet<String>(1);
    excludeSet.add(entryToExclude);
    mergeWith(archive, excludeSet);
  }

  /**
   * Merges an archive with this archive filtering the entries to exclude some of them.
   *
   * @param archive the archive to merge
   * @param entriesToExclude the entries to exclude from merge. Contains the archive paths of the
   * entries to exclude.
   * @since 1.0
   */
  public void mergeWith(ReadOnlyArchive archive, Set entriesToExclude) throws AppBuilderException {
    ApplicationBuilderItem[] entries = archive.getEntries();
    boolean filterOn = ((entriesToExclude != null) && (!entriesToExclude.isEmpty()));
    for (ApplicationBuilderItem myEntry : entries) {
      if (!filterOn || !entriesToExclude.contains(myEntry.getArchivePath())) {
        if (alreadyAddedFiles.containsKey(myEntry.getArchivePath())) {
          console.printMessage(getName() + " : already added from \"" + alreadyAddedFiles.get(
              myEntry.getArchivePath()) + "\" : \"" + archive.getName() + '!' + myEntry.
              getArchivePath() + "\" ");
        } else {
          alreadyAddedFiles.put(myEntry.getArchivePath(), archive.getName());
          InputStream contents = archive.getEntry(myEntry);
          try {
            add(myEntry, contents);
          } finally {
            IOUtils.closeQuietly(contents);
          }
        }
      }
    }
  }

  /**
   * Adds a new entry from a stream. The entry is placed and named according to the entry. It can be
   * usefull when merging two archives.
   *
   * @param entry the description of the new entry
   * @param in the stream carrying the contents of the new entry
   */
  public void add(ApplicationBuilderItem entry, InputStream contents) throws AppBuilderException {
    OutputStream out = null;
    try {
      File destEntry;
      if (entry.getLocation() != null) {
        destEntry = getNormalizedEntry(entry.getLocation());
      } else {
        destEntry = earDir;
      }
      out = new BufferedOutputStream(new FileOutputStream(new File(destEntry, entry.getName())));
    } catch (Exception e) {
      throw new AppBuilderException(getName() + " : impossible to create new entry \"" + entry.
          getArchivePath() + '"', e);
    }
    try {
      IOUtils.copy(contents, out);
      out.flush();
    } catch (Exception e) {
      throw new AppBuilderException(getName() + " : impossible to write contents of \"" + entry.
          getArchivePath() + '"', e);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  private File getNormalizedEntry(String path) {
    File normalizedEntry = new File(this.earDir, path);
    normalizedEntry.mkdirs();
    return normalizedEntry;
  }
}
