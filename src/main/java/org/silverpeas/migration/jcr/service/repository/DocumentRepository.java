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
package org.silverpeas.migration.jcr.service.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.ChildNode;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.DescendantNode;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import org.apache.commons.io.FileUtils;
import org.silverpeas.migration.jcr.service.ConverterUtil;
import org.silverpeas.migration.jcr.service.RepositoryManager;
import org.silverpeas.migration.jcr.service.model.DocumentType;
import org.silverpeas.migration.jcr.service.model.SimpleAttachment;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.util.Console;
import org.silverpeas.util.StringUtil;
import org.silverpeas.util.file.FileUtil;

import static javax.jcr.Property.JCR_LANGUAGE;
import static javax.jcr.nodetype.NodeType.MIX_SIMPLE_VERSIONABLE;

import static org.silverpeas.migration.jcr.service.JcrConstants.*;

/**
 * @author ehugonnet
 */
public class DocumentRepository {

  private static final Console console = new Console(DocumentConverter.class);
  private final RepositoryManager repositoryManager;
  private static final Pattern COMPONENTNAME_PATTERN = Pattern.compile("[a-zA-Z]+\\d+");

  public DocumentRepository(RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }
  private static final String SIMPLE_ATTACHMENT_ALIAS = "SimpleAttachments";
  private static final String SIMPLE_DOCUMENT_ALIAS = "SimpleDocuments";
  final DocumentConverter converter = new DocumentConverter();

  public Node prepareComponentAttachments(Session session, String instanceId, String folder)
      throws RepositoryException {
    Node targetInstanceNode = converter.getFolder(session.getRootNode(), instanceId);
    return converter.getFolder(targetInstanceNode, folder);
  }

  /**
   * Creates the specified document into the JCR. The document represents an attachment of a given
   * contribution, identified by the foreignId attribute. It gathers the meta-data about the
   * attachment whereas the embedded SimpleAttachment instance represents the actual attached file.
   *
   * @param session the current session in the JCR.
   * @param document the document to create.
   * @return the identifier of the document which refers the identifier of the document node in the
   * JCR.
   * @throws RepositoryException if an error occurs.
   */
  public SimpleDocumentPK createDocument(Session session, SimpleDocument document) throws
      RepositoryException {
    // set the order of this document relative to others for the given contribution to which
    // it belongs. (The document is an attachment of the contribution.) Indeed, info about some
    // non existing documents for a given contribution can exist in database, so their order
    // hasn't to be taken for true.
    SimpleDocument last = findLast(session, document.getInstanceId(), document.getForeignId());
    if ((null != last) && (0 >= document.getOrder())) {
      document.setOrder(last.getOrder() + 1);
    }

    // get/create the parent nodes to the document: /<instance id>/<document type>
    Node docsNode = prepareComponentAttachments(session, document.getInstanceId(), document.
        getFolder());
    // add the node representing the document, child of the above node (the oldSilverpeasId is set
    // at node name computation)
    Node documentNode = docsNode.addNode(document.computeNodeName(), SLV_SIMPLE_DOCUMENT);
    converter.fillNode(document, documentNode);
    if (document.isVersioned()) {
      documentNode.addMixin(MIX_SIMPLE_VERSIONABLE);
    }
    document.setId(documentNode.getIdentifier());
    document.setOldSilverpeasId(documentNode.getProperty(SLV_PROPERTY_OLD_ID).getLong());
    return document.getPk();
  }

  /**
   * Create file attached to an object who is identified by "PK" SimpleDocument object contains an
   * attribute who identifie the link by a foreign key.
   *
   * @param session
   * @param document
   * @throws RepositoryException
   * @throws IOException
   */
  public void updateDocument(Session session, SimpleDocument document) throws
      RepositoryException, IOException {
    Node documentNode = session.getNodeByIdentifier(document.getPk().getId());
    if (StringUtil.isDefined(document.getEditedBy())) {
      document.setUpdatedBy(document.getEditedBy());
    }
    converter.fillNode(document, documentNode);
  }

