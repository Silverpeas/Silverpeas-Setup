/*
 * Copyright (C) 2000 - 2014 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception. You should have recieved a copy of the text describing
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
package org.silverpeas.migration.jcr.service.model;

import java.io.File;
import java.util.Date;

public class SimpleAttachmentBuilder {
  private String filename;
  private String language;
  private String title;
  private String description;
  private long size;
  private String contentType;
  private String createdBy;
  private Date created;
  private String xmlFormId;
  private String updatedBy;
  private Date updated;
  private File file;

  public SimpleAttachmentBuilder setFilename(final String filename) {
    this.filename = filename;
    return this;
  }

  public SimpleAttachmentBuilder setLanguage(final String language) {
    this.language = language;
    return this;
  }

  public SimpleAttachmentBuilder setTitle(final String title) {
    this.title = title;
    return this;
  }

  public SimpleAttachmentBuilder setDescription(final String description) {
    this.description = description;
    return this;
  }

  public SimpleAttachmentBuilder setSize(final long size) {
    this.size = size;
    return this;
  }

  public SimpleAttachmentBuilder setContentType(final String contentType) {
    this.contentType = contentType;
    return this;
  }

  public SimpleAttachmentBuilder setCreatedBy(final String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public SimpleAttachmentBuilder setCreated(final Date created) {
    this.created = created;
    return this;
  }

  public SimpleAttachmentBuilder setXmlFormId(final String xmlFormId) {
    this.xmlFormId = xmlFormId;
    return this;
  }

  public SimpleAttachmentBuilder setUpdatedBy(final String updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  public SimpleAttachmentBuilder setUpdated(final Date updated) {
    this.updated = updated;
    return this;
  }

  public SimpleAttachmentBuilder setFile(final File file) {
    this.file = file;
    return this;
  }

  public SimpleAttachment build() {
    return new SimpleAttachment(filename, language, title, description, size, contentType,
        createdBy, created, updatedBy,updated,xmlFormId,file);
  }
}