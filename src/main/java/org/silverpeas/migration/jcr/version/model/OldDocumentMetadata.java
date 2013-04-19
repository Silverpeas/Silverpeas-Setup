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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 *
 * @author ehugonnet
 */
public class OldDocumentMetadata {
  private int order;
  private String description;
  private Date alert;
  private Date expiry;
  private String editedBy;
  private Date reservation;
  private String instanceId;
  private String foreignId;
  private long oldSilverpeasId;
  private String title;
  
  private List<Version> versions = new ArrayList<Version>(10);

  public OldDocumentMetadata(int order, String description, Date alert, Date expiry, String editedBy,
      Date reservation, String instanceId, String foreignId, long oldSilverpeasId, String title) {
    this.order = order;
    this.description = description;
    this.alert = alert;
    this.expiry = expiry;
    this.editedBy = editedBy;
    this.reservation = reservation;
    this.instanceId = instanceId;
    this.foreignId = foreignId;
    this.oldSilverpeasId = oldSilverpeasId;
    this.title = title;
  }
  
  public void addVersion(Version version) {
    versions.add(version);
  }

  
  public List<Version> getHistory() {
    return Collections.unmodifiableList(versions);
  }
  
  public int getOrder() {
    return order;
  }

  public String getDescription() {
    return description;
  }

  public Date getAlert() {
    return alert;
  }

  public Date getExpiry() {
    return expiry;
  }

  public String getEditedBy() {
    return editedBy;
  }

  public Date getReservation() {
    return reservation;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getForeignId() {
    return foreignId;
  }

  public long getOldSilverpeasId() {
    return oldSilverpeasId;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return "OldDocumentMetadata{" + "order=" + order + ", description=" + description + ", alert=" +
        alert + ", expiry=" + expiry + ", editedBy=" + editedBy + ", reservation=" + reservation +
        ", instanceId=" + instanceId + ", foreignId=" + foreignId + ", oldSilverpeasId=" +
        oldSilverpeasId + ", title=" + title + ", versions=" + versions + '}';
  }
  

}