  /**
   * Delete a file attached to an object who is identified by "PK" SimpleDocument object contains an
   * attribute who identifie the link by a foreign key.
   *
   * @param session
   * @param documentPk
   * @throws RepositoryException
   */
  public void deleteDocument(Session session, SimpleDocumentPK documentPk) throws
      RepositoryException {
    try {
      Node documentNode = session.getNodeByIdentifier(documentPk.getId());
      deleteContent(documentNode, documentPk.getInstanceId());
      deleteDocumentNode(documentNode);
    } catch (ItemNotFoundException infex) {
      console.printError("DocumentRepository.deleteDocument()", infex);
    }
  }

  /**
   * @param session
   * @param documentPk
   * @param lang
   * @return
   * @throws RepositoryException
   */
  public SimpleDocument findDocumentById(Session session, SimpleDocumentPK documentPk, String lang)
      throws RepositoryException {
    try {
      Node documentNode = session.getNodeByIdentifier(documentPk.getId());
      return converter.convertNode(documentNode, lang);
    } catch (ItemNotFoundException infex) {
      console.printError("DocumentRepository.findDocumentById()", infex);
    }
    return null;
  }

  /**
   * @param session
   * @param instanceId
   * @param oldSilverpeasId
   * @param versioned
   * @param lang
   * @return
   * @throws RepositoryException
   */
  public SimpleDocument findDocumentByOldSilverpeasId(Session session, String instanceId,
      long oldSilverpeasId, boolean versioned, String lang) throws RepositoryException {
    QueryManager manager = session.getWorkspace().getQueryManager();
    QueryObjectModelFactory factory = manager.getQOMFactory();
    Selector source = factory.selector(SLV_SIMPLE_DOCUMENT, SIMPLE_DOCUMENT_ALIAS);
    DescendantNode descendantdNodeConstraint = factory.descendantNode(SIMPLE_DOCUMENT_ALIAS,
        session.getRootNode().getPath() + instanceId);
    Comparison oldSilverpeasIdComparison = factory.comparison(factory.propertyValue(
        SIMPLE_DOCUMENT_ALIAS, SLV_PROPERTY_OLD_ID), QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO,
        factory.literal(session.getValueFactory().createValue(oldSilverpeasId)));
    Comparison versionedComparison = factory.comparison(factory.propertyValue(SIMPLE_DOCUMENT_ALIAS,
        SLV_PROPERTY_VERSIONED), QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, factory.
        literal(session.getValueFactory().createValue(versioned)));

    QueryObjectModel query = factory.createQuery(source, factory.and(descendantdNodeConstraint,
        factory.and(oldSilverpeasIdComparison, versionedComparison)), null, null);
    QueryResult result = query.execute();
    NodeIterator iter = result.getNodes();
    if (iter.hasNext()) {
      return converter.convertNode(iter.nextNode(), lang);
    }
    return null;
  }

  /**
   * Add the content.
   *
   * @param session the current JCR session.
   * @param documentPk the document which content is to be added.
   * @param attachment the attachment metadata.
   * @throws RepositoryException
   */
  public void addContent(Session session, SimpleDocumentPK documentPk, SimpleAttachment attachment)
      throws RepositoryException {
    Node documentNode = session.getNodeByIdentifier(documentPk.getId());
    if (converter.isVersioned(documentNode) && !documentNode.isCheckedOut()) {
      String owner = attachment.getUpdatedBy();
      if (!StringUtil.isDefined(owner)) {
        owner = attachment.getCreatedBy();
      }
      checkoutNode(documentNode, owner);
    }
    converter.addAttachment(documentNode, attachment);
  }

  private void deleteDocumentNode(Node documentNode) throws RepositoryException {
    if (null != documentNode) {
      if (converter.isVersioned(documentNode)) {
        removeHistory(documentNode);
      }
      documentNode.remove();
    }
  }

