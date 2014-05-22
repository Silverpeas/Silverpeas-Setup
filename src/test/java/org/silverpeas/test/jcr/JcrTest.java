/*
 * Copyright (C) 2000 - 2014 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception. You should have recieved a copy of the text describing
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
package org.silverpeas.test.jcr;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.tika.mime.MimeTypes;
import org.silverpeas.migration.jcr.service.AttachmentException;
import org.silverpeas.migration.jcr.service.RepositoryManager;
import org.silverpeas.migration.jcr.service.SimpleDocumentService;
import org.silverpeas.migration.jcr.service.model.SimpleAttachment;
import org.silverpeas.migration.jcr.service.model.SimpleAttachmentBuilder;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentBuilder;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.migration.jcr.service.repository.DocumentConverter;
import org.silverpeas.migration.jcr.service.repository.DocumentRepository;
import org.silverpeas.test.SystemInitializationForTests;
import org.silverpeas.util.file.FileUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Timestamp;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * This class handle a JCR test with using a String context.
 * @author: Yohann Chastagnier
 */
public abstract class JcrTest {
  private ClassPathXmlApplicationContext appContext;

  private SimpleDocumentService simpleDocumentService;
  private DocumentRepository documentRepository;


  private final DocumentConverter converter = new DocumentConverter();

  public ClassPathXmlApplicationContext getAppContext() {
    return appContext;
  }

  public void setAppContext(final ClassPathXmlApplicationContext appContext) {
    this.appContext = appContext;
  }

  public SimpleDocumentService getSimpleDocumentService() {
    return simpleDocumentService;
  }

  public DocumentRepository getDocumentRepository() {
    return documentRepository;
  }

  public Session openJCRSession() {
    try {
      return simpleDocumentService.getRepositoryManager().getSession();
    } catch (RepositoryException ex) {
      throw new AttachmentException(ex);
    }
  }

  public abstract void run() throws Exception;


  /**
   * Execute the test with its context.
   * @throws Exception
   */
  @SuppressWarnings("ConstantConditions")
  public void execute() throws Exception {
    SystemInitializationForTests.initialize();
    setAppContext(new ClassPathXmlApplicationContext("/spring-pure-memory-jcr.xml"));
    RepositoryManager.setIsJcrTestEnv();
    File root = new File(new File(FileUtil.getAbsolutePath("")), "root-folder-for-jcr-repo-tests");
    FileUtils.deleteQuietly(root.getParentFile());
    System.setProperty("rep.home", root.getPath());
    try {
      File jcrXmlConf =
          new File(JcrTest.class.getClassLoader().getResource("repository-in-memory.xml").toURI());
      File jcrTestRepoHome = new File(root, "repository/jackrabbit");
      FileUtils.copyFileToDirectory(jcrXmlConf, jcrTestRepoHome);
      simpleDocumentService =
          new SimpleDocumentService(jcrTestRepoHome.getPath(), jcrXmlConf.getPath());
      documentRepository = simpleDocumentService.getDocumentRepository();
      // Test
      run();
    } finally {
      FileUtils.deleteQuietly(root.getParentFile());
      shutdownJcrRepository();
      getAppContext().close();
    }
  }

  /**
   * Shutdown the JCR repository.
   */
  protected void shutdownJcrRepository() {
    if (simpleDocumentService != null) {
      try {
        simpleDocumentService.shutdown();
      } catch (Exception e) {
        // Jcr repository was already shutdown.
      }
    }
  }

  /**
   * Common method to assert a document existence.
   * @param uuId the uuId to retrieve the document.
   */
  public void assertDocumentExists(String uuId) throws Exception {
    SimpleDocument document = getDocumentById(uuId);
    assertThat(document, notNullValue());
  }

  /**
   * Common method to assert a document existence.
   * @param uuId the uuId to retrieve the document.
   */
  public void assertDocumentDoesNotExist(String uuId) throws Exception {
    SimpleDocument document = getDocumentById(uuId);
    assertThat(document, nullValue());
  }

  /**
   * Common method to assert the content for a language of a document.
   * @param uuId the uuId to retrieve the document.
   * @param language the language in which the content must be verified.
   * @param expectedContent if null, the content for the language does not exist.
   * @return the document content loaded for the assertions.
   */
  public SimpleDocument assertContent(String uuId, String language, String expectedContent)
      throws Exception {
    SimpleDocument document = getDocumentById(uuId, language);
    assertThat(document, notNullValue());
    final File physicalContent;
    if (document.getAttachment() != null) {
      physicalContent = new File(FileUtil.getAbsolutePath(document.getInstanceId()),
          document.getNodeName() + "/" +
              document.getMajorVersion() + "_" + document.getMinorVersion() + "/" + language + "/" +
              document.getFilename()
      );
    } else {
      physicalContent = new File(FileUtil.getAbsolutePath(document.getInstanceId()),
          document.getNodeName() + "/" +
              document.getMajorVersion() + "_" + document.getMinorVersion() + "/" + language
      );
    }
    if (expectedContent == null) {
      assertThat(document.getAttachment(), nullValue());
      assertThat(physicalContent.exists(), is(false));
    } else {
      assertThat(document.getAttachment(), notNullValue());
      assertThat(document.getLanguage(), is(language));
      assertThat(physicalContent.exists(), is(true));
      assertThat("It must exist one file only...", physicalContent.getParentFile().listFiles(),
          arrayWithSize(1));
      ByteArrayOutputStream content = new ByteArrayOutputStream();
      getSimpleDocumentService().getBinaryContent(content, document.getPk(), language);
      assertThat(content.toString(Charsets.UTF_8.name()), is(expectedContent));
    }
    return document;
  }

