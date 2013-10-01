/*
 * Copyright (C) 2000-2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Writer Free/Libre
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.silverpeas.dbbuilder.sql.ConnectionFactory;
import org.silverpeas.migration.jcr.service.AttachmentException;
import org.silverpeas.migration.jcr.service.ConverterUtil;
import org.silverpeas.migration.jcr.service.RepositoryManager;
import org.silverpeas.migration.jcr.service.model.HistorisedDocument;
import org.silverpeas.migration.jcr.service.model.SimpleAttachment;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.migration.jcr.service.repository.DocumentConverter;
import org.silverpeas.migration.jcr.service.repository.DocumentRepository;
import org.silverpeas.migration.jcr.version.model.OldDocumentMetadata;
import org.silverpeas.migration.jcr.version.model.Version;
import org.silverpeas.util.Console;
import org.silverpeas.util.DateUtil;
import org.silverpeas.util.StringUtil;

/**
 * An optimized alternative to the ComponentDocumentMigrator class for migrating in the JCR all the
 * versioned documents within a given component instance.
 *
 * @author mmoquillon
 */
class VersionedDocumentMigration implements Callable<Long> {

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
  private final RepositoryManager repositoryManager;
  private final DocumentRepository documentRepository;
  private final Console console;
  private static final DocumentConverter converter = new DocumentConverter();

  VersionedDocumentMigration(String instanceId, RepositoryManager repositoryManager, Console console) {
    this.componentId = instanceId;
    this.repositoryManager = repositoryManager;
    this.documentRepository = new DocumentRepository(repositoryManager);
    this.console = console;
  }

  protected long migrateComponent() throws Exception {
    long processStart = System.currentTimeMillis();
    console.printMessage("Migrating component " + componentId);
    long migratedDocumentCount = 0;
    Session session = null;
    try {
      List<OldDocumentMetadata> documents = listAllDocuments();
      session = openJCRSession();
      for (OldDocumentMetadata document : documents) {
        migratedDocumentCount += migrateAllDocumentVersions(session, document);
      }
      if (session.hasPendingChanges()) {
        session.save();
      }
      cleanAll(documents);
    } catch (Exception ex) {
      console.printError("Error during the migration of the versioned documents in component "
          + componentId + ": " + ex.getMessage(), ex);
      throw ex;
    } finally {
      repositoryManager.logout(session);
    }
    long processEnd = System.currentTimeMillis();
    console.printMessage("Migrating the component " + componentId
        + " required the migration of "
        + migratedDocumentCount + " documents in " + (processEnd - processStart) + "ms");
    console.printMessage("");

    return migratedDocumentCount;
  }

  private long migrateAllDocumentVersions(Session session, OldDocumentMetadata metadata) throws
      SQLException, ParseException, IOException {
    console.printMessage("=> Creating document for " + metadata.getTitle() + " with " + metadata
        .getHistory().size() + " versions");
    long processStart = System.currentTimeMillis();
    long migratedDocumentCount = 0L;
    HistorisedDocument document = buildHistorisedDocument(metadata);
    createDocumentNodeInJCR(session, document);
    createDocumentPermalink(document, metadata);
    for (Version version : metadata.getHistory()) {
      migrateDocumentVersion(session, document, version, metadata);
      createVersionPermalink(getDocumentVersionUUID(document, version), version.getId());
      migratedDocumentCount++;
    }
    long processEnd = System.currentTimeMillis();
    console.printMessage("   we have created  " + migratedDocumentCount + " for " + metadata.
        getTitle() + " with " + metadata.getHistory().size() + " versions in " + (processEnd
        - processStart) + "ms");
    return migratedDocumentCount;
  }

  private HistorisedDocument buildHistorisedDocument(OldDocumentMetadata metadata) {
    HistorisedDocument document = new HistorisedDocument();
    document.setPK(new SimpleDocumentPK(null, metadata.getInstanceId()));
    document.setAlert(metadata.getAlert());
    document.setForeignId(metadata.getForeignId());
    document.setReservation(metadata.getReservation());
    document.setExpiry(metadata.getExpiry());
    document.setOrder(metadata.getOrder());
    return document;
  }

