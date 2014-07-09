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
package org.silverpeas.migration.jcr.wysiwyg.purge;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.silverpeas.migration.jcr.service.AttachmentException;
import org.silverpeas.migration.jcr.service.ConverterUtil;
import org.silverpeas.migration.jcr.service.SimpleDocumentService;
import org.silverpeas.migration.jcr.service.model.SimpleDocument;
import org.silverpeas.util.Console;
import org.silverpeas.util.MapUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Purge treatment.
 */
class WysiwygDocumentPurger implements Callable<WysiwygDocumentPurger.Result> {

  private final String componentId;
  private final SimpleDocumentService service;
  private final Console console;

  /**
   * Default constructor.
   * @param instanceId the identifier of the current performed component.
   * @param service the attachement services.
   * @param console the console to report logs.
   */
  WysiwygDocumentPurger(String instanceId, SimpleDocumentService service, Console console) {
    this.componentId = instanceId;
    this.service = service;
    this.console = console;
  }

  @Override
  public Result call() throws Exception {
    console
        .printMessage("Starting wysiwyg contents purge for component instance id " + componentId);
    Result result = purgeWysiwyg();
    String message = "Finishing wysiwyg contents purge for component instance id " + componentId;
    if (result.getNbWysiwygContentPurged() > 0L) {
      message += " with " + result.getNbWysiwygContentPurged() + " wysiwyg content(s) purged";
    } else {
      message += " with no wysiwyg content purged";
    }
    if (result.getNbWysiwygFilenameRenamed() > 0L) {
      message +=
          " and " + result.getNbWysiwygFilenameRenamed() + " wysiwyg content file name(s) renamed";
    } else {
      message += " and no wysiwyg content file name renamed";
    }
    console.printMessage(message);
    return result;
  }

  private Result purgeWysiwyg() {
    Result result = new Result();
    List<String> foreignIdsWithWysiwyg = service.listForeignIdsWithWysiwyg(componentId);
    for (String foreignIdWithWysiwyg : foreignIdsWithWysiwyg) {
      String commonLogPart = "instanceId=" + componentId + ", foreignId=" +
          foreignIdWithWysiwyg;
      console.printMessage(commonLogPart + " - verifying wysiwyg contents ...");

      // Getting documents ordered by their names
      ForeignIdProcessContext context = new ForeignIdProcessContext(
          service.listWysiwygByForeignId(componentId, foreignIdWithWysiwyg));

      int nbContents = context.getWysiwygOfForeignId().size();

      if (nbContents > 0) {

        // Removing firstly the empty ones.
        removeEmptyOnes(commonLogPart, context);

        // For each language, removing duplicates
        removeDuplicatesForEachLanguage(commonLogPart, context);

        // Removing duplicates between all languages
        removeDuplicatesBetweenAllLanguages(commonLogPart, context);

        // Renaming wysiwyg content file names according to the language
        renameFilenames(commonLogPart, context);
      }

      result.addNbWysiwygContentPurged(context.getNbRemovedContent());
      result.addNbWysiwygFilenameRenamed(context.getNbRenamedContent());

      console.printMessage(
          commonLogPart + " - " + context.getNbRemovedContent() + "/" + nbContents + " purged.");
      console.printMessage(
          commonLogPart + " - " + context.getNbRenamedContent() + "/" + nbContents + " renamed.");
    }
    return result;
  }

  /**
   * Removes all empty wysiwyg.
   * @param context the context.
   */
  private void removeEmptyOnes(String commonLogPart, ForeignIdProcessContext context) {
    commonLogPart += " - empty content";
    for (WysiwygContent wysiwygContent : new ArrayList<WysiwygContent>(
        context.getWysiwygOfForeignId())) {
      if (isPhysicalFileExistingWithEmptyContent(commonLogPart, wysiwygContent.getAttachment())) {
        service.removeContent(wysiwygContent.getAttachment(), wysiwygContent.getLanguage());
        console.printMessage(commonLogPart + " - doc=" + wysiwygContent.getNodeName() + ", lang=" +
                wysiwygContent.getLanguage() + " - removed"
        );
        context.remove(wysiwygContent);
      }
    }
    context.resetIndexation();
  }

