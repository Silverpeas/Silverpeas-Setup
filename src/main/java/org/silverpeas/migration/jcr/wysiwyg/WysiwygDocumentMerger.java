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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.silverpeas.dbbuilder.util.Configuration;
import org.silverpeas.migration.jcr.service.ConverterUtil;
import org.silverpeas.migration.jcr.service.SimpleDocumentService;
import org.silverpeas.migration.jcr.service.model.SimpleAttachment;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.migration.jcr.service.model.SimpleDocumentPK;
import org.silverpeas.util.Console;
import org.silverpeas.util.StringUtil;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

class WysiwygDocumentMerger implements Callable<Long> {
  private static final Object synchronizeObject = new Object();
  private static final File workspaceFiles = new File(Configuration.getSilverpeasData());
  private static final File workspaceSavesFiles =
      new File(Configuration.getSilverpeasDataMigrationSaves());

  private final String componentId;
  private final SimpleDocumentService service;
  private final Console console;

  WysiwygDocumentMerger(String instanceId, SimpleDocumentService service, Console console) {
    this.componentId = instanceId;
    this.service = service;
    this.console = console;
  }

  @Override
  public Long call() throws Exception {
    console.printMessage("Starting wysiwyg adjustment for component instance id " + componentId);
    long nbAdjustments = mergeWysiwyg();
    String message = "Finishing wysiwyg adjustment for component instance id " + componentId;
    if (nbAdjustments > 0L) {
      console.printMessage(message + " with " + nbAdjustments + " adjustment(s)");
    } else {
      console.printMessage(message + " (but no adjustment done)");
    }
    return nbAdjustments;
  }

  private Long mergeWysiwyg() {
    long nbAdjustedDocuments = 0L;
    List<String> basenames = service.listBasenames(componentId);
    for (String basename : basenames) {
      console.printMessage("Adjusting wysiwyg for basename " + basename);
      // Getting documents ordered by their names
      List<SimpleDocument> documents = service.listWysiwygForBasename(basename, componentId);
      if (!documents.isEmpty()) {
        if (documents.size() > 1) {
          console.printMessage(
              "There is " + documents.size() + " documents to handle for basename " + basename);
          SimpleDocument merge = getMergeDocument(documents, basename);
          documents.remove(merge);
          if (mergeDocuments(merge, documents)) {
            nbAdjustedDocuments++;
          }
        } else {
          console.printMessage("There is 1 document to handle for basename " + basename);
          if (copyIntoRightLanguageLocation(documents.get(0))) {
            nbAdjustedDocuments++;
          }
        }
      }
    }
    return nbAdjustedDocuments;
  }

  private SimpleDocument getMergeDocument(List<SimpleDocument> documents, String basename) {
    for (SimpleDocument document : documents) {
      if (basename.equals(FilenameUtils.getBaseName(document.getFilename())) &&
          ConverterUtil.defaultLanguage.equals(document.getLanguage())) {
        return document;
      }
    }
    return documents.get(0);
  }

  /**
   * Copy in right language location (if it doesn't exist) the content which is in an other
   * language than the default one (language picked from the name of file).
   * @param document
   * @return
   */
  private boolean copyIntoRightLanguageLocation(SimpleDocument document) {
    boolean adjustmentDone = false;
    String languageFromFilename =
        ConverterUtil.checkLanguage(ConverterUtil.extractLanguage(document.getFilename()));
    // Adjusting location of content that is in other language than this of default language
    if (!languageFromFilename.equals(document.getLanguage())) {
      String documentLanguageBeforeAdjustment = document.getLanguage();
      File contentToCopy = new File(document.getAttachmentPath());
      if (contentToCopy.exists() && contentToCopy.isFile()) {

        // Setting the right language
        document.setLanguage(languageFromFilename);
        // Updating the document if right path doesn't exist
        File parentDirectory = new File(document.getAttachmentPath()).getParentFile();
        if (!parentDirectory.exists()) {
          service.updateAttachment(document, contentToCopy);
          console.printMessage(
              "Copy of " + contentToCopy.getAbsolutePath() + " into right location language : " +
                  new File(document.getAttachmentPath()).getPath());

          // Renaming the original document
          document.setLanguage(documentLanguageBeforeAdjustment);
          document.setFilename(ConverterUtil
              .replaceLanguage(document.getFilename(), documentLanguageBeforeAdjustment));
          service.updateAttachment(document, contentToCopy);
          console.printMessage(
              "Renaming " + contentToCopy.getName() + " into with the right language : " +
                  document.getFilename());
          adjustmentDone = true;
          FileUtils.deleteQuietly(contentToCopy);

        } else {
          console.printWarning(parentDirectory.getPath() + " already exists!");
        }
      }
    }
    return adjustmentDone;
  }

