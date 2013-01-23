/*
 * Copyright (C) 2000 - 2012 Silverpeas
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
package org.silverpeas.migration.classified;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import org.silverpeas.migration.jcr.attachment.model.DocumentType;
import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;
import org.silverpeas.migration.jcr.attachment.SimpleDocumentService;
import org.silverpeas.migration.jcr.attachment.model.SimpleDocument;
import org.silverpeas.migration.jcr.attachment.model.SimpleDocumentPK;
import org.silverpeas.util.StringUtil;

/**
 * @author cbonin
 */
public class XmlFormImagesToAttachment extends DbBuilderDynamicPart {
  
  private final SimpleDocumentService service;
  
  public XmlFormImagesToAttachment() {
    this.service = new SimpleDocumentService();
  }

  public void migrate() throws IOException, SQLException {
    try {

      getConsole()
          .printMessage("Migrate Classified Images in XML Form To Attachment and Classified Description in XML Form to Classified DB");
      getConsole().printMessage("");

      Collection<String> listInstanceId = getListClassifiedInstance();

      for (String instanceId : listInstanceId) {
        int instanceIntId = Integer.parseInt(instanceId);
        getConsole().printMessage("Migrate Classified instance id = " + instanceId);
        String xmlFormName = getXMLFormName(instanceIntId);
        getConsole().printMessage("Migrate Classified XML Form = "+xmlFormName);
        Collection<Integer> listTemplateId = getListTemplate(xmlFormName, instanceId);
        for (Integer templateId : listTemplateId) {
          getConsole().printMessage("TemplateId = " + templateId);
          Collection<RecordTemplate> listRecord = getListRecord(templateId.intValue());
          for (RecordTemplate record : listRecord) {
            int recordId = record.getRecordId();
            String classifiedId = record.getExternalId();
            int classifiedIntId = Integer.parseInt(classifiedId);
            getConsole().printMessage("Record [recordId = " + recordId + ", externalId = " +
                classifiedId + "]");
            Collection<FieldTemplate> listValue = getListValue(recordId);
            for (FieldTemplate fieldTemplate : listValue) {
              getConsole().printMessage("Field Name = "+fieldTemplate.getFieldName());//category | type | description | photo 
              if (fieldTemplate.getFieldName().startsWith("photo")) {
                if (fieldTemplate.getFieldValue() != null &&
                    !"".equals(fieldTemplate.getFieldValue())) {
                  getConsole().printMessage("Photo field value = " + fieldTemplate.getFieldValue());
                  
                  getConsole().printMessage("Delete field template recordId = " + recordId +
                      ", fieldName = '" + fieldTemplate.getFieldName() +
                      "', fieldValue = " + fieldTemplate.getFieldValue());
                  try {
                    deletePhotoValue(recordId, fieldTemplate.getFieldName());
                  } catch (SQLException e) {
                    getConsole()
                        .printError("ERROR when Deleting field template recordId = " +
                        recordId + ", fieldName = '" + fieldTemplate.getFieldName() +
                        "', fieldValue = " + fieldTemplate.getFieldValue() +
                        ", error = " +
                        e.getMessage(), e);
                  }

                  if(StringUtil.isLong(fieldTemplate.getFieldValue())) {
                    SimpleDocumentPK simpleDocumentPk = new SimpleDocumentPK(null, "classifieds"+instanceId);
                    simpleDocumentPk.setOldSilverpeasId(Long.valueOf(fieldTemplate.getFieldValue()));
                    SimpleDocument simpleDocument = this.service.searchDocumentById(simpleDocumentPk, null);
                    if(simpleDocument != null) {
                      boolean verifFormatImage =
                          verifFormatImage(simpleDocument.getFilename());
                      if (verifFormatImage) {
                        getConsole().printMessage("Update attachment context of attachmentId = " +
                            simpleDocument.getId() + ", oldSilverpeasId = "+fieldTemplate.getFieldValue());
                        simpleDocument.setDocumentType(DocumentType.attachment);
                        this.service.updateAttachment(simpleDocument);
                      } else {// format Image not correct
                        getConsole().printMessage("Format Image not correct, delete attachment attachmentId = " + simpleDocument.getId() + ", oldSilverpeasId = "+fieldTemplate.getFieldValue());
                        this.service.deleteAttachment(simpleDocument);
                      }
                    } else {
                      getConsole().printError("ERROR Simple Document with oldSilverpeasId "+fieldTemplate.getFieldValue()+" not found");
                    }
                  } 
                } else {
                  getConsole().printMessage("Photo field value = null");
                  getConsole().printMessage("Delete field template recordId = " + recordId +
                      ", fieldName = '" + fieldTemplate.getFieldName() + "', fieldValue = null");
                  try {
                    deletePhotoValue(recordId, fieldTemplate.getFieldName());
                  } catch (SQLException e) {
                    getConsole().printError("ERROR when Deleting field template recordId = " +
                        recordId + ", fieldName = '" + fieldTemplate.getFieldName() +
                        "', fieldValue = null, error = " +
                        e.getMessage(), e);
                  }
                }
              } else if ("description".equals(fieldTemplate.getFieldName())) {
                getConsole()
                    .printMessage("Description field value = " + fieldTemplate.getFieldValue());
                getConsole().printMessage("Update classified instanceId = " + instanceId +
                    ", classifiedId = " + classifiedId + ", description = " +
                    fieldTemplate.getFieldValue());
                boolean updated = false;
                try {
                  updateClassified(instanceId, classifiedIntId, fieldTemplate.getFieldValue());
                  updated = true;
                } catch (SQLException e) {
                  getConsole().printError("ERROR when Updating classified instanceId = " +
                      instanceId + ", classifiedId = " + classifiedId + ", description = " +
                      fieldTemplate.getFieldValue() + ", error = " + e.getMessage(), e);
                }
                if (updated) {
                  getConsole().printMessage("Delete field template recordId = " + recordId +
                      ", fieldName = 'description'");
                  try {
                    deleteDescriptionValue(recordId);
                  } catch (SQLException e) {
                    getConsole().printError("ERROR when Deleting field template recordId = " +
                        recordId + ", fieldName = 'description', error = " + e.getMessage(), e);
                  }
                }
              }
            }
            getConsole().printMessage("");
          }
          getConsole().printMessage("---------------------");
        }

        getConsole()
            .printMessage("--------------------------------------------------------------------");
      }
    } finally {
      getConsole().close();
    }
  }

