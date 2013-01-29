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
package org.silverpeas.migration.jcr.version;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.dbutils.DbUtils;

import org.silverpeas.dbbuilder.sql.ConnectionFactory;
import org.silverpeas.migration.jcr.service.AttachmentService;
import org.silverpeas.migration.jcr.service.ConverterUtil;
import org.silverpeas.migration.jcr.service.SimpleDocumentService;
import org.silverpeas.migration.jcr.service.model.HistorisedDocument;
import org.silverpeas.migration.jcr.service.model.SimpleAttachment;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.migration.jcr.service.model.UnlockContext;
import org.silverpeas.migration.jcr.service.model.UnlockOption;
import org.silverpeas.migration.jcr.version.model.OldDocumentMetadata;
import org.silverpeas.migration.jcr.version.model.Version;
import org.silverpeas.util.Console;
import org.silverpeas.util.DateUtil;
import org.silverpeas.util.StringUtil;

class ComponentDocumentMigrator implements Callable<Long> {

  public static final String SELECT_DOCUMENTS = "SELECT documentid, documentname, "
      + "documentdescription, documentstatus, documentownerid, documentcheckoutdate, documentinfo, "
      + "foreignid, instanceid, typeworklist, currentworklistorder, alertdate, expirydate, "
      + "documentordernum FROM sb_version_document WHERE instanceid = ? ORDER BY foreignid, "
      + "documentordernum";
  public static final String SELECT_DOCUMENT_VERSION = "SELECT versionid, documentid, "
      + "versionmajornumber, versionminornumber, versionauthorid, versioncreationdate, "
      + "versioncomments, versiontype,  versionstatus, versionphysicalname, versionlogicalname, "
      + "versionmimetype, versionsize, instanceid, xmlform FROM sb_version_version WHERE "
      + "documentid = ? ORDER BY versionmajornumber, versionminornumber";
  public static final String DELETE_DOCUMENT_VERSIONS =
      "DELETE FROM sb_version_version WHERE documentid = ?";
  public static final String DELETE_DOCUMENT =
      "DELETE FROM sb_version_document WHERE documentid = ?";
  private final String componentId;
  private final AttachmentService service;
  private final Console console;

  ComponentDocumentMigrator(String instanceId, SimpleDocumentService service, Console console) {
    this.componentId = instanceId;
    this.service = service;
    this.console = console;
  }

  private Connection getConnection() throws SQLException {
    return ConnectionFactory.getConnection();
  }

  protected long migrateComponent() throws SQLException, ParseException, IOException {
    console.printMessage("Migrating component " + componentId);
    List<OldDocumentMetadata> documents = listAllDocuments();
    long nbMigratedDocuments = 0L;
    for (OldDocumentMetadata document : documents) {
      nbMigratedDocuments += createVersions(document);
    }
    console.printMessage("Migrating the component " + componentId + " required the migration of "
        + nbMigratedDocuments + " documents");
    return nbMigratedDocuments;
  }

  protected long createVersions(OldDocumentMetadata metadata) throws SQLException, ParseException,
      IOException {
    console.printMessage("Creating document for " + metadata.getTitle() + " with " + metadata
        .getHistory().size() + " versions");
    long nbMigratedDocuments = 0L;
    HistorisedDocument document = new HistorisedDocument();
    document.setPK(new SimpleDocumentPK(null, metadata.getInstanceId()));
    document.setAlert(metadata.getAlert());
    document.setForeignId(metadata.getForeignId());
    document.setReservation(metadata.getReservation());
    document.setExpiry(metadata.getExpiry());
    document.setOrder(metadata.getOrder());
    for (Version version : metadata.getHistory()) {
      SimpleAttachment attachment = new SimpleAttachment(version.getFileName(),
          ConverterUtil.defaultLanguage, metadata.getTitle(), metadata.getDescription(), version
          .getSize(), version.getContentType(), version.getCreatedBy(), version.getCreation(),
          version.getXmlFormId());
      document.setFile(attachment);
      File file = version.getAttachment();
      document.setComment(version.getComment());
      document.setPublicDocument(version.isPublic());
      document.setUpdated(version.getCreation());
      document.setUpdatedBy(version.getCreatedBy());
      if (file != null) {
        if (!StringUtil.isDefined(document.getId())) {
          document = (HistorisedDocument) service.createAttachment(document, file);
        } else {
          service.updateAttachment(document, file);
          UnlockContext context = new UnlockContext(document.getId(), version.getCreatedBy(),
              document.getLanguage());
          if (!version.isPublic()) {
            context.addOption(UnlockOption.PRIVATE_VERSION);
          }
          service.unlock(context);
          document = (HistorisedDocument) service.searchDocumentById(document.getPk(), document
              .getLanguage());
        }
        nbMigratedDocuments++;
      }
    }
    console.printMessage("We have migrated  " + nbMigratedDocuments + " for " + metadata.getTitle()
        + " with " + metadata.getHistory().size() + " versions");
    cleanAll(metadata);
    return nbMigratedDocuments;
  }

