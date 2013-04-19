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
package org.silverpeas.dbbuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.CharEncoding;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.silverpeas.dbbuilder.DBBuilderItem.CURRENT_TAG;
import static org.silverpeas.dbbuilder.DBBuilderItem.PREVIOUS_TAG;

/**
 *
 * @author ehugonnet
 */
public class DBBuilderFileItemTest {

  public DBBuilderFileItemTest() {
  }

  /**
   * Test of getVersionFromFile method, of class DBBuilderFileItem.
   */
  @Test
  public void testGetVersionFromFile() throws Exception {
    DBXmlDocument doc = new DBXmlDocument();
    doc.loadFrom(this.getClass().getClassLoader().getResourceAsStream(
        "contribution/versioning-contribution.xml"));
    DBBuilderFileItem instance = new DBBuilderFileItem(doc);
    String expResult = "012";
    String result = instance.getVersionFromFile();
    assertThat(result, is(expResult));
  }

  @Test
  public void testMergeInstallation() throws Exception {
    DBXmlDocument destXml = new DBXmlDocument();
    ByteArrayInputStream input = new ByteArrayInputStream(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<allcontributions>\n</allcontributions>\n"
        .getBytes(Charsets.UTF_8));
    destXml.loadFrom(input);
    destXml.setOutputEncoding(CharEncoding.UTF_8);
    DBXmlDocument doc = new DBXmlDocument();
    doc.loadFrom(this.getClass().getClassLoader().getResourceAsStream(
        "contribution/versioning-contribution.xml"));
    DBBuilderFileItem instance = new DBBuilderFileItem(doc);
    destXml.mergeWith(instance, DBBuilder.TAGS_TO_MERGE_4_INSTALL, Collections.singletonList(
        new VersionTag(
        CURRENT_TAG, instance.getVersionFromFile())));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    destXml.saveTo(out);
    assertThat(out.toString(CharEncoding.UTF_8), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
        + "<allcontributions>\r\n"
        + "    <module id=\"versioning\" version=\"012\">\r\n"
        + "        <create_table>\r\n"
        + "            <file name=\"postgres\\versioning\\012\\create_table.sql\" type=\"sqlstatementlist\" delimiter=\";\" keepdelimiter=\"NO\" />\r\n"
        + "        </create_table>\r\n"
        + "    </module>\r\n"
        + "</allcontributions>\r\n\r\n"));
  }

  @Test
  public void testMergeUpdate() throws Exception {
    DBXmlDocument destXml = new DBXmlDocument();
    ByteArrayInputStream input = new ByteArrayInputStream(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<allcontributions>\n</allcontributions>\n"
        .getBytes(Charsets.UTF_8));
    destXml.loadFrom(input);
    destXml.setOutputEncoding(CharEncoding.UTF_8);
    DBXmlDocument doc = new DBXmlDocument();
    doc.loadFrom(this.getClass().getClassLoader().getResourceAsStream(
        "contribution/versioning-contribution.xml"));
    DBBuilderFileItem instance = new DBBuilderFileItem(doc);
    List<VersionTag> blocks_merge = new ArrayList<VersionTag>();
    int iversionDB = 9;
    int versionFile = Integer.parseInt(instance.getVersionFromFile());
    for (int i = 0; i < versionFile - iversionDB; i++) {
      String sversionFile = "000" + (iversionDB + i);
      sversionFile = sversionFile.substring(sversionFile.length() - 3);
      blocks_merge.add(new VersionTag(PREVIOUS_TAG, sversionFile));
    }
    destXml.mergeWith(instance, DBBuilder.TAGS_TO_MERGE_4_INSTALL, blocks_merge);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    destXml.saveTo(out);
    assertThat(out.toString(CharEncoding.UTF_8), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
        + "<allcontributions>\r\n"
        + "    <module id=\"versioning\" version=\"010\">\r\n"
        + "        <create_table>\r\n"
        + "            <file name=\"postgres\\versioning\\up009\\alter_table.sql\" type=\"sqlstatementlist\" delimiter=\";\" keepdelimiter=\"NO\" />\r\n"
        + "        </create_table>\r\n"
        + "    </module>\r\n"
         + "    <module id=\"versioning\" version=\"011\">\r\n"
        + "        <create_table>\r\n"
        + "            <file name=\"postgres\\versioning\\up010\\create_table.sql\" type=\"sqlstatementlist\" delimiter=\";\" keepdelimiter=\"NO\" />\r\n"
        + "        </create_table>\r\n"
        + "    </module>\r\n"
         + "    <module id=\"versioning\" version=\"012\">\r\n"
        + "        <create_table>\r\n"
        + "            <file name=\"VersioningMigrator.jar\" type=\"javalib\" classname=\"org.silverpeas.migration.jcr.version.VersioningMigrator\" methodname=\"migrateDocuments\" />\r\n"
        + "        </create_table>\r\n"
        + "    </module>\r\n"
        + "</allcontributions>\r\n\r\n"));
  }
}
