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
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.silverpeas.migration.jcr.service.AttachmentException;
import org.silverpeas.migration.jcr.service.ConverterUtil;
import org.silverpeas.migration.jcr.service.RepositoryManager;
import org.silverpeas.migration.jcr.service.SimpleDocumentService;
import org.silverpeas.migration.jcr.service.model.HistorisedDocument;
import org.silverpeas.migration.jcr.service.model.SimpleAttachment;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.migration.jcr.service.repository.DocumentConverter;
import org.silverpeas.migration.jcr.service.repository.DocumentRepository;
import org.silverpeas.migration.jcr.version.model.OldDocumentMetadata;
import org.silverpeas.migration.jcr.version.model.Version;
import org.silverpeas.util.Console;
import org.silverpeas.util.StringUtil;

import static javax.jcr.nodetype.NodeType.MIX_SIMPLE_VERSIONABLE;

import static org.silverpeas.migration.jcr.service.JcrConstants.*;

/**
 * An optimized alternative to the ComponentDocumentMigrator class for migrating in the JCR all the
 * versioned documents within a given component instance.
 *
 * @author mmoquillon
 */
class VersionedDocumentMigration extends ComponentDocumentMigrator {

  private static final DocumentConverter converter = new DocumentConverter();

  VersionedDocumentMigration(String instanceId, SimpleDocumentService service, Console console) {
    super(instanceId, service, console);
  }

  @Override
  protected long migrateComponent() throws SQLException, ParseException, IOException {
    long processStart = System.currentTimeMillis();
    Console console = getConsole();
    console.printMessage("Migrating component " + getComponentId());
    long migratedDocumentCount = 0;
    List<OldDocumentMetadata> documents = listAllDocuments();
    Session session = openJCRSession();
    try {
      for (OldDocumentMetadata document : documents) {
        migratedDocumentCount += migrateAllDocumentVersions(session, document);
      }
    } finally {
      session.logout();
    }
    long processEnd = System.currentTimeMillis();
    console.printMessage("Migrating the component " + getComponentId()
        + " required the migration of "
        + migratedDocumentCount + " documents in " + (processEnd - processStart) + "ms");
    console.printMessage("");

    return migratedDocumentCount;
  }

  protected long migrateAllDocumentVersions(Session session, OldDocumentMetadata metadata) throws
      SQLException, ParseException, IOException {
    Console console = getConsole();
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
    cleanAll(metadata);
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

  private RepositoryManager getRepositoryManager() {
    SimpleDocumentService serviceImpl = (SimpleDocumentService) getService();
    return serviceImpl.getRepositoryManager();
  }

  private DocumentRepository getDocumentRepository() {
    SimpleDocumentService serviceImpl = (SimpleDocumentService) getService();
    return serviceImpl.getRepository();
  }

  private void createDocumentNodeInJCR(Session session, HistorisedDocument document) {
    try {
      DocumentRepository documentRepository = getDocumentRepository();
      // set the order of this document relative to others for the given contribution to which
      // it belongs. (The document is an attachment of the contribution.) Indeed, info about some
      // non existing documents for a given contribution can exist in database, so their order
      // hasn't to be taken for true.
      SimpleDocument lastDocument = documentRepository.findLast(session, document.getInstanceId(),
          document.getForeignId());
      if ((null != lastDocument) && (0 >= document.getOrder())) {
        document.setOrder(lastDocument.getOrder() + 1);
      }

      // get/create the parent nodes to the document: /<instance id>/attachments
      Node targetInstanceNode = converter.getFolder(session.getRootNode(), document.
          getInstanceId());
      Node docsNode = converter.getFolder(targetInstanceNode, document.getFolder());
      // add the node representing the document as a versioned one, child of the above node
      // (the oldSilverpeasId is set at node name computation)
      Node documentNode = docsNode.addNode(document.computeNodeName(), SLV_SIMPLE_DOCUMENT);
      converter.setDocumentNodeProperties(document, documentNode);
      documentNode.addMixin(MIX_SIMPLE_VERSIONABLE);
      document.setId(documentNode.getIdentifier());
      document.setOldSilverpeasId(documentNode.getProperty(SLV_PROPERTY_OLD_ID).getLong());

      // save the created node(s) in the JCR
      session.save();
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    }
  }

  private void createVersionNodeInJCR(Session session, HistorisedDocument document)
      throws IOException {
    try {
      // checkout the document node to apply some changes on it
      Node documentNode = session.getNodeByIdentifier(document.getId());
      if (!documentNode.isCheckedOut()) {
        session.getWorkspace().getVersionManager().checkout(documentNode.getPath());
      }

      // set the last owner of the attachment to the document node, who is the author of this
      // attachment version.
      String owner = document.getEditedBy();
      if (!StringUtil.isDefined(owner)) {
        owner = document.getUpdatedBy();
      }
      converter.addStringProperty(documentNode, SLV_PROPERTY_OWNER, owner);

      // set the last changes carried by this version. If there is no one already attachment node
      // for the attachment, then create it as a child of the document node.
      converter.fillNode(document, documentNode);

      // create a version node for the current attachment version
      VersionManager versionManager = documentNode.getSession().getWorkspace().getVersionManager();
      String versionLabel = converter.updateVersion(documentNode, document.getLanguage(), document.
          isPublic());
      session.save();
      javax.jcr.version.Version lastVersion = versionManager.checkin(documentNode.getPath());
      lastVersion.getContainingHistory().addVersionLabel(lastVersion.getName(), versionLabel, false);
      Node versionNode = converter.getCurrentNodeForVersion(lastVersion);
      document.getHistory().add(converter.convertNode(versionNode, document.getLanguage()));

      // get the location, in the filesystem, of the last version if any to duplicate the content
      // of this directory later
      File previousVersionDirectory = null;
      if (!document.getHistory().isEmpty()) {
        String path = document.getDirectoryPath(document.getLanguage());
        previousVersionDirectory = new File(path).getParentFile();
      }

      // update the version of the document.
      document.setMajorVersion(converter.getIntProperty(documentNode, SLV_PROPERTY_MAJOR));
      document.setMinorVersion(converter.getIntProperty(documentNode, SLV_PROPERTY_MINOR));

      // now create the file at the location corresponding to the current version in the filesystem,
      copyContent(document);

      // duplicate the content of the previous version into the location of the new one.
      String path = document.getDirectoryPath(document.getLanguage()).replace('/',
          File.separatorChar);
      File currentVersionDirectory = new File(path).getParentFile();
      duplicateContents(previousVersionDirectory, currentVersionDirectory);

      // save the change in the JCR.
      session.save();
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
      getConsole().printWarning("We have a null id for the author of document " + document
          + " and version " + metadata);
    }
    createVersionNodeInJCR(session, document);
  }

  private Session openJCRSession() {
    try {
      return getRepositoryManager().getSession();
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    }
  }

  private void copyContent(SimpleDocument document) throws IOException, RepositoryException {
    InputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(document.getAttachment().getFile()));
      getDocumentRepository().storeContent(document, in);
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
}
