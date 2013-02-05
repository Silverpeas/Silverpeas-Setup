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
package org.silverpeas.util;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;

import bsh.EvalError;
import bsh.Interpreter;

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
    for (String key : System.getProperties().stringPropertyNames()) {
      listeVariables.put(key, System.getProperties().getProperty(key));
    }
    for (Entry<String, String> entry : System.getenv().entrySet()) {
      listeVariables.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Build the class in charge for all variable operations.
   *
   * @param defaultConfig the default configuration.
   * @throws IOException
   */
  public GestionVariables(Properties defaultConfig) throws IOException {
    this();
    for (String key : defaultConfig.stringPropertyNames()) {
      if(!listeVariables.containsKey(key)) {
        listeVariables.put(key, resolveAndEvalString(defaultConfig.getProperty(key)));
      }
    }
  }

  /**
   * @param config the current configuration overloading thedefault one.
   * @param defaultConfig the default configuration.
   * @throws IOException
   * @constructor construtor principale de la classe
   */
  public GestionVariables(Properties config, Properties defaultConfig) throws IOException {
    this(defaultConfig);
     for (String key : config.stringPropertyNames()) {
      listeVariables.put(key, resolveAndEvalString(config.getProperty(key)));
    }
  }

  /**
   * Add a new variable.
   *
   * @param variable the variable.
   * @param value the variable value.
   */
  public void addVariable(String variable, String value) {
    listeVariables.put(variable, value);
  }

  /**
   * modification d'une variable deja existante
   *
   * @param varName
   * @param varValue
   */
  public void modifyVariable(String varName, String varValue) {
    listeVariables.remove(varName);
    listeVariables.put(varName, varValue);
  }

  /**
   * Gets the name of all variables defines in this object
   *
   * @return an enumeration with all variable names.
   */
  @SuppressWarnings("unchecked")
  public Enumeration<String> getVariableNames() {
    return (Enumeration<String>) listeVariables.propertyNames();
  }

  /**
   * Resolution de string les variables doivent etre de la forme ${variable} il n'y a pas de
   * contrainte aux niveaux du nombre de variables utilisees ex: path=c:\tmp rep=\lib\
   * ${path}{$rep}\toto ->c:\tmp\lib\toto
   *
   * @param unresolvedString
   * @return
   * @throws IOException
   */
  public final String resolveString(String unresolvedString) throws IOException {
    StringBuilder newString = new StringBuilder("");
    int index = unresolvedString.indexOf("${");
    if (-1 != index) {
      int index_fin;
      String currentStringPortion = unresolvedString;
      while (-1 != index) {
        newString.append(currentStringPortion.substring(0, index));
        index_fin = currentStringPortion.indexOf('}');
        newString.append(getValue(currentStringPortion.substring(index + 2, index_fin)));
        currentStringPortion = currentStringPortion.substring(index_fin + 1);
        index = currentStringPortion.indexOf("${");
      }
      newString.append(currentStringPortion);
      return newString.toString();
    }
    return unresolvedString;
  }

  /**
   * Resolves all he varibles of ythe specified String then evaluates dynamically the result using
   * $eval{{.....}}
   *
   * @param unresolvedString
   * @return
   * @throws IOException
   */
  public final String resolveAndEvalString(String unresolvedString) throws IOException {
    int index = unresolvedString.indexOf("$eval{{");
    if (-1 == index) {
      return resolveString(unresolvedString);
    } else {
      if (0 != index) {
        throw new IllegalArgumentException("(Unable to evaluate " + unresolvedString
            + " because string is not beginning with \"$eval{{\" sequence.");
      }

      int index_fin = unresolvedString.indexOf("}}");

      if (index_fin != unresolvedString.length() - 2) {
        throw new IllegalArgumentException("(unable to evaluate " + unresolvedString
            + " because string is not endding with \"}}\" sequence.");
      }

      String resolvedString = unresolvedString.substring(0, index_fin);
      resolvedString = resolvedString.substring(7);
      resolvedString = resolveString(resolvedString);

      // evaluation dynamique
      String evaluatedString = null;
      try {
        Interpreter bsh = new Interpreter();
        bsh.set("value", "");
        bsh.eval(resolvedString);
        evaluatedString = (String) bsh.get("value");
      } catch (EvalError e) {
        throw new IllegalArgumentException("(unable to evaluate " + resolvedString, e);
      }
      return evaluatedString;
    }
  }

  /**
   * Return the value for the specified variable.
   *
   * @param variable : the variable.
   * @return l the value for the specified variable.
   * @throws IOException
   */
  public String getValue(String variable) throws IOException {
    String tmp = listeVariables.getProperty(variable);
    if (null == tmp) {
      throw new IOException("La variable :\"" + variable + "\" n'existe pas dans la base");
    }
    return tmp;
  }
}