  public Collection<String> getListClassifiedInstance() throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    Collection<String> listInstanceId = new ArrayList<String>();
    try {
      stmt = getConnection().createStatement();
      rs =
          stmt
          .executeQuery("SELECT id FROM st_componentinstance where componentname='classifieds'");
      while (rs.next()) {
        String instanceId = rs.getString("id");
        listInstanceId.add(instanceId);
      }
      return listInstanceId;
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (stmt != null) {
        stmt.close();
      }
    }
  }

  public String getXMLFormName(int instanceId) throws SQLException {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String xmlFormName = null;
    try {
      pstmt =
          getConnection().prepareStatement(
          "SELECT value FROM st_instance_data where componentid = ? and name='XMLFormName'");
      pstmt.setInt(1, instanceId);
      rs = pstmt.executeQuery();
      if (rs.next()) {
        xmlFormName = rs.getString("value");
      }
      return xmlFormName;
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (pstmt != null) {
        pstmt.close();
      }
    }
  }

  public Collection<Integer> getListTemplate(String xmlFormName, String instanceId)
      throws SQLException {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Collection<Integer> listTemplateId = new ArrayList<Integer>();
    try {
      pstmt =
          getConnection()
              .prepareStatement(
                  "SELECT templateId FROM sb_formtemplate_template where templateName=? and externalId like ?");
      pstmt.setString(1, xmlFormName);
      pstmt.setString(2, "classifieds" + instanceId + "%");
      rs = pstmt.executeQuery();

      while (rs.next()) {
        int templateId = rs.getInt("templateId");
        listTemplateId.add(new Integer(templateId));
      }
      return listTemplateId;
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (pstmt != null) {
        pstmt.close();
      }
    }
  }

  public Collection<RecordTemplate> getListRecord(int templateId) throws SQLException {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Collection<RecordTemplate> listRecord = new ArrayList<RecordTemplate>();
    try {
      pstmt =
          getConnection().prepareStatement(
          "SELECT recordId, externalId FROM sb_formtemplate_record where templateid = ?");
      pstmt.setInt(1, templateId);
      rs = pstmt.executeQuery();

      while (rs.next()) {
        int recordId = rs.getInt("recordId");
        String externalId = rs.getString("externalId");
        RecordTemplate recordTemplate = new RecordTemplate(recordId, externalId);
        listRecord.add(recordTemplate);
      }
      return listRecord;
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (pstmt != null) {
        pstmt.close();
      }
    }
  }

  public Collection<FieldTemplate> getListValue(int recordId) throws SQLException {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Collection<FieldTemplate> listValue = new ArrayList<FieldTemplate>();
    try {
      pstmt =
          getConnection().prepareStatement(
          "SELECT fieldName, fieldValue FROM sb_formtemplate_textfield where recordid = ?");
      pstmt.setInt(1, recordId);
      rs = pstmt.executeQuery();

      while (rs.next()) {
        String fieldName = rs.getString("fieldName");
        String fieldValue = rs.getString("fieldValue");
        FieldTemplate fieldTemplate = new FieldTemplate(fieldName, fieldValue);
        listValue.add(fieldTemplate);
      }
      return listValue;
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (pstmt != null) {
        pstmt.close();
      }
    }
  }

  public void updateClassified(String instanceId, int classifiedId, String description)
      throws SQLException {
    PreparedStatement pstmt = null;
    try {
      pstmt =
          getConnection()
              .prepareStatement(
                  "UPDATE SC_Classifieds_Classifieds set description = ? where instanceId = ? and classifiedId = ? ");
      pstmt.setString(1, description);
      pstmt.setString(2, "classifieds" + instanceId);
      pstmt.setInt(3, classifiedId);
      pstmt.executeUpdate();
    } finally {
      if (pstmt != null) {
        pstmt.close();
      }
    }
  }

  public void deleteDescriptionValue(int recordId) throws SQLException {
    PreparedStatement pstmt = null;
    try {
      pstmt = getConnection().prepareStatement(
          "DELETE FROM sb_formtemplate_textfield where recordId = ? and fieldName = 'description'");
      pstmt.setInt(1, recordId);
      pstmt.executeUpdate();
    } finally {
      if (pstmt != null) {
        pstmt.close();
      }
    }
  }

  public boolean verifFormatImage(String filename) {
    int indexPoint = filename.lastIndexOf(".");
    if (indexPoint != -1) {
      // le fichier contient une extension. On recupere l'extension
      String extension = filename.substring(indexPoint + 1);
      extension = extension.toLowerCase();
      if ("jpg".equals(extension) ||
          "gif".equals(extension) ||
          "bmp".equals(extension) ||
          "tiff".equals(extension) ||
          "tif".equals(extension) ||
          "jpeg".equals(extension) ||
          "png".equals(extension)) {
        return true;
      }
    }
    return false;
  }

  public void deletePhotoValue(int recordId, String fieldPhotoName) throws SQLException {
    PreparedStatement pstmt = null;
    try {
      pstmt = getConnection().prepareStatement(
          "DELETE FROM sb_formtemplate_textfield where recordId = ? and fieldName = ?");
      pstmt.setInt(1, recordId);
      pstmt.setString(2, fieldPhotoName);
      pstmt.executeUpdate();
    } finally {
      if (pstmt != null) {
        pstmt.close();
      }
    }
  }
}
