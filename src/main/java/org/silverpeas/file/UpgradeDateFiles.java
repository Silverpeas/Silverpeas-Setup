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
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;

public class UpgradeDateFiles {

  private Date dateMaj = null;
  public int nbDocuments = -1;
  public int nbRepertoires = -1;

  /**
   * ---------------------------------------------------------------------
   * @see
   */
  public UpgradeDateFiles() throws Exception {
    dateMaj = new Date();
    nbDocuments = 0;
    nbRepertoires = 0;
  }

  /**
   * met a jour la valeur de la date de mise a jour a l'aide de la string passee en parametre (ex:
   * 10/10/2001/22:12).
   */
  public void setUpgradeDate(String pDate) throws Exception {
    SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy/hh:mm");
    ParsePosition pos = new ParsePosition(0);
    dateMaj = formatDate.parse(pDate, pos);
  }

  /**
   * recupere un clone de la date de mise a jour
   */
  public Date getUpgradeDate() throws Exception {
    return (Date) dateMaj.clone();
  }

  /**
   * fonction : qui met a jour tous les fichiers et repertoire du repertoire passer en parametre
   */
  private void majDirectory(File pRep) throws Exception {
    if (pRep.isDirectory()) {
      nbRepertoires++;
      pRep.setLastModified(dateMaj.getTime());
      File[]  listFiles = pRep.listFiles();
      if(listFiles == null) {
        listFiles = new File[0];
      }
      for (int i = 0; i < listFiles.length; i++) {
        if (listFiles[i].isDirectory()) {
          majDirectory(listFiles[i]);
        } else {
          listFiles[i].setLastModified(dateMaj.getTime());
          nbDocuments++;
        }
      }
    } else {
      throw new IllegalArgumentException(
          "la mise à jour ne concerne pas un repertoire");
    }
  }

  /**
   * methode pour la mise a jour de toute l'arborescence a partir du chemin donne en parametre
   */
  public void upgradeFiles(String pPathName) throws Exception {
    File tmpFile;
    try {
      tmpFile = new File(pPathName);
      tmpFile.setLastModified(dateMaj.getTime());
      if (tmpFile.isDirectory()) {
        majDirectory(tmpFile);
      } else {
        nbDocuments++;
      }
    } catch (Exception ex) {
      throw new Exception(
          "Un problème est apparue lors de la mise à jour d'un fichier nb:"
          + nbRepertoires + ex);
    }
  }
}
