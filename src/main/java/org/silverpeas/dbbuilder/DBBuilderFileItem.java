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

import java.util.List;

import org.jdom.Element;

public class DBBuilderFileItem extends DBBuilderItem {

  public DBBuilderFileItem(DBXmlDocument fileXml) throws Exception {
    setFileXml(fileXml);
    setRoot(((org.jdom.Document) fileXml.getDocument().clone()).getRootElement());
    super.setModule(getRoot().getAttributeValue(MODULENAME_ATTRIB));
  }

  @Override
  public String getVersionFromFile() throws Exception {
    if (versionFromFile == null) {
      List<Element> listeCurrent = (List<Element>) getRoot().getChildren(CURRENT_TAG);
      if (listeCurrent == null || listeCurrent.isEmpty()) {
        throw new Exception(getModule() + ": no <" + CURRENT_TAG
            + "> tag found for this module into contribution file.");
      }
      if (listeCurrent.size() != 1) {
        throw new Exception(getModule() + ": tag <" + CURRENT_TAG + "> appears more than one.");
      }
      for (Element eltCurrent : listeCurrent) {
        versionFromFile = eltCurrent.getAttributeValue(VERSION_ATTRIB);
      }
    } // if
    return versionFromFile;
  }
}
