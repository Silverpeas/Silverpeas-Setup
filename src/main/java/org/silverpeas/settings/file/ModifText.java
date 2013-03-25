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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModifText extends ModifFile {

  /**
   * @constructor prend en param�tre le chemin du fichier a modifier
   */
  public ModifText(String path) throws Exception {
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
  protected String analyseLigne(String line) {
    String tmpLine = line;
    for (ElementModif modif : listeModifications) {
      tmpLine = analyseLine(tmpLine, modif);
    }
    if (tmpLine != null && tmpLine.isEmpty()) {
      tmpLine = line;
    }
    return tmpLine;
  }

  protected String analyseLine(String line, ElementModif modification) {
    StringBuffer resStr = new StringBuffer();
    if (modification instanceof RegexpElementMotif) {
      RegexpElementMotif emv = (RegexpElementMotif) modification;
      Pattern pattern = emv.getPattern();
      Matcher match = pattern.matcher(line);
      while (match.find()) {
        match.appendReplacement(resStr, emv.getRemplacement());
      }
      match.appendTail(resStr);
    } else {
      // remplace uniquement la derniere occurence de chaque ligne
      int res = line.lastIndexOf(modification.getSearch());
      if (res != -1) {
        resStr.append(line.substring(0, res)).append(modification.getModif()).append(
            line.substring((res + modification.getSearch().length()), line.length()));
      }
    }
    return resStr.toString().trim();
  }

  /**
   * lance la modification du fichier Attention la modification s'effectue par ligne du fichier
   */
  @Override
  public void executeModification() throws Exception {
    File inFile = new File(path);
    List<String> lines = FileUtils.readLines(inFile);
    List<String> newLines = new ArrayList<String>(lines.size());
    for (String line : lines) {
      newLines.add(analyseLigne(line));
    }
    FileUtils.writeLines(inFile, newLines);
    // Specifique Linux/Solaris: on met le fichier de script en executable
    if ((path.endsWith(".sh") || path.endsWith(".csh") || path.endsWith(".ksh"))
        && IOUtils.DIR_SEPARATOR_UNIX == File.separatorChar) {
      String[] commande = new String[3];
      commande[0] = "/bin/chmod";
      commande[1] = "755";
      commande[2] = path;
      Runtime.getRuntime().exec(commande);
    }
  }
}
