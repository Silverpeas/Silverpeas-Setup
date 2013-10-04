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
package org.silverpeas.settings.file;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ModifPropertiesTest {

  static final String PROPERTIES_PATH = "org/silverpeas/converter/openoffice.properties";
  static final String EXPECTED_PROPERTIES_PATH =
      "org/silverpeas/converter/expected_openoffice.properties";
  static final String OPENOFFICE_HOST_KEY = "openoffice.host";
  static final String OPENOFFICE_PORT_KEY = "openoffice.port";
  static final String ADDED_ENTRY_KEY = "added.entry";

  public ModifPropertiesTest() {
  }

  /**
   * Test of executeModification method, of class ModifProperties. Test for adding a new property,
   * keeping the properties order and the comments.
   */
  @Test
  public void testExecuteModification() throws Exception {
    String path = this.getClass().getClassLoader().getResource(PROPERTIES_PATH).getPath();
    ModifProperties instance = new ModifProperties(path);
    instance.addModification(new ElementModif(OPENOFFICE_HOST_KEY, "localhost"));
    instance.addModification(new ElementModif(ADDED_ENTRY_KEY, "new entry"));
    instance.executeModification();

    String expectedPath = this.getClass().getClassLoader().getResource(EXPECTED_PROPERTIES_PATH).
        getPath();
    List<String> result = FileUtils.readLines(new File(path), "UTF-8");
    List<String> expectedResult = FileUtils.readLines(new File(expectedPath), "UTF-8");
    assertThat(result, is(expectedResult));

  }

  private Properties loadProperties(String path) throws IOException {
    Properties props = new Properties();
    props.load(getClass().getClassLoader().getResourceAsStream(path));
    return props;
  }
}
