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
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;
import org.silverpeas.dbbuilder.sql.ConnectionFactory;
import org.silverpeas.migration.jcr.service.AttachmentException;
import org.silverpeas.migration.jcr.service.ConverterUtil;
import org.silverpeas.migration.jcr.service.DocumentMigration;
import org.silverpeas.migration.jcr.service.RepositoryManager;
import org.silverpeas.migration.jcr.service.model.DocumentType;
import org.silverpeas.migration.jcr.service.model.SimpleAttachment;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.migration.jcr.service.repository.DocumentConverter;
import org.silverpeas.migration.jcr.service.repository.DocumentRepository;
import org.silverpeas.util.ConfigurationHolder;
import org.silverpeas.util.Console;
import org.silverpeas.util.DateUtil;
import org.silverpeas.util.StringUtil;
import org.silverpeas.util.file.FileUtil;

import static java.io.File.separatorChar;

/**
 * An optimized alternative to the ComponentAttachmentMigrator class for migrating in the JCR all
 * the non-versioned attachments within a given component instance.
 *
 * @author mmoquillon
 */
public class AttachmentMigration implements Callable<Long> {

  public static final String SELECT_ATTACHMENTS = "SELECT attachmentid, attachmentphysicalname, "
      + "attachmentlogicalname, attachmentdescription, attachmenttype, attachmentsize, "
      + "attachmentcontext, attachmentforeignkey, instanceid, attachmentcreationdate, "
      + "attachmentauthor, attachmenttitle, attachmentinfo, attachmentordernum, workerid, cloneid, "
      + "lang , reservationdate, alertdate, expirydate, xmlform FROM sb_attachment_attachment "
      + "WHERE instanceid = ? ORDER BY attachmentforeignkey, attachmentphysicalname, attachmentordernum";
  public static final String SELECT_ATTACHMENT_TRANSLATION = "SELECT id, attachmentid, lang, "
      + "attachmentphysicalname, attachmentlogicalname, attachmenttype, attachmentsize, "
      + "instanceid, attachmentcreationdate, attachmentauthor, attachmenttitle, attachmentinfo, "
      + "xmlform FROM sb_attachment_attachmenti18n WHERE attachmentid = ? ORDER BY lang";
  public static final String DELETE_ATTACHMENT_TRANSLATIONS =
      "DELETE FROM sb_attachment_attachmenti18n "
      + "WHERE attachmentid = ?";
  public static final String DELETE_ATTACHMENT =
      "DELETE FROM sb_attachment_attachment WHERE attachmentid = ?";
  private final String componentId;
  private final RepositoryManager repositoryManager;
  private final DocumentRepository documentRepository;
  private final Console console;
  private static final DocumentConverter converter = new DocumentConverter();

  public AttachmentMigration(String instanceId, RepositoryManager repositoryManager, Console console) {
    this.componentId = instanceId;
    this.repositoryManager = repositoryManager;
    this.documentRepository = new DocumentRepository(repositoryManager);
    this.console = console;
  }

  protected long migrateComponent() throws Exception {
    console.printMessage("Migrating component " + componentId);
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
      if (session.hasPendingChanges()) {
        session.save();
      }
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

  protected long migrateDocument(Session session, SimpleDocument document) throws Exception {
    File attachment = document.getAttachment().getFile();
    console.printMessage("=> Creating document " + document.getFilename() + " for "
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
        console.printWarning("Attachment " + document.getFilename() + " for " + attachment.
            getAbsolutePath() + " seems to exists already", ex);
      } else {
        console.printError("Error in component " + componentId + " while migrating attachment "
            + document.getFilename() + ": " + ex.getMessage(), ex);
        throw ex;
      }
    }
    long processEnd = System.currentTimeMillis();
    console.printMessage("   document  " + document.getFilename() + " with "
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
        console.printMessage("   => Creating translation " + document.getFilename() + " in "
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
      documentRepository.createDocument(session, document);

      // save the created node(s) in the JCR
      session.save();

      // now copy the content of the file to the new location.
      copyContent(document);
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
      statement.setString(1, componentId);
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
          console.printWarning("The file refered by " + attachment
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
        } else {
          console.printWarning("The translation in " + language
              + " doesn't exist in the filesystem! So, it is not taken into account");
        }
      }
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(pstmt);
      DbUtils.closeQuietly(connection);
    }
  }

  protected File getAttachmenFile(String instanceId, String context, String physicalName) throws
      IOException {
    String baseDirectory = ConfigurationHolder.getDataHome() + separatorChar + "workspaces"
        + separatorChar + instanceId;
    String contextDirectory = "";
    if (context != null) {
      contextDirectory = context;
    }
    String attachmentDirectory = (baseDirectory + separatorChar + "Attachment" + separatorChar
        + contextDirectory).replace('/', separatorChar);
    String directory = (baseDirectory + separatorChar + contextDirectory)
        .replace('/', separatorChar);
    File file = new File(attachmentDirectory, physicalName);
    if (!file.exists() || !file.isFile()) {
      file = new File(directory, physicalName);
      if (!file.exists() || !file.isFile()) {
        file = null;
      }
    }
    if (file == null) {
      console.printError("File " + physicalName + " not found in " + attachmentDirectory + " or in "
          + directory);
    }
    return file;
  }

  protected void cleanAll(long oldSilverpeasId, Set<File> files) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement deleteTranslations = null;
    PreparedStatement deleteAttachment = null;
    try {
      connection.setAutoCommit(false);
      deleteTranslations = connection.prepareStatement(DELETE_ATTACHMENT_TRANSLATIONS);
      deleteTranslations.setLong(1, oldSilverpeasId);
      deleteTranslations.executeUpdate();
      DbUtils.closeQuietly(deleteTranslations);
      deleteAttachment = connection.prepareStatement(DELETE_ATTACHMENT);
      deleteAttachment.setLong(1, oldSilverpeasId);
      deleteAttachment.executeUpdate();
      for (File file : files) {
        ConverterUtil.deleteFile(file);
      }
      connection.commit();
    } catch (SQLException ex) {
      throw ex;
    } finally {
      DbUtils.closeQuietly(deleteTranslations);
      DbUtils.closeQuietly(deleteAttachment);
      DbUtils.closeQuietly(connection);
    }
  }

  protected Connection getConnection() throws SQLException {
    return ConnectionFactory.getConnection();
  }

  @Override
  public Long call() throws Exception {
    return migrateComponent();
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
}
