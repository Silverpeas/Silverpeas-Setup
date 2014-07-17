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

import org.apache.commons.lang.time.DurationFormatUtils;
import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;
import org.silverpeas.migration.jcr.service.RepositoryManager;
import org.silverpeas.migration.jcr.service.SimpleDocumentService;
import org.silverpeas.util.DateUtil;
import org.silverpeas.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class handles the purge of unnecessary WYSIWYG documents registered into the Silverpeas JCR
 * repository.
 * Empty WYSIWYGS for a language is considered as unnecessary.
 * When WYSIWYG content is the same for all language of a WYSIWYG document, then one is kept and
 * the others are considered as unnecessary.
 * All unnecessary document contents are removed from the JCR and also from the filesystem.
 * Deleted files from the filesystem are saved.
 */
public class WysiwygPurger extends DbBuilderDynamicPart {

  private final ExecutorService executor;
  private final SimpleDocumentService service;

  /**
   * The default constructor.
   * A thread pool of 10 threads is handled.
   */
  public WysiwygPurger() {
    executor = Executors.newFixedThreadPool(10);
    this.service = new SimpleDocumentService();
  }

  /**
   * Hidden constructor used by tests.
   * @param service
   * @param fixedThreadPool
   */
  WysiwygPurger(SimpleDocumentService service, int fixedThreadPool) {
    executor = Executors.newFixedThreadPool(fixedThreadPool);
    this.service = service;
  }

  /**
   * This method must be called to perform the purge treatment.
   * A thread pool is handled.
   * @throws Exception
   */
  public void purgeDocuments() throws Exception {
    Date startDate = new Date();
    getConsole().printMessage("Starting the WYSIWYG purge process at " +
        DateUtil.formatAsISO8601WithUtcOffset(startDate));
    long totalNumberOfPurgedFiles = 0L;
    long totalNumberOfRenamedFiles = 0L;
    List<WysiwygDocumentPurger> mergers = buildWysiwygDocumentPurgers();
    List<Future<WysiwygDocumentPurger.Result>> results = executor.invokeAll(mergers);
    try {
      for (Future<WysiwygDocumentPurger.Result> result : results) {
        totalNumberOfPurgedFiles += result.get().getNbWysiwygContentPurged();
        totalNumberOfRenamedFiles += result.get().getNbWysiwygFilenameRenamed();
      }
    } catch (InterruptedException ex) {
      throw ex;
    } catch (Exception ex) {
      getConsole().printError(
          "Error during the purge or the physical WYSIWYG file renaming of wysiwyg contents " + ex,
          ex);
      throw ex;
    } finally {
      executor.shutdown();
    }
    getConsole().printMessage("Nb of purged wysiwyg contents : " + totalNumberOfPurgedFiles);
    getConsole()
        .printMessage("Nb of renamed wysiwyg content file names : " + totalNumberOfRenamedFiles);
    Date endDate = new Date();
    getConsole().printMessage(
        "Finishing the WYSIWYG purge process at " + DateUtil.formatAsISO8601WithUtcOffset(endDate) +
            " (total duration of " +
            DurationFormatUtils.formatDurationHMS(endDate.getTime() - startDate.getTime()) + ")");
    if (!RepositoryManager.isIsJcrTestEnv()) {
      this.service.shutdown();
    }
  }

  private List<WysiwygDocumentPurger> buildWysiwygDocumentPurgers() {
    getConsole().printMessage(
        "All components to be parsed to find empty WYSIWYG to purged or to find wysiwyg " +
            "file name to rename : "
    );
    List<String> componentIds = service.listComponentIdsWithWysiwyg();
    List<WysiwygDocumentPurger> result = new ArrayList<WysiwygDocumentPurger>(componentIds.size());
    for (String componentId : componentIds) {
      if (StringUtil.isDefined(componentId)) {
        result.add(new WysiwygDocumentPurger(componentId, service, getConsole()));
        getConsole().printMessage(componentId + ", ");
      }
    }
    getConsole().printMessage("*************************************************************");
    return result;
  }
}
