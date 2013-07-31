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
package org.silverpeas.migration.jcr.attachment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;
import org.silverpeas.migration.jcr.service.AttachmentException;
import org.silverpeas.migration.jcr.service.AttachmentService;
import org.silverpeas.migration.jcr.service.ConverterUtil;
import org.silverpeas.migration.jcr.service.DocumentMigration;
import org.silverpeas.migration.jcr.service.RepositoryManager;
import org.silverpeas.migration.jcr.service.SimpleDocumentService;
import org.silverpeas.migration.jcr.service.model.DocumentType;
import org.silverpeas.migration.jcr.service.model.SimpleAttachment;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.migration.jcr.service.repository.DocumentConverter;
import org.silverpeas.migration.jcr.service.repository.DocumentRepository;
import org.silverpeas.util.Console;
import org.silverpeas.util.DateUtil;
import org.silverpeas.util.StringUtil;
import org.silverpeas.util.file.FileUtil;

import static org.silverpeas.migration.jcr.attachment.ComponentAttachmentMigrator.SELECT_ATTACHMENTS;
import static org.silverpeas.migration.jcr.attachment.ComponentAttachmentMigrator.SELECT_ATTACHMENT_TRANSLATION;
import static org.silverpeas.migration.jcr.service.JcrConstants.SLV_PROPERTY_OLD_ID;
import static org.silverpeas.migration.jcr.service.JcrConstants.SLV_SIMPLE_DOCUMENT;

/**
 * An optimized alternative to the ComponentAttachmentMigrator class for migrating in the JCR all
 * the non-versioned attachments within a given component instance.
 *
 * @author mmoquillon
 */
public class AttachmentMigration extends ComponentAttachmentMigrator {

  private static final DocumentConverter converter = new DocumentConverter();

  public AttachmentMigration(String instanceId, AttachmentService service, Console console) {
    super(instanceId, service, console);
  }

