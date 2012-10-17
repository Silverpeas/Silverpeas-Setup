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

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

/**
 * @author ehugonnet
 */
public class FileComparator implements Serializable, Comparator<File> {

  private static final long serialVersionUID = 1l;

  @Override
  public int compare(final File file1, final File file2) {
    int result;
    if (file1 == null && file2 == null) {
      result = 0;
    } else if (file1 == null) {
      result = -10;
    } else if (file2 == null) {
      result = 10;
    } else if (file1.equals(file2)) {
      result = 0;
    } else {
      result = file1.getName().compareTo(file2.getName());
    }
    return result;
  }

}