  protected void cleanAll(OldDocumentMetadata metadata) throws SQLException {
    Connection connection = getConnection();
    connection.setAutoCommit(false);
    PreparedStatement deleteTranslations = null;
    PreparedStatement deleteAttachment = null;
    try {
      deleteTranslations = connection.prepareStatement(DELETE_DOCUMENT_VERSIONS);
      deleteTranslations.setLong(1, metadata.getOldSilverpeasId());
      deleteTranslations.executeUpdate();
      DbUtils.closeQuietly(deleteTranslations);
      deleteAttachment = connection.prepareStatement(DELETE_DOCUMENT);
      deleteAttachment.setLong(1, metadata.getOldSilverpeasId());
      deleteAttachment.executeUpdate();
      connection.commit();
    } catch (SQLException ex) {
      throw ex;
    } finally {
      DbUtils.closeQuietly(deleteTranslations);
      DbUtils.closeQuietly(deleteAttachment);
      DbUtils.closeQuietly(connection);
    }
    for (Version version : metadata.getHistory()) {
      try {
        File file = version.getAttachment();
        if (file != null) {
          ConverterUtil.deleteFile(file);
        }
      } catch (IOException ioex) {
        console.printError("Error deleting file", ioex);
      }
    }
  }

  @Override
  public Long call() throws Exception {
    console.printMessage("Migrating component " + componentId);
    return migrateComponent();
  }

  protected List<OldDocumentMetadata> listAllDocuments() throws ParseException, IOException,
      SQLException {
    Connection connection = getConnection();
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<OldDocumentMetadata> documents = new ArrayList<OldDocumentMetadata>(500);
    try {
      pstmt = connection.prepareStatement(SELECT_DOCUMENTS);
      pstmt.setString(1, componentId);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        OldDocumentMetadata metadata = new OldDocumentMetadata(rs.getInt("documentordernum"),
            rs.getString("documentdescription"), DateUtil.parse(rs.getString("alertdate")),
            DateUtil.parse(rs.getString("expirydate")), rs.getString("documentownerid"),
            DateUtil.parse(rs.getString("documentcheckoutdate")), rs.getString("instanceid"),
            rs.getString("foreignid"), rs.getLong("documentid"), rs.getString("documentname"));
        documents.add(fillWithVersion(metadata));
      }
    } catch (SQLException sqlex) {
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(pstmt);
      DbUtils.closeQuietly(connection);
    }
    return documents;
  }

  protected OldDocumentMetadata fillWithVersion(OldDocumentMetadata document) throws ParseException,
      IOException, SQLException {
    Connection connection = getConnection();
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      pstmt = connection.prepareStatement(SELECT_DOCUMENT_VERSION);
      pstmt.setLong(1, document.getOldSilverpeasId());
      rs = pstmt.executeQuery();
      while (rs.next()) {
        Version version = new Version(rs.getInt("versionminornumber"), rs.getInt(
            "versionmajornumber"), DateUtil.parse(rs.getString("versioncreationdate")), rs
            .getString("versionauthorid"), rs.getString("versionlogicalname"), rs.getString(
            "versionphysicalname"), rs.getString("versionmimetype"), rs.getLong("versionsize"), rs
            .getString("xmlform"), rs.getString("versioncomments"), document.getInstanceId());
        document.addVersion(version);
      }
    } catch (SQLException sqlex) {
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(pstmt);
      DbUtils.closeQuietly(connection);
    }
    return document;
  }
}
