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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jdom.Element;

import org.silverpeas.util.GestionVariables;

import static org.silverpeas.settings.SilverpeasSettings.*;

/**
 * @author ehugonnet
 */
public class XmlConfiguration {

  private String fileName;
  private List<Parameter> parameters;

  public XmlConfiguration(String dir, Element eltConfigFile, GestionVariables gv)
      throws IOException {
    fileName = gv.resolveAndEvalString(dir + eltConfigFile.getAttributeValue(FILE_NAME_ATTRIB));
    parameters = new ArrayList<Parameter>();
    @SuppressWarnings("unchecked")
    List<Element> eltParameters = eltConfigFile.getChildren(PARAMETER_TAG);
    for (Element eltParameter : eltParameters) {
      Parameter parameter = new Parameter(gv.resolveAndEvalString(eltParameter.getAttributeValue(
          PARAMETER_KEY_ATTRIB)), getXmlMode(eltParameter.getAttributeValue(XPATH_MODE_ATTRIB)));
      if (eltParameter.getChildren() != null && !eltParameter.getChildren().isEmpty()) {
        @SuppressWarnings("unchecked")
        List<Element> eltValues = eltParameter.getChildren(VALUE_TAG);
        if (eltValues == null || eltValues.isEmpty()) {
          parameter.addValue(new Value(null, null, gv.resolveAndEvalString(
              eltParameter.getTextTrim()), parameter.getMode()));
        } else {
          for (Element eltValue : eltValues) {
            String relativePath = eltValue.getAttributeValue(RELATIVE_VALUE_ATTRIB);
            if (relativePath != null && !relativePath.isEmpty()) {
              relativePath = gv.resolveAndEvalString(relativePath);
            }
            String location = eltValue.getAttributeValue(VALUE_LOCATION_ATTRIB);
            if (location != null && !location.isEmpty()) {
              location = gv.resolveAndEvalString(location);
            }
            parameter.addValue(new Value(location, relativePath, gv.resolveAndEvalString(
                eltValue.getTextTrim()), parameter.getMode()));
          }
        }
      } else {
        parameter.addValue(new Value(null, null, gv.resolveAndEvalString(
            eltParameter.getTextTrim()), parameter.getMode()));
      }
      parameters.add(parameter);
    }
  }

  public String getFileName() {
    return fileName;
  }

  public List<Parameter> getParameters() {
    return Collections.unmodifiableList(parameters);
  }
}
