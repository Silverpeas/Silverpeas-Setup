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
import org.silverpeas.migration.jcr.service.SimpleDocumentService;

public class VersioningMigrator extends DbBuilderDynamicPart {

  private static final ExecutorService executor = Executors.newFixedThreadPool(10);
  private final SimpleDocumentService service;
  public static final String SELECT_COMPONENTS = "SELECT DISTINCT instanceid FROM "
      + "sb_version_document ORDER BY instanceid";

  public VersioningMigrator() {
    this.service = new SimpleDocumentService();
  }

  public void migrateDocuments() throws Exception {
    long totalNumberOfMigratedFiles = 0L;
    List<ComponentDocumentMigrator> migrators = buildComponentMigrators();
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
    getConsole().printMessage("Nb of migrated versioned documents : " + totalNumberOfMigratedFiles);
    this.service.shutdown();
  }

  private List<ComponentDocumentMigrator> buildComponentMigrators() throws SQLException {
    List<ComponentDocumentMigrator> result = new ArrayList<ComponentDocumentMigrator>(500);
    Statement stmt = null;
    ResultSet rs = null;
    getConsole().printMessage("All components to be migrated : ");
    try {
      stmt = getConnection().createStatement();
      rs = stmt.executeQuery(SELECT_COMPONENTS);
      while (rs.next()) {
        result.add(new ComponentDocumentMigrator(rs.getString("instanceid"), service, getConsole()));
        getConsole().printMessage(rs.getString("instanceid"));
        if (!rs.isLast()) {
          getConsole().printMessage(", ");
        }
      }
      getConsole().printMessage("*************************************************************");
      return result;
    } catch (SQLException sqlex) {
      throw sqlex;
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
  }
}
