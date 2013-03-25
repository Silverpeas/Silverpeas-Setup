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
package org.silverpeas.util.xml.transform;

import java.util.StringTokenizer;

/**
 * @author ehugonnet
 */
public class Value {

  private String location;
  private char mode;
  private String value;

  /**
   * Get the value of value
   * @return the value of value
   */
  public String getValue() {
    return value;
  }

  public Value(String location, String relativePath, String value, char mode) {
    if (null != location) {
      this.location = location;
    } else {
      this.location = ".";
    }
    this.mode = mode;
    this.value = value;
    if (null != relativePath && !relativePath.isEmpty()) {
      this.value = getRelativePath(relativePath, value);
    }
  }

  /**
   * Get the value of mode
   * @return the value of mode
   */
  public char getMode() {
    return mode;
  }

  /**
   * Get the value of location
   * @return the value of location
   */
  public String getLocation() {
    return location;
  }

  private String getRelativePath(final String base, final String path) {
    String result = path;
    String relBase = base;
    String resultBase = null;
    boolean baseUnixSep;
    int nbLevel;

    // BASE (../.. etc)

    // removes drive
    if (null != relBase && 2 <= relBase.length() && ':' == relBase.charAt(1)) {
      relBase = relBase.substring(2);
    }
    // detects file separator
    baseUnixSep = (null != relBase && -1 != relBase.indexOf('/'));
    // removes starting file separator
    if (null != relBase && 1 <= relBase.length()
        && relBase.charAt(0) == (baseUnixSep ? '/' : '\\')) {
      relBase = relBase.substring(1);
    }
    // removes ending file separator
    if (null != relBase && 1 <= relBase.length()
        && relBase.endsWith(baseUnixSep ? "/" : "\\")) {
      relBase = relBase.substring(0, relBase.length() - 2);
    }
    // detects number of levels
    if (null == relBase || 0 == relBase.length()) {
      nbLevel = 0;
    } else {
      StringTokenizer st = new StringTokenizer(relBase, baseUnixSep ? "/" : "\\");
      nbLevel = st.countTokens();
    }
    // creates the base (../.. etc)
    for (int i = 0; i < nbLevel; i++) {
      if (0 == i) {
        resultBase = "..";
      } else {
        resultBase += (baseUnixSep ? "/" : "\\") + "..";
      }
    }
    // removes drive
    if (null != result && 2 <= result.length() && ':' == result.charAt(1)) {
      result = result.substring(2);
    }
    // detects file separator
    baseUnixSep = (null != result && -1 != result.indexOf('/'));
    // adds starting file separator
    if (null != result && 1 <= result.length() && result.charAt(0) != (baseUnixSep ? '/' : '\\')) {
      result = (baseUnixSep ? "/" : "\\") + result;
    }
    result = resultBase + result;
    return result;
  }
}