  private SimpleAttachment buildSimpleAttachment(Version version, OldDocumentMetadata metadata)
      throws IOException {
    SimpleAttachment attachment = new SimpleAttachment(version.getFileName(),
        ConverterUtil.defaultLanguage, metadata.getTitle(), metadata.getDescription(), version
        .getSize(), version.getContentType(), version.getCreatedBy(), version.getCreation(),
        version.getXmlFormId());
    // link this attachment to the actual file in order to relocate it later with the migration in
    // the JCR.
    attachment.setFile(version.getAttachment());
    return attachment;
  }

  private void setVersioningAttributes(HistorisedDocument document, Version version) {
    document.setComment(version.getComment());
    document.setPublicDocument(version.isPublic());
    document.setUpdated(version.getCreation());
    document.setUpdatedBy(version.getCreatedBy());
  }

  private void createDocumentNodeInJCR(Session session, HistorisedDocument document) {
    try {
      // create the document node in the JCR
      documentRepository.createDocument(session, document);
      // save the created node(s) in the JCR
      session.save();
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    }
  }

  private void createVersionNodeInJCR(Session session, HistorisedDocument document)
      throws IOException {
    try {
      // get the location, in the filesystem, of the last version if any to duplicate the content
      // of this directory later
      File previousVersionDirectory = null;
      if (!document.getHistory().isEmpty()) {
        String path = document.getDirectoryPath(document.getLanguage());
        previousVersionDirectory = new File(path).getParentFile();
      }

      // set the last owner of the attachment to the document node, who is the author of this
      // attachment version.
      String owner = document.getEditedBy();
      if (!StringUtil.isDefined(owner)) {
        owner = document.getUpdatedBy();
      }
      // lock the document node for update. The new owner is set as a node property.
      documentRepository.lock(session, document, owner);

      // create a version node for the current attachment version. If there is no one already
      // attachment node for the attachment, then create it as a child of the document node.
      Node versionNode = documentRepository.unlock(session, document);
      document.getHistory().add(converter.convertNode(versionNode, document.getLanguage()));

      // save the change in the JCR.
      session.save();

      // now create the file at the location corresponding to the current version in the filesystem,
      copyContent(document);
      if (previousVersionDirectory != null) {
        // duplicate the content of the previous version into the location of the new one.
        String path = document.getDirectoryPath(document.getLanguage()).replace('/',
            File.separatorChar);
        File currentVersionDirectory = new File(path).getParentFile();
        duplicateContents(previousVersionDirectory, currentVersionDirectory);
      }

    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    }
  }

  /**
   * Migrates the specified version of the specified document in the JCR.
   *
   * @param session the current JCR session.
   * @param document the document to which the version is related.
   * @param version the version of the document to migrate.
   * @param metadata some metadata shared by all the document versions.
   * @throws IOException if an error occurs with the attachment file.
   */
  private void migrateDocumentVersion(Session session, HistorisedDocument document,
      Version version, OldDocumentMetadata metadata) throws IOException {
    SimpleAttachment attachment = buildSimpleAttachment(version, metadata);
    document.setAttachment(attachment);
    setVersioningAttributes(document, version);
    if (!StringUtil.isDefined(version.getCreatedBy())) {
      console.printWarning("We have a null id for the author of document " + document
          + " and version " + metadata);
    }
    createVersionNodeInJCR(session, document);
  }

  private Session openJCRSession() {
    try {
      return repositoryManager.getSession();
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    }
  }

