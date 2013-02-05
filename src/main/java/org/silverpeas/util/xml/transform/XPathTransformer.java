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

import org.apache.commons.io.IOUtils;
import org.silverpeas.util.GestionVariables;
import org.silverpeas.util.xml.ClasspathEntityResolver;
import org.silverpeas.util.xml.XmlTransformer;
import org.silverpeas.util.xml.XmlTreeHandler;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.silverpeas.util.Console;

/**
 * This is the original transformer used by settings since the 5.3 release of Silverpeas. It is
 * using full XPATH capacities for detecting nodes to be updated, inserted or deleted.
 *
 * @since 5.3
 */
public class XPathTransformer implements XmlTransformer {

  private final Console console;

  public XPathTransformer(Console console) {
    this.console = console;
  }

  @Override
  public void xmlfile(String dir, org.jdom.Element eltConfigFile, GestionVariables gv)
      throws Exception {
    XmlConfiguration configuration = new XmlConfiguration(dir, eltConfigFile, gv);
    transform(configuration);
  }

  public void transform(XmlConfiguration configuration) {
    InputStream in = null;
    Document doc = null;
    String xmlFile = configuration.getFileName();
    try {
      console.printMessage(xmlFile);
      in = new BufferedInputStream(new FileInputStream(xmlFile));
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      docFactory.setValidating(false);
      docFactory.setIgnoringElementContentWhitespace(false);
      docFactory.setIgnoringComments(false);
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      docBuilder.setEntityResolver(new ClasspathEntityResolver(null));
      doc = docBuilder.parse(in);
      applyTransformation(configuration, doc);
    } catch (SAXException ex) {
      Logger.getLogger(XPathTransformer.class.getName()).log(Level.SEVERE, null, ex);
    } catch (ParserConfigurationException ex) {
      Logger.getLogger(XPathTransformer.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ioex) {
      Logger.getLogger(XPathTransformer.class.getName()).log(Level.SEVERE, null, ioex);
    } finally {
      IOUtils.closeQuietly(in);
    }
    if (doc != null) {
      saveDoc(xmlFile, doc);
    }
  }

  /**
   * Save the resulting DOM tree into a file.
   *
   * @param xmlFile the target file.
   * @param doc the DOM tree.
   */
  public void saveDoc(String xmlFile, Document doc) {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
      StreamResult result = new StreamResult(new File(xmlFile));
      DOMSource source = new DOMSource(doc);
      transformer.transform(source, result);
    } catch (TransformerException ex) {
      Logger.getLogger(XPathTransformer.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Transform the DOM tree using the configuration.
   *
   * @param configuration the transformation configuration.
   * @param doc the DOM tree to be updated.
   */
  protected void applyTransformation(XmlConfiguration configuration, Document doc) {
    List<Parameter> parameters = configuration.getParameters();
    for (Parameter parameter : parameters) {
      console.printMessage('\t' + parameter.getKey() + " (mode:" + parameter.getMode() + ')');
      Node rootXpathNode = getMatchingNode(parameter.getKey(), doc);
      if (rootXpathNode != null) {
        for (Value value : parameter.getValues()) {
          switch (value.getMode()) {
            case XmlTreeHandler.MODE_INSERT: {
              createNewNode(doc, rootXpathNode, value);
            }
            break;
            case XmlTreeHandler.MODE_DELETE: {
              Node deletedNode = getMatchingNode(value.getLocation(), rootXpathNode);
              rootXpathNode.removeChild(deletedNode);
            }
            break;
            case XmlTreeHandler.MODE_UPDATE: {
              Node oldNode = getMatchingNode(value.getLocation(), rootXpathNode);
              if (oldNode == null) {
                createNewNode(doc, rootXpathNode, value);
              } else {
                if (rootXpathNode.equals(oldNode)) {
                  rootXpathNode = rootXpathNode.getParentNode();
                }
                Node newNode = oldNode.cloneNode(true);
                if (oldNode instanceof Element) {
                  newNode.setTextContent(value.getValue());
                  rootXpathNode.replaceChild(newNode, oldNode);
                } else {
                  ((Attr) newNode).setValue(value.getValue());
                  rootXpathNode.getAttributes().setNamedItem(newNode);
                }

              }
              break;
            }
          }
        }
      }
    }
  }

  /**
   * Find the matching node in the specified DOM tree.
   *
   * @param xpathExpression the XPATh expression.
   * @param doc the DOM tree
   * @return the Node matching the XPATH expression - null otherwise.
   */
  public Node getMatchingNode(String xpathExpression, Node doc) {
    try {
      XPath xpath = XPathFactory.newInstance().newXPath();
      return (Node) xpath.evaluate(xpathExpression, doc, XPathConstants.NODE);
    } catch (XPathExpressionException ex) {
      Logger.getLogger(XPathTransformer.class.getName()).log(Level.SEVERE,
          "Incorrect expression " + xpathExpression, ex);
    } catch (Exception ex) {
      Logger.getLogger(XPathTransformer.class.getName()).log(Level.SEVERE,
          "Incorrect expression " + xpathExpression, ex);
    }
    return null;
  }

  /**
   * Create a new node (element or attribute) to be inserted into the target node as a child or
   * attribute.
   *
   * @param doc DOM document
   * @param target the target ode
   * @param value the value for creating the new node.
   */
  public void createNewNode(Document doc, Node target, Value value) {
    if (value.getLocation().startsWith("@")) {
      Attr newAttribute = doc.createAttribute(value.getLocation().substring(1));
      newAttribute.setValue(value.getValue());
      target.getAttributes().setNamedItem(newAttribute);
    } else {
      Element newElement = doc.createElement(value.getLocation());
      newElement.setTextContent(value.getValue());
      target.appendChild(newElement);
    }
  }
}
