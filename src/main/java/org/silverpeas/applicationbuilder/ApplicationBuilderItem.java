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

//Source file: R:\\StraProduct\\Pkg1.0\\Dev\\SrcJava\\Java\\ApplicationBuilder\\JBuilderEnv\\src\\com\\silverpeas\\applicationbuilder\\ApplicationBuilderItem.java

package org.silverpeas.applicationbuilder;

import java.io.File;

/**
 * @author Silverpeas
 * @version 1.0
 * @since 1.0
 */
public class ApplicationBuilderItem {

  /**
   * The name of the item. It can be the name of a file or the name of an archive entry.
   * @since 1.0
   */
  private String name = null;

  /**
   * The location of the item in an archive. examples : "library" for JAR libraries, "WEB-INF" for a
   * descriptor.
   * @since 1.0
   */
  private String location = null;

  /**
   * Used for a file item. The absolute path of a directory in the file system hosting the file.
   * @since 1.0
   */
  private File home = null;

  /**
   * A convenience attribute built as needed from attributes "home" and "name". The absolute path to
   * the file item in the file system.
   * @since 1.0
   */
  private File path = null;

  /**
   * A convenience attribute built as needed from attributes "location" and "name". The path to the
   * item in an archive
   * @since 1.0
   */
  private String archivePath = null;

  private long size = -1;

  public ApplicationBuilderItem() {
  }

  /**
   * This builder is for an Archive Item.
   * @param directory the name of a directory in an archive hosting the item.
   * @param itemName the name of the item
   * @since 1.0
   */
  public ApplicationBuilderItem(String directory, String itemName) {
    setLocation(directory);
    setName(itemName);
  }

  /**
   * This constructor is for a file item.
   * @param directory the absolute path to the directory
   * @param itemName the name of the file and only the name (no directory).
   * @since 1.0
   */
  public ApplicationBuilderItem(File directory, String itemName) {
    setHome(directory);
    setName(itemName);

  }

  /**
   * Setter for "home" attribute.
   * @param directory the absolute path to the directory hosting the file
   * @since 1.0
   * @roseuid 3AB2196D0273
   */
  public void setHome(File directory) {
    home = directory;
    updatePath();
  }

  public File getHome() {
    return home;
  }

  /**
   * Sets the name of the item.
   * @param itemName the name of the item
   * @since 1.0
   * @roseuid 3AB219E00319
   */
  public void setName(String itemName) {
    if (itemName.trim().equals("")) {
      name = null;
    } else {
      name = itemName;
    }
    updatePath();
    updateArchivePath();
  }

  /**
   * Gets the name of the item
   * @return the name of the item
   * @since 1.0
   * @roseuid 3AB21BA80089
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the location of the item in an archive
   * @param directory the name of a directory in an archive hosting the item.
   * @since 1.0
   * @roseuid 3AB21C4500C1
   */
  public void setLocation(String directory) {
    if (directory == null) {
      location = null;
    } else if (directory.trim().equals("")) {
      location = null;
    } else {
      location = directory;
    }
    updateArchivePath();
  }

  /**
   * @return the directory hosting the item in an archive
   * @since 1.0
   * @roseuid 3AB21E2E01DD
   */
  public String getLocation() {
    return location;
  }

  /**
   * Gets the absolute path to the item in the file system
   * @return the absolute path to the item in the file system
   * @since 1.0
   * @roseuid 3AB2214F0076
   */
  public File getPath() {
    return path;
  }

  /**
   * Gets the path to the item in an archive
   * @return the path to the item in an archive
   * @since 1.0
   */
  public String getArchivePath() {
    return archivePath;
  }

  /**
   * Gets the size of the item
   * @return the size of the item, <code>-1</code> if unknown.
   */
  public long getSize() {
    if (getPath() == null) {
      return size;
    }
    if (getPath().exists() && !getPath().isDirectory()) {
      return getPath().length();
    }
    return -1;
  }

  public void setSize(long length) {
    size = length;
  }

  /**
   * Updates the "path" field
   * @see com.silverpeas.applicationbuilder.ApplicationBuilderItem#setName
   * @see com.silverpeas.applicationbuilder.ApplicationBuilderItem#setHome
   */
  private void updatePath() {
    if (getHome() != null && getName() != null) {
      path = new File(getHome(), getName());
    }
  }

  /**
   * Updates the "archivePath" field
   * @see com.silverpeas.applicationbuilder.ApplicationBuilderItem#setName
   * @see com.silverpeas.applicationbuilder.ApplicationBuilderItem#setLocation
   */
  private void updateArchivePath() {
    if (getName() != null) {
      archivePath = "";
      if (getLocation() != null) {
        archivePath += getLocation() + File.separator;
      }
      archivePath += getName();
    }
  }

}
