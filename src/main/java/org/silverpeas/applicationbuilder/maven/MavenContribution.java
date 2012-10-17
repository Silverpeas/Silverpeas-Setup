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

package org.silverpeas.applicationbuilder.maven;

import org.silverpeas.applicationbuilder.AppBuilderException;
import org.silverpeas.applicationbuilder.ApplicationBuilderItem;
import org.silverpeas.applicationbuilder.ReadOnlyArchive;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Administrateur
 */
public class MavenContribution {

  public static final int TYPE_WAR = 0;
  public static final int TYPE_EJB = 1;
  public static final int TYPE_CLIENT = 2;
  public static final int TYPE_LIB = 3;
  public static final int TYPE_EXTERNAL = 4;
  private int type;
  private transient final StringBuffer packageName = new StringBuffer();
  /** attributes */
  private ReadOnlyArchive client = null;
  private transient final List<ApplicationBuilderItem> ejbs =
      new ArrayList<ApplicationBuilderItem>();
  private ReadOnlyArchive warPart = null;
  private transient final List<ReadOnlyArchive> librairies = new ArrayList<ReadOnlyArchive>();
  private transient final List<ReadOnlyArchive> externals = new ArrayList<ReadOnlyArchive>();

  public MavenContribution(final File contribution, final int type) throws AppBuilderException {
    this.type = type;
    addContribution(contribution, type);
  }

  private void addContribution(final File contribution, final int type) throws AppBuilderException {
    packageName.append(contribution.getAbsolutePath());
    packageName.append(File.pathSeparatorChar).append(' ');
    switch (type) {
      case TYPE_WAR:
        warPart = new ReadOnlyArchive(contribution.getParentFile(), contribution.getName());
        break;
      case TYPE_EJB:
        ejbs.add(new ApplicationBuilderItem(contribution.getParentFile(), contribution.getName()));
        break;
      case TYPE_CLIENT:
        client = new ReadOnlyArchive(contribution.getParentFile(), contribution.getName());
        break;
      case TYPE_LIB:
        librairies.add(new ReadOnlyArchive(contribution.getParentFile(), contribution.getName()));
        break;
      case TYPE_EXTERNAL:
        externals.add(new ReadOnlyArchive(contribution.getParentFile(), contribution.getName()));
        break;
      default:
        break;
    }
  }

  public MavenContribution(final File[] contributions, final int type) throws AppBuilderException {
    for (File contribution : contributions) {
      addContribution(contribution, type);
    }
  }

  /**
   * If no client part is contributed, returns <code>null</code>
   * @return the client archive
   * @roseuid 3AAE586D01D4
   */
  public ReadOnlyArchive getClientPart() {
    return client;
  }

  /**
   * @return the array of contributed EJB archives. <code>null</code> if none
   * @roseuid 3AAE5877025A
   */
  public ApplicationBuilderItem[] getEJBs() {
    return ejbs.toArray(new ApplicationBuilderItem[ejbs.size()]);
  }

  public ReadOnlyArchive getWARPart() {
    return warPart;
  }

  public ReadOnlyArchive[] getLibraries() {
    return librairies.toArray(new ReadOnlyArchive[librairies.size()]);
  }

  public ReadOnlyArchive[] getExternals() {
    return externals.toArray(new ReadOnlyArchive[externals.size()]);
  }

  public String getPackageName() {
    return packageName.toString();
  }

  public String getPackageType() {
    switch (type) {
      case TYPE_WAR:
        return "WAR";
      case TYPE_EJB:
        return "EJB";
      case TYPE_CLIENT:
        return "CLIENT";
      case TYPE_LIB:
        return "LIBRAIRY";
      case TYPE_EXTERNAL:
        return "EXTERNAL";
      default:
        return "";
    }
  }
}
