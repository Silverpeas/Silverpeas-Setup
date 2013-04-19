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
package org.silverpeas.dbbuilder;

public class VersionTag {

  private final String action;
  private final String version;

  public VersionTag(String cp, String v) {
    action = cp;
    version = v;
  }

  public String getCurrent_or_previous() {
    return action;
  }

  public String getVersion() {
    return version;
  }
  
  public boolean isUpgrade() {
    return DBBuilderDBItem.PREVIOUS_TAG.equals(action);
  }
  
  public boolean isInstall() {
    return DBBuilderDBItem.CURRENT_TAG.equals(action);
  }
  
  /**
   * Return the version of the module after applying the action.
   * @return 
   */
  public String getResultingVersion() {
    int nexVersion = Integer.parseInt(version);
    if(isUpgrade()) {
     nexVersion = nexVersion + 1;
    }
    String sversionFile = "000" + nexVersion;
    return sversionFile.substring(sversionFile.length() - 3);
  }
}