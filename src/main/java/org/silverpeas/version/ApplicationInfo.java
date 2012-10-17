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

package org.silverpeas.version;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.silverpeas.installedtree.DirectoryLocator;

/**
 * Titre : Application Builder Description : Copyright : Copyright (c) 2001 Société : Stratélia
 * @author Jean-Christophe Carry
 * @version 1.0
 */

public class ApplicationInfo {

  private static final String homeDirectoryKey = "silverpeas.home";
  private static final String applicationSubdir = "silverpeas";
  private static final String versionFileSuffix = "-version.properties";
  private static final String busVersionName = "applicativeBus";

  /**
   * Classe implémentant l'interface java.io.FilenameFilter et permettant de récupérer la liste des
   * fichiers correspondant au masque
   */
  private static class VersionFilter implements FilenameFilter {
    public VersionFilter() {
    }

    public boolean accept(java.io.File dir, String name) {
      return name.toLowerCase().endsWith(versionFileSuffix.toLowerCase());
    }
  } // VersionFilter

  private static Map thePackages = null;

  public static Map getPackages() throws IOException {
    if (thePackages == null) {
      File versionDirectory = new File(DirectoryLocator.getVersionHome());
      String[] versionNames = versionDirectory.list(new VersionFilter());
      thePackages = new TreeMap();
      PackageInfo aPackage = null;
      for (int i = 0; i < versionNames.length; i++) {
        aPackage = new PackageInfo(new File(versionDirectory, versionNames[i]));
        thePackages.put(aPackage.getName(), aPackage);
      }
    }
    return thePackages;
  }

  public static Set getJobBoxes() throws IOException {
    Iterator iPackage = getPackages().keySet().iterator();
    Set jobBoxesNames = new TreeSet();
    PackageInfo info = null;
    while (iPackage.hasNext()) {
      info = (PackageInfo) getPackages().get(iPackage.next());
      jobBoxesNames.add(info.getBoxName());
    }
    Iterator iBox = jobBoxesNames.iterator();
    Set jobBoxes = new TreeSet();
    while (iBox.hasNext()) {
      jobBoxes.add(new JobBoxInfo((String) iBox.next(), getPackages()));
    }
    return jobBoxes;
  }

  public static PackageInfo getBusInfo() throws IOException {
    return (PackageInfo) getPackages().get(busVersionName);
  }
}