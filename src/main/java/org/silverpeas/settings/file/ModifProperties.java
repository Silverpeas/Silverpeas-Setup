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
package org.silverpeas.settings.file;

import java.io.File;

import org.apache.commons.configuration.PropertiesConfiguration;

public class ModifProperties extends ModifFile {

  /**
   * message de la nouvelle mise a jour du fichier
   */
  private String messageProperties = null;

  /**
   * @constructor prend en parametre le fichier a modifier
   */
  public ModifProperties(String path) throws Exception {
    this.setPath(path);
  }

  /**
   * met a jour le message d'information du propertie
   */
  public void setMessageProperties(String str) {
    messageProperties = str;
  }

  /**
   * @return le message de mise a jour du properties
   */
  public String getMessageProperties() {
    return messageProperties;
  }

  /**
   * lance la modification du fichier properties
   * @throws Exception
   */
  @Override
  public void executeModification() throws Exception {
    File file = new File(path);
    if (file.exists()) {
      PropertiesConfiguration configuration = new PropertiesConfiguration(file);
      for (ElementModif em : listeModifications) {
        if (configuration.containsKey(em.getSearch())) {
          if (!em.getModif().equals(configuration.getProperty(em.getSearch()))) {
            if (!isModified) {
              isModified = true;
              BackupFile bf = new BackupFile(new File(path));
              bf.makeBackup();
            }
            configuration.setProperty(em.getSearch(), em.getModif());
          }
        } else {
          configuration.setProperty(em.getSearch(), em.getModif());
        }
      }
      configuration.save(file);
    } else {
      throw new Exception("ATTENTION le fichier:" + path + " n'existe pas");
    }
  }
}