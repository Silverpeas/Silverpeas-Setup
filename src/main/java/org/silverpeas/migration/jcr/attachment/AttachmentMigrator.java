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
package org.silverpeas.migration.jcr.attachment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.dbutils.DbUtils;
import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;
import org.silverpeas.migration.jcr.service.RepositoryManager;
import org.silverpeas.util.ConfigurationHolder;

public class AttachmentMigrator extends DbBuilderDynamicPart {

  private static final ExecutorService executor;
  private static final int threadCount;

  static {
    threadCount = ConfigurationHolder.getMaxThreadsCount();
    executor = Executors.newFixedThreadPool(threadCount);
  }
  private final RepositoryManager repositoryManager;
  public static final String SELECT_COMPONENTS = "SELECT DISTINCT instanceid FROM "
      + "sb_attachment_attachment ORDER BY instanceid";
  public static final String SELECT_MAX_ID =
      "SELECT maxid FROM uniqueid WHERE tablename = 'sb_attachment_attachment'";
  public static final String UPDATE_UNIQUEID =
      "INSERT INTO uniqueid (maxid, tablename) VALUES (?, 'sb_simple_document')";

  public AttachmentMigrator() {
    this.repositoryManager = new RepositoryManager();
  }

  public void migrateAttachments() throws Exception {
    getConsole().printMessage("Migration of the attachments in JCR with a pool of "
        + threadCount + " threads");
    updateUniqueId();
    long totalNumberOfMigratedFiles = 0L;
    List<AttachmentMigration> migrators = buildComponentMigrators();
    List<Future<Long>> result = executor.invokeAll(migrators);
    try {
      for (Future<Long> nbOfMigratedDocuments : result) {
        totalNumberOfMigratedFiles += nbOfMigratedDocuments.get();
      }
    } catch (InterruptedException ex) {
      throw ex;
    } catch (Exception ex) {
      getConsole().printError("Error during migration of attachments " + ex, ex);
      throw ex;
    } finally {
      executor.shutdown();
    }
    getConsole().printMessage("Nb of migrated documents : " + totalNumberOfMigratedFiles);
    getConsole().printMessage("*************************************************************");
    this.repositoryManager.shutdown();
  }

  protected List<AttachmentMigration> buildComponentMigrators() throws SQLException {
    List<AttachmentMigration> result = new ArrayList<AttachmentMigration>(500);
    Statement stmt = null;
    ResultSet rs = null;
    getConsole().printMessage("All components to be migrated : ");
    try {
      stmt = getConnection().createStatement();
      rs = stmt.executeQuery(SELECT_COMPONENTS);
      StringBuilder message = new StringBuilder();
      while (rs.next()) {
        String instanceId = rs.getString("instanceid");
        result.add(
            new AttachmentMigration(instanceId, repositoryManager, getConsole()));
        message.append(instanceId).append(" ");
      }
      getConsole().printMessage(message.toString());
      getConsole().printMessage("");
      return result;
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
  }

  protected void updateUniqueId() throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    long maxId = 0L;
    getConsole().printMessage("Updating uniqueId");
    try {
      stmt = getConnection().createStatement();
      rs = stmt.executeQuery(SELECT_MAX_ID);
      if (rs.next()) {
        maxId = rs.getLong("maxid");
      }
    } catch (SQLException sqlex) {
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    PreparedStatement pstmt;
    try {
      pstmt = getConnection().prepareStatement(UPDATE_UNIQUEID);
      pstmt.setLong(1, maxId);
      pstmt.executeUpdate();
    } catch (SQLException sqlex) {
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
  }
}
