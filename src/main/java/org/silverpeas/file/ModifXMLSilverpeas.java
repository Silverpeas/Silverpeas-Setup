/**
 * Copyright (C) 2000 - 2012 Silverpeas
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
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.file;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

public class ModifXMLSilverpeas extends ModifFile {

  public ModifXMLSilverpeas(String path) throws Exception {
    super(path);
  }

  public void setModif(String str[]) throws Exception {
    for (int i = 0; i < str.length; i++) {
      this.addModification(str[i]);
    }
  }

  private void traiteChildren(List children) throws Exception {

    for (int i = 0; i < children.size(); i++) {
      Element currentNode = (Element) children.get(i);
      if (currentNode.getName().compareTo("param-name") == 0) {
        String value = currentNode.getTextTrim();
        Iterator<ElementModif> listeModif = listeModifications.iterator();
        while (listeModif.hasNext()) {
          ElementModif em = listeModif.next();
          if (em.getSearch().compareTo(value) == 0) {
            String val = currentNode.getParentElement().getChild("param-value").getTextTrim();
            if (!val.equals(em.getModif())) {
              if (!isModified) {
                isModified = true;
                BackupFile bf = new BackupFile(new File(path));
                bf.makeBackup();
              }
              currentNode.getParentElement().getChild("param-value").setText(em.getModif());
            }
          }
        }
      }
      // appel recursif , car le noeud courant peut avoir des enfants
      List chil = currentNode.getChildren();
      if (chil != null) {
        traiteChildren(chil);
      }

    }
  }

  @Override
  public void executeModification() throws Exception {
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new File(path));
    Element root = doc.getRootElement();
    traiteChildren(root.getChildren());
    FileOutputStream out = new FileOutputStream(path);
    Format format = Format.getPrettyFormat();
    format.setIndent("\t");
    format.setTextMode(Format.TextMode.TRIM);
    XMLOutputter serializer = new XMLOutputter(format);
    serializer.output(doc, out);
    out.flush();
    out.close();
  }
}