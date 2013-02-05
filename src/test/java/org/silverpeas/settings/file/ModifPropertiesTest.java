/*
 * Copyright (C) 2000 - 2012 Silverpeas
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
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ModifPropertiesTest {

  public ModifPropertiesTest() {
  }

  /**
   * Test of executeModification method, of class ModifProperties.
   * Test for adding a new property, keeping the propertyies order and the comments.
   */
  @Test
  public void testExecuteModification() throws Exception {
    String path = this.getClass().getClassLoader().getResource(
        "org/silverpeas/converter/openoffice.properties").getPath();
    ModifProperties instance = new ModifProperties(path);
    instance.addModification(new ElementModif("openoffice.host", "localhost"));
    instance.addModification(new ElementModif("added.entry", "new entry"));
    instance.executeModification();
    String result = FileUtils.readFileToString(new File(path));
    String expectedResult = FileUtils.readFileToString(new File(this.getClass().getClassLoader().
        getResource("org/silverpeas/converter/expected_openoffice.properties").getPath()));
    assertThat(result, is(expectedResult));

  }
}