  /**
   * Removes all wysiwyg duplication for each language.
   * @param context the context.
   */
  private void removeDuplicatesForEachLanguage(String commonLogPart,
      ForeignIdProcessContext context) {
    commonLogPart += " - duplicates for a language";
    for (String language : new ArrayList<String>(context.getWysiwygIndexedByLangs().keySet())) {
      List<WysiwygContent> wysiwygContentsForCurrentLanguage =
          context.getWysiwygIndexedByLangs().get(language);
      if (wysiwygContentsForCurrentLanguage.size() > 1) {
        List<WysiwygContent> wysiwygContentsToRemove =
            new ArrayList<WysiwygContent>(wysiwygContentsForCurrentLanguage);
        WysiwygContent firstWysiwygContent = wysiwygContentsToRemove.remove(0);
        String firstContent = firstWysiwygContent.getContent(commonLogPart);
        for (WysiwygContent wysiwygContent : wysiwygContentsForCurrentLanguage) {
          if (!wysiwygContent.getContent(commonLogPart).equals(firstContent)) {
            wysiwygContentsToRemove.clear();
            break;
          }
        }
        removeWysiwygContents(commonLogPart, context, firstWysiwygContent, wysiwygContentsToRemove);
      }
    }
  }

  /**
   * Removes all wysiwyg duplication between all languages.
   * Method
   * {@link #removeDuplicatesForEachLanguage(String, WysiwygDocumentPurger.ForeignIdProcessContext)}
   * must be called before.
   * @param context the context.
   */
  private void removeDuplicatesBetweenAllLanguages(String commonLogPart,
      ForeignIdProcessContext context) {
    commonLogPart += " - duplicates between all languages";
    if (context.getWysiwygIndexedByLangs().size() > 1) {
      WysiwygContent defaultWysiwygContent = null;
      String firstContent = null;
      List<WysiwygContent> wysiwygContentsToRemove =
          new ArrayList<WysiwygContent>(context.getWysiwygIndexedByLangs().size());
      for (Map.Entry<String, List<WysiwygContent>> languageWysiwygContents : context
          .getWysiwygIndexedByLangs().entrySet()) {
        if (!languageWysiwygContents.getValue().isEmpty()) {
          // It must exist at most one SimpleDocument per language, otherwise treatments stops here.
          if (languageWysiwygContents.getValue().size() > 1) {
            console.printWarning(commonLogPart +
                " - contains several JCR master nodes with different language contents, ...");
            return;
          }
          WysiwygContent currentWysiwygContent = languageWysiwygContents.getValue().get(0);
          if (defaultWysiwygContent == null ||
              !defaultWysiwygContent.getLanguage().equals(ConverterUtil.defaultLanguage) &&
                  currentWysiwygContent.getLanguage().equals(ConverterUtil.defaultLanguage)) {
            defaultWysiwygContent = currentWysiwygContent;
            if (firstContent == null) {
              firstContent = defaultWysiwygContent.getContent(commonLogPart);
            }
          }
          if (firstContent.equals(currentWysiwygContent.getContent(commonLogPart))) {
            wysiwygContentsToRemove.add(currentWysiwygContent);
          } else {
            // Contents between languages are differents, stopping the treatment of this method.
            return;
          }
        }
      }
      removeWysiwygContents(commonLogPart, context, defaultWysiwygContent, wysiwygContentsToRemove);
    }
  }

  /**
   * Renames all wysiwyg file name according to the language.
   * @param context the context.
   */
  private void renameFilenames(String commonLogPart, ForeignIdProcessContext context) {
    commonLogPart += " - wysiwyg content file name";
    for (WysiwygContent wysiwygContent : new ArrayList<WysiwygContent>(
        context.getWysiwygOfForeignId())) {
      String fileNameLanguage = ConverterUtil.checkLanguage(
          ConverterUtil.extractLanguage(wysiwygContent.getAttachment().getFilename()));
      if (!wysiwygContent.getLanguage().equals(fileNameLanguage)) {
        File contentToRename = new File(wysiwygContent.getAttachment().getAttachmentPath());
        if (contentToRename.exists() && contentToRename.isFile()) {
          wysiwygContent.getAttachment().setFilename(ConverterUtil
              .replaceLanguage(wysiwygContent.getAttachment().getFilename(),
                  wysiwygContent.getLanguage()));
          File contentRenamed = new File(wysiwygContent.getAttachment().getAttachmentPath());
          console
              .printMessage(commonLogPart + " - doc=" + wysiwygContent.getNodeName() + ", lang=" +
                      wysiwygContent.getLanguage() + " - " + contentToRename.getName() +
                      " renaming to " + contentRenamed.getName()
              );
          if (contentRenamed.exists()) {
            console
                .printWarning(commonLogPart + " - doc=" + wysiwygContent.getNodeName() + ", lang=" +
                        wysiwygContent.getLanguage() +
                        " aborted because " + contentRenamed.getName() +
                        " already exists! Nothing is renamed and a manual intervention " +
                        "must be performed."
                );
            return;
          }
          service.updateAttachment(wysiwygContent.getAttachment(), contentToRename);
          FileUtils.deleteQuietly(contentToRename);
          context.addRenamedContent();
        }
      }
    }
  }