  /**
   * Merging documents into the one document.
   * @param document the final document
   * @param documents document to merge
   * @return true if a merge has been done.
   */
  private boolean mergeDocuments(SimpleDocument document, List<SimpleDocument> documents) {
    boolean adjustmentDone = false;
    SimpleDocument documentTarget = document;
    for (SimpleDocument documentToMerge : documents) {
      String lang = ConverterUtil.extractLanguage(documentToMerge.getFilename());
      if (!StringUtil.isDefined(lang)) {
        lang = documentToMerge.getLanguage();
      }
      documentTarget.setAttachment(
          new SimpleAttachment(documentToMerge.getFilename(), lang, documentToMerge.getTitle(),
              documentToMerge.getDescription(), documentToMerge.getSize(),
              documentToMerge.getContentType(), documentToMerge.getCreatedBy(),
              documentToMerge.getCreated(), documentToMerge.getXmlFormId()));
      File content = new File(documentToMerge.getAttachmentPath());
      if (content.exists() && content.isFile()) {

        if (documentTarget.getAttachmentPath().equals(documentToMerge.getAttachmentPath())) {
          // Nothing has to be done in that case ...
          continue;
        }

        // Loading existing JCR data before adjustments, if any
        SimpleDocument documentTargetBeforeAdjustment =
            loadDocumentFrom(documentTarget.getPk(), documentTarget.getLanguage());

        if (documentTargetBeforeAdjustment != null &&
            documentTargetBeforeAdjustment.getUpdated().after(documentToMerge.getUpdated())) {

          // In that case, the current used JCR node has been updated between a first migration and
          // this execution of treatment

          console.printMessage(content.getAbsolutePath() + " has not been merged into the target " +
              documentTarget.getAttachmentPath() +
              " (because the target has been already updated)");

          // Deleting documents that are not necessary (but they are saved just before,
          // we never know !)
          backupDocument(documentToMerge);
          service.deleteAttachment(documentToMerge);
          adjustmentDone = true;

          console.printMessage("The wysiwyg document " + content.getAbsolutePath() +
              " has been deleted from the JCR");

          // No merge has to be done here, performing the next document content if any.
          continue;
        }

        /*
        A merge or a copy has to be done ...
         */

        // Saving the new last update (in order to keep the most recent file)
        boolean wasBackupDone = backupDocument(documentTarget);
        documentTarget = service.mergeDocument(documentTarget, documentToMerge);
        if (documentTargetBeforeAdjustment != null &&
            !documentTargetBeforeAdjustment.getAttachmentPath()
                .equals(documentTarget.getAttachmentPath())) {
          FileUtils.deleteQuietly(new File(documentTargetBeforeAdjustment.getAttachmentPath()));
        }
        console.printMessage(
            content.getAbsolutePath() + " has been " + (wasBackupDone ? "merged" : "copied") +
                " into " + documentTarget.getAttachmentPath());
        adjustmentDone = true;
      } else {
        console.printMessage(content.getAbsolutePath() + " has not been found");
      }
    }
    return adjustmentDone;
  }

  /**
   * This method does a backup of files of a JCR SimpleDocument entry (that will be deleted).
   * @param documentToBackup
   */
  private boolean backupDocument(SimpleDocument documentToBackup) {

    if (!workspaceSavesFiles.exists()) {
      synchronized (synchronizeObject) {
        workspaceSavesFiles.mkdirs();
      }
    }

    File documentDirectoryToBackup = new File(documentToBackup.getAttachmentPath()).getParentFile();
    if (documentDirectoryToBackup.exists() && documentDirectoryToBackup.isDirectory()) {
      File saveDestination = FileUtils.getFile(workspaceSavesFiles,
          StringUtils.replace(documentDirectoryToBackup.getPath(), workspaceFiles.getPath(), ""));
      try {
        FileUtils.copyDirectory(documentDirectoryToBackup, saveDestination);
        console.printMessage(
            "Physical backup has been performed for directory " + documentDirectoryToBackup +
                " into " + saveDestination);
        return true;
      } catch (IOException e) {
        console.printError(
            "copy of directory " + documentDirectoryToBackup.getPath() + " to directory " +
                saveDestination.getPath() + " failed");
      }
    }

    return false;
  }

  private SimpleDocument loadDocumentFrom(SimpleDocumentPK pk, String lang) {
    String language = ConverterUtil.checkLanguage(lang);
    SimpleDocument document = service.searchDocumentById(pk, lang);
    if (document == null || document.getAttachment() == null ||
        !language.equals(document.getLanguage())) {
      // If the loaded document is not exactly the one required, then returning null
      document = null;
    }
    return document;
  }
}
