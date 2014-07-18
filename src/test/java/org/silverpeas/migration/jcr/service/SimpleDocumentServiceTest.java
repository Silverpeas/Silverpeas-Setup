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
package org.silverpeas.migration.jcr.service;

import org.apache.commons.io.Charsets;
import org.apache.tika.mime.MimeTypes;
import org.junit.Test;
import org.silverpeas.migration.jcr.service.model.DocumentType;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentBuilder;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.test.jcr.JcrTest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.silverpeas.migration.jcr.service.model.DocumentType.attachment;
import static org.silverpeas.migration.jcr.service.model.DocumentType.wysiwyg;

public class SimpleDocumentServiceTest {

  private static final String instanceId = "kmelia26";

  /**
   * Test of removeContent method, of class DocumentRepository.
   * TODO The remove of versioned contents is not yet verified. A unit test must be added...
   */
  @Test
  public void testRemoveContent() throws Exception {
    new JcrSimpleDocumentServiceTest() {
      @Override
      public void run() throws Exception {
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
        getSimpleDocumentService().removeContent(document, "fr");

        // Vérifying the attachments
        enDocument = getDocumentById(document.getId(), "en");
        assertThat(enDocument, notNullValue());
        assertThat(enDocument.getAttachment(), notNullValue());
        checkEnglishSimpleDocument(enDocument);
        frDocument = getDocumentById(document.getId(), "fr");
        assertThat(frDocument, notNullValue());
        assertThat(frDocument.getAttachment(), nullValue());
        assertThat(enPhysicalFile.exists(), is(true));
        assertThat(frPhysicalFile.exists(), is(false));
        assertThat(frPhysicalFile.getParentFile().exists(), is(false));
        assertThat(frPhysicalFile.getParentFile().getParentFile().exists(), is(true));

        // Removing the EN content
        getSimpleDocumentService().removeContent(document, "en");

        // Vérifying the attachments
        enDocument = getDocumentById(document.getId(), "en");
        frDocument = getDocumentById(document.getId(), "fr");
        assertThat(enDocument, nullValue());
        assertThat(frDocument, nullValue());
        assertThat(enPhysicalFile.exists(), is(false));
        assertThat(frPhysicalFile.exists(), is(false));
        assertThat(frPhysicalFile.getParentFile().exists(), is(false));
        assertThat(frPhysicalFile.getParentFile().getParentFile().exists(), is(false));
        assertThat(frPhysicalFile.getParentFile().getParentFile().getParentFile().exists(),
            is(false));
      }
    }.execute();
  }