  /**
   * Common treatments to remove content.
   * @param commonLogPart
   * @param context
   * @param wysiwygContentToKeep
   * @param wysiwygContentsToRemove
   */
  private void removeWysiwygContents(String commonLogPart, ForeignIdProcessContext context,
      WysiwygContent wysiwygContentToKeep, List<WysiwygContent> wysiwygContentsToRemove) {
    if (wysiwygContentToKeep != null) {
      wysiwygContentsToRemove.remove(wysiwygContentToKeep);
      if (!wysiwygContentsToRemove.isEmpty()) {
        console.printMessage(
            commonLogPart + " - doc=" + wysiwygContentToKeep.getNodeName() + ", lang=" +
                wysiwygContentToKeep.getLanguage() + " - content kept"
        );
        for (WysiwygContent wysiwygContentToRemove : wysiwygContentsToRemove) {
          service.removeContent(wysiwygContentToRemove.getAttachment(),
              wysiwygContentToRemove.getLanguage());
          console.printMessage(
              commonLogPart + " - doc=" + wysiwygContentToRemove.getNodeName() + ", lang=" +
                  wysiwygContentToRemove.getLanguage() + " - content removed"
          );
          context.remove(wysiwygContentToRemove);
        }
        context.resetIndexation();
      }
    }
  }

