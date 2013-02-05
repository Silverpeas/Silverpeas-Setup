/*
 *  Copyright (C) 2000 - 2012 Silverpeas
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  As a special exception to the terms and conditions of version 3.0 of
 *  the GPL, you may redistribute this Program in connection with Free/Libre
 *  Open Source Software ("FLOSS") applications as described in Silverpeas's
 *  FLOSS exception.  You should have recieved a copy of the text describing
 *  the FLOSS exception, and it is also available here:
 *  "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.silverpeas.migration.questionreply;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;
import org.silverpeas.util.file.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.io.File.separatorChar;

/**
 * @author ehugonnet
 */
public class ReplyContentToWysiwygMigration extends DbBuilderDynamicPart {

  public static final String QUERY = "SELECT sc_questionreply_question.instanceid AS instanceid, "
      + "sc_questionreply_reply.id AS id, sc_questionreply_reply.content AS content FROM "
      + "sc_questionreply_question, sc_questionreply_reply WHERE "
      + "sc_questionreply_question.id = sc_questionreply_reply.questionid";

  public ReplyContentToWysiwygMigration() {
  }

  public void migrateReplyContentToWysiwyg() throws Exception {
    Properties props = FileUtil.loadResource("/org/silverpeas/general.properties");
    String upLoadPath = props.getProperty("uploadsPath");
    List<ReplyContent> contents = getContent();
    for (ReplyContent content : contents) {
      migrate(content, upLoadPath);
    }
  }

  public List<ReplyContent> getContent() throws Exception {
    List<ReplyContent> result = new ArrayList<ReplyContent>();
    Connection connection = this.getConnection();
    Statement stmt = connection.createStatement();
    ResultSet rs = null;
    try {
      rs = stmt.executeQuery(QUERY);
      while (rs.next()) {
        result.add(new ReplyContent(rs.getInt("id"), rs.getString("instanceid"), rs.getString(
            "content")));
      }
      return result;
    } catch (SQLException ex) {
      throw new Exception("Error during content migration for QuestionReply : " + ex.getMessage());
    } finally {
      if (rs != null) {
        rs.close();
      }
      stmt.close();
    }
  }

  void migrate(ReplyContent content, String upLoadPath) {
    File targetDir = new File(upLoadPath + separatorChar + content.getInstanceId()
        + separatorChar + "Attachment" + separatorChar + "wysiwyg" + separatorChar);
    InputStream in = null;
    try {
      targetDir.mkdirs();
      in = new ByteArrayInputStream(content.getContent().getBytes("UTF-8"));
      FileUtils.copyInputStreamToFile(in, new File(targetDir, content.getId() + "wysiwyg.txt"));
    } catch (IOException ioex) {
      if (in != null) {
        IOUtils.closeQuietly(in);
      }
    }
  }
}