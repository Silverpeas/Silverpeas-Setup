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

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Name = "war-ic.war" Handles the "war-ic.war" archive. The archive file is filled with the added
 * WARParts and finally with the built WARDescriptor. With the help of the java.util.zip or
 * java.util.jar package combined with streams, it must be possible to achieve this goal without
 * uncompressing the archives.
 * @author Silverpeas
 * @version 1.0/B
 * @since 1.0/B
 */
public class ExternalWar extends WARDirectory {

  private WARDescriptor theWARDescriptor;

  protected static final String MANIFEST_PATH = "META-INF" + File.separator
      + "MANIFEST.MF";

  public ExternalWar(File directory, String name) throws AppBuilderException {
    super(directory, name);
    this.setName(name);
    setWARDescriptor();
  }

  /**
   * Adds the entries in the WARPart to the WAR. The descriptor entry in the WARPart is not directly
   * added to the WAR. The WARPart descriptor is added to the WARDescriptor. When all the WARParts
   * are added, the WARDescriptor can be added.
   * @roseuid 3AAE3DB80074
   */
  public void mergeWARPart(ReadOnlyArchive warPart) throws AppBuilderException {
    Set excludeSet = new HashSet(2);
    excludeSet.add(MANIFEST_PATH);
    excludeSet.add(getWARDescriptor().getArchivePath());
    mergeWith(warPart, excludeSet);
    ApplicationBuilderItem entry = new ApplicationBuilderItem(
        getWARDescriptor().getLocation(), getWARDescriptor().getName());
    InputStream descriptorIn = warPart.getEntry(entry);
    if (descriptorIn != null) {
      XmlDocument warPartDesc = new XmlDocument("", "web.xml from "
          + warPart.getName());
      warPartDesc.loadFrom(descriptorIn);
      getWARDescriptor().mergeWARPartDescriptor(warPartDesc);
    }
  }

  /**
   * When all entries have been added, call this method to close the archive
   * @roseuid 3AB1EAFE02FD
   */
  public void close() throws AppBuilderException {
    integrateDescriptor();
  }

  public WARDescriptor getWARDescriptor() {
    return theWARDescriptor;
  }

  private void setWARDescriptor() {
    theWARDescriptor = new WARDescriptor();
  }

  /**
   * @roseuid 3AAE3DBE00EB
   */
  private void integrateDescriptor() throws AppBuilderException {
    getWARDescriptor().sort();
    add(getWARDescriptor());
  }

}
