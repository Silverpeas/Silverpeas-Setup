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

package org.silverpeas.applicationbuilder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.silverpeas.installedtree.DirectoryLocator;

/**
 * @todo vérifier l'existence des répertoires attendus avant de laisser planter X fois pour les
 * fichiers que l'on cherche dedans
 */
public class Repository {

  /**
   * Classe implémentant l'interface java.io.FilenameFilter et permettant de récupérer la liste des
   * fichiers correspondant au masque
   */
  private class ContributionFilter implements FilenameFilter {
    private String contributionFileSuffix = "-contribution.xml";

    public ContributionFilter() {
    }

    public boolean accept(java.io.File dir, String name) {
      return name.toLowerCase().endsWith(contributionFileSuffix.toLowerCase());
    }
  } // ContributionFilter

  private Contribution[] theContributions = null;
  private List theBusContributions = null;
  private List thePeasContributions = null;
  private String appServerSpecificSubdir = null;

  public Repository() throws AppBuilderException {
    setContributions();
  }

  /**
   * @roseuid 3AAF977E0370 Renvoie un tableau pour chaque fichier de contribution présent ds le
   * répertoire "repository\data".
   */
  public Contribution[] getContributions() {
    return theContributions;
  }

  public List getBusContributions() {
    return theBusContributions;
  }

  public List getPeasContributions() {
    return thePeasContributions;
  }

  /**
   * sets the list of contribution files
   */
  private void setContributions() throws AppBuilderException {
    boolean errorFound = false;

    File contribDir = new File(DirectoryLocator.getContribFilesHome());
    String[] contributionNames = contribDir.list(new ContributionFilter());
    if (contributionNames == null)
      contributionNames = new String[0];
    theContributions = new Contribution[contributionNames.length];
    thePeasContributions = new ArrayList();
    theBusContributions = new ArrayList();
    int errorCount = 0;
    for (int i = 0; i < contributionNames.length; i++) {
      try {
        theContributions[i] = new Contribution(contribDir, contributionNames[i]);
        if (theContributions[i].isApplicativeBusPackage()) {
          theBusContributions.add(theContributions[i]);
        } else {
          thePeasContributions.add(theContributions[i]);
        }
      } catch (AppBuilderException abe) {
        Log.add(abe);
        errorCount++;
        errorFound = true;
      }
    }
    if (errorFound) {
      throw new AppBuilderException("found errors related to "
          + Integer.toString(errorCount) + " contribution files");
    }
    Arrays.sort(theContributions);
  }

}