  protected SimpleDocumentBuilder defaultDocumentBuilder(String instanceId, String foreignId,
      SimpleAttachment file) {
    return new SimpleDocumentBuilder().setPk(new SimpleDocumentPK("-1", instanceId))
        .setForeignId(foreignId).setFile(file);
  }

  public SimpleDocumentBuilder defaultDocumentBuilder(String instanceId, String foreignId) {
    return defaultDocumentBuilder(instanceId, foreignId, null);
  }

  protected SimpleAttachmentBuilder defaultENContentBuilder() {
    return new SimpleAttachmentBuilder().setFilename("test.pdf").setLanguage("en")
        .setTitle("My test document").setDescription("This is a test document")
        .setSize("This is a test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.OCTET_STREAM).setCreatedBy("0").setCreated(randomDate())
        .setXmlFormId("18");

  }

  public SimpleAttachmentBuilder defaultFRContentBuilder() {
    return new SimpleAttachmentBuilder().setFilename("test.odp").setLanguage("fr")
        .setTitle("Mon document de test").setDescription("Ceci est un document de test")
        .setSize(28L).setContentType(MimeTypes.PLAIN_TEXT).setCreatedBy("10")
        .setCreated(randomDate()).setXmlFormId("5");
  }

  protected Date randomDate() {
    long offset = Timestamp.valueOf("2014-01-01 00:00:00").getTime();
    long end = Timestamp.valueOf("2014-04-30 00:00:00").getTime();
    long diff = end - offset + 1;
    return new Timestamp(offset + (long) (Math.random() * diff));
  }

  /**
   * Creates an Image Master Node into the JCR
   * @param sdBuilder
   * @param saBuilder
   * @param content
   * @return
   * @throws Exception
   */
  public SimpleDocument createAttachmentForTest(SimpleDocumentBuilder sdBuilder,
      SimpleAttachmentBuilder saBuilder, String content) throws Exception {
    SimpleDocument document = sdBuilder.setFile(saBuilder.build()).build();
    return createDocumentIntoJcr(document, content);
  }

  /**
   * Creates an Image Master Node into the JCR
   * @param document
   * @param language
   * @param content
   * @return
   * @throws Exception
   */
  public SimpleDocument updateAttachmentForTest(SimpleDocument document, String language,
      String content) throws Exception {
    SimpleDocument documentToUpdate = document.clone();
    documentToUpdate.setLanguage(language);
    return updateDocumentIntoJcr(documentToUpdate, content);
  }

  /**
   * Creates an Image Master Node into the JCR
   * @param document
   * @return
   * @throws Exception
   */
  protected SimpleDocument updateAttachmentForTest(SimpleDocument document) throws Exception {
    SimpleDocument documentToUpdate = document.clone();
    return updateDocumentIntoJcr(documentToUpdate, null);
  }

  public SimpleDocument getDocumentById(String uuId, String language) throws Exception {
    Session session = openJCRSession();
    try {
      return getDocumentRepository()
          .findDocumentById(session, new SimpleDocumentPK(uuId), language);
    } finally {
      session.logout();
    }
  }

  protected SimpleDocument getDocumentById(String uuId) throws Exception {
    return getDocumentById(uuId, null);
  }

  /**
   * Creates a master document NODE into the JCR.
   * @param document
   * @return
   * @throws Exception
   */
  private SimpleDocument createDocumentIntoJcr(SimpleDocument document, String content)
      throws Exception {
    Session session = openJCRSession();
    try {
      SimpleDocumentPK createdPk = getDocumentRepository().createDocument(session, document);
      session.save();
      long contentSizeWritten = getDocumentRepository()
          .storeContent(document, new ByteArrayInputStream(content.getBytes(Charsets.UTF_8)));
      assertThat(contentSizeWritten, is((long) content.length()));
      SimpleDocument createdDocument =
          getDocumentRepository().findDocumentById(session, createdPk, document.getLanguage());
      assertThat(createdDocument, notNullValue());
      assertThat(createdDocument.getOrder(), is(document.getOrder()));
      assertThat(createdDocument.getLanguage(), is(document.getLanguage()));
      return createdDocument;
    } finally {
      session.logout();
    }
  }

  /**
   * Creates a master document NODE into the JCR.
   * @param document
   * @return
   * @throws Exception
   */
  private SimpleDocument updateDocumentIntoJcr(SimpleDocument document, String content)
      throws Exception {
    assertThat(document.getPk(), notNullValue());
    assertThat(document.getId(), not(isEmptyString()));
    assertThat(document.getInstanceId(), not(isEmptyString()));
    assertThat(document.getOldSilverpeasId(), greaterThan(0L));
    Session session = openJCRSession();
    try {
      Node documentNode = session.getNodeByIdentifier(document.getPk().getId());
      converter.fillNode(document, documentNode);
      session.save();
      if (content != null) {
        long contentSizeWritten = getDocumentRepository()
            .storeContent(document, new ByteArrayInputStream(content.getBytes(Charsets.UTF_8)));
        assertThat(contentSizeWritten, is((long) content.length()));
      }
      SimpleDocument updatedDocument = getDocumentRepository()
          .findDocumentById(session, document.getPk(), document.getLanguage());
      assertThat(updatedDocument, notNullValue());
      assertThat(updatedDocument.getLanguage(), is(document.getLanguage()));
      return updatedDocument;
    } finally {
      session.logout();
    }
  }
}