  private void copyContent(SimpleDocument document) throws IOException, RepositoryException {
    InputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(document.getAttachment().getFile()));
      documentRepository.storeContent(document, in);
    } catch (FileNotFoundException ex) {
      throw new AttachmentException(ex);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  private void duplicateContents(File source, File target)
      throws IOException {
    if (!source.exists() || !source.isDirectory() || source.listFiles() == null) {
      return;
    }
    if (!target.exists()) {
      target.mkdir();
    }
    for (File langDir : source.listFiles()) {
      File targetLangDir = new File(target, langDir.getName());
      if (!targetLangDir.exists()) {
        FileUtils.copyDirectory(langDir, targetLangDir);
      }
    }
  }

  private String getDocumentVersionUUID(HistorisedDocument document, Version version) {
    for (SimpleDocument doc : document.getHistory()) {
      if (doc.getMajorVersion() == version.getMajor() && doc.getMinorVersion() == version.getMinor()) {
        return doc.getId();
      }
    }
    return document.getId();
  }

  private void cleanAll(List<OldDocumentMetadata> metadata) throws SQLException {
    console.printMessage("   Clean all the deprecated documents in " + this.componentId);
    Connection connection = getConnection();
    connection.setAutoCommit(false);
    PreparedStatement deleteVersions = connection.prepareStatement(DELETE_DOCUMENT_VERSIONS);
    PreparedStatement deleteAttachment = connection.prepareStatement(DELETE_DOCUMENT);
    try {
      for (OldDocumentMetadata document : metadata) {
        try {
          deleteVersions.setLong(1, document.getOldSilverpeasId());
          deleteVersions.executeUpdate();
          deleteAttachment.setLong(1, document.getOldSilverpeasId());
          deleteAttachment.executeUpdate();
          connection.commit();
          deleteVersions.clearParameters();
          deleteAttachment.clearParameters();
        } catch (SQLException ex) {
          console.printError("Error while cleaning up in database the document " + document.
              getTitle() + " (id = " + document.getOldSilverpeasId() + ")");
          throw ex;
        }
        for (Version version : document.getHistory()) {
          File file = null;
          try {
            file = version.getAttachment();
            if (file != null) {
              ConverterUtil.deleteFile(file);
            }
          } catch (IOException ioex) {
            String fileName = (file != null ? file.getPath() : "");
            console.printError("Error deleting file " + fileName, ioex);
          }
        }
      }
    } finally {
      DbUtils.closeQuietly(deleteVersions);
      DbUtils.closeQuietly(deleteAttachment);
      DbUtils.closeQuietly(connection);
    }
  }

  @Override
  public Long call() throws Exception {
    console.printMessage("Migrating component " + componentId);
    return migrateComponent();
  }

  private List<OldDocumentMetadata> listAllDocuments() throws ParseException, IOException,
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
        OldDocumentMetadata aDocument = fillWithVersion(metadata);
        if (aDocument.getHistory().isEmpty()) {
          console.printWarning("The document " + metadata
              + " doesn't belong to any component instance! So it is not taken into account");
        } else {
          documents.add(aDocument);
        }
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

  private OldDocumentMetadata fillWithVersion(OldDocumentMetadata document) throws ParseException,
      IOException, SQLException {
    Connection connection = getConnection();
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      pstmt = connection.prepareStatement(SELECT_DOCUMENT_VERSION);
      pstmt.setLong(1, document.getOldSilverpeasId());
      rs = pstmt.executeQuery();
      while (rs.next()) {
        Version version = new Version(rs.getInt("versionid"), rs.getInt("versionminornumber"),
            rs.getInt("versionmajornumber"), DateUtil.parse(rs.getString("versioncreationdate")),
            rs.getString("versionauthorid"), rs.getString("versionlogicalname"),
            rs.getString("versionphysicalname"), rs.getString("versionmimetype"),
            rs.getLong("versionsize"), rs.getString("xmlform"), rs.getString("versioncomments"),
            document.getInstanceId());
        // the file related to this version
        File attachment = version.getAttachment();
        // some times, a version of the document can exist in database but references actually no
        // attachements!
        if (attachment != null && attachment.exists() && attachment.isFile() && attachment.length()
            > 0L) {
          document.addVersion(version);
        } else {
          console.printWarning("The file refered by " + version
              + " doesn't exist in the filesystem! So, it is not taken into account");
        }
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

  private void createDocumentPermalink(HistorisedDocument document, OldDocumentMetadata metadata)
      throws SQLException {
    Connection connection = getConnection();
    PreparedStatement pstmt = null;
    try {
      pstmt = connection.prepareStatement(
          "INSERT INTO permalinks_document (documentId, documentUuid) VALUES( ?, ?)");
      pstmt.setLong(1, metadata.getOldSilverpeasId());
      pstmt.setString(2, document.getId());
      pstmt.executeUpdate();
      connection.commit();
    } catch (SQLException sqlex) {
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(pstmt);
      DbUtils.closeQuietly(connection);
    }
  }

  private void createVersionPermalink(String uuid, int versionId) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement pstmt = null;
    try {
      pstmt = connection.prepareStatement(
          "INSERT INTO permalinks_version (versionId, versionUuid) VALUES( ?, ?)");
      pstmt.setLong(1, versionId);
      pstmt.setString(2, uuid);
      pstmt.executeUpdate();
      connection.commit();
    } catch (SQLException sqlex) {
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(pstmt);
      DbUtils.closeQuietly(connection);
    }
  }

  private Connection getConnection() throws SQLException {
    return ConnectionFactory.getConnection();
  }
}
