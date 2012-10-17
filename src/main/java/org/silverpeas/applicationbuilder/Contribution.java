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

//Source file: R:\\StraProduct\\Pkg1.0\\Dev\\SrcJava\\Java\\ApplicationBuilder\\JBuilderEnv\\src\\com\\silverpeas\\applicationbuilder\\Contribution.java

package org.silverpeas.applicationbuilder;

import java.io.File;
import java.util.Arrays;

import org.silverpeas.installedtree.DirectoryLocator;

/**
 * Uses one contribution file to provide the elements that can be included directly in the target
 * structure (EAR and Client).
 * @author Silverpeas
 * @version 1.0
 * @since 1.0
 */
public class Contribution extends XmlDocument implements Comparable {

  /** root tag */
  private static final String CONTRIBUTION_TAG = "contribution";

  /** root attributes */
  private static final String PRODUCT_ATTRIB = "product";
  private static final String TYPE_ATTRIB = "type";
  private static final String VERSION_ATTRIB = "version";

  /** root children */
  private static final String WEB_APP_TAG = "web-application";
  private static final String EJB_TAG = "ejb";
  private static final String CLIENT_TAG = "client";
  private static final String LIB_TAG = "library";

  /** attributes */
  private ReadOnlyArchive theClientPart = null;
  private ApplicationBuilderItem[] theEJBs = null;
  private ReadOnlyArchive theWARPart = null;
  private ReadOnlyArchive[] theLibrairies = null;

  private String packageType = null;

  public Contribution(File contributionHome, String name)
      throws AppBuilderException {
    super(contributionHome, name);
    this.load();

    boolean errorFound = false;

    try {
      setClientPart();
    } catch (AppBuilderException abe) {
      errorFound = true;
    }
    try {
      setEJBs();
    } catch (AppBuilderException abe) {
      errorFound = true;
    }
    try {
      setLibraries();
    } catch (AppBuilderException abe) {
      errorFound = true;
    }
    try {
      setWARPart();
    } catch (AppBuilderException abe) {
      errorFound = true;
    }
    if (errorFound) {
      throw new AppBuilderException("ERRORS related to \"" + getName()
          + "\" contribution file");
    }
  }

  /**
   * Private setter for "theClientPart" attribute. Retrieves the client part archive name in the
   * contribution descriptor and wraps it as a ReadOnlyArchive.
   * @throw AppBuilderException if there is more than one client part in the descriptor
   */
  private void setClientPart() throws AppBuilderException {
    String[] values = this.getTagValues(CLIENT_TAG);
    if (values == null) {
      theClientPart = null;
      return;
    }
    if (values.length > 1) {
      Log.add(getName() + " : \"" + CLIENT_TAG + "\" tag must be unique");
      throw new AppBuilderException();
    }
    try {
      theClientPart = new ReadOnlyArchive(new File(DirectoryLocator
          .getClientContribHome()), values[0]);
    } catch (AppBuilderException abe) {
      Log.add(abe);
      throw new AppBuilderException();
    }
  }

  /**
   * If no client part is contributed, returns <code>null</code>
   * @return the client archive
   * @roseuid 3AAE586D01D4
   */
  public ReadOnlyArchive getClientPart() {
    return theClientPart;
  }

  /**
   * private setter for "theEJBs" attribute
   */
  private void setEJBs() throws AppBuilderException {
    String names[] = getTagValues(EJB_TAG);
    if (names == null) {
      theEJBs = null;
      return;
    }
    theEJBs = new ApplicationBuilderItem[names.length];
    boolean errorFound = false;
    for (int i = 0; i < names.length; i++) {
      theEJBs[i] = new ApplicationBuilderItem(new File(DirectoryLocator
          .getEjbContribHome()), names[i]);
      if (!theEJBs[i].getPath().exists() || !theEJBs[i].getPath().canRead()) {
        Log.add("\"" + theEJBs[i].getPath().getAbsolutePath()
            + "\" not found or not readable");
        errorFound = true;
      }
    }
    if (errorFound) {
      theEJBs = null;
      throw new AppBuilderException();
    }
  }

