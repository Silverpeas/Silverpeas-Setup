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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.silverpeas.migration.jcr.service.model.DocumentType;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.migration.jcr.service.repository.DocumentRepository;
import org.silverpeas.util.StringUtil;
import org.silverpeas.util.file.FileUtil;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SimpleDocumentService {

  private final DocumentRepository repository;
  private final RepositoryManager repositoryManager;

  public SimpleDocumentService(String repositoryHome, String repositoryXml) throws
      AttachmentException {
    repositoryManager = new RepositoryManager(repositoryHome, repositoryXml);
    repository = new DocumentRepository(repositoryManager);
  }

  public SimpleDocumentService() throws AttachmentException {
    repositoryManager = new RepositoryManager();
    repository = new DocumentRepository(repositoryManager);
  }

  public DocumentRepository getDocumentRepository() {
    return repository;
  }

  public RepositoryManager getRepositoryManager() {
    return repositoryManager;
  }

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
      repository.addContent(session, document.getPk(), document.getAttachment());
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

  public SimpleDocument mergeDocument(SimpleDocument document, SimpleDocument merge,
      boolean deleteAfterMerge) {
    File content = new File(merge.getAttachmentPath());
    if (!content.exists() || !content.isFile()) {
      merge.setLanguage(ConverterUtil.defaultLanguage);
      content = new File(merge.getAttachmentPath());
    }
    if (content.exists() && content.isFile()) {
      updateAttachment(document, content);
      if (deleteAfterMerge) {
        deleteAttachment(merge);
      }
    }
    return searchDocumentById(document.getPk(), document.getLanguage());
  }

  public void removeContent(SimpleDocument document, String lang) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      boolean requireLock = repository.lock(session, document, document.getEditedBy());
      boolean existsOtherContents = repository.removeContent(session, document.getPk(), lang);
      session.save();
      SimpleDocument finalDocument = document;
      if (requireLock) {
        finalDocument = repository.unlockFromContentDeletion(session, document);
        if (existsOtherContents) {
          repository.duplicateContent(document, finalDocument);
        }
      }
      finalDocument.setLanguage(lang);
      final File fileToDelete;
      if (!existsOtherContents) {
        fileToDelete =
            new File(finalDocument.getDirectoryPath(null)).getParentFile().getParentFile();
      } else {
        fileToDelete = new File(finalDocument.getAttachmentPath());
      }
      FileUtils.deleteQuietly(fileToDelete);
      FileUtil.deleteEmptyDir(fileToDelete.getParentFile());
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } catch (IOException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  public void getBinaryContent(OutputStream output, SimpleDocumentPK pk, String lang) {
    getBinaryContent(output, pk, lang, 0, -1);
  }

  public void getBinaryContent(final OutputStream output, final SimpleDocumentPK pk,
      final String lang, final long contentOffset, final long contentLength) {
    Session session = null;
    InputStream in = null;
    try {
      session = repositoryManager.getSession();
      in = repository.getContent(session, pk, lang);
      if (in != null) {
        IOUtils.copyLarge(in, output, contentOffset, contentLength);
      }
    } catch (IOException ex) {
      throw new AttachmentException(ex);
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      IOUtils.closeQuietly(in);
      repositoryManager.logout(session);
    }
  }

  public List<String> listBasenames(String instanceId) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      return new ArrayList<String>(repository.listWysiwygFileNames(session, instanceId));
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  public List<String> listForeignIdsWithWysiwyg(String instanceId) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      return new ArrayList<String>(
          repository.listForeignIdsByType(session, instanceId, DocumentType.wysiwyg));
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  /**
   * Gets the list of attachments of WYSIWYG type for the given instance identifier .
   * For each document represented by a master JCR Node, the number of
   * SimpleDocument returned depends on the number of languages registered for the JCR Node.
   * For a document, if it exists one version in "fr" and an other one in "en" (for example), two
   * SimpleDocument are returned, one for the "fr" language and an other one for "en" language.
   * @param instanceId the identifier of the component instance limitation.
   * @return
   * @throws RepositoryException
   */
  public List<SimpleDocument> listWysiwygByInstanceId(String instanceId) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      return repository
          .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, DocumentType.wysiwyg);
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  /**
   * Gets the list of attachments of WYSIWYG type for the given instance identifier and
   * foreign identifier. For each document represented by a master JCR Node, the number of
   * SimpleDocument returned depends on the number of languages registered for the JCR Node.
   * For a document, if it exists one version in "fr" and an other one in "en" (for example), two
   * SimpleDocument are returned, one for the "fr" language and an other one for "en" language.
   * @param instanceId the identifier of the component instance limitation.
   * @param foreignId the identifier of object limitation.
   * @return
   * @throws RepositoryException
   */
  public List<SimpleDocument> listWysiwygByForeignId(String instanceId, String foreignId) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      return repository.listAttachmentsByForeignIdAndDocumentType(session, instanceId, foreignId,
          DocumentType.wysiwyg);
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  public List<SimpleDocument> listWysiwygForBasename(String basename, String instanceId) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      return repository.listWysiwygAttachmentsByBasename(session, instanceId, basename);
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  public List<String> listComponentIdsWithWysiwyg() {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      return repository.listComponentsWithWysiwyg(session);
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }

  public void shutdown() {
    this.repositoryManager.shutdown();
  }

  /**
   * Delete a given attachment.
   *
   * @param document the attachmentDetail object to deleted.
   */
  public void deleteAttachment(SimpleDocument document) {
    Session session = null;
    try {
      session = repositoryManager.getSession();
      repository.fillNodeName(session, document);
      repository.deleteDocument(session, document.getPk());
      session.save();
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    } finally {
      repositoryManager.logout(session);
    }
  }
}
