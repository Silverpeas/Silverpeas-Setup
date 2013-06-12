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
package org.silverpeas.migration.jcr.wysiwyg;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import org.silverpeas.migration.jcr.service.AttachmentService;
import org.silverpeas.migration.jcr.service.ConverterUtil;
import org.silverpeas.migration.jcr.service.SimpleDocumentService;
import org.silverpeas.migration.jcr.service.model.SimpleAttachment;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.util.Console;
import org.silverpeas.util.StringUtil;

import org.apache.commons.io.FilenameUtils;

class WysiwygDocumentMerger implements Callable<Long> {

  private final String componentId;
  private final AttachmentService service;
  private final Console console;

  WysiwygDocumentMerger(String instanceId, SimpleDocumentService service, Console console) {
    this.componentId = instanceId;
    this.service = service;
    this.console = console;
  }

  @Override
  public Long call() throws Exception {
    console.printMessage("Migrating wysiwyg for " + componentId);
    return mergeWysiwyg();
  }

  private Long mergeWysiwyg() {
    long nbMigratedDocuments = 0L;
    List<String> basenames = service.listBasenames(componentId);
    for (String basename : basenames) {
      console.printMessage("Migrating wysiwyg for basename " + basename);
      List<SimpleDocument> documents = service.listWysiwygForBasename(basename, componentId);
      if (documents.size() > 1) {
        console.printMessage("We have " + documents.size() + " to merge for basename " + basename);
        SimpleDocument merge = getMergeDocument(documents, basename);
        documents.remove(merge);
        mergeDocuments(merge, documents);
        nbMigratedDocuments++;
      }
    }
    return nbMigratedDocuments;
  }

  private SimpleDocument getMergeDocument(List<SimpleDocument> documents, String basename) {
    for (SimpleDocument document : documents) {
      if (basename.equals(FilenameUtils.getBaseName(document.getFilename()))) {
        return document;
      }
    }
    return documents.get(0);
  }

  public SimpleDocument mergeDocuments(SimpleDocument document, List<SimpleDocument> documents) {
    SimpleDocument mergedDocument = document;
    for (SimpleDocument doc : documents) {
      String lang = ConverterUtil.extractLanguage(doc.getFilename());
      if (!StringUtil.isDefined(lang)) {
        lang = doc.getLanguage();
      }
      mergedDocument.setFile(new SimpleAttachment(doc.getFilename(), lang, doc.getTitle(), doc
          .getDescription(), doc.getSize(), doc.getContentType(), doc.getCreatedBy(), doc
          .getCreated(), doc.getXmlFormId()));
      File content = new File(doc.getAttachmentPath());
      if (!content.exists() || !content.isFile()) {
        console.printMessage("We have not found " + content.getAbsolutePath());
        doc.setLanguage(ConverterUtil.defaultLanguage);
        content = new File(doc.getAttachmentPath());
      }
      if (content.exists() && content.isFile()) {
        mergedDocument = service.mergeDocument(mergedDocument, doc);
        console.printMessage(content.getAbsolutePath() + " was merged into " + mergedDocument
            .getAttachmentPath());
      } else {
        console.printMessage("We have not found " + content.getAbsolutePath());
      }
    }
    return mergedDocument;
  }
}
