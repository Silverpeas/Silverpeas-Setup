/**
 * Copyright (C) 2000 - 2012 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.settings.file;

import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ehugonnet
 */
public class ModifTextTest {

  private static String base = System.getProperty("basedir");

  @Test
  public void testModificationWithRegexp() throws Exception {
    File testFile = new File(base + File.separatorChar + "target" + File.separatorChar + "test-classes" +
         File.separatorChar + "run.sh");
    List<String>  lines = FileUtils.readLines(testFile);
    assertNotNull(lines);
    assertEquals(27, lines.size());
    assertEquals("set JAVA_OPTS=%JAVA_OPTS% -Xms128m -Xmx256m", lines.get(26));
    ModifText mp = new ModifText(testFile.getPath());
    mp.createFileBak(null);
    RegexpElementMotif elt = new RegexpElementMotif("-Xms[0-9]+m");
    elt.setRemplacement("-Xms512m");
    mp.addModification(elt);
    mp.executeModification();
    lines = FileUtils.readLines(testFile);
    assertNotNull(lines);
    assertEquals(27, lines.size());
    assertEquals("set JAVA_OPTS=%JAVA_OPTS% -Xms512m -Xmx256m", lines.get(26));

  }

  @Test
  public void testDoubleModificationWithRegexp() throws Exception {
    File testFile = new File(base + File.separatorChar + "target" + File.separatorChar + "test-classes" +
         File.separatorChar + "run.bat");
    ModifText mp = new ModifText(testFile.getPath());
    mp.createFileBak(null);
    RegexpElementMotif elt1 = new RegexpElementMotif("-Xms[0-9]+m");
    elt1.setRemplacement("-Xms512m");
    RegexpElementMotif elt2 = new RegexpElementMotif("-Xmx[0-9]+m");
    elt2.setRemplacement("-Xmx512m");
    mp.addModification(elt1);
    mp.addModification(elt2);
    mp.executeModification();
    List<String> lines = FileUtils.readLines(testFile);
    assertNotNull(lines);
    assertEquals(27, lines.size());
    assertEquals("set JAVA_OPTS=%JAVA_OPTS% -Xms512m -Xmx512m", lines.get(26));

  }

  @Test
  public void testModificationWithRegexpMultimatch() throws Exception {
    File testFile = new File(base + File.separatorChar + "target" + File.separatorChar + "test-classes" +
         File.separatorChar + "run.txt");
    ModifText mp = new ModifText(testFile.getPath());
    mp.createFileBak(null);
    RegexpElementMotif elt1 = new RegexpElementMotif("-Xms[0-9]+m");
    elt1.setRemplacement("-Xms512m");
    RegexpElementMotif elt2 = new RegexpElementMotif("-Xmx[0-9]+m");
    elt2.setRemplacement("-Xmx512m");
    mp.addModification(elt1);
    mp.addModification(elt2);
    mp.executeModification();
    List<String>  lines = FileUtils.readLines(testFile);
    assertNotNull(lines);
    assertEquals(27, lines.size());
    assertEquals("set JAVA_OPTS=%JAVA_OPTS% -Xms512m -Xmx512m -Xms512m", lines.get(26));

  }
}
