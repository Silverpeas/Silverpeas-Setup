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

import org.silverpeas.util.file.FileUtil;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;

public class ModifTextSilverpeas extends ModifFile {

  /**
   * @constructor prend en param�tre le chemin du fichier a modifier
   */
  public ModifTextSilverpeas(String path) throws Exception {
    super.setPath(path);
  }

  /**
   * ajoute des modifications sous forme de tableau de chaine "key=replace"
   */
  public void setModif(String str[]) throws Exception {
    for (int i = 0; i < str.length; i++) {
      this.addModification(str[i]);
    }
  }

  /**
   * analyse une ligne du fichier
   */
  protected void analyseLigne(DataOutput out, String pBuf) throws Exception {

    String tmpStr = pBuf.trim();
    ElementModif em;

    Iterator i = listeModifications.iterator();

    while (i.hasNext()) {
      em = ((ElementModif) i.next());
      int egal = tmpStr.indexOf('=');
      // if (tmpStr.startsWith(em.getSearch())){
      if ((egal != -1)
          && (tmpStr.substring(0, egal).trim().equalsIgnoreCase(em.getSearch()))) {
        if ((egal != -1)
            && (!tmpStr.substring(egal + 1, tmpStr.length()).trim()
            .equalsIgnoreCase(em.getModif()))) {

          if (!isModified) {
            isModified = true;
            BackupFile bf = new BackupFile(new File(path));
            bf.makeBackup();
          }
          tmpStr = em.getSearch() + '=' + em.getModif();
        }
      }
    }
    try {
      out.writeBytes(tmpStr);
      out.writeBytes(System.getProperty("line.separator"));
    } catch (Exception e) {
      throw new Exception("erreur lors de la modification du fichier :" + path);
    }
  }

  /**
   * lance la modification du fichier Attention la modification s'effectue par ligne du fichier
   */
  @Override
  public void executeModification() throws Exception {

    File tmpFile = new File(System.getProperty("java.io.tmpdir"), "ModifText.sh");
    File inFile = new File(path);

    FileInputStream dis = new FileInputStream(path);
    FileOutputStream dos = new FileOutputStream(tmpFile);

    DataInput dataInput = new DataInputStream(dis);
    DataOutput dataOutput = new DataOutputStream(dos);

    String line;
    while ((line = dataInput.readLine()) != null) {
      analyseLigne(dataOutput, line);
    }

    dis.close();
    dos.close();
    inFile.delete();
    FileUtil.copyFile(tmpFile, inFile);
    tmpFile.delete();

    // Specifique Linux/Solaris: on met le fichier de script en executable
    if (path.endsWith(".sh") || path.endsWith(".csh") || path.endsWith(".ksh")) {
      String[] commande = new String[3];
      commande[0] = "/bin/chmod";
      commande[1] = "755";
      commande[2] = path;
      Runtime.getRuntime().exec(commande);
    }
  }
}