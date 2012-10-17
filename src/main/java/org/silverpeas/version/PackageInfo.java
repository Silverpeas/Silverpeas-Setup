/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.version;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.PropertyResourceBundle;

public class PackageInfo implements Comparable<PackageInfo> {

  // expected property names
  private static final String packageKey = "Package";
  private static final String nameKey = "Name";
  private static final String shortnameKey = "ShortName";
  private static final String boxKey = "Box";
  private static final String versionKey = "Version";
  private static final String releaseKey = "Release";
  private static final String buildKey = "Build";
  private static final String keyKey = "Key";
  private PropertyResourceBundle infos = null;

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PackageInfo)) {
      return false;
    }
    return getDisplayNameIfAvailable().equals(
        ((PackageInfo) o).getDisplayNameIfAvailable());
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash =
        71 * hash + (this.getDisplayNameIfAvailable() != null ? this.getDisplayNameIfAvailable().
        hashCode() : 0);
    return hash;
  }

  @Override
  public int compareTo(PackageInfo o) {
    return getDisplayNameIfAvailable().compareTo(o.getDisplayNameIfAvailable());
  }

  public PackageInfo(File propertiesFile) throws MalformedURLException,
      IOException {
    infos = new PropertyResourceBundle(propertiesFile.toURI().toURL().openStream());
  }

  public String getName() {
    return infos.getString(packageKey).trim();
  }

  public String getShortName() {
    return infos.getString(shortnameKey).trim();
  }

  public String getDisplayName() {
    return infos.getString(nameKey).trim();
  }

  public String getDisplayNameIfAvailable() {
    if (getDisplayName().isEmpty()) {
      return getName();
    }
    return getDisplayName();
  }

  public String getBoxName() {
    return infos.getString(boxKey).trim();
  }

  public String getVersion() {
    return infos.getString(versionKey).trim();
  }

  public String getReleaseId() {
    return infos.getString(releaseKey).trim();
  }

  public String getBuildTag() {
    return infos.getString(buildKey).trim();
  }

  public String getLicenceKey() {
    return infos.getString(keyKey).trim();
  }

  @Override
  public String toString() {
    String _displayName = getName();
    if (!getDisplayName().isEmpty()) {
      _displayName += "|" + getDisplayName();
    }
    _displayName += " " + getVersion();
    if (!getBoxName().isEmpty()) {
      _displayName += " (" + getBoxName() + ")";
    }
    return _displayName + " build:" + getBuildTag() + " release:" + getReleaseId();
  }

  public static String specialName(String source) {

    String s = sReplace(" ", "", source);
    s = sReplace("&", "", s);
    s = sReplace("'", "", s);

    return s;
  }

  private static String sReplace(String search, String replace, String source) {
    int spot;
    String returnString;
    String origSource = new String(source);
    spot = source.indexOf(search);
    if (spot > -1) {
      returnString = "";
    } else {
      returnString = source;
    }
    while (spot > -1) {
      if (spot == source.length() + 1) {
        returnString =
            returnString.concat(source.substring(0, source.length() - 1).concat(replace));
        source = "";
      } else if (spot > 0) {
        returnString = returnString.concat(source.substring(0, spot).concat(replace));
        source = source.substring(spot + search.length(), source.length());
      } else {
        returnString = returnString.concat(replace);
        source = source.substring(spot + search.length(), source.length());
      }
      spot = source.indexOf(search);
    }
    if (!source.equals(origSource)) {
      return returnString.concat(source);
    }
    return returnString;
  }
}