  /**
   * Test of getBinaryContent method, of class DocumentRepository.
   */
  @Test
  public void testGetBinaryContent() throws Exception {
    new JcrSimpleDocumentServiceTest() {
      @Override
      public void run() throws Exception {
        String foreignId = "node18";
        SimpleDocument document =
            createAttachmentForTest(defaultDocumentBuilder(foreignId).setOrder(10),
                defaultENContentBuilder(), "This is a test");

        ByteArrayOutputStream bibaryContent = new ByteArrayOutputStream();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "en");
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is("This is a test"));
        bibaryContent.reset();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "en", 1, 2);
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is("hi"));
        bibaryContent.reset();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "en", 1, 50);
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is("his is a test"));
        bibaryContent.reset();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "en", -10, 50);
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is("This is a test"));
        bibaryContent.reset();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "fr");
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is(""));

        updateAttachmentForTest(document, "fr", "Ceci est un test");

        bibaryContent.reset();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "en");
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is("This is a test"));
        bibaryContent.reset();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "fr");
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is("Ceci est un test"));

        updateAttachmentForTest(document, "en", "A");
        updateAttachmentForTest(document, "fr", "B");

        bibaryContent.reset();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "en");
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is("A"));
        bibaryContent.reset();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "en", 1, 4);
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is(""));
        bibaryContent.reset();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "fr");
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is("B"));
        bibaryContent.reset();
        getSimpleDocumentService().getBinaryContent(bibaryContent, document.getPk(), "fr", 1, 2);
        assertThat(bibaryContent.toString(Charsets.UTF_8.name()), is(""));
      }
    }.execute();
  }

  /**
   * Test of listForeignIdsWithWysiwyg method, of class DocumentRepository.
   */
  @Test
  public void listForeignIdsWithWysiwyg() throws Exception {
    new JcrSimpleDocumentServiceTest() {
      @Override
      public void run() throws Exception {
        Set<String> createdIds = new HashSet<String>();
        // No WYSIWYG content exists
        List<String> foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg(instanceId);
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
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg(instanceId);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(0));

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
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg(instanceId);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(0));

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
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg(instanceId);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));

        // Updating wysiwyg file name
        createdDocument.setFilename("fId_26_wysiwyg_en.txt");
        updateAttachmentForTest(createdDocument);

        // One WYSIWYG base name
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg(instanceId);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg("otherKmelia38");
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
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg(instanceId);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg("otherKmelia38");
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));

        // Adding the second WYSIWYG document (on same component)
        SimpleDocument secondCreatedDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_27").setDocumentType(wysiwyg),
                defaultFRContentBuilder().setFilename("fId_27_wysiwyg_fr.txt"), "fId_27_fr");
        createdIds.add(secondCreatedDocument.getId());

        // Two WYSIWYG on one Component
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg(instanceId);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(2));
        assertThat(foreignIds, containsInAnyOrder("fId_26", "fId_27"));
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg("otherKmelia38");
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));

        // Updating wysiwyg file name
        setEnData(secondCreatedDocument);
        secondCreatedDocument.setFilename(secondCreatedDocument.getFilename());
        updateAttachmentForTest(secondCreatedDocument, "en", "fId_27_en");

        // Two WYSIWYG (each one in two languages) on one Component
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg(instanceId);
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(2));
        assertThat(foreignIds, containsInAnyOrder("fId_26", "fId_27"));
        foreignIds = getSimpleDocumentService().listForeignIdsWithWysiwyg("otherKmelia38");
        assertThat(foreignIds, notNullValue());
        assertThat(foreignIds, hasSize(1));
        assertThat(foreignIds, contains("fId_26"));

        assertThat(createdIds, hasSize(nbDocuments + 2));
      }
    }.execute();
  }

  /**
   * Test of listWysiwygByForeignId method, of class DocumentRepository.
   */
  @Test
  public void listWysiwygByForeignId() throws Exception {
    new JcrSimpleDocumentServiceTest() {
      @Override
      public void run() throws Exception {
        Set<String> createdIds = new HashSet<String>();
        // No WYSIWYG content exists
        List<String> wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByForeignId(instanceId, ""));
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
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByForeignId(instanceId, "fId_1"));
        assertThat(wysiwygFIdLangFilenames, hasSize(0));

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
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByForeignId(instanceId, "fId_1"));
        assertThat(wysiwygFIdLangFilenames, hasSize(0));

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
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByForeignId(instanceId, "fId_1"));
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
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByForeignId(instanceId, "fId_26"));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_26|en|test.pdf"));

        // Updating wysiwyg file name
        createdDocument.setFilename("fId_26_wysiwyg_en.txt");
        updateAttachmentForTest(createdDocument);

        // One WYSIWYG base name
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByForeignId(instanceId, "fId_26"));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_26|en|fId_26_wysiwyg_en.txt"));

        // Adding the FR content to the first WYSIWYG document
        enDocument = getDocumentById(createdDocument.getId(), "en");
        setFrData(enDocument);
        enDocument.setFilename("fId_26_wysiwyg_fr.txt");
        updateAttachmentForTest(enDocument, "fr", "fId_26_fr");
        createdIds.add(enDocument.getId());

        // One WYSIWYG on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByForeignId(instanceId, "fId_26"));
        assertThat(wysiwygFIdLangFilenames, hasSize(2));
        assertThat(wysiwygFIdLangFilenames, containsInAnyOrder("fId_26|fr|fId_26_wysiwyg_fr.txt",
            "fId_26|en|fId_26_wysiwyg_en.txt"));

        // Adding the second WYSIWYG document (on same component)
        SimpleDocument secondCreatedDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_27").setDocumentType(wysiwyg),
                defaultFRContentBuilder().setFilename("fId_27_wysiwyg_fr.txt"), "fId_27_fr");
        createdIds.add(secondCreatedDocument.getId());

        // Two WYSIWYG on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByForeignId(instanceId, "fId_27"));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_27|fr|fId_27_wysiwyg_fr.txt"));

        // Updating wysiwyg file name
        setEnData(secondCreatedDocument);
        secondCreatedDocument.setFilename(secondCreatedDocument.getFilename());
        updateAttachmentForTest(secondCreatedDocument, "en", "fId_27_en");

        // Two WYSIWYG (each one in two languages) on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByForeignId(instanceId, "fId_27"));
        assertThat(wysiwygFIdLangFilenames, hasSize(2));
        assertThat(wysiwygFIdLangFilenames,
            contains("fId_27|fr|fId_27_wysiwyg_fr.txt", "fId_27|en|fId_27_wysiwyg_fr.txt"));

        assertThat(createdIds, hasSize(nbDocuments + 2));
      }
    }.execute();
  }

  /**
   * Test of listWysiwygByInstanceId method, of class DocumentRepository.
   */
  @Test
  public void listWysiwygByInstanceId() throws Exception {
    new JcrSimpleDocumentServiceTest() {
      @Override
      public void run() throws Exception {
        Set<String> createdIds = new HashSet<String>();
        // No WYSIWYG content exists
        List<String> wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByInstanceId(instanceId));
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
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByInstanceId(instanceId));
        assertThat(wysiwygFIdLangFilenames, hasSize(0));

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
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByInstanceId(instanceId));
        assertThat(wysiwygFIdLangFilenames, hasSize(0));

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
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByInstanceId(instanceId));
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
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByInstanceId(instanceId));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_26|en|test.pdf"));

        // Updating wysiwyg file name
        createdDocument.setFilename("fId_26_wysiwyg_en.txt");
        updateAttachmentForTest(createdDocument);

        // One WYSIWYG base name
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByInstanceId(instanceId));
        assertThat(wysiwygFIdLangFilenames, hasSize(1));
        assertThat(wysiwygFIdLangFilenames, contains("fId_26|en|fId_26_wysiwyg_en.txt"));

        // Adding the FR content to the first WYSIWYG document
        enDocument = getDocumentById(createdDocument.getId(), "en");
        setFrData(enDocument);
        enDocument.setFilename("fId_26_wysiwyg_fr.txt");
        updateAttachmentForTest(enDocument, "fr", "fId_26_fr");
        createdIds.add(enDocument.getId());

        // One WYSIWYG on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByInstanceId(instanceId));
        assertThat(wysiwygFIdLangFilenames, hasSize(2));
        assertThat(wysiwygFIdLangFilenames, containsInAnyOrder("fId_26|fr|fId_26_wysiwyg_fr.txt",
            "fId_26|en|fId_26_wysiwyg_en.txt"));

        // Adding the second WYSIWYG document (on same component)
        SimpleDocument secondCreatedDocument =
            createAttachmentForTest(defaultDocumentBuilder("fId_27").setDocumentType(wysiwyg),
                defaultFRContentBuilder().setFilename("fId_27_wysiwyg_fr.txt"), "fId_27_fr");
        createdIds.add(secondCreatedDocument.getId());

        // Two WYSIWYG on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByInstanceId(instanceId));
        assertThat(wysiwygFIdLangFilenames, hasSize(3));
        assertThat(wysiwygFIdLangFilenames,
            containsInAnyOrder("fId_26|fr|fId_26_wysiwyg_fr.txt", "fId_26|en|fId_26_wysiwyg_en.txt",
                "fId_27|fr|fId_27_wysiwyg_fr.txt"));

        // Updating wysiwyg file name
        setEnData(secondCreatedDocument);
        secondCreatedDocument.setFilename(secondCreatedDocument.getFilename());
        updateAttachmentForTest(secondCreatedDocument, "en", "fId_27_en");

        // Two WYSIWYG (each one in two languages) on one Component
        wysiwygFIdLangFilenames = extractForeignIdLanguageFilenames(
            getSimpleDocumentService().listWysiwygByInstanceId(instanceId));
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
   * Executing an adapted JcrTest
   */
  private abstract class JcrSimpleDocumentServiceTest extends JcrTest {

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
  }
}