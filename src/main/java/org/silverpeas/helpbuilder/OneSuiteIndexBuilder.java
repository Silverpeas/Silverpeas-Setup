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

package org.silverpeas.helpbuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

import org.silverpeas.installedtree.DirectoryLocator;
import org.silverpeas.version.ApplicationInfo;
import org.silverpeas.version.JobBoxInfo;
import org.silverpeas.version.PackageInfo;

/**
 * Titre : Application Builder Description : Copyright : Copyright (c) 2001 Société : Stratélia
 * @author Jean-Christophe Carry
 * @version 1.0
 */

public class OneSuiteIndexBuilder extends TemplateBasedBuilder {

  private static final String suiteName = "suite-silverpeas.htm";

  public OneSuiteIndexBuilder() throws IOException, Exception {
    super(suiteName);
    // writeInDirectory(DirectoryLocator.getHelpHome());
    getTargetContents();
  }

  // surcharge de la methode parente
  public String getTargetContents() throws IOException, Exception {

    if (targetFileContents == null) {

      Iterator iBox = ApplicationInfo.getJobBoxes().iterator();
      Iterator iPackage = null;
      JobBoxInfo boxInfo = null;
      PackageInfo packInfo = null;
      String master = null;
      String tmpBoxes = "";
      String tmpStr = null;

      while (iBox.hasNext()) {
        boxInfo = (JobBoxInfo) iBox.next();
        iPackage = boxInfo.getPackages().iterator();
        String tmpPackages = "";

        tmpStr = getBoxTemplate();

        tmpStr = setTemplateValue(tmpStr, boxNamePlaceHolder, boxInfo.getName());

        while (iPackage.hasNext()) {
          packInfo = (PackageInfo) iPackage.next();
          if (isPackagePertinent(packInfo)) {
            tmpPackages += getPackageContents(packInfo);
          }
        } // while (packages)

        master = getMasterTemplate();

        targetFileContents = setTemplateValue(master, boxesContentsPlaceHolder,
            tmpStr);

        targetFileContents = setTemplateValue(targetFileContents,
            packagesContentsPlaceHolder, tmpPackages);

        // sauvegarde en renommant
        Reader srcText = new StringReader(targetFileContents);
        FileWriter out = new FileWriter(new File(
            DirectoryLocator.getHelpHome(), PackageInfo.specialName(boxInfo
            .getName())
            + "-" + suiteName));
        int charsRead;
        while ((charsRead = srcText.read(data, 0, BUFSIZE)) > 0) {
          out.write(data, 0, charsRead);
        }
        out.close();
        srcText.close();

      } // while (boxes)

    } // if

    return null;
  }

  protected boolean isPackagePertinent(PackageInfo packInfo) throws Exception {
    String packageUrl = packInfo.getName() + File.separator
        + packInfo.getName() + ".htm";
    File packageIndex = new File(DirectoryLocator.getHelpHome(), packageUrl);
    return packageIndex.exists();
  }
}
