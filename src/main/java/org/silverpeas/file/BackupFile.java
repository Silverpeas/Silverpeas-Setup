/**
 * Copyright (C) 2000 - 2012 Silverpeas
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
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BackupFile {
  private static final String EXT = ".bak~";
  private File file = null;
  private File[] listFileBackup = null;
  private File firstFileBackup = null;
  private File lastFileBackup = null;
  private int nbFileBackup = 0;
  private int lastNumFileBackup = 0;

  public BackupFile(File pathname) throws Exception {
    file = pathname;
    if (!file.exists()) {
      throw new Exception("file not found : " + file.getAbsolutePath());
    }
    refresh();
  }

  public void makeBackup() throws Exception {
  }

  private void refresh() throws Exception {
    setList();
    setFirst();
    setLast();
  }

  public File[] getListBackup() throws Exception {
    return listFileBackup;
  }

  public File getFirstBackup() throws Exception {
    return firstFileBackup;
  }

  /**
   * @return
   * @throws Exception
   * @see
   */
  public File getLastBackup() throws Exception {
    return lastFileBackup;
  }

  /**
   * @return
   * @throws Exception
   * @see
   */
  public int getNumberBackup() throws Exception {
    return nbFileBackup;
  }

  /**
   * @return
   * @throws Exception
   * @see
   */
  public boolean existBackup() throws Exception {
    return (0 != nbFileBackup);
  }

  /**
   * @throws Exception
   * @see
   */
  private void setList() throws Exception {
    File[] listeAll = file.getParentFile().listFiles();
    if(listeAll == null) {
      listeAll = new File[0];
    }
    List<File> listeGood = new ArrayList<File>();
    for (int i = 0; i < listeAll.length; i++) {
      File tmpFile = listeAll[i];
      if (tmpFile.isFile() && (-1 != tmpFile.getName().indexOf(file.getName()))
          && -1 != tmpFile.getName().indexOf(EXT)) {
        listeGood.add(tmpFile);
      }
    }
    File[] listeF = new File[listeGood.size()];
    for (int i = 0; i < listeF.length; i++) {
      listeF[i] = listeGood.get(i);
    }
    listFileBackup = listeF;
    nbFileBackup = listFileBackup.length;
  }

  /**
   * @throws Exception
   * @see
   */
  private void setFirst() throws Exception {
    File f = null;
    if (this.existBackup()) {
      f = listFileBackup[0];
      for (int i = 0; i < nbFileBackup; i++) {
        if (listFileBackup[i].lastModified() < f.lastModified()) {
          f = listFileBackup[i];
        }
      }
    }
    firstFileBackup = f;
  }

  /**
   * @throws Exception
   * @see
   */
  private void setLast() throws Exception {
    lastFileBackup = null;
    if (this.existBackup()) {
      lastFileBackup = listFileBackup[0];
      for (int i = 0; i < nbFileBackup; i++) {
        if (listFileBackup[i].lastModified() > lastFileBackup.lastModified()) {
          lastFileBackup = listFileBackup[i];
        }
      }
    }
    lastNumFileBackup = -1;
    if (this.existBackup()) {
      String name = lastFileBackup.getName();
      String endName = name.substring(name.lastIndexOf(EXT) + EXT.length(),
          name.length());
      int i = 1;
      while (i <= endName.length()) {
        try {
          lastNumFileBackup = Integer.parseInt(endName.substring(0, i));
          i++;
        } catch (Exception e) {
          i = endName.length() + 2;
        }
      }
    }
  }
}
