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
package org.silverpeas.migration.jcr.service.repository;

import org.apache.commons.io.Charsets;
import org.apache.tika.mime.MimeTypes;
import org.junit.Test;
import org.silverpeas.migration.jcr.service.model.DocumentType;
import org.silverpeas.migration.jcr.service.model.SimpleAttachment;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentBuilder;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.test.jcr.JcrTest;

import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.silverpeas.migration.jcr.service.model.DocumentType.attachment;
import static org.silverpeas.migration.jcr.service.model.DocumentType.wysiwyg;

public class DocumentRepositoryTest {

  private static final String instanceId = "kmelia26";


  /**
   * Test of createDocument method, of class DocumentRepository.
   */
  @Test
  public void testCreateDocument() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        String foreignId = "node18";
        InputStream content = new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
        SimpleAttachment attachment = defaultENContentBuilder().build();
        Date creationDate = attachment.getCreated();
        SimpleDocument document = defaultDocumentBuilder(foreignId, attachment).build();
        SimpleDocumentPK result = getDocumentRepository().createDocument(session, document);
        getDocumentRepository().storeContent(document, content);
        SimpleDocumentPK expResult = new SimpleDocumentPK(result.getId(), instanceId);
        assertThat(result, is(expResult));
        SimpleDocument doc = getDocumentRepository().findDocumentById(session, expResult, "en");
        assertThat(doc, is(notNullValue()));
        assertThat(doc.getOldSilverpeasId(), greaterThan(0L));
        assertThat(doc.getCreated(), is(creationDate));
        checkEnglishSimpleDocument(doc);
      }
    }.execute();
  }

  /**
   * Test of deleteDocument method, of class DocumentRepository.
   */
  @Test
  public void testDeleteDocument() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        String language = "en";
        ByteArrayInputStream content =
            new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
        SimpleAttachment attachment = defaultENContentBuilder().build();
        Date creationDate = attachment.getCreated();
        String foreignId = "node18";
        SimpleDocument document = defaultDocumentBuilder(foreignId, attachment).build();
        SimpleDocumentPK result = getDocumentRepository().createDocument(session, document);
        getDocumentRepository().storeContent(document, content);
        SimpleDocumentPK expResult = new SimpleDocumentPK(result.getId(), instanceId);
        assertThat(result, is(expResult));
        SimpleDocument doc = getDocumentRepository().findDocumentById(session, expResult, language);
        assertThat(doc, is(notNullValue()));
        checkEnglishSimpleDocument(doc);
        assertThat(doc.getOldSilverpeasId(), greaterThan(0L));
        assertThat(doc.getCreated(), is(creationDate));
        getDocumentRepository().deleteDocument(session, expResult);
        doc = getDocumentRepository().findDocumentById(session, expResult, language);
        assertThat(doc, is(nullValue()));
      }
    }.execute();
  }

  /**
   * Test of findDocumentById method, of class DocumentRepository.
   */
  @Test
  public void testFindDocumentById() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        ByteArrayInputStream content =
            new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
        SimpleAttachment attachment = defaultENContentBuilder().build();
        Date creationDate = attachment.getCreated();
        String foreignId = "node18";
        SimpleDocument document = defaultDocumentBuilder(foreignId, attachment).build();
        SimpleDocumentPK result = getDocumentRepository().createDocument(session, document);
        getDocumentRepository().storeContent(document, content);
        SimpleDocumentPK expResult = new SimpleDocumentPK(result.getId(), instanceId);
        assertThat(result, is(expResult));
        SimpleDocument doc = getDocumentRepository().findDocumentById(session, expResult, "en");
        assertThat(doc, is(notNullValue()));
        assertThat(doc.getOldSilverpeasId(), is(not(0L)));
        assertThat(doc.getAttachment(), notNullValue());
        assertThat(doc.getCreated(), is(creationDate));
        checkEnglishSimpleDocument(doc);
        doc = getDocumentRepository().findDocumentById(session, expResult, "fr");
        assertThat(doc, is(notNullValue()));
        assertThat(doc.getOldSilverpeasId(), is(not(0L)));
        assertThat(doc.getAttachment(), nullValue());
      }
    }.execute();
  }

  /**
   * Test of findLast method, of class DocumentRepository.
   */
  @Test
  public void testFindLast() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        String foreignId = "node18";
        ByteArrayInputStream content =
            new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
        SimpleAttachment attachment = defaultENContentBuilder().build();
        SimpleDocument document =
            defaultDocumentBuilder(foreignId, attachment).setOrder(10).build();
        SimpleDocumentPK result = getDocumentRepository().createDocument(session, document);
        getDocumentRepository().storeContent(document, content);
        SimpleDocumentPK expResult = new SimpleDocumentPK(result.getId(), instanceId);
        assertThat(result, is(expResult));
        long oldSilverpeasId = document.getOldSilverpeasId();
        content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
        attachment = defaultFRContentBuilder().build();
        foreignId = "node18";
        document = defaultDocumentBuilder(foreignId, attachment).setOrder(5).build();
        result = getDocumentRepository().createDocument(session, document);
        getDocumentRepository().storeContent(document, content);
        expResult = new SimpleDocumentPK(result.getId(), instanceId);
        assertThat(result, is(expResult));
        session.save();
        SimpleDocument doc = getDocumentRepository().findLast(session, instanceId, foreignId);
        assertThat(doc, is(notNullValue()));
        assertThat(doc.getOldSilverpeasId(), is(oldSilverpeasId));
      }
    }.execute();
  }

  /**
   * Test of updateDocument method, of class DocumentRepository.
   */
  @Test
  public void testUpdateDocument() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        ByteArrayInputStream content =
            new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
        SimpleAttachment attachment = defaultENContentBuilder().build();
        String foreignId = "node18";
        SimpleDocument document =
            defaultDocumentBuilder(foreignId, attachment).setOrder(10).build();
        SimpleDocumentPK result = getDocumentRepository().createDocument(session, document);
        getDocumentRepository().storeContent(document, content);
        SimpleDocumentPK expResult = new SimpleDocumentPK(result.getId(), instanceId);
        assertThat(result, is(expResult));
        attachment = defaultFRContentBuilder().build();
        document = defaultDocumentBuilder(foreignId, attachment).setPk(result).setOrder(15).build();
        getDocumentRepository().updateDocument(session, document);
        session.save();
        SimpleDocument doc = getDocumentRepository().findDocumentById(session, result, "fr");
        assertThat(doc, is(notNullValue()));
        assertThat(doc.getOrder(), is(15));
        assertThat(doc.getContentType(), is(MimeTypes.PLAIN_TEXT));
        assertThat(doc.getSize(), is(28L));
      }
    }.execute();
  }

  /**
   * Test of listComponentsWithWysiwyg method, of class DocumentRepository.
   */
  @Test
  public void listComponentsWithWysiwyg() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        Set<String> createdIds = new HashSet<String>();
        // No WYSIWYG content exists
        List<String> components = getDocumentRepository().listComponentsWithWysiwyg(session);
        assertThat(components, notNullValue());
        assertThat(components, hasSize(0));

        // Creating an FR "attachment" content.
        String createdUuid =
            createAttachmentForTest(defaultDocumentBuilder("fId_1").setDocumentType(attachment),
                defaultFRContentBuilder(), "fId_1_fr").getId();
        createdIds.add(createdUuid);
        SimpleDocument enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), nullValue());
        SimpleDocument frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));

        // Updating attachment with EN content.
        setEnData(frDocument);
        updateAttachmentForTest(frDocument, "en", "fId_1_en");
        createdIds.add(frDocument.getId());

        // Vérifying the attachment exists into both of tested languages.
        enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        assertThat(enDocument.getDocumentType(), is(attachment));
        checkEnglishSimpleDocument(enDocument);
        frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));
        checkFrenchSimpleDocument(frDocument);

        // No WYSIWYG : that is what it is expected
        components = getDocumentRepository().listComponentsWithWysiwyg(session);
        assertThat(components, hasSize(0));

        // Adding several documents, but no WYSIWYG
        Set<DocumentType> documentTypes = EnumSet.allOf(DocumentType.class);
        documentTypes.remove(DocumentType.wysiwyg);
        int id = 2;
        for (DocumentType documentType : documentTypes) {
          createdIds.add(createAttachmentForTest(
              defaultDocumentBuilder("fId_" + id).setDocumentType(documentType),
              defaultFRContentBuilder(), "fId_" + id + "_fr").getId());
          id++;
        }

        // No WYSIWYG : that is what it is expected
        components = getDocumentRepository().listComponentsWithWysiwyg(session);
        assertThat(components, hasSize(0));

        // Number of expected created documents
        int nbDocuments = 1 + (DocumentType.values().length - 1);
        assertThat(createdIds.size(), is(nbDocuments));

        // Adding the first WYSIWYG EN content
        createdUuid =
            createAttachmentForTest(defaultDocumentBuilder("fId_26").setDocumentType(wysiwyg),
                defaultENContentBuilder(), "fId_26_en").getId();
        createdIds.add(createdUuid);

        // One WYSIWYG
        components = getDocumentRepository().listComponentsWithWysiwyg(session);
        assertThat(components, hasSize(1));
        assertThat(components, contains(instanceId));

        // Adding the FR content to the first WYSIWYG document
        enDocument = getDocumentById(createdUuid, "en");
        setEnData(enDocument);
        updateAttachmentForTest(enDocument, "fr", "fId_26_fr");
        createdIds.add(enDocument.getId());

        // One WYSIWYG on one Component
        components = getDocumentRepository().listComponentsWithWysiwyg(session);
        assertThat(components, hasSize(1));
        assertThat(components, contains(instanceId));

        // Adding the second WYSIWYG document (on same component)
        createdUuid =
            createAttachmentForTest(defaultDocumentBuilder("fId_27").setDocumentType(wysiwyg),
                defaultFRContentBuilder(), "fId_27_fr").getId();
        createdIds.add(createdUuid);

        // Two WYSIWYG on one Component
        components = getDocumentRepository().listComponentsWithWysiwyg(session);
        assertThat(components, hasSize(1));
        assertThat(components, contains(instanceId));

        // Adding a third WYSIWYG document (on other component that does not respect component id
        // pattern)
        createdUuid = createAttachmentForTest(defaultDocumentBuilder("fId_28")
                .setPk(new SimpleDocumentPK("-1", "badInstanceIdPattern")).setDocumentType(wysiwyg),
            defaultENContentBuilder(), "fId_28_en"
        ).getId();
        createdIds.add(createdUuid);

        // Three WYSIWYG on two Components (one component id is not taken into account because
        // its instanceId)
        components = getDocumentRepository().listComponentsWithWysiwyg(session);
        assertThat(components, hasSize(1));
        assertThat(components, containsInAnyOrder(instanceId));

        // Adding a fourth WYSIWYG document (on other component that respects component id
        // pattern)
        createdUuid = createAttachmentForTest(
            defaultDocumentBuilder("fId_29").setPk(new SimpleDocumentPK("-1", "otherInstanceId38"))
                .setDocumentType(wysiwyg), defaultFRContentBuilder(), "fId_29_fr"
        ).getId();
        createdIds.add(createdUuid);

        // Number of expected created documents
        nbDocuments = nbDocuments + 4;
        assertThat(createdIds.size(), is(nbDocuments));

        // Four WYSIWYG on three Components (one component id is not taken into account because
        // its instanceId)
        components = getDocumentRepository().listComponentsWithWysiwyg(session);
        assertThat(components, hasSize(2));
        assertThat(components, containsInAnyOrder(instanceId, "otherInstanceId38"));
      }
    }.execute();
  }

  /**
   * Test of listWysiwygFileNames method, of class DocumentRepository.
   */
  @Test
  public void listWysiwygFileNames() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        Set<String> createdIds = new HashSet<String>();
        // No WYSIWYG content exists
        Set<String> wysiwygFilenames =
            getDocumentRepository().listWysiwygFileNames(session, instanceId);
        assertThat(wysiwygFilenames, notNullValue());
        assertThat(wysiwygFilenames, hasSize(0));

        // Creating an FR "attachment" content.
        String createdUuid =
            createAttachmentForTest(defaultDocumentBuilder("fId_1").setDocumentType(attachment),
                defaultFRContentBuilder(), "fId_1_fr").getId();
        createdIds.add(createdUuid);
        SimpleDocument enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), nullValue());
        SimpleDocument frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));

        // Updating attachment with EN content.
        setEnData(frDocument);
        updateAttachmentForTest(frDocument, "en", "fId_1_en");
        createdIds.add(frDocument.getId());

        // Vérifying the attachment exists into both of tested languages.
        enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        assertThat(enDocument.getDocumentType(), is(attachment));
        checkEnglishSimpleDocument(enDocument);
        frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));
        checkFrenchSimpleDocument(frDocument);

        // No WYSIWYG : that is what it is expected
        wysiwygFilenames = getDocumentRepository().listWysiwygFileNames(session, instanceId);
        assertThat(wysiwygFilenames, hasSize(0));

        // Adding several documents, but no WYSIWYG
        Set<DocumentType> documentTypes = EnumSet.allOf(DocumentType.class);
        documentTypes.remove(DocumentType.wysiwyg);
        int id = 2;
        for (DocumentType documentType : documentTypes) {
          createdIds.add(createAttachmentForTest(
              defaultDocumentBuilder("fId_" + id).setDocumentType(documentType),
              defaultFRContentBuilder().setFilename("fId_" + id + "_wysiwyg_en.txt"),
              "fId_" + id + "_fr").getId());
          id++;
        }

        // No WYSIWYG : that is what it is expected
        wysiwygFilenames = getDocumentRepository().listWysiwygFileNames(session, instanceId);
        assertThat(wysiwygFilenames, hasSize(0));

        // Number of expected created documents
        int nbDocuments = 1 + (DocumentType.values().length - 1);
        assertThat(createdIds.size(), is(nbDocuments));

        // Adding the first WYSIWYG EN content
        SimpleDocument createdDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_26").setDocumentType(wysiwyg),
                defaultENContentBuilder(), "fId_26_en");
        createdIds.add(createdDocument.getId());

        // One wrong WYSIWYG base name
        wysiwygFilenames = getDocumentRepository().listWysiwygFileNames(session, instanceId);
        assertThat(wysiwygFilenames, hasSize(0));

        // Updating wysiwyg file name
        createdDocument.setFilename("fId_26_wysiwyg_en.txt");
        updateAttachmentForTest(createdDocument);

        // One WYSIWYG base name
        wysiwygFilenames = getDocumentRepository().listWysiwygFileNames(session, instanceId);
        assertThat(wysiwygFilenames, hasSize(1));
        assertThat(wysiwygFilenames, contains("fId_26_wysiwyg"));

        // Adding the FR content to the first WYSIWYG document
        enDocument = getDocumentById(createdDocument.getId(), "en");
        setFrData(enDocument);
        enDocument.setFilename("fId_26_wysiwyg_fr.txt");
        updateAttachmentForTest(enDocument, "fr", "fId_26_fr");
        createdIds.add(enDocument.getId());

        // One WYSIWYG on one Component
        wysiwygFilenames = getDocumentRepository().listWysiwygFileNames(session, instanceId);
        assertThat(wysiwygFilenames, hasSize(1));
        assertThat(wysiwygFilenames, contains("fId_26_wysiwyg"));

        // Adding the second WYSIWYG document (on same component)
        createdUuid =
            createAttachmentForTest(defaultDocumentBuilder("fId_27").setDocumentType(wysiwyg),
                defaultFRContentBuilder().setFilename("fId_27_wysiwyg_fr.txt"), "fId_27_fr")
                .getId();
        createdIds.add(createdUuid);

        // Two WYSIWYG on one Component
        wysiwygFilenames = getDocumentRepository().listWysiwygFileNames(session, instanceId);
        assertThat(wysiwygFilenames, hasSize(2));
        assertThat(wysiwygFilenames, containsInAnyOrder("fId_26_wysiwyg", "fId_27_wysiwyg"));

        assertThat(createdIds, hasSize(nbDocuments + 2));
      }
    }.execute();
  }

  /**
   * Test of listWysiwygAttachmentsByBasename method, of class DocumentRepository.
   */
  @Test
  public void listWysiwygAttachmentsByBasename() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        Set<String> createdIds = new HashSet<String>();
        // No WYSIWYG content exists
        List<String> wysiwygLangFilenames = extractForeignIdLanguageFilenames(
            getDocumentRepository().listWysiwygAttachmentsByBasename(session, instanceId, ""));
        assertThat(wysiwygLangFilenames, notNullValue());
        assertThat(wysiwygLangFilenames, hasSize(0));

        // Creating an FR "attachment" content.
        String createdUuid =
            createAttachmentForTest(defaultDocumentBuilder("fId_1").setDocumentType(attachment),
                defaultFRContentBuilder(), "fId_1_fr").getId();
        createdIds.add(createdUuid);
        SimpleDocument enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), nullValue());
        SimpleDocument frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));

        // Updating attachment with EN content.
        setEnData(frDocument);
        updateAttachmentForTest(frDocument, "en", "fId_1_en");
        createdIds.add(frDocument.getId());

        // Vérifying the attachment exists into both of tested languages.
        enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        assertThat(enDocument.getDocumentType(), is(attachment));
        checkEnglishSimpleDocument(enDocument);
        frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));
        checkFrenchSimpleDocument(frDocument);

        // No WYSIWYG : that is what it is expected
        wysiwygLangFilenames = extractForeignIdLanguageFilenames(
            getDocumentRepository().listWysiwygAttachmentsByBasename(session, instanceId, ""));
        assertThat(wysiwygLangFilenames, hasSize(0));

        // Adding several documents, but no WYSIWYG
        Set<DocumentType> documentTypes = EnumSet.allOf(DocumentType.class);
        documentTypes.remove(DocumentType.wysiwyg);
        int id = 2;
        for (DocumentType documentType : documentTypes) {
          createdIds.add(createAttachmentForTest(
              defaultDocumentBuilder("fId_" + id).setDocumentType(documentType),
              defaultFRContentBuilder().setFilename("fId_" + id + "_wysiwyg_en.txt"),
              "fId_" + id + "_fr").getId());
          id++;
        }

        // No WYSIWYG : that is what it is expected
        wysiwygLangFilenames = extractForeignIdLanguageFilenames(
            getDocumentRepository().listWysiwygAttachmentsByBasename(session, instanceId, ""));
        assertThat(wysiwygLangFilenames, hasSize(0));

        // Number of expected created documents
        int nbDocuments = 1 + (DocumentType.values().length - 1);
        assertThat(createdIds.size(), is(nbDocuments));

        // Adding the first WYSIWYG EN content
        SimpleDocument createdDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_26").setDocumentType(wysiwyg),
                defaultENContentBuilder(), "fId_26_en");
        createdIds.add(createdDocument.getId());

        // One wrong WYSIWYG base name
        wysiwygLangFilenames = extractForeignIdLanguageFilenames(
            getDocumentRepository().listWysiwygAttachmentsByBasename(session, instanceId, ""));
        assertThat(wysiwygLangFilenames, hasSize(0));

        // Updating wysiwyg file name
        createdDocument.setFilename("fId_26_wysiwyg_en.txt");
        updateAttachmentForTest(createdDocument);

        // One WYSIWYG base name
        wysiwygLangFilenames = extractForeignIdLanguageFilenames(
            getDocumentRepository().listWysiwygAttachmentsByBasename(session, instanceId, ""));
        assertThat(wysiwygLangFilenames, hasSize(1));
        assertThat(wysiwygLangFilenames, contains("fId_26|en|fId_26_wysiwyg_en.txt"));
        wysiwygLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listWysiwygAttachmentsByBasename(session, instanceId, "fId_26_wysiwyg"));
        assertThat(wysiwygLangFilenames, hasSize(1));
        assertThat(wysiwygLangFilenames, contains("fId_26|en|fId_26_wysiwyg_en.txt"));
        wysiwygLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listWysiwygAttachmentsByBasename(session, instanceId, "wrong_fId_26_wysiwyg"));
        assertThat(wysiwygLangFilenames, hasSize(0));

        // Adding the FR content to the first WYSIWYG document
        enDocument = getDocumentById(createdDocument.getId(), "en");
        setFrData(enDocument);
        enDocument.setFilename("fId_26_wysiwyg_fr.txt");
        updateAttachmentForTest(enDocument, "fr", "fId_26_fr");
        createdIds.add(enDocument.getId());

        // One WYSIWYG on one Component
        wysiwygLangFilenames = extractForeignIdLanguageFilenames(
            getDocumentRepository().listWysiwygAttachmentsByBasename(session, instanceId, ""));
        assertThat(wysiwygLangFilenames, hasSize(2));
        assertThat(wysiwygLangFilenames, containsInAnyOrder("fId_26|en|fId_26_wysiwyg_en.txt",
            "fId_26|fr|fId_26_wysiwyg_fr.txt"));

        // Adding the second WYSIWYG document (on same component)
        SimpleDocument secondCreatedDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_27").setDocumentType(wysiwyg),
                defaultFRContentBuilder().setFilename("fId_27_wysiwyg_fr.txt"), "fId_27_fr");
        createdIds.add(secondCreatedDocument.getId());

        // Two WYSIWYG on one Component
        wysiwygLangFilenames = extractForeignIdLanguageFilenames(
            getDocumentRepository().listWysiwygAttachmentsByBasename(session, instanceId, ""));
        assertThat(wysiwygLangFilenames, hasSize(3));
        assertThat(wysiwygLangFilenames,
            containsInAnyOrder("fId_26|en|fId_26_wysiwyg_en.txt", "fId_26|fr|fId_26_wysiwyg_fr.txt",
                "fId_27|fr|fId_27_wysiwyg_fr.txt")
        );

        // Updating wysiwyg file name
        setEnData(secondCreatedDocument);
        secondCreatedDocument.setFilename(secondCreatedDocument.getFilename());
        updateAttachmentForTest(secondCreatedDocument, "en", "fId_27_en");

        // Two WYSIWYG (each one in two languages) on one Component
        wysiwygLangFilenames = extractForeignIdLanguageFilenames(
            getDocumentRepository().listWysiwygAttachmentsByBasename(session, instanceId, ""));
        assertThat(wysiwygLangFilenames, hasSize(4));
        assertThat(wysiwygLangFilenames,
            containsInAnyOrder("fId_26|en|fId_26_wysiwyg_en.txt", "fId_26|fr|fId_26_wysiwyg_fr.txt",
                "fId_27|fr|fId_27_wysiwyg_fr.txt", "fId_27|en|fId_27_wysiwyg_fr.txt")
        );

        assertThat(createdIds, hasSize(nbDocuments + 2));
      }
    }.execute();
  }

  /**
   * Test of listForeignIdsByType method, of class DocumentRepository.
   */
  @Test
  public void listForeignIdsByType() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        Set<String> createdIds = new HashSet<String>();
        // No WYSIWYG content exists
        Set<String> foreignIds =
            getDocumentRepository().listForeignIdsByType(session, instanceId, wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(0));
        foreignIds = getDocumentRepository().listForeignIdsByType(session, instanceId, attachment);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(0));

        // Creating an FR "attachment" content.
        String createdUuid =
            createAttachmentForTest(defaultDocumentBuilder("fId_1").setDocumentType(attachment),
                defaultFRContentBuilder(), "fId_1_fr").getId();
        createdIds.add(createdUuid);
        SimpleDocument enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), nullValue());
        SimpleDocument frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));

        // Updating attachment with EN content.
        setEnData(frDocument);
        updateAttachmentForTest(frDocument, "en", "fId_1_en");
        createdIds.add(frDocument.getId());

        // Vérifying the attachment exists into both of tested languages.
        enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        assertThat(enDocument.getDocumentType(), is(attachment));
        checkEnglishSimpleDocument(enDocument);
        frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));
        checkFrenchSimpleDocument(frDocument);

        // No WYSIWYG : that is what it is expected
        foreignIds = getDocumentRepository().listForeignIdsByType(session, instanceId, wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(0));
        foreignIds = getDocumentRepository().listForeignIdsByType(session, instanceId, attachment);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_1"));

        // Adding several documents, but no WYSIWYG
        Set<DocumentType> documentTypes = EnumSet.allOf(DocumentType.class);
        documentTypes.remove(DocumentType.wysiwyg);
        int id = 2;
        for (DocumentType documentType : documentTypes) {
          createdIds.add(createAttachmentForTest(
              defaultDocumentBuilder("fId_" + id).setDocumentType(documentType),
              defaultFRContentBuilder().setFilename("fId_" + id + "_wysiwyg_en.txt"),
              "fId_" + id + "_fr").getId());
          id++;
        }

        // No WYSIWYG : that is what it is expected
        foreignIds = getDocumentRepository().listForeignIdsByType(session, instanceId, wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(0));
        foreignIds = getDocumentRepository().listForeignIdsByType(session, instanceId, attachment);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(2));

        // Number of expected created documents
        int nbDocuments = 1 + (DocumentType.values().length - 1);
        assertThat(createdIds.size(), is(nbDocuments));

        // Adding the first WYSIWYG EN content
        SimpleDocument createdDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_26").setDocumentType(wysiwyg),
                defaultENContentBuilder(), "fId_26_en");
        createdIds.add(createdDocument.getId());
        createAttachmentForTest(
            defaultDocumentBuilder("otherKmelia38", "fId_26").setDocumentType(wysiwyg),
            defaultENContentBuilder(), "fId_26_en");

        // One wrong WYSIWYG base name
        foreignIds = getDocumentRepository().listForeignIdsByType(session, instanceId, wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));
        foreignIds =
            getDocumentRepository().listForeignIdsByType(session, "otherKmelia38", wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));

        // Updating wysiwyg file name
        createdDocument.setFilename("fId_26_wysiwyg_en.txt");
        updateAttachmentForTest(createdDocument);

        // One WYSIWYG base name
        foreignIds = getDocumentRepository().listForeignIdsByType(session, instanceId, wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));
        foreignIds =
            getDocumentRepository().listForeignIdsByType(session, "otherKmelia38", wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));

        // Adding the FR content to the first WYSIWYG document
        enDocument = getDocumentById(createdDocument.getId(), "en");
        setFrData(enDocument);
        enDocument.setFilename("fId_26_wysiwyg_fr.txt");
        updateAttachmentForTest(enDocument, "fr", "fId_26_fr");
        createdIds.add(enDocument.getId());

        // One WYSIWYG on one Component
        foreignIds = getDocumentRepository().listForeignIdsByType(session, instanceId, wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));
        foreignIds =
            getDocumentRepository().listForeignIdsByType(session, "otherKmelia38", wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));

        // Adding the second WYSIWYG document (on same component)
        SimpleDocument secondCreatedDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_27").setDocumentType(wysiwyg),
                defaultFRContentBuilder().setFilename("fId_27_wysiwyg_fr.txt"), "fId_27_fr");
        createdIds.add(secondCreatedDocument.getId());

        // Two WYSIWYG on one Component
        foreignIds = getDocumentRepository().listForeignIdsByType(session, instanceId, wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(2));
        assertThat(foreignIds, containsInAnyOrder("fId_26", "fId_27"));
        foreignIds =
            getDocumentRepository().listForeignIdsByType(session, "otherKmelia38", wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));

        // Updating wysiwyg file name
        setEnData(secondCreatedDocument);
        secondCreatedDocument.setFilename(secondCreatedDocument.getFilename());
        updateAttachmentForTest(secondCreatedDocument, "en", "fId_27_en");

        // Two WYSIWYG (each one in two languages) on one Component
        foreignIds = getDocumentRepository().listForeignIdsByType(session, instanceId, wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(2));
        assertThat(foreignIds, containsInAnyOrder("fId_26", "fId_27"));
        foreignIds =
            getDocumentRepository().listForeignIdsByType(session, "otherKmelia38", wysiwyg);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));

        assertThat(createdIds, hasSize(nbDocuments + 2));
      }
    }.execute();
  }

  /**
   * Test of listAttachmentsByForeignIdAndDocumentType method, of class DocumentRepository.
   */
  @Test
  public void listAttachmentsByForeignIdAndDocumentType() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        Set<String> createdIds = new HashSet<String>();
        // No WYSIWYG content exists
        List<String> wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getDocumentRepository()
                .listAttachmentsByForeignIdAndDocumentType(session, instanceId, "", wysiwyg)
        );
        assertThat(wysiwygFIdLangFilenames, notNullValue());
        assertThat(wysiwygFIdLangFilenames, hasSize(0));

        // Creating an FR "attachment" content.
        String createdUuid =
            createAttachmentForTest(defaultDocumentBuilder("fId_1").setDocumentType(attachment),
                defaultFRContentBuilder(), "fId_1_fr").getId();
        createdIds.add(createdUuid);
        SimpleDocument enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), nullValue());
        SimpleDocument frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByForeignIdAndDocumentType(session, instanceId, "fId_1", attachment));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_1|fr|test.odp"));

        // Updating attachment with EN content.
        setEnData(frDocument);
        updateAttachmentForTest(frDocument, "en", "fId_1_en");
        createdIds.add(frDocument.getId());

        // Vérifying the attachment exists into both of tested languages.
        enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        assertThat(enDocument.getDocumentType(), is(attachment));
        checkEnglishSimpleDocument(enDocument);
        frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));
        checkFrenchSimpleDocument(frDocument);

        // No WYSIWYG : that is what it is expected
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByForeignIdAndDocumentType(session, instanceId, "fId_1", wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(0));
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByForeignIdAndDocumentType(session, instanceId, "fId_1", attachment));
        assertThat(wysiwygFIdLangFilenames, hasSize(2));
        assertThat(wysiwygFIdLangFilenames,
            containsInAnyOrder("fId_1|fr|test.odp", "fId_1|en|test.odp"));

        // Adding several documents, but no WYSIWYG
        Set<DocumentType> documentTypes = EnumSet.allOf(DocumentType.class);
        documentTypes.remove(DocumentType.wysiwyg);
        int id = 2;
        for (DocumentType documentType : documentTypes) {
          createdIds.add(createAttachmentForTest(
              defaultDocumentBuilder("fId_" + id).setDocumentType(documentType),
              defaultFRContentBuilder().setFilename("fId_" + id + "_wysiwyg_en.txt"),
              "fId_" + id + "_fr").getId());
          id++;
        }

        // No WYSIWYG : that is what it is expected
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByForeignIdAndDocumentType(session, instanceId, "fId_1", wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(0));

        // Number of expected created documents
        int nbDocuments = 1 + (DocumentType.values().length - 1);
        assertThat(createdIds.size(), is(nbDocuments));

        // Adding the first WYSIWYG EN content
        SimpleDocument createdDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_26").setDocumentType(wysiwyg),
                defaultENContentBuilder(), "fId_26_en");
        createdIds.add(createdDocument.getId());

        // One wrong WYSIWYG base name
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByForeignIdAndDocumentType(session, instanceId, "fId_26", wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_26|en|test.pdf"));

        // Updating wysiwyg file name
        createdDocument.setFilename("fId_26_wysiwyg_en.txt");
        updateAttachmentForTest(createdDocument);

        // One WYSIWYG base name
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByForeignIdAndDocumentType(session, instanceId, "fId_26", wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_26|en|fId_26_wysiwyg_en.txt"));

        // Adding the FR content to the first WYSIWYG document
        enDocument = getDocumentById(createdDocument.getId(), "en");
        setFrData(enDocument);
        enDocument.setFilename("fId_26_wysiwyg_fr.txt");
        updateAttachmentForTest(enDocument, "fr", "fId_26_fr");
        createdIds.add(enDocument.getId());

        // One WYSIWYG on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByForeignIdAndDocumentType(session, instanceId, "fId_26", wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(2));
        assertThat(wysiwygFIdLangFilenames, containsInAnyOrder("fId_26|fr|fId_26_wysiwyg_fr.txt",
            "fId_26|en|fId_26_wysiwyg_en.txt"));

        // Adding the second WYSIWYG document (on same component)
        SimpleDocument secondCreatedDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_27").setDocumentType(wysiwyg),
                defaultFRContentBuilder().setFilename("fId_27_wysiwyg_fr.txt"), "fId_27_fr");
        createdIds.add(secondCreatedDocument.getId());

        // Two WYSIWYG on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByForeignIdAndDocumentType(session, instanceId, "fId_27", wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_27|fr|fId_27_wysiwyg_fr.txt"));

        // Updating wysiwyg file name
        setEnData(secondCreatedDocument);
        secondCreatedDocument.setFilename(secondCreatedDocument.getFilename());
        updateAttachmentForTest(secondCreatedDocument, "en", "fId_27_en");

        // Two WYSIWYG (each one in two languages) on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByForeignIdAndDocumentType(session, instanceId, "fId_27", wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(2));
        assertThat(wysiwygFIdLangFilenames,
            contains("fId_27|fr|fId_27_wysiwyg_fr.txt", "fId_27|en|fId_27_wysiwyg_fr.txt"));

        assertThat(createdIds, hasSize(nbDocuments + 2));
      }
    }.execute();
  }

  /**
   * Test of listAttachmentsByInstanceIdAndDocumentType method, of class DocumentRepository.
   */
  @Test
  public void listAttachmentsByInstanceIdAndDocumentType() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        Set<String> createdIds = new HashSet<String>();
        // No WYSIWYG content exists
        List<String> wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getDocumentRepository()
                .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, wysiwyg));
        assertThat(wysiwygFIdLangFilenames, notNullValue());
        assertThat(wysiwygFIdLangFilenames, hasSize(0));

        // Creating an FR "attachment" content.
        String createdUuid =
            createAttachmentForTest(defaultDocumentBuilder("fId_1").setDocumentType(attachment),
                defaultFRContentBuilder(), "fId_1_fr").getId();
        createdIds.add(createdUuid);
        SimpleDocument enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), nullValue());
        SimpleDocument frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, attachment));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_1|fr|test.odp"));

        // Updating attachment with EN content.
        setEnData(frDocument);
        updateAttachmentForTest(frDocument, "en", "fId_1_en");
        createdIds.add(frDocument.getId());

        // Vérifying the attachment exists into both of tested languages.
        enDocument = getDocumentById(createdUuid, "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        assertThat(enDocument.getDocumentType(), is(attachment));
        checkEnglishSimpleDocument(enDocument);
        frDocument = getDocumentById(createdUuid, "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        assertThat(frDocument.getDocumentType(), is(attachment));
        checkFrenchSimpleDocument(frDocument);

        // No WYSIWYG : that is what it is expected
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(0));
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, attachment));
        assertThat(wysiwygFIdLangFilenames, hasSize(2));
        assertThat(wysiwygFIdLangFilenames,
            containsInAnyOrder("fId_1|fr|test.odp", "fId_1|en|test.odp"));

        // Adding several documents, but no WYSIWYG
        Set<DocumentType> documentTypes = EnumSet.allOf(DocumentType.class);
        documentTypes.remove(DocumentType.wysiwyg);
        int id = 2;
        for (DocumentType documentType : documentTypes) {
          createdIds.add(createAttachmentForTest(
              defaultDocumentBuilder("fId_" + id).setDocumentType(documentType),
              defaultFRContentBuilder().setFilename("fId_" + id + "_wysiwyg_en.txt"),
              "fId_" + id + "_fr").getId());
          id++;
        }

        // No WYSIWYG : that is what it is expected
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(0));

        // Number of expected created documents
        int nbDocuments = 1 + (DocumentType.values().length - 1);
        assertThat(createdIds.size(), is(nbDocuments));

        // Adding the first WYSIWYG EN content
        SimpleDocument createdDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_26").setDocumentType(wysiwyg),
                defaultENContentBuilder(), "fId_26_en");
        createdIds.add(createdDocument.getId());

        // One wrong WYSIWYG base name
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_26|en|test.pdf"));

        // Updating wysiwyg file name
        createdDocument.setFilename("fId_26_wysiwyg_en.txt");
        updateAttachmentForTest(createdDocument);

        // One WYSIWYG base name
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_26|en|fId_26_wysiwyg_en.txt"));

        // Adding the FR content to the first WYSIWYG document
        enDocument = getDocumentById(createdDocument.getId(), "en");
        setFrData(enDocument);
        enDocument.setFilename("fId_26_wysiwyg_fr.txt");
        updateAttachmentForTest(enDocument, "fr", "fId_26_fr");
        createdIds.add(enDocument.getId());

        // One WYSIWYG on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(2));
        assertThat(wysiwygFIdLangFilenames, containsInAnyOrder("fId_26|fr|fId_26_wysiwyg_fr.txt",
            "fId_26|en|fId_26_wysiwyg_en.txt"));

        // Adding the second WYSIWYG document (on same component)
        SimpleDocument secondCreatedDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_27").setDocumentType(wysiwyg),
                defaultFRContentBuilder().setFilename("fId_27_wysiwyg_fr.txt"), "fId_27_fr");
        createdIds.add(secondCreatedDocument.getId());

        // Two WYSIWYG on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(3));
        assertThat(wysiwygFIdLangFilenames,
            containsInAnyOrder("fId_26|fr|fId_26_wysiwyg_fr.txt", "fId_26|en|fId_26_wysiwyg_en.txt",
                "fId_27|fr|fId_27_wysiwyg_fr.txt"));

        // Updating wysiwyg file name
        setEnData(secondCreatedDocument);
        secondCreatedDocument.setFilename(secondCreatedDocument.getFilename());
        updateAttachmentForTest(secondCreatedDocument, "en", "fId_27_en");

        // Two WYSIWYG (each one in two languages) on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(getDocumentRepository()
            .listAttachmentsByInstanceIdAndDocumentType(session, instanceId, wysiwyg));
        assertThat(wysiwygFIdLangFilenames, hasSize(4));
        assertThat(wysiwygFIdLangFilenames,
            containsInAnyOrder("fId_26|fr|fId_26_wysiwyg_fr.txt", "fId_26|en|fId_26_wysiwyg_en.txt",
                "fId_27|fr|fId_27_wysiwyg_fr.txt", "fId_27|en|fId_27_wysiwyg_fr.txt"));

        assertThat(createdIds, hasSize(nbDocuments + 2));
      }
    }.execute();
  }

  private List<String> extractForeignIdLanguageFilenames(List<SimpleDocument> documents) {
    List<String> languageFilenames = new ArrayList<String>(documents.size());
    for (SimpleDocument document : documents) {
      languageFilenames.add(
          document.getForeignId() + "|" + document.getLanguage() + "|" + document.getFilename());
    }
    return languageFilenames;
  }

  /**
   * Test of addContent method, of class DocumentRepository.
   */
  @Test
  public void testAddContent() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        String foreignId = "node18";
        SimpleDocument document =
            createAttachmentForTest(defaultDocumentBuilder(foreignId).setOrder(10),
                defaultENContentBuilder(), "This is a test");
        SimpleDocumentPK expResult = new SimpleDocumentPK(document.getId(), instanceId);
        assertThat(document.getPk(), is(expResult));

        // Vérifying the attachments
        SimpleDocument enDocument = getDocumentById(document.getId(), "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        checkEnglishSimpleDocument(enDocument);
        assertThat(new File(enDocument.getAttachmentPath()).exists(), is(true));
        SimpleDocument frDocument = getDocumentById(document.getId(), "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), nullValue());

        ByteArrayInputStream frContent =
            new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
        SimpleAttachment attachment = defaultFRContentBuilder().build();
        getDocumentRepository().addContent(session, document.getPk(), attachment);
        document.setLanguage(attachment.getLanguage());
        getDocumentRepository().storeContent(document, frContent);
        session.save();

        // Vérifying the attachments
        enDocument = getDocumentById(document.getId(), "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        checkEnglishSimpleDocument(enDocument);
        assertThat(new File(enDocument.getAttachmentPath()).exists(), is(true));
        frDocument = getDocumentById(document.getId(), "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        checkFrenchSimpleDocument(frDocument);
        // This method does not add the content physically (just into JCR)
        assertThat(new File(frDocument.getAttachmentPath()).exists(), is(false));
      }
    }.execute();
  }

  /**
   * Test of removeContent method, of class DocumentRepository.
   */
  @Test
  public void testRemoveContent() throws Exception {
    new JcrDocumentRepositoryTest() {
      @Override
      public void run(final Session session) throws Exception {
        String foreignId = "node18";
        SimpleDocument document =
            createAttachmentForTest(defaultDocumentBuilder(foreignId).setOrder(10),
                defaultENContentBuilder(), "This is a test");
        SimpleDocumentPK expResult = new SimpleDocumentPK(document.getId(), instanceId);
        assertThat(document.getPk(), is(expResult));
        setFrData(document);
        updateAttachmentForTest(document, "fr", "Ceci est un test");

        // Vérifying the attachment exists into both of tested languages.
        SimpleDocument enDocument = getDocumentById(document.getId(), "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        checkEnglishSimpleDocument(enDocument);
        SimpleDocument frDocument = getDocumentById(document.getId(), "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), notNullValue());
        checkFrenchSimpleDocument(frDocument);
        File enPhysicalFile = new File(enDocument.getAttachmentPath());
        File frPhysicalFile = new File(frDocument.getAttachmentPath());
        assertThat(enPhysicalFile.getPath(), not(is(frPhysicalFile.getPath())));
        assertThat(enPhysicalFile.exists(), is(true));
        assertThat(frPhysicalFile.exists(), is(true));

        // Removing the FR content
        getDocumentRepository().removeContent(session, document.getPk(), "fr");
        session.save();

        // Vérifying the attachments
        enDocument = getDocumentById(document.getId(), "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        checkEnglishSimpleDocument(enDocument);
        frDocument = getDocumentById(document.getId(), "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), nullValue());
        assertThat(enPhysicalFile.exists(), is(true));
        // The content is just removed from JCR, not removed from the filesystem
        assertThat(frPhysicalFile.exists(), is(true));

        // Removing the EN content
        getDocumentRepository().removeContent(session, document.getPk(), "en");
        session.save();

        // Vérifying the attachments
        enDocument = getDocumentById(document.getId(), "en");
        frDocument = getDocumentById(document.getId(), "fr");
        assertThat(enDocument, nullValue());
        assertThat(frDocument, nullValue());
        // The content is just removed from JCR, not removed from the filesystem
        assertThat(enPhysicalFile.exists(), is(true));
        assertThat(frPhysicalFile.exists(), is(true));
      }
    }.execute();
  }

  /**
   * Executing an adapted JcrTest
   */
  private abstract class JcrDocumentRepositoryTest extends JcrTest {

    protected SimpleDocumentBuilder defaultDocumentBuilder(final String foreignId,
        final SimpleAttachment file) {
      return super.defaultDocumentBuilder(instanceId, foreignId, file);
    }

    protected SimpleDocumentBuilder defaultDocumentBuilder(final String foreignId) {
      return super.defaultDocumentBuilder(instanceId, foreignId);
    }

    protected void setEnData(SimpleDocument document) {
      document.setLanguage("en");
      document.setContentType(MimeTypes.OCTET_STREAM);
      document.setSize(14L);
      document.setDescription("This is a test document");
      document.getAttachment().setCreatedBy("0");
    }

    protected void setFrData(SimpleDocument document) {
      document.setLanguage("fr");
      document.setContentType(MimeTypes.PLAIN_TEXT);
      document.setSize(28L);
      document.setDescription("Ceci est un document de test");
      document.getAttachment().setCreatedBy("10");
    }

    protected void checkEnglishSimpleDocument(SimpleDocument doc) {
      assertThat(doc.getLanguage(), is("en"));
      assertThat(doc, is(notNullValue()));
      assertThat(doc.getContentType(), is(MimeTypes.OCTET_STREAM));
      assertThat(doc.getSize(), is(14L));
      assertThat(doc.getDescription(), is("This is a test document"));
      assertThat(doc.getCreatedBy(), is("0"));
    }

    protected void checkFrenchSimpleDocument(SimpleDocument doc) {
      assertThat(doc.getLanguage(), is("fr"));
      assertThat(doc, is(notNullValue()));
      assertThat(doc.getContentType(), is(MimeTypes.PLAIN_TEXT));
      assertThat(doc.getSize(), is(28L));
      assertThat(doc.getDescription(), is("Ceci est un document de test"));
    }

    @Override
    public void run() throws Exception {
      Session session = openJCRSession();
      try {
        run(session);
      } finally {
        session.logout();
      }
    }

    abstract public void run(Session session) throws Exception;
  }
}