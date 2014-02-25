/*
 * Copyright (C) 2000-2014 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Writer Free/Libre
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
package org.silverpeas.migration.publication;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

/**
 * Migration of data in publications and in comments containing HTML entities to their original
 * representations.
 *
 * @author mmoquillon
 */
public class HtmlUnescaper extends DbBuilderDynamicPart {

  private static final String PUBLICATION_SELECTION
      = "select pubId, pubName, pubDescription from sb_publication_publi where pubUpdateDate > '2013/10/07'";
  private static final String PUBLICATION_UPDATE
      = "update sb_publication_publi set pubName = ?, pubDescription = ? where pubId = ?";
  private static final String COMMENT_SELECTION
      = "select commentId, commentComment from sb_comment_comment where commentModificationDate > '2013/10/07'";
  private static final String COMMENT_UPDATE
      = "update sb_comment_comment set commentComment = ? where commentId = ?";

  public void unescapeHtml() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    // use futures to block at the tasks termination to ensure no other sql executions are
    // interleaved within the execution of these tasks.
    List<Future<Void>> futures = executor.invokeAll(Arrays.asList(unescapePublications(),
        unescapeComments()));
    for (Future<Void> future : futures) {
      future.get();
    }
    executor.shutdown();
  }

  private Callable<Void> unescapePublications() throws Exception {
    return new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        getConsole().printMessage("Update of the publications after 07 october 2013");
        PreparedStatement selector = null;
        PreparedStatement updater = null;
        ResultSet rs = null;
        try {
          selector = getConnection().prepareStatement(PUBLICATION_SELECTION);
          updater = getConnection().prepareStatement(PUBLICATION_UPDATE);
          rs = selector.executeQuery();
          while (rs.next()) {
            int id = rs.getInt("pubId");
            String name = unescapeHtml4(rs.getString("pubName"));
            String description = unescapeHtml4(rs.getString("pubDescription"));
            updater.setString(1, name);
            updater.setString(2, description);
            updater.setInt(3, id);
            if (updater.executeUpdate() == 0) {
              getConsole().printWarning("Update of the publication " + id + " does nothing!");
            }
          }
        } finally {
          if (rs != null) {
            rs.close();
          }
          if (selector != null) {
            selector.close();
          }
          if (updater != null) {
            updater.close();
          }
        }
        return null;
      }
    };
  }

  private Callable<Void> unescapeComments() throws SQLException {
    return new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        getConsole().printMessage("Update of the comments after 07 october 2013");
        PreparedStatement selector = null;
        PreparedStatement updater = null;
        ResultSet rs = null;
        try {
          selector = getConnection().prepareStatement(COMMENT_SELECTION);
          updater = getConnection().prepareStatement(COMMENT_UPDATE);
          rs = selector.executeQuery();
          while (rs.next()) {
            int id = rs.getInt("commentId");
            String content = unescapeHtml4(rs.getString("commentComment"));
            updater.setString(1, content);
            updater.setInt(2, id);
            if (updater.executeUpdate() == 0) {
              getConsole().printWarning("Update of the comment " + id + " does nothing!");
            }
          }
        } finally {
          if (rs != null) {
            rs.close();
          }
          if (selector != null) {
            selector.close();
          }
          if (updater != null) {
            updater.close();
          }
        }
        return null;
      }
    };
  }
}
