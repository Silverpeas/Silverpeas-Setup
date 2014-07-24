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
package org.silverpeas.migration.jcr.version;

import org.apache.commons.dbutils.DbUtils;
import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;
import org.silverpeas.migration.jcr.service.RepositoryManager;
import org.silverpeas.util.StringUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class VersioningMigrator extends DbBuilderDynamicPart {

  private final RepositoryManager repositoryManager;
  public static final String SELECT_COMPONENTS = "SELECT DISTINCT instanceid FROM "
      + "sb_version_document ORDER BY instanceid";

  public VersioningMigrator() {
    this.repositoryManager = new RepositoryManager();
  }

  public void migrateDocuments() throws Exception {
    getConsole().printMessage("Migration of the versioned documents in JCR with a pool of "
        + getThreadExecutorPoolCount() + " threads");
    long totalNumberOfMigratedFiles = 0L;
    List<VersionedDocumentMigration> migrators = buildComponentMigrators();
    List<Future<Long>> result = getThreadExecutor().invokeAll(migrators);
    try {
      for (Future<Long> nbOfMigratedDocuments : result) {
        totalNumberOfMigratedFiles += nbOfMigratedDocuments.get();
      }
    } finally {
      getThreadExecutor().shutdown();
      repositoryManager.shutdown();
    }
    getConsole().printMessage("Nb of migrated versioned documents : " + totalNumberOfMigratedFiles);
    getConsole().printMessage("*************************************************************");
  }

  private List<VersionedDocumentMigration> buildComponentMigrators() throws SQLException {
    List<VersionedDocumentMigration> result = new ArrayList<VersionedDocumentMigration>(500);
    Statement stmt = null;
    ResultSet rs = null;
    getConsole().printMessage("All components to be migrated : ");
    try {
      stmt = getConnection().createStatement();
      rs = stmt.executeQuery(SELECT_COMPONENTS);
      StringBuilder message = new StringBuilder();
      while (rs.next()) {
        String instanceId = rs.getString("instanceid");
        // some times there are documents that don't belong to any component instance in Silverpeas!
        if (StringUtil.isDefined(instanceId)) {
          result.add(new VersionedDocumentMigration(instanceId, repositoryManager, getConsole()));
          message.append(instanceId).append(" ");
        }
      }
      getConsole().printMessage(message.toString());
      getConsole().printMessage("");
      return result;
    } catch (SQLException sqlex) {
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
  }
}
