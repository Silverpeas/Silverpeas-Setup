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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.silverpeas.applicationbuilder.maven;

import org.silverpeas.applicationbuilder.maven.ArchiveFilenameFilter;
import junit.framework.TestCase;

/**
 *
 * @author Administrateur
 */
public class ArchiveFilenameFilterTest extends TestCase {
    
    public ArchiveFilenameFilterTest(String testName) {
        super(testName);
    }

    public void testAccept() throws Exception {
      ArchiveFilenameFilter filter = new ArchiveFilenameFilter();
      assertTrue(filter.accept(null, "toto.war"));
      assertTrue(filter.accept(null, "toto.WAR"));
      assertTrue(filter.accept(null, "toto.War"));
      assertTrue(filter.accept(null, "toto.wAr"));
      assertTrue(filter.accept(null, "toto.waR"));
      assertTrue(filter.accept(null, "toto.jar"));
      assertTrue(filter.accept(null, "toto.csqdf.WAR"));

      assertFalse(filter.accept(null, "toto.waj"));
      assertFalse(filter.accept(null, "toto.war.titi"));

    }

}
