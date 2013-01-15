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
package org.silverpeas.migration.jcr.attachment;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.concurrent.Callable;

import org.apache.commons.dbutils.DbUtils;

import org.silverpeas.dbbuilder.sql.ConnectionFactory;
import org.silverpeas.migration.jcr.attachment.model.DocumentType;
import org.silverpeas.migration.jcr.attachment.model.SimpleAttachment;
import org.silverpeas.migration.jcr.attachment.model.SimpleDocument;
import org.silverpeas.migration.jcr.attachment.model.SimpleDocumentPK;
import org.silverpeas.migration.jcr.util.ConverterUtil;
import org.silverpeas.util.Console;
import org.silverpeas.util.DateUtil;
import org.silverpeas.util.SilverpeasHomeResolver;
import org.silverpeas.util.StringUtil;
import org.silverpeas.util.file.FileUtil;

import static java.io.File.separatorChar;

/**
 * Migrat the content of a component to JCR storage.
 */
public class ComponentAttachmentMigrator implements Callable<Long> {

  public static final String SELECT_ATTACHMENTS = "SELECT attachmentid, attachmentphysicalname, "
      + "attachmentlogicalname, attachmentdescription, attachmenttype, attachmentsize, "
      + "attachmentcontext, attachmentforeignkey, instanceid, attachmentcreationdate, "
      + "attachmentauthor, attachmenttitle, attachmentinfo, attachmentordernum, workerid, cloneid, "
      + "lang , reservationdate, alertdate, expirydate, xmlform FROM sb_attachment_attachment "
      + "WHERE instanceid = ? ORDER BY attachmentforeignkey, attachmentordernum";
  public static final String SELECT_ATTACHMENT_TRANSLATION = "SELECT id, attachmentid, lang, "
      + "attachmentphysicalname, attachmentlogicalname, attachmenttype, attachmentsize, "
      + "instanceid, attachmentcreationdate, attachmentauthor, attachmenttitle, attachmentinfo, "
      + "xmlform FROM sb_attachment_attachmenti18n WHERE attachmentid = ? ORDER BY lang";
  public static final String DELETE_ATTACHMENT_TRANSLATIONS = "DELETE sb_attachment_attachmenti18n "
      + "WHERE attachmentid = ?";
  public static final String DELETE_ATTACHMENT =
      "DELETE sb_attachment_attachment WHERE attachmentid = ?";
  private final String componentId;
  private final AttachmentService service;
  private final Console console;

  public ComponentAttachmentMigrator(String instanceId, AttachmentService service, Console console) {
    this.componentId = instanceId;
    this.service = service;
    this.console = console;
  }

