/**
 * Copyright (C) 2000 - 2013 Silverpeas
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.silverpeas.settings.file;

import java.util.regex.Pattern;

/**
 * @author ehugonnet
 */
public class RegexpElementMotif extends ElementModif {

  /**
   * tableau de chaine des valeurs
   */
  private Pattern pattern;
  private String remplacement;

  /**
   * @Constructor : prend en parametre la chaine de recherche
   */
  public RegexpElementMotif(String pSearch) {
    super(pSearch, null);
    pattern = Pattern.compile(pSearch);
  }

  public Pattern getPattern() {
    return this.pattern;
  }

  /**
   * met a jour le tableau des valeurs
   */
  public void setRemplacement(String pValues) {
    remplacement = pValues;
  }

  /**
   * met a jour le tableau des valeurs
   */
  public String getRemplacement() {
    return remplacement;
  }
}