  /**
   * Gets the content of the given wysiwyg/language.
   * @param commonLogPart
   * @param wysiwygLangAttachment
   * @return a String that represents the content in UTF8, a dummy string if physical file does
   * not exist.
   */
  private String getContent(String commonLogPart, SimpleDocument wysiwygLangAttachment) {
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    try {
      service.getBinaryContent(content, wysiwygLangAttachment.getPk(),
          wysiwygLangAttachment.getLanguage());
      return content.toString(Charsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      console
          .printError(commonLogPart + " - doc=" + wysiwygLangAttachment.getNodeName() + ", lang=" +
              wysiwygLangAttachment.getLanguage() + " - unsupported encoding", e);
      return UUID.randomUUID().toString();
    } catch (AttachmentException ae) {
      console
          .printError(commonLogPart + " - doc=" + wysiwygLangAttachment.getNodeName() + ", lang=" +
              wysiwygLangAttachment.getLanguage() + " - inconsistent data, nothing is done", ae);
      return UUID.randomUUID().toString();
    }
  }

  /**
   * Indicates if the content of given wysiwyg/language is empty.
   * @param commonLogPart
   * @param wysiwygLangAttachment
   * @return true if exists and is empty, false otherwise.
   */
  private boolean isPhysicalFileExistingWithEmptyContent(String commonLogPart,
      SimpleDocument wysiwygLangAttachment) {
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    try {
      service.getBinaryContent(content, wysiwygLangAttachment.getPk(),
          wysiwygLangAttachment.getLanguage(), 0, 1);
      return content.size() == 0;
    } catch (AttachmentException ae) {
      console
          .printError(commonLogPart + " - doc=" + wysiwygLangAttachment.getNodeName() + ", lang=" +
              wysiwygLangAttachment.getLanguage() + " - inconsistent data, nothing is done", ae);
      return false;
    }
  }

  /**
   * Gets the last update date from a document.
   * If the last update date is not explicitly known, the date of creation is returned.
   * @param document
   * @return
   */
  private static Date getLastDocumentUpdateDate(SimpleDocument document) {
    Date lastUpdateDate = document.getUpdated();
    if (lastUpdateDate == null) {
      lastUpdateDate = document.getCreated();
    }
    return lastUpdateDate;
  }

  /**
   * Internal class to handled a context.
   */
  private class ForeignIdProcessContext {
    private int nbRemovedContent = 0;
    private int nbRenamedContent = 0;
    private final List<WysiwygContent> wysiwygOfForeignId = new ArrayList<WysiwygContent>();
    private Map<String, List<WysiwygContent>> wysiwygIndexedByLangs =
        new HashMap<String, List<WysiwygContent>>();

    public ForeignIdProcessContext(List<SimpleDocument> wysiwygOfForeignId) {
      // Firstly
      Collections.sort(wysiwygOfForeignId, new Comparator<SimpleDocument>() {
        @Override
        public int compare(final SimpleDocument sd1, final SimpleDocument sd2) {
          return getLastDocumentUpdateDate(sd2).compareTo(getLastDocumentUpdateDate(sd1));
        }
      });
      for (SimpleDocument wysiwygLang : wysiwygOfForeignId) {
        this.wysiwygOfForeignId.add(new WysiwygContent(wysiwygLang));
      }
      resetIndexation();
    }

    public void resetIndexation() {
      wysiwygIndexedByLangs.clear();
      for (WysiwygContent wysiwygContent : wysiwygOfForeignId) {

        // Index by language
        MapUtil.putAddList(wysiwygIndexedByLangs, wysiwygContent.getLanguage(), wysiwygContent);
      }
    }

    public void remove(WysiwygContent wysiwygContent) {
      wysiwygOfForeignId.remove(wysiwygContent);
      addRemovedContent();
    }

    public List<WysiwygContent> getWysiwygOfForeignId() {
      return wysiwygOfForeignId;
    }

    public Map<String, List<WysiwygContent>> getWysiwygIndexedByLangs() {
      return wysiwygIndexedByLangs;
    }

    public int getNbRemovedContent() {
      return nbRemovedContent;
    }

    private void addRemovedContent() {
      this.nbRemovedContent++;
    }

    public int getNbRenamedContent() {
      return nbRenamedContent;
    }

    private void addRenamedContent() {
      this.nbRenamedContent++;
    }
  }

  /**
   * This class represents an attachment in the JCR for an instanceId, foreignId and a language.
   */
  private class WysiwygContent {
    private final SimpleDocument attachment;
    private String content = null;

    public WysiwygContent(SimpleDocument wysiwygAttachment) {
      attachment = wysiwygAttachment;
    }

    public SimpleDocument getAttachment() {
      return attachment;
    }

    public String getContent(String commonLogPart) {
      if (content == null) {
        content = WysiwygDocumentPurger.this.getContent(commonLogPart, attachment);
      }
      return content;
    }

    public String getId() {
      return attachment.getId();
    }

    public String getNodeName() {
      return attachment.getNodeName();
    }

    public String getLanguage() {
      return attachment.getLanguage();
    }

    @Override
    public int hashCode() {
      HashCodeBuilder hash = new HashCodeBuilder();
      hash.append(getId());
      hash.append(getLanguage());
      return hash.toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      if (super.equals(obj)) {
        return true;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final WysiwygContent other = (WysiwygContent) obj;
      EqualsBuilder matcher = new EqualsBuilder();
      matcher.append(getId(), other.getId());
      matcher.append(getLanguage(), other.getLanguage());
      return matcher.isEquals();
    }
  }

  /**
   * A result.
   */
  public static class Result {
    private long nbWysiwygContentPurged = 0;
    private long nbWysiwygFilenameRenamed = 0;

    public long getNbWysiwygContentPurged() {
      return nbWysiwygContentPurged;
    }

    public void addNbWysiwygContentPurged(final long nbWysiwygContentPurgedToAdd) {
      this.nbWysiwygContentPurged += nbWysiwygContentPurgedToAdd;
    }

    public long getNbWysiwygFilenameRenamed() {
      return nbWysiwygFilenameRenamed;
    }

    public void addNbWysiwygFilenameRenamed(final long nbWysiwygFilenameRenamedToAdd) {
      this.nbWysiwygFilenameRenamed += nbWysiwygFilenameRenamedToAdd;
    }
  }
}
