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
package org.silverpeas.migration.jcr.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.silverpeas.migration.jcr.service.model.DocumentType;
import org.silverpeas.migration.jcr.service.model.ForeignPK;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.migration.jcr.service.model.UnlockContext;
import org.silverpeas.migration.jcr.service.model.WAPrimaryKey;

/**
 * @author ehugonnet
 */
public interface AttachmentService {

  /**
   * Create file attached to an object who is identified by the foreignId.
   *
   * @param document the document to be created.
   * @param content the binary content of the document.
   * @return the stored document.
   * @throws AttachmentException
   */
  SimpleDocument createAttachment(SimpleDocument document, InputStream content)
      throws AttachmentException;

  /**
   * Create file attached to an object who is identified by the foreignId.
   *
   * @param document the document to be created.
   * @param content the binary content of the document.
   * @return the stored document.
   * @throws AttachmentException
   */
  SimpleDocument createAttachment(SimpleDocument document, File content) throws AttachmentException;

  /**
   * Search the document.
   *
   * @param primaryKey the primary key of document.
   * @param lang the lang of the document.
   * @return java.util.Vector: a collection of AttachmentDetail
   * @throws AttachmentException when is impossible to search
   */
  SimpleDocument searchDocumentById(SimpleDocumentPK primaryKey, String lang);

  /**
   * Search all files attached to a foreign object.
   *
   * @param foreignKey : the primary key of foreign object.
   * @param lang the language of the documents.
   * @return the list of attached documents.
   * @throws AttachmentException when is impossible to search
   */
  List<SimpleDocument> listDocumentsByForeignKey(WAPrimaryKey foreignKey, String lang);

  /**
   * Search all documents (files, xmlform content, wysiwyg) attached to a foreign object.
   *
   * @param foreignKey : the primary key of foreign object.
   * @param lang the language of the documents.
   * @return the list of attached documents.
   * @throws AttachmentException when is impossible to search
   */
  List<SimpleDocument> listAllDocumentsByForeignKey(WAPrimaryKey foreignKey, String lang);

  /**
   * Search all file attached to a foreign object.
   *
   * @param foreignKey : the primary key of foreign object.
   * @param type : the type of document
   * @param lang the lang for the documents.
   * @return the list of attached documents.
   * @throws AttachmentException when is impossible to search
   */
  List<SimpleDocument> listDocumentsByForeignKeyAndType(WAPrimaryKey foreignKey, DocumentType type,
      String lang);

  /**
   * To update the document : status, metadata but not its content.
   *
   * @param document
   */
  void updateAttachment(SimpleDocument document);

  /**
   * To update a document content by updating or adding some content.
   *
   * @param document
   * @param content
   */
  void updateAttachment(SimpleDocument document, File content);

  /**
   * To update a document content by updating or adding some content.
   *
   * @param document
   * @param content
   */
  void updateAttachment(SimpleDocument document, InputStream content);

  /**
   * Checkout a file to be updated by user.
   *
   * @param attachmentId the id of the attachemnt to be locked.
   * @param userId : the user locking and modifying the attachment.
   * @param language the language of the attachment.
   * @return false if the attachment is already checkout - true if the attachment was successfully
   * checked out.
   */
  public boolean lock(String attachmentId, String userId, String language);

  /**
   * Release a locked file.
   *
   * @param context : the unlock parameters.
   * @return false if the file is locked - true if the unlock succeeded.
   * @throws AttachmentException
   */
  public boolean unlock(UnlockContext context);

  /**
   * Find documents with the same name attached to the specified foreign id.
   *
   * @param fileName the name of the file.
   * @param pk the id of the document.
   * @param lang the language of the document.
   * @param foreign the id of the container of the document.
   * @return a document with the same filename - null if none is found.
   */
  public SimpleDocument findExistingDocument(SimpleDocumentPK pk, String fileName,
      ForeignPK foreign, String lang);

  /**
   * Shutdowns the repository.
   */
  public void shutdown();

  /**
   * Delete a given attachment.
   *
   * @param document the document to deleted.
   */
  void deleteAttachment(SimpleDocument document);

  /**
   * Find all components that contains wysiwyg.
   *
   * @return list all component ids with wysiwyg.
   */
  public List<String> listComponentIdsWithWysiwyg();

  /**
   * Find all basenames for wysiwyg content.
   *
   * @param instanceId the component containing the wysiwyg.
   * @return list all the wysiwyg basenames.
   */
  public List<String> listBasenames(String instanceId);

  /**
   * Merge the documents into the document.
   *
   * @param source the document into which the document will be merged.
   * @param toMerge the documents to merge.
   * @return the merged document.
   */
  public SimpleDocument mergeDocument(SimpleDocument source, SimpleDocument toMerge);

  /**
   *
   * @param basename
   * @param instanceId
   * @return
   */
  public List<SimpleDocument> listWysiwygForBasename(String basename, String instanceId);
}
