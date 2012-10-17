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
package org.silverpeas.file;

import bsh.EvalError;
import bsh.Interpreter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;

public class GestionVariables {

  /**
   * Hashtable contenant toutes les variables et leurs valeurs
   */
  private Properties listeVariables;

  /**
   * @constructor construtor principale de la classe
   */
  private GestionVariables() {
    listeVariables = new Properties();
    for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
      listeVariables.put(entry.getKey().toString(), entry.getValue().toString());
    }
    for (Entry<String, String> entry : System.getenv().entrySet()) {
      addVariable(entry.getKey(), entry.getValue());
    }
  }

  /**
   * @constructor construtor principale de la classe
   */
  public GestionVariables(Properties defaultConfig) throws IOException {
    this();
    for (Entry<Object, Object> entry : defaultConfig.entrySet()) {
      listeVariables.put(entry.getKey().toString(),
          resolveAndEvalString(entry.getValue().toString()));
    }
  }

  /**
   * @constructor construtor principale de la classe
   */
  public GestionVariables(Properties config, Properties defaultConfig) throws IOException {
    this(defaultConfig);
    for (Entry<Object, Object> entry : config.entrySet()) {
      listeVariables.put(entry.getKey().toString(), resolveAndEvalString(entry.getValue()
          .toString()));
    }
  }

  /**
   * ajout d'une variable dans la base
   */
  public final void addVariable(String pName, String pValue) {
    listeVariables.put(pName, pValue);
  }

  /**
   * modification d'une variable deja existante
   */
  public void modifyVariable(String pName, String pValue) {
    listeVariables.remove(pName);
    listeVariables.put(pName, pValue);
  }

  /**
   * Gets the name of all variables defines in this object
   * @return an enumeration with all variable names.
   */
  public Enumeration<String> getVariableNames() {
    return (Enumeration<String>) listeVariables.propertyNames();
  }

  /**
   * resolution de string les variables doivent etre de la forme ${variable} il n'y a pas de
   * contrainte aux niveaux du nombre de variables utilisees ex: path=c:\tmp rep=\lib\
   * ${path}{$rep}\toto ->c:\tmp\lib\toto
   */
  public String resolveString(String pStr) throws IOException {
    String newString = "";
    int index = pStr.indexOf("${");
    if (index != -1) {
      int index_fin;
      String tmp = pStr;
      while (index != -1) {
        newString = newString.concat(tmp.substring(0, index));
        index_fin = tmp.indexOf('}');
        newString = newString.concat(this.getValue(tmp.substring(index + 2, index_fin)));
        tmp = tmp.substring(index_fin + 1);
        index = tmp.indexOf("${");
      }
      newString = newString.concat(tmp);
      return newString;
    } else {
      return pStr;
    }
  }

  /**
   * resolution des variables d'une string puis evaluation dynamique d'une string de la forme
   * $eval{{.....}}
   */
  public final String resolveAndEvalString(String pStr) throws IOException,
      IllegalArgumentException {
    int index = pStr.indexOf("$eval{{");
    if (index == -1) {
      return resolveString(pStr);
    } else {
      if (index != 0) {
        throw new IllegalArgumentException("(Unable to evaluate " + pStr +
            " because string is not beginning with \"$eval{{\" sequence.");
      }

      int index_fin = pStr.indexOf("}}");

      if (index_fin != pStr.length() - 2) {
        throw new IllegalArgumentException("(unable to evaluate " + pStr +
            " because string is not endding with \"}}\" sequence.");
      }

      String resolvedString = pStr.substring(0, index_fin);
      resolvedString = resolvedString.substring(7);
      resolvedString = resolveString(resolvedString);

      // evaluation dynamique
      String evaluatedString = null;
      try {
        Interpreter bsh = new Interpreter();
        bsh.set("value", new String());
        bsh.eval(resolvedString);
        evaluatedString = (String) bsh.get("value");
      } catch (EvalError e) {
        throw new IllegalArgumentException("(unable to evaluate " + resolvedString, e);
      }
      return evaluatedString;
    }
  }

  /**
   * @return la valeur de la variable
   */
  public String getValue(String pVar) throws IOException {
    String tmp = listeVariables.getProperty(pVar);
    if (tmp == null) {
      throw new IOException("La variable :\"" + pVar + "\" n'existe pas dans la base");
    }
    return tmp;
  }
}