  /**
   * @return the array of contributed EJB archives. <code>null</code> if none
   * @roseuid 3AAE5877025A
   */
  public ApplicationBuilderItem[] getEJBs() {
    return theEJBs;
  }

  /**
   * Private setter for <code>theWARPart</code> attribute. Instantiate a
   * <code>ReadOnlyArchive</code> object with the contribution descriptor information.
   */
  private void setWARPart() throws AppBuilderException {
    String[] values = this.getTagValues(WEB_APP_TAG);
    if (values == null) {
      theWARPart = null;
      return;
    }
    if (values.length > 1) {
      Log.add(getName() + " : found more than one \"" + WEB_APP_TAG + "\" tag");
      throw new AppBuilderException();
    }
    try {
      theWARPart = new ReadOnlyArchive(new File(DirectoryLocator
          .getWarContribHome()), values[0]);
    } catch (AppBuilderException abe) {
      Log.add(abe);
      throw new AppBuilderException();
    }
  }

  /**
   * @roseuid 3AAE585D028F
   */
  public ReadOnlyArchive getWARPart() {
    return theWARPart;
  }

  /**
	 */
  private void setLibraries() throws AppBuilderException {
    String names[] = getTagValues(LIB_TAG);
    if (names == null) {
      theLibrairies = null;
      return;
    }
    theLibrairies = new ReadOnlyArchive[names.length];
    ReadOnlyArchive oneLib = null;
    boolean errorFound = false;
    for (int i = 0; i < names.length; i++) {
      try {
        theLibrairies[i] = new ReadOnlyArchive(new File(DirectoryLocator
            .getLibContribHome()), names[i]);
      } catch (AppBuilderException abe) {
        Log.add(abe);
        errorFound = true;
      } // if
    } // for
    if (errorFound) {
      theLibrairies = null;
      throw new AppBuilderException();
    }
  }

  /**
   * @roseuid 3AAE588900F8
   */
  public ReadOnlyArchive[] getLibraries() {
    return theLibrairies;
  }

  /**
   * @return the name of the product described by this contribution
   * @roseuid 3AB0A1A4016E
   */
  public String getPackageName() {
    return getAttributeValues(PRODUCT_ATTRIB)[0];
  }

  /**
   * @return the type of the product described by this contribution. Currently, there are two types
   * : 'component' and 'applicativebus'.
   * @roseuid 3AB0A23E01C0
   */
  public String getPackageType() {
    if (packageType == null) {
      packageType = getAttributeValues(TYPE_ATTRIB)[0];
    }
    return packageType;
  }

  public boolean isApplicativeBusPackage() {
    // return getPackageType().trim().toLowerCase().indexOf("bus") != -1;
    return getPackageType().trim().equalsIgnoreCase("bus");
  }

  /**
   * @return the version of the product described by this contribution
   * @roseuid 3AB0A2F40302
   */
  public String getPackageVersion() {
    return this.getAttributeValues(VERSION_ATTRIB)[0];
  }

  /**
   * Comparable interface
   */
  public int compareTo(Object o) {
    if (equals(o)) {
      return 0;
    }
    // same priorities => compare names
    // same priorities <=> both have a priority and the priorities are equal
    // or both have no priority.
    if (getPriority().equals(((Contribution) o).getPriority())) {
      return getName().compareTo(((Contribution) o).getName());
    }
    // priorities not equal => compare priorities
    return -getPriority().compareTo(((Contribution) o).getPriority());
  }

  /**
   * Needed to avoid weird behaviour with sorted collections
   * @return True if and only if priorities are equal and names are equal Since priority is
   * contained in name, True if and only if names are equal
   */
  public boolean equals(Object o) {
    return ((Contribution) o).getName().equals(getName());
  }

  // for compareTo and equals
  private Integer priority = null;

  private Integer getPriority() {
    if (priority == null) {
      boolean intFound = false;
      char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
      Arrays.sort(digits);
      int iChar = 0;
      do {
        if (Arrays.binarySearch(digits, getName().charAt(iChar)) > 0) {
          iChar++;
          intFound = true;
        } else {
          break;
        }
      } while (iChar < getName().length());
      if (intFound) {
        priority = new Integer(getName().substring(0, iChar));
      } else {
        priority = new Integer(-1);
      }
    }
    return priority;
  }
}
