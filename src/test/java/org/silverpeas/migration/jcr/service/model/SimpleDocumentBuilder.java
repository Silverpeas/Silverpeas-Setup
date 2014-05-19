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

import java.util.Date;

public class SimpleDocumentBuilder {
  private SimpleDocumentPK pk;
  private String foreignId;
  private int order = 0;
  private boolean versioned = false;
  private String editedBy = null;
  private Date reservation = null;
  private Date alert = null;
  private Date expiry = null;
  private String status;
  private String cloneId;
  private int minorVersion = 0;
  private int majorVersion = 0;
  private boolean publicDocument = false;
  private String nodeName;
  private String comment = null;
  private DocumentType documentType = DocumentType.attachment;
  private String oldContext;
  private SimpleAttachment file;

  public SimpleDocumentBuilder setPk(final SimpleDocumentPK pk) {
    this.pk = pk;
    return this;
  }

  public SimpleDocumentBuilder setForeignId(final String foreignId) {
    this.foreignId = foreignId;
    return this;
  }

  public SimpleDocumentBuilder setOrder(final int order) {
    this.order = order;
    return this;
  }

  public SimpleDocumentBuilder setVersioned(final boolean versioned) {
    this.versioned = versioned;
    return this;
  }

  public SimpleDocumentBuilder setEditedBy(final String editedBy) {
    this.editedBy = editedBy;
    return this;
  }

  public SimpleDocumentBuilder setReservation(final Date reservation) {
    this.reservation = reservation;
    return this;
  }

  public SimpleDocumentBuilder setAlert(final Date alert) {
    this.alert = alert;
    return this;
  }

  public SimpleDocumentBuilder setExpiry(final Date expiry) {
    this.expiry = expiry;
    return this;
  }

  public SimpleDocumentBuilder setStatus(final String status) {
    this.status = status;
    return this;
  }

  public SimpleDocumentBuilder setCloneId(final String cloneId) {
    this.cloneId = cloneId;
    return this;
  }

  public SimpleDocumentBuilder setMinorVersion(final int minorVersion) {
    this.minorVersion = minorVersion;
    return this;
  }

  public SimpleDocumentBuilder setMajorVersion(final int majorVersion) {
    this.majorVersion = majorVersion;
    return this;
  }

  public SimpleDocumentBuilder setPublicDocument(final boolean publicDocument) {
    this.publicDocument = publicDocument;
    return this;
  }

  public SimpleDocumentBuilder setNodeName(final String nodeName) {
    this.nodeName = nodeName;
    return this;
  }

  public SimpleDocumentBuilder setComment(final String comment) {
    this.comment = comment;
    return this;
  }

  public SimpleDocumentBuilder setDocumentType(final DocumentType documentType) {
    this.documentType = documentType;
    return this;
  }

  public SimpleDocumentBuilder setOldContext(final String oldContext) {
    this.oldContext = oldContext;
    return this;
  }

  public SimpleDocumentBuilder setFile(final SimpleAttachment file) {
    this.file = file;
    return this;
  }

  public SimpleDocument build() {
    return new SimpleDocument(pk, foreignId, order, versioned, editedBy, reservation, alert, expiry,
        status, cloneId, minorVersion, majorVersion, publicDocument, nodeName, comment,
        documentType, oldContext, file);
  }
}