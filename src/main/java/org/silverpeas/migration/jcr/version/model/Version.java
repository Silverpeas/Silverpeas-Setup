/*
 * Copyright (C) 2000 - 2013 Silverpeas
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
 * "http://www.silverpeas.org/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.migration.jcr.version.model;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.silverpeas.dbbuilder.DBBuilder;
import org.silverpeas.util.Console;
import org.silverpeas.util.SilverpeasHomeResolver;

import static java.io.File.separatorChar;

/**
 *
 * @author ehugonnet
 */
public class Version {

  private static Console console = new Console(DBBuilder.class);

  private int id;
  private int minor;
  private int major;
  private Date creation;
  private String createdBy;
  private String fileName;
  private String physicalFilename;
  private String contentType;
  private long size;
  private String xmlFormId;
  private String comment;
  private String instanceId;

  public Version(int id, int minor, int major, Date creation, String createdBy, String fileName,
      String physicalFilename, String contentType, long size, String xmlFormId, String comment,
      String instanceId) {
    this.id = id;
    this.minor = minor;
    this.major = major;
    this.creation = creation;
    this.createdBy = createdBy;
    this.fileName = fileName;
    this.physicalFilename = physicalFilename;
    this.contentType = contentType;
    this.size = size;
    this.xmlFormId = xmlFormId;
    this.comment = comment;
    this.instanceId = instanceId;
  }

  public int getMinor() {
    return minor;
  }

  public int getMajor() {
    return major;
  }

  public Date getCreation() {
    return creation;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public String getFileName() {
    return fileName;
  }

  public String getPhysicalFilename() {
    return physicalFilename;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSize() {
    return size;
  }

  public String getXmlFormId() {
    return xmlFormId;
  }

  public String getComment() {
    return comment;
  }

  public boolean isPublic() {
    return minor == 0;
  }

  public int getId() {
    return this.id;
  }

  public File getAttachment() throws IOException {
    String baseDirectory = SilverpeasHomeResolver.getDataHome() + separatorChar + "workspaces"
        + separatorChar + instanceId + separatorChar + "Versioning";
    File file = new File(baseDirectory, physicalFilename);
    if (!file.exists() || !file.isFile()) {
      console.printWarning("The file " + file.getAbsolutePath() + " doesn't exist");
      file = null;
    }
    return file;
  }
  
  
  @Override
  public String toString() {
    return "Version{" + "id=" + id + ", minor=" + minor + ", major=" + major + ", creation=" +
        creation + ", createdBy=" + createdBy + ", fileName=" + fileName + ", physicalFilename=" +
        physicalFilename + ", contentType=" + contentType + ", size=" + size + ", xmlFormId=" +
        xmlFormId + ", comment=" + comment + ", instanceId=" + instanceId + '}';
  }
}