  protected long migrateComponent() throws SQLException, ParseException, IOException {
    Connection connection = getConnection();
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    long nbMigratedDocuments = 0L;
    try {
      pstmt = connection.prepareStatement(SELECT_ATTACHMENTS);
      pstmt.setString(1, componentId);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        SimpleDocumentPK pk = new SimpleDocumentPK(null, rs.getString("instanceid"));
        pk.setOldSilverpeasId(rs.getLong("attachmentid"));
        String contentType = rs.getString("attachmenttype");
        if (!StringUtil.isDefined(contentType)) {
          contentType = FileUtil.getMimeType(rs.getString("attachmentlogicalname"));
        }
        String author = rs.getString("attachmentauthor");
        if (!StringUtil.isDefined(author)) {
          author = "0";
        }
        SimpleAttachment attachment = new SimpleAttachment(rs.getString("attachmentlogicalname"),
            ConverterUtil.checkLanguage(rs.getString("lang")), rs.getString("attachmenttitle"),
            rs.getString("attachmentinfo"), rs.getLong("attachmentsize"), contentType, author,
            DateUtil.parse(rs.getString("attachmentcreationdate")), rs.getString("xmlform"));
        SimpleDocument document = new SimpleDocument(pk, rs.getString("attachmentforeignkey"),
            rs.getInt("attachmentordernum"), false, rs.getString("workerid"),
            DateUtil.parse(rs.getString("reservationdate")),
            DateUtil.parse(rs.getString("alertdate")), DateUtil.parse(rs.getString("expirydate")),
            rs.getString("attachmentdescription"), attachment);
        document.setDocumentType(DocumentType.fromOldContext(rs.getString("attachmentcontext")));
        File file = getAttachmenFile(rs.getString("instanceid"), rs.getString("attachmentcontext"),
            rs.getString("attachmentphysicalname"));
        if (file != null) {
          createDocument(document, file);
          nbMigratedDocuments++;
        }
      }
    } catch (SQLException sqlex) {
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(pstmt);
      DbUtils.closeQuietly(connection);
    }
    return nbMigratedDocuments;
  }

  protected void createTranslations(SimpleDocument document) throws SQLException, ParseException,
      IOException {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Connection connection = getConnection();
    try {
      pstmt = connection.prepareStatement(SELECT_ATTACHMENT_TRANSLATION);
      pstmt.setLong(1, document.getOldSilverpeasId());
      rs = pstmt.executeQuery();
      while (rs.next()) {
        SimpleDocumentPK pk = new SimpleDocumentPK(null, rs.getString("instanceid"));
        pk.setOldSilverpeasId(rs.getLong("attachmentid"));
        String contentType = rs.getString("attachmenttype");
        if (!StringUtil.isDefined(contentType)) {
          contentType = FileUtil.getMimeType(rs.getString("attachmentlogicalname"));
        }
        String author = rs.getString("attachmentauthor");
        if (!StringUtil.isDefined(author)) {
          author = "0";
        }
        SimpleAttachment attachment = new SimpleAttachment(rs.getString("attachmentlogicalname"),
            ConverterUtil.checkLanguage(rs.getString("lang")), rs.getString("attachmenttitle"),
            rs.getString("attachmentinfo"), rs.getLong("attachmentsize"), contentType, author,
            DateUtil.parse(rs.getString("attachmentcreationdate")), rs.getString("xmlform"));
        document.setFile(attachment);
        document.setDocumentType(DocumentType.fromOldContext(rs.getString("attachmentcontext")));
        File file = getAttachmenFile(rs.getString("instanceid"), rs.getString("attachmentcontext"),
            rs.getString("attachmentphysicalname"));
        if (file != null) {
          console.printMessage("Creating translation " + document.getFilename() + " for " + file.
              getAbsolutePath());
          service.createAttachment(document, file);
        }
      }
    } catch (SQLException sqlex) {
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(pstmt);
      DbUtils.closeQuietly(connection);
    }
  }

  protected void createDocument(SimpleDocument document, File file) throws SQLException,
      ParseException, IOException {
    console.printMessage("Creating document " + document.getFilename() + " for " + file.
        getAbsolutePath());
    SimpleDocument result = service.createAttachment(document, file);
    createTranslations(result);
  }

  protected File getAttachmenFile(String instanceId, String context, String physicalName) throws
      IOException {
    String directory = SilverpeasHomeResolver.getDataHome() + separatorChar + "workspaces"
        + separatorChar + instanceId + separatorChar + "Attachment" + separatorChar + context;
    directory = directory.replace('/', separatorChar);
    File file = new File(directory, physicalName);
    if (!file.exists() || !file.isFile()) {
      console.printError("File " + physicalName + " not found in " + directory);
      directory = SilverpeasHomeResolver.getDataHome() + separatorChar + "workspaces"
          + separatorChar + instanceId + separatorChar + context;
      directory = directory.replace('/', separatorChar);
      file = new File(directory, physicalName);
      if (!file.exists() || !file.isFile()) {
        file = null;
      }
      console.printError("File " + physicalName + " not found in " + directory);
    }
    return file;
  }

  protected void cleanAll(long oldSilverpeasId, Connection connection) throws SQLException {
    PreparedStatement deleteTranslations = null;
    try {
      deleteTranslations = connection.prepareStatement(DELETE_ATTACHMENT_TRANSLATIONS);
      deleteTranslations.setLong(1, oldSilverpeasId);
      deleteTranslations.executeUpdate();
    } catch (SQLException ex) {
      throw ex;
    } finally {
      DbUtils.closeQuietly(deleteTranslations);
    }
    PreparedStatement deleteAttachment = null;
    try {
      deleteAttachment = connection.prepareStatement(DELETE_ATTACHMENT);
      deleteAttachment.setLong(1, oldSilverpeasId);
      deleteAttachment.executeUpdate();
      connection.commit();
    } catch (SQLException ex) {
      throw ex;
    } finally {
      DbUtils.closeQuietly(deleteAttachment);
    }
  }

  private Connection getConnection() throws SQLException {
    return ConnectionFactory.getConnection();
  }

  @Override
  public Long call() throws Exception {
    return migrateComponent();
  }
}
