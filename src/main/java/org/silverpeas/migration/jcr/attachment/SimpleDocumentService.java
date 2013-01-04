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
package org.silverpeas.migration.jcr.attachment;

import org.apache.commons.io.IOUtils;
import org.silverpeas.migration.jcr.attachment.model.DocumentType;
import org.silverpeas.migration.jcr.attachment.model.ForeignPK;
import org.silverpeas.migration.jcr.attachment.model.SimpleDocument;
import org.silverpeas.migration.jcr.attachment.model.SimpleDocumentPK;
import org.silverpeas.migration.jcr.attachment.model.UnlockContext;
import org.silverpeas.migration.jcr.attachment.model.WAPrimaryKey;
import org.silverpeas.migration.jcr.attachment.repository.DocumentRepository;
import org.silverpeas.migration.jcr.util.RepositoryManager;
import org.silverpeas.util.StringUtil;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

public class SimpleDocumentService implements AttachmentService {

  private final DocumentRepository repository;
  private final RepositoryManager repositoryManager;

  public SimpleDocumentService(String repositoryHome, String conf) {
    repositoryManager = new RepositoryManager(repositoryHome, conf);
    repository = new DocumentRepository(repositoryManager);
  }

  public SimpleDocumentService() {
    repositoryManager = new RepositoryManager();
    repository = new DocumentRepository(repositoryManager);
  }

  /**
   * Create file attached to an object who is identified by the foreignId.
   * @param document the document to be created.
   * @param content the binary content of the document.
   * @return the stored document.
   */
  @Override
  public SimpleDocument createAttachment(SimpleDocument document, InputStream content)
      throws AttachmentException {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      SimpleDocumentPK docPk = repository.createDocument(session, document);

      session.save();
      SimpleDocument createdDocument = repository.findDocumentById(session, docPk, document.
          getLanguage());
      createdDocument.setPublicDocument(document.isPublic());
      SimpleDocument finalDocument = repository.unlock(session, createdDocument, false);
      repository.storeContent(finalDocument, content);
      return finalDocument;
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } catch (IOException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  @Override
  public SimpleDocument searchDocumentById(SimpleDocumentPK primaryKey, String lang) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      if (StringUtil.isDefined(primaryKey.getId())) {
        return repository.findDocumentById(session, primaryKey, lang);
      }
      SimpleDocument doc = repository
          .findDocumentByOldSilverpeasId(session, primaryKey.getComponentName(),
          primaryKey.getOldSilverpeasId(), false, lang);
      if (doc == null) {
        doc = repository.findDocumentByOldSilverpeasId(session, primaryKey.getComponentName(),
            primaryKey.getOldSilverpeasId(), true, lang);
      }
      return doc;
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  @Override
  public List<SimpleDocument> listAllDocumentsByForeignKey(WAPrimaryKey foreignKey, String lang) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      return repository.listAllDocumentsByForeignId(session, foreignKey.getInstanceId(), foreignKey
          .
          getId(), lang);
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  @Override
  public List<SimpleDocument> listDocumentsByForeignKey(WAPrimaryKey foreignKey, String lang) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      return repository.listDocumentsByForeignId(session, foreignKey.getInstanceId(), foreignKey.
          getId(), lang);
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  @Override
  public void updateAttachment(SimpleDocument document) throws AttachmentException {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      SimpleDocument oldAttachment =
          repository.findDocumentById(session, document.getPk(), document.getLanguage());
      repository.fillNodeName(session, document);
      repository.updateDocument(session, document);
      session.save();
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } catch (IOException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  @Override
  public void updateAttachment(SimpleDocument document, File content) {
    InputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(content));
      updateAttachment(document, in);
    } catch (FileNotFoundException ex) {
      throw new AttachmentException(ex);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Override
  public void updateAttachment(SimpleDocument document, InputStream in) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      String owner = document.getEditedBy();
      if (!StringUtil.isDefined(owner)) {
        owner = document.getUpdatedBy();
      }
      boolean checkinRequired = repository.lock(session, document, owner);
      repository.updateDocument(session, document);
      repository.addContent(session, document.getPk(), document.getFile());
      repository.fillNodeName(session, document);
      SimpleDocument finalDocument = document;
      if (checkinRequired) {
        finalDocument = repository.unlock(session, document, false);
      }
      repository.storeContent(finalDocument, in);
      repository.duplicateContent(document, finalDocument);
      session.save();
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } catch (IOException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  @Override
  public SimpleDocument createAttachment(SimpleDocument document, File content) {
    InputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(content));
      return createAttachment(document, in);
    } catch (FileNotFoundException ex) {
      throw new AttachmentException(ex);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Release a locked file.
   * @param context : the unlock parameters.
   * @return false if the file is locked - true if the unlock succeeded.
   * @throws AttachmentException
   */
  @Override
  public boolean unlock(UnlockContext context) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      SimpleDocument document = repository
          .findDocumentById(session, new SimpleDocumentPK(context.getAttachmentId()),
          context.getLang());
      if (!context.isForce() && document.isReadOnly() && !document.getEditedBy().equals(context.
          getUserId())) {
        return false;
      }
      if (context.isWebdav() || context.isUpload()) {
        String workerId = document.getEditedBy();
        document.setUpdated(new Date());
        document.setUpdatedBy(workerId);
      }
      document.setPublicDocument(context.isPublicVersion());
      document.setComment(context.getComment());
      SimpleDocument finalDocument = repository.unlock(session, document, context.isForce());

      File file = new File(finalDocument.getAttachmentPath());
      if (!file.exists() && !context.isForce()) {
        repository.duplicateContent(document, finalDocument);
      }
      session.save();
    } catch (IOException e) {
      throw new AttachmentException(e);
    } catch (RepositoryException e) {
      throw new AttachmentException(e);
    } finally {
      repositoryManager.logout(session);
    }
    return true;
  }

  /**
   * Lock a file so it can be edited by an user.
   * @param attachmentId
   * @param userId
   * @param language
   * @return false if the attachment is already checkout - true if the attachment was successfully
   * checked out.
   */
  @Override
  public boolean lock(String attachmentId, String userId, String language) {
    Session session = null;
    try {
      SimpleDocumentPK pk = new SimpleDocumentPK(attachmentId);
      session = repositoryManager.getSession();
      SimpleDocument document = repository.findDocumentById(session, pk, language);
      if (document.isReadOnly()) {
        return document.getEditedBy().equals(userId);
      }
      repository.lock(session, document, document.getEditedBy());
      document.edit(userId);
      updateAttachment(session, document);
      session.save();
      return true;
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } catch (IOException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  private void updateAttachment(Session session, SimpleDocument document)
      throws RepositoryException, IOException {
    repository.updateDocument(session, document);
  }

  @Override
  public SimpleDocument findExistingDocument(SimpleDocumentPK pk, String fileName,
      ForeignPK foreign, String lang) {
    List<SimpleDocument> exisitingsDocuments = listDocumentsByForeignKey(foreign, lang);
    SimpleDocument document = searchDocumentById(pk, lang);
    if (document == null) {
      for (SimpleDocument doc : exisitingsDocuments) {
        if (doc.getFilename().equalsIgnoreCase(fileName)) {
          return doc;
        }
      }
    }
    return document;
  }

  @Override
  public List<SimpleDocument> listDocumentsByForeignKeyAndType(WAPrimaryKey foreignKey,
      DocumentType type, String lang) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      return repository
          .listDocumentsByForeignIdAndType(session, foreignKey.getInstanceId(), foreignKey.
          getId(), type, lang);
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }
}
