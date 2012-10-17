/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.applicationbuilder.maven;

import org.silverpeas.applicationbuilder.maven.FileComparator;
import java.util.Collections;
import java.util.List;
import java.io.File;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ehugonnet
 */
public class FileComparatorTest {

  public FileComparatorTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of compare method, of class FileComparator.
   */
  @Test
  public void testCompareWithNullFiles() {
    File file1 = null;
    File file2 = null;
    FileComparator instance = new FileComparator();
    int expResult = 0;
    int result = instance.compare(file1, file2);
    assertEquals(expResult, result);
    file2 = new File("toto.txt");
    result = instance.compare(file1, file2);
    expResult = -10;
    assertEquals(expResult, result);
    file1 = new File("toto.txt");
    file2 = null;
    result = instance.compare(file1, file2);
    expResult = 10;
    assertEquals(expResult, result);
  }



  @Test
  public void testSortList() {
    List<File> files = new ArrayList<File>(5);
    File file1 = new File("Antoine.txt");
    files.add(file1);
    File file2 = new File("antoine.txt");
    files.add(file2);
    File file3 = new File("Xavier.txt");
    files.add(file3);
    File file4 = new File("benoit.txt");
    files.add(file4);
    FileComparator comparator = new FileComparator();
    Collections.sort(files, comparator);
    assertEquals("Antoine.txt", files.get(0).getName());
    assertEquals("Xavier.txt", files.get(1).getName());
    assertEquals("antoine.txt", files.get(2).getName());
    assertEquals("benoit.txt", files.get(3).getName());
    
  }
}