  @Override
  protected long migrateComponent() throws Exception {
    Console console = getConsole();
    console.printMessage("Migrating component " + getComponentId());
    long processStart = System.currentTimeMillis();
    long migratedDocumentCount = 0;
    final Session session = openJCRSession();
    try {
      migratedDocumentCount += migrateAllDocuments(new DocumentMigration() {
        @Override
        public long migrate(SimpleDocument document) throws Exception {
          return migrateDocument(session, document);
        }
      });
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

  protected long migrateDocument(Session session, SimpleDocument document) throws Exception {
    File attachment = document.getAttachment().getFile();
    getConsole().printMessage("=> Creating document " + document.getFilename() + " for "
        + attachment.getAbsolutePath());
    long processStart = System.currentTimeMillis();
    long translationCount = 0;
    try {
      createDocumentNodeInJCR(session, document);
      Set<File> files = migrateDocumentTranslations(session, document);
      translationCount = files.size();
      files.add(attachment);
      cleanAll(document.getOldSilverpeasId(), files);
    } catch (AttachmentException ex) {
      if (ex.getCause() instanceof ItemExistsException) {
        getConsole().printWarning("Attachment " + document.getFilename() + " for " + attachment.
            getAbsolutePath() + " seems to exists already", ex);
      } else {
        throw ex;
      }
    }
    long processEnd = System.currentTimeMillis();
    getConsole().printMessage("   document  " + document.getFilename() + " with "
        + translationCount + " translations has been created in " + (processEnd - processStart)
        + "ms");
    return 1;
  }

  private Set<File> migrateDocumentTranslations(final Session session, SimpleDocument document)
      throws Exception {
    final Set<File> translationFiles = new HashSet<File>();
    migrateAllTranslations(document, new DocumentMigration() {
      @Override
      public long migrate(SimpleDocument document) throws Exception {
        File file = document.getAttachment().getFile();
        getConsole().printMessage("   => Creating translation " + document.getFilename() + " in "
            + document.getAttachment().getLanguage() + " for " + file.getAbsolutePath());
        createTranslationInJCR(session, document);
        translationFiles.add(file);
        return 0;
      }
    });
    return translationFiles;
  }

  private void createDocumentNodeInJCR(Session session, SimpleDocument document) throws IOException {
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

      // get/create the parent nodes to the document: /<instance id>/<document type>
      Node targetInstanceNode = converter.getFolder(session.getRootNode(), document.
          getInstanceId());
      Node docsNode = converter.getFolder(targetInstanceNode, document.getFolder());
      // add the node representing the document as a versioned one, child of the above node
      // (the oldSilverpeasId is set at node name computation)
      Node documentNode = docsNode.addNode(document.computeNodeName(), SLV_SIMPLE_DOCUMENT);
      // set the properties of the node and create the attachment node as a child of the node above
      // and set also its properties
      converter.fillNode(document, documentNode);
      document.setId(documentNode.getIdentifier());
      document.setOldSilverpeasId(documentNode.getProperty(SLV_PROPERTY_OLD_ID).getLong());

      // now copy the content of the file to the new location.
      copyContent(document);

      // save the created node(s) in the JCR
      session.save();
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    }
  }

  private void createTranslationInJCR(Session session, SimpleDocument document) throws IOException {
    try {
      // create the attachment node as a child of the document node.
      Node documentNode = session.getNodeByIdentifier(document.getId());
      converter.addAttachment(documentNode, document.getAttachment());

      // save the translation node.
      session.save();

      // copy now the content of the attachment.
      copyContent(document);
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    }
  }

  /**
   * Fetches the attachments to migrate and for each of them perform the specified migration
   * process.
   *
   * @param migration the task of migrating a document.
   * @return the total number of migrated documents.
   * @throws Exception if an error occurs while fetching the attachments or while migrating them.
   */
  private long migrateAllDocuments(DocumentMigration migration) throws Exception {
    long migratedDocumentCount = 0;
    Connection connection = getConnection();
    PreparedStatement statement = null;
    ResultSet result = null;
    try {
      statement = connection.prepareStatement(SELECT_ATTACHMENTS);
      statement.setString(1, getComponentId());
      result = statement.executeQuery();
      while (result.next()) {
        String instanceId = result.getString("instanceid");
        SimpleDocumentPK pk = new SimpleDocumentPK(null, instanceId);
        pk.setOldSilverpeasId(result.getLong("attachmentid"));
        String contentType = result.getString("attachmenttype");
        if (!StringUtil.isDefined(contentType)) {
          contentType = FileUtil.getMimeType(result.getString("attachmentlogicalname"));
        }
        String author = result.getString("attachmentauthor");
        if (!StringUtil.isDefined(author)) {
          author = "0";
        }
        String useContext = result.getString("attachmentcontext");
        SimpleAttachment attachment = new SimpleAttachment(
            result.getString("attachmentlogicalname"),
            ConverterUtil.checkLanguage(result.getString("lang")),
            result.getString("attachmenttitle"),
            result.getString("attachmentinfo"),
            result.getLong("attachmentsize"),
            contentType,
            author,
            DateUtil.parse(result.getString("attachmentcreationdate")),
            result.getString("xmlform"));
        // the file to relocate with the migration in the JCR
        File file = getAttachmenFile(result.getString("instanceid"), useContext, result.getString(
            "attachmentphysicalname"));
        // some times, the info about an attachment can exist in database but references actually no
        // file in the filesystem!
        if (file != null) {
          // link the attachment to the actual file in the filesystem in order to relocate it later
          // with the migration in the JCR.
          attachment.setFile(file);
          SimpleDocument document = new SimpleDocument(pk, result.getString("attachmentforeignkey"),
              result.getInt("attachmentordernum"), false, result.getString("workerid"),
              DateUtil.parse(result.getString("reservationdate")),
              DateUtil.parse(result.getString("alertdate")), DateUtil.parse(result.getString(
              "expirydate")),
              result.getString("attachmentdescription"), attachment);
          document.setDocumentType(DocumentType.fromOldContext(instanceId, useContext));
          document.setOldContext(useContext);
          document.setUpdated(document.getCreated());
          document.setUpdatedBy(author);
          migratedDocumentCount += migration.migrate(document);
        } else {
          getConsole().printWarning("The file refered by " + attachment
              + " doesn't exist in the filesystem! So, it is not taken into account");
        }
      }
    } finally {
      DbUtils.closeQuietly(result);
      DbUtils.closeQuietly(statement);
      DbUtils.closeQuietly(connection);
    }
    return migratedDocumentCount;
  }

  private void migrateAllTranslations(SimpleDocument document,
      DocumentMigration migration) throws Exception {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Connection connection = getConnection();
    try {
      pstmt = connection.prepareStatement(SELECT_ATTACHMENT_TRANSLATION);
      pstmt.setLong(1, document.getOldSilverpeasId());
      rs = pstmt.executeQuery();
      while (rs.next()) {
        String instanceId = rs.getString("instanceid");
        SimpleDocumentPK pk = new SimpleDocumentPK(null, instanceId);
        pk.setOldSilverpeasId(rs.getLong("attachmentid"));
        String contentType = rs.getString("attachmenttype");
        if (!StringUtil.isDefined(contentType)) {
          contentType = FileUtil.getMimeType(rs.getString("attachmentlogicalname"));
        }
        String author = rs.getString("attachmentauthor");
        if (!StringUtil.isDefined(author)) {
          author = "0";
        }
        String language = rs.getString("lang");
        if (!StringUtil.isDefined(language)) {
          language = ConverterUtil.extractLanguage(rs.getString("attachmentlogicalname"));
        }
        language = ConverterUtil.checkLanguage(language);
        File file = getAttachmenFile(rs.getString("instanceid"), document.getOldContext(),
            rs.getString("attachmentphysicalname"));
        if (file != null) {
          SimpleAttachment attachment = new SimpleAttachment(rs.getString("attachmentlogicalname"),
              language, rs.getString("attachmenttitle"), rs.getString("attachmentinfo"),
              rs.getLong("attachmentsize"), contentType, author, DateUtil.parse(rs.getString(
              "attachmentcreationdate")), rs.getString("xmlform"));
          attachment.setFile(file);
          document.setAttachment(attachment);
          document.setUpdated(attachment.getCreated());
          document.setUpdatedBy(attachment.getCreatedBy());
          migration.migrate(document);
        }
      }
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(pstmt);
      DbUtils.closeQuietly(connection);
    }
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

  private RepositoryManager getRepositoryManager() {
    SimpleDocumentService serviceImpl = (SimpleDocumentService) getService();
    return serviceImpl.getRepositoryManager();
  }

  private DocumentRepository getDocumentRepository() {
    SimpleDocumentService serviceImpl = (SimpleDocumentService) getService();
    return serviceImpl.getRepository();
  }
}