  /**
   * The last document in an instance with the specified foreignId.
   *
   * @param session the current JCR session.
   * @param instanceId the component id containing the documents.
   * @param foreignId the id of the container owning the documents.
   * @return the last document in an instance with the specified foreignId.
   * @throws RepositoryException
   */
  private SimpleDocument findLast(Session session, String instanceId, String foreignId) throws
      RepositoryException {
    NodeIterator iter = selectDocumentsByForeignIdAndType(session, instanceId, foreignId,
        DocumentType.attachment);
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      if (!iter.hasNext()) {
        return converter.convertNode(node, ConverterUtil.defaultLanguage);
      }
    }
    return null;
  }

  /**
   * Search all the documents of the specified type in an instance with the specified foreignId.
   *
   * @param session the current JCR session.
   * @param instanceId the component id containing the documents.
   * @param foreignId the id of the container owning the documents.
   * @return an ordered list of the documents.
   * @throws RepositoryException
   */
  private NodeIterator selectDocumentsByForeignIdAndType(Session session, String instanceId,
      String foreignId, DocumentType type) throws RepositoryException {
    QueryManager manager = session.getWorkspace().getQueryManager();
    QueryObjectModelFactory factory = manager.getQOMFactory();
    Selector source = factory.selector(SLV_SIMPLE_DOCUMENT, SIMPLE_DOCUMENT_ALIAS);
    ChildNode childNodeConstraint = factory.childNode(SIMPLE_DOCUMENT_ALIAS, session.getRootNode().
        getPath() + instanceId + '/' + type.getFolderName());
    Comparison foreignIdComparison = factory.comparison(factory.propertyValue(SIMPLE_DOCUMENT_ALIAS,
        SLV_PROPERTY_FOREIGN_KEY), QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, factory.
        literal(session.getValueFactory().createValue(foreignId)));
    Ordering order = factory.ascending(factory.propertyValue(SIMPLE_DOCUMENT_ALIAS,
        SLV_PROPERTY_ORDER));
    QueryObjectModel query = factory.createQuery(source, factory.and(childNodeConstraint,
        foreignIdComparison), new Ordering[]{order}, null);
    QueryResult result = query.execute();
    return result.getNodes();
  }

  /**
   * Locks a document in order to perform some changes. This action check-out the document node for
   * a versioned document in order to keep track of changes, otherwise it does nothing.
   *
   * @param session the session in the JCR.
   * @param document the document to lock for changes.
   * @param owner the user locking the node.
   * @return true if document node has be checked-out, false otherwise.
   * @throws RepositoryException if an error occurs.
   */
  public boolean lock(Session session, SimpleDocument document, String owner) throws
      RepositoryException {
    if (document.isVersioned()) {
      Node documentNode = session.getNodeByIdentifier(document.getId());
      if (!documentNode.isCheckedOut()) {
        checkoutNode(documentNode, owner);
      }
      return true;
    }
    return false;
  }

  /**
   * Unlocks the specified document. It updates the document and check-in its corresponding node: if
   * the document is versioned, then a new version is created for the specified document in the JCR,
   * otherwise, it is updated. As the document node is checked-in, the change are saved
   * automatically.
   *
   * @param session the session in the JCR.
   * @param document the document to unlock.
   * @return the node corresponding either to the new version (for a versioned document) or the
   * updated document node itself.
   * @throws RepositoryException if an error occurs.
   */
  public Node unlock(Session session, SimpleDocument document) throws RepositoryException {
    Node documentNode = session.getNodeByIdentifier(document.getId());
    if (document.isVersioned() && documentNode.isCheckedOut()) {
      converter.fillNode(document, documentNode);
      VersionManager versionManager = documentNode.getSession().getWorkspace().getVersionManager();
      String versionLabel = converter.updateVersion(documentNode, document.getLanguage(), document.
          isPublic());
      session.save();
      javax.jcr.version.Version lastVersion = versionManager.checkin(documentNode.getPath());
      lastVersion.getContainingHistory().addVersionLabel(lastVersion.getName(), versionLabel, false);
      Node versionNode = converter.getCurrentNodeForVersion(lastVersion);
      // update the version of the document.
      document.setMajorVersion(converter.getIntProperty(documentNode, SLV_PROPERTY_MAJOR));
      document.setMinorVersion(converter.getIntProperty(documentNode, SLV_PROPERTY_MINOR));
      return versionNode;
    } else if (!document.isVersioned()) {
      converter.fillNode(document, documentNode);
      session.save();
      return documentNode;
    }
    return null;
  }

  /**
   * Unlock a document if it is versionned to create a new version.
   *
   * @param session
   * @param document
   * @param restore
   * @return
   * @throws RepositoryException
   */
  public SimpleDocument unlock(Session session, SimpleDocument document, boolean restore)
      throws RepositoryException {
    Node documentNode;
    try {
      documentNode = session.getNodeByIdentifier(document.getId());
    } catch (ItemNotFoundException ex) {
      // Node may have been deleted after removing all its content.
      return document;
    }
    if (document.isVersioned() && documentNode.isCheckedOut()) {
      if (restore) {
        VersionIterator iter = session.getWorkspace().getVersionManager().
            getVersionHistory(document.getFullJcrPath()).getAllVersions();
        Version lastVersion = null;
        while (iter.hasNext()) {
          lastVersion = iter.nextVersion();
        }
        if (null != lastVersion) {
          session.getWorkspace().getVersionManager().restore(lastVersion, true);
          return converter.convertNode(lastVersion.getFrozenNode(), document.getLanguage());
        }
      }
      converter.fillNode(document, documentNode);
      return checkinNode(documentNode, document.getLanguage(), document.isPublic());
    }
    if (!document.isVersioned()) {
      converter.fillNode(document, documentNode);
      converter.releaseDocumentNode(documentNode, document.getLanguage());
      return converter.convertNode(documentNode, document.getLanguage());
    }
    document.release();
    return document;
  }

  /**
   * Check the document out.
   *
   * @param node the node to checkout.
   * @param owner the user checkouting the node.
   * @throws RepositoryException
   */
  private void checkoutNode(Node node, String owner) throws RepositoryException {
    node.getSession().getWorkspace().getVersionManager().checkout(node.getPath());
    converter.addStringProperty(node, SLV_PROPERTY_OWNER, owner);
  }

  /**
   * Check the document in.
   *
   * @param documentNode the node to checkin.
   * @param isMajor true if the new version is a major one - false otherwise.
   * @return the document for this new version.
   * @throws RepositoryException
   */
  private SimpleDocument checkinNode(Node documentNode, String lang, boolean isMajor) throws
      RepositoryException {
    VersionManager versionManager = documentNode.getSession().getWorkspace().getVersionManager();
    String versionLabel = converter.updateVersion(documentNode, lang, isMajor);
    documentNode.getSession().save();
    Version lastVersion = versionManager.checkin(documentNode.getPath());
    lastVersion.getContainingHistory().addVersionLabel(lastVersion.getName(), versionLabel, false);
    SimpleDocument doc = converter.convertNode(documentNode, lang);
    return doc;
  }

  private void removeHistory(Node documentNode) throws RepositoryException {
    VersionHistory history = documentNode.getSession().getWorkspace().getVersionManager().
        getVersionHistory(documentNode.getPath());
    Version root = history.getRootVersion();
    VersionIterator versions = history.getAllVersions();
    while (versions.hasNext()) {
      Version version = versions.nextVersion();
      if (!version.isSame(root)) {
        history.removeVersion(version.getName());
      }
    }
  }

  public void fillNodeName(Session session, SimpleDocument document) throws RepositoryException {
    Node documentNode = session.getNodeByIdentifier(document.getId());
    if (!StringUtil.isDefined(document.getNodeName())) {
      document.setNodeName(documentNode.getName());
    }
  }

  public long storeContent(SimpleDocument document, InputStream in) throws
      RepositoryException, IOException {
    File file = new File(document.getAttachmentPath());
    console.printTrace("Storing file for document in " + document.getAttachmentPath());
    FileUtils.copyInputStreamToFile(in, file);
    return file.length();
  }

  public void duplicateContent(SimpleDocument origin, SimpleDocument document)
      throws IOException, RepositoryException {
    String originDir = origin.getDirectoryPath(null);
    String targetDir = document.getDirectoryPath(null);
    targetDir = targetDir.replace('/', File.separatorChar);
    File target = new File(targetDir).getParentFile();
    File source = new File(originDir).getParentFile();
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

  public void deleteContent(Node documentNode, String instanceId) throws RepositoryException {
    String directory = FileUtil.getAbsolutePath(instanceId) + documentNode.getName();
    directory = directory.replace('/', File.separatorChar);
    File documentDirectory = new File(directory);
    if (documentDirectory.exists() && documentDirectory.isDirectory()) {
      FileUtils.deleteQuietly(documentDirectory);
    }
  }

  public List<String> listComponentsWithWysiwyg(Session session) throws RepositoryException {
    List<String> componentIds = new ArrayList<String>(5000);
    NodeIterator iter = session.getRootNode().getNodes();
    while (iter.hasNext()) {
      Node childNode = iter.nextNode();
      if (COMPONENTNAME_PATTERN.matcher(childNode.getName()).matches() && childNode.hasNode(
          DocumentType.wysiwyg.getFolderName())) {
        componentIds.add(childNode.getName());
      }
    }
    return componentIds;
  }

  public Set<String> listWysiwygFileNames(Session session, String instanceId) throws
      RepositoryException {
    Set<String> baseFileNames = new HashSet<String>(5000);
    if (session.getRootNode().hasNode(instanceId) && session.getRootNode().getNode(instanceId)
        .hasNode(DocumentType.wysiwyg.getFolderName())) {
      NodeIterator iter = session.getRootNode().getNode(instanceId).getNode(
          DocumentType.wysiwyg.getFolderName()).getNodes();
      while (iter.hasNext()) {
        Node documentNode = iter.nextNode();
        NodeIterator attachmentsIter = documentNode.getNodes();
        while (attachmentsIter.hasNext()) {
          Node attachmentNode = attachmentsIter.nextNode();
          String basename = ConverterUtil.extractBaseName(attachmentNode.getProperty(
              SLV_PROPERTY_NAME).getString());
          if (StringUtil.isDefined(basename)) {
            baseFileNames.add(basename);
          }
        }
      }
    }
    return baseFileNames;
  }

  public List<SimpleDocument> listWysiwygAttachmentsByBasename(Session session, String instanceId,
      String baseName) throws RepositoryException {
    NodeIterator iter = selectWysiwygAttachmentsByBasename(session, instanceId, baseName);
    List<SimpleDocument> result = new ArrayList<SimpleDocument>((int) iter.getSize());
    while (iter.hasNext()) {
      Node attachmentNode = iter.nextNode();
      String language = attachmentNode.getProperty(JCR_LANGUAGE).getString();
      result.add(converter.convertNode(attachmentNode.getParent(), language));
    }
    return result;
  }

  protected NodeIterator selectWysiwygAttachmentsByBasename(Session session, String instanceId,
      String baseName) throws RepositoryException {
    QueryManager manager = session.getWorkspace().getQueryManager();
    QueryObjectModelFactory factory = manager.getQOMFactory();
    Selector source = factory.selector(SLV_SIMPLE_ATTACHMENT, SIMPLE_ATTACHMENT_ALIAS);
    DescendantNode descendantdNodeConstraint = factory.descendantNode(SIMPLE_ATTACHMENT_ALIAS,
        session.getRootNode().getPath() + instanceId);
    Comparison baseNameLike = factory.comparison(factory.propertyValue(
        SIMPLE_ATTACHMENT_ALIAS, SLV_PROPERTY_NAME), QueryObjectModelFactory.JCR_OPERATOR_LIKE,
        factory.literal(session.getValueFactory().createValue(baseName + "%.txt")));
    Ordering order = factory.ascending(factory.propertyValue(SIMPLE_ATTACHMENT_ALIAS,
        SLV_PROPERTY_NAME));
    QueryObjectModel query = factory.createQuery(source, factory.and(descendantdNodeConstraint,
        baseNameLike), new Ordering[]{order}, null);
    QueryResult result = query.execute();
    return result.getNodes();
  }
}
