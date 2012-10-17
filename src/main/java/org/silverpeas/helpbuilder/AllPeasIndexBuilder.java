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
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.silverpeas.installedtree.DirectoryLocator;
import org.silverpeas.version.ApplicationInfo;
import org.silverpeas.version.JobBoxInfo;
import org.silverpeas.version.PackageInfo;

public class AllPeasIndexBuilder extends TemplateBasedBuilder {

  private static final String allpeasName = "allpeas-silverpeas.htm";

  public AllPeasIndexBuilder() throws IOException, Exception {
    super(allpeasName);
    writeInDirectory(DirectoryLocator.getHelpHome());
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

      // Liste des packages eligibles pour la doc classes par ordre alphabetique
      // du ShortName
      Vector v = new Vector();

      while (iBox.hasNext()) {
        boxInfo = (JobBoxInfo) iBox.next();
        iPackage = boxInfo.getPackages().iterator();

        while (iPackage.hasNext()) {
          packInfo = (PackageInfo) iPackage.next();
          if (isPackagePertinent(packInfo)) {
            String shortName = packInfo.getShortName();
            int j = 0;
            String csn = "";
            for (int i = 0; i < v.size(); i++) {
              csn = ((PackageInfo) v.get(i)).getShortName();
              if (csn.compareTo(shortName) > 0)
                break;
              else
                j++;
            } // for
            v.add(j, packInfo);
          } // if
        } // while
      } // while

      String tmpPackages = "";
      for (int i = 0; i < v.size(); i++)
        tmpPackages += getPackageContents((PackageInfo) v.get(i));

      if (!tmpPackages.trim().equals("")) {
        tmpStr = getBoxTemplate();
        tmpStr = setTemplateValue(tmpStr, boxNamePlaceHolder, boxInfo.getName());
        tmpStr = setTemplateValue(tmpStr, packagesContentsPlaceHolder,
            tmpPackages);
        tmpBoxes += tmpStr;
      }

      master = getMasterTemplate();

      targetFileContents = setTemplateValue(master, boxesContentsPlaceHolder,
          tmpBoxes);
    } // if

    return targetFileContents;
  }

  protected boolean isPackagePertinent(PackageInfo packInfo) throws Exception {
    String packageUrl = packInfo.getName() + File.separator
        + packInfo.getName() + ".htm";
    File packageIndex = new File(DirectoryLocator.getHelpHome(), packageUrl);
    return packageIndex.exists();
  }
}
