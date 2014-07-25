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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.owasp.encoder.Encode;
import org.silverpeas.test.Database;
import org.silverpeas.test.SpringContext;
import org.silverpeas.test.SystemInitializationForTests;
import org.silverpeas.util.Console;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit tests on the migration of the contents of the publications and of the comments updated from
 * 2013/10/07.
 *
 * @author mmoquillon
 */
public class HtmlUnescaperTest {

  @Rule
  public SpringContext context = new SpringContext(this, "/spring-publi-datasource.xml");
  @Rule
  public Database database = new Database(context.getBean(DataSource.class), getDataSet());

  public HtmlUnescaperTest() {
  }

  @BeforeClass
  public static void setUp() throws IOException {
    SystemInitializationForTests.initialize();
  }

  @Test
  public void unescapeSomeEncodedHtmlText() {
    String htmlText = "L'origine de ce livre est à <celui-ci>";
    String encodedText = Encode.forHtml(htmlText);
    String decodedText = StringEscapeUtils.unescapeHtml4(encodedText);
    assertThat(decodedText, is(htmlText));
  }

  @Test
  public void unescapePublicationAndCommentsText() throws Exception {
    HtmlUnescaper unescaper = new HtmlUnescaper();
    unescaper.setConnection(database.getConnection());
    unescaper.setConsole(new Console());

    unescaper.unescapeHtml();

    String text = (String) getActualTable("sb_publication_publi").getValue(1, "pubname");
    assertThat(text, not(containsString("&#39;")));

    text = (String) getActualTable("sb_publication_publi").getValue(1, "pubdescription");
    assertThat(text, not(containsString("&#39;")));

    text = (String) getActualTable("sb_comment_comment").getValue(1, "commentcomment");
    assertThat(text, not(containsString("&#39;")));

    text = (String) getActualTable("sb_comment_comment").getValue(3, "commentcomment");
    assertThat(text, not(containsString("&#39;")));
    assertThat(text, not(containsString("&gt;")));
    assertThat(text, not(containsString("&#lt;")));
  }

  protected ITable getActualTable(String tableName) throws Exception {
    Connection connection = database.openConnection();
    IDatabaseConnection databaseConnection = new DatabaseConnection(connection);
    IDataSet dataSet = databaseConnection.createDataSet();
    ITable table = dataSet.getTable(tableName);
    database.closeConnection(connection);
    return table;
  }

  protected static IDataSet getDataSet() {
    InputStream in = HtmlUnescaperTest.class.getResourceAsStream("publication-dataset.xml");
    try {
      ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSetBuilder().build(in));
      dataSet.addReplacementObject("[NULL]", null);
      return dataSet;
    } catch (Exception e) {
      return null;
    } finally {
      IOUtils.closeQuietly(in);
    }
  }
}
