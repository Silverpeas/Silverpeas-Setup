/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.applicationbuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.silverpeas.xml.ClasspathEntityResolver;

/**
 * Represents an XML Document and provides convenient methods. The methods are used basically to
 * load a document (parse it and obtain a tree representation), save a document (save the tree
 * representation to a well formed XML file). More sophisticated methods are used to merge the
 * document with another one and to sort the tags in the document (it is needed when used in
 * application server).
 * @author Silverpeas
 * @version 1.0
 * @since 1.0
 */
public class XmlDocument extends ApplicationBuilderItem {

  // private String doctypeDecl = null;
  private XMLOutputter outputter = null;
  /**
   * @since 1.0
   */
  private org.jdom.Document underlyingDocument = null;
  /**
   * @since 1.0
   */
  private java.lang.String outputEncoding = "UTF-8";

  public XmlDocument() {
  }

  public XmlDocument(String directory, String name) {
    super(directory, name);
    setOutputter();
  }

  public XmlDocument(File directory, String name) {
    super(directory, name);
    setOutputter();
  }

  /**
   * Save the document tree in the file item
   * @since 1.0
   * @roseuid 3AAF323B0003
   */
  public void save() throws AppBuilderException {
    try {
      saveTo(new FileOutputStream(getPath()));
    } catch (FileNotFoundException fnfe) {
      throw new AppBuilderException("Could not save \""
          + getPath().getAbsolutePath() + "\"", fnfe);
    }
  }

  /**
   * Save the document tree to a stream. This is convenient for writing in an archive
   * @roseuid 3AAF41A601C1
   */
  public void saveTo(java.io.OutputStream outStream) throws AppBuilderException {
    try {
      getOutputter().output(getDocument(), outStream);
    } catch (IOException ioe) {
      throw new AppBuilderException("Could not save " + getName()
          + " to output stream", ioe);
    }
  }

  /**
   * Loads the document tree from the file system
   * @roseuid 3AAF337D004C
   */
  public void load() throws AppBuilderException {
    if (!getPath().exists()) {
      throw new AppBuilderException("\"" + getPath().getAbsolutePath()
          + "\" does not exist");
    }
    try {
      loadFrom(getPath().toURI().toURL().openStream());
    } catch (java.net.MalformedURLException mue) {
      throw new AppBuilderException("Could not load \""
          + getPath().getAbsolutePath() + "\"", mue);
    } catch (java.io.IOException ioe) {
      throw new AppBuilderException("Could not load \""
          + getPath().getAbsolutePath() + "\"", ioe);
    }
  }

  /**
   * Loads the document tree from the contents of an XML file provided as a stream. This can happen
   * when loading from an archive.
   * @param xmlStream the contents of an XML file
   * @throws AppBuilderException
   * @since 1.0
   * @roseuid 3AAF4099035F
   */
  public void loadFrom(InputStream xmlStream) throws AppBuilderException {
    // Attention a la configuration HTTP ! (Proxy : sys. props.
    // "http.proxy[Host|Port])
    // pour acces au DOCTYPE
    try {
      SAXBuilder builder = new SAXBuilder(false);
      builder.setEntityResolver(new ClasspathEntityResolver(builder.getEntityResolver()));
      underlyingDocument = builder.build(xmlStream);
    } catch (IOException jde) {
      throw new AppBuilderException("Could not load \"" + getName()
          + "\" from input stream", jde);
    } catch (JDOMException jde) {
      throw new AppBuilderException("Could not load \"" + getName()
          + "\" from input stream", jde);
    }
  }

  /**
   * Merges only the children of the root element of each document. It takes all the elements
   * concerned by the array of tags from all the documents to merge and adds them to the resulting
   * document. <strong>In the resulting document, the comments, processing instructions and entities
   * are removed.</strong>
   * @param tagsToMerge
   * @param xmlFile
   * @throws AppBuilderException
   * @roseuid 3AAF3793006E
   */
  public void mergeWith(String[] tagsToMerge, XmlDocument xmlFile) throws AppBuilderException {
    /**
     * gets the resulting document from the master document. Cloning the document is important. If
     * you clone or copy an element, the copy keeps his owner and, as a result, the element appears
     * twice in the document
     */
    Element root = getDocument().getRootElement();
    root.detach();

    /**
     * gets the root element of the documents to merge (excluding master)
     */
    org.jdom.Document documentToBeMerged = (org.jdom.Document) xmlFile.getDocument().clone();
    Element tempRoot = documentToBeMerged.getRootElement();

    /**
     * gets all the elements which will be included in the resulting document
     */
    for (int iTag = 0; iTag < tagsToMerge.length; iTag++) {
      for (Object child : tempRoot.getChildren(tagsToMerge[iTag], tempRoot.getNamespace())) {
        if (child instanceof Element) {
          Element newElement = (Element) ((Element) child).clone();
          newElement.detach();
          newElement.setNamespace(root.getNamespace());
          Iterator descendantsIter = newElement.getDescendants();
          while (descendantsIter.hasNext()) {
            Object descendant = descendantsIter.next();
            if (descendant instanceof Element) {
              ((Element) descendant).setNamespace(root.getNamespace());
            }
          }
          root.addContent(newElement);
        }
      }
    }
    setDocument(new Document(root));

  }

  /**
   * Sorts the children elements of the document root according to the array order. The tags not
   * found in the array remain in the same order but at the beginning of the document
   * @param tagsToSort
   * @throws AppBuilderException
   */
  public void sort(java.lang.String[] tagsToSort) throws AppBuilderException {
    /**
     * gets the resulting document from the master document. Cloning the document is important. If
     * you clone or copy an element, the copy keeps his owner and, as a result, the element appears
     * twice in the document
     */
    Element root = getDocument().getRootElement();
    setDocument(new Document(sort(root, tagsToSort)));
  }

  public Element sort(Element root, java.lang.String[] tagsToSort) throws AppBuilderException {
    Element tempRoot = (Element) root.clone();
    tempRoot.detach();
    tempRoot.removeContent();

    /**
     * Makes groups of elements by tag
     */
    List eltLstLst = new ArrayList(tagsToSort.length);
    for (int iTag = 0; iTag < tagsToSort.length; iTag++) {
      List children = root.getChildren(tagsToSort[iTag], root.getNamespace());
      List eltLst = new ArrayList();
      if (children != null && !children.isEmpty()) {
        for (Object child : children) {
          if (child instanceof Content) {
            Content newElement = (Content) ((Content) child).clone();
            newElement.detach();
            eltLst.add(newElement);
          }
        }
      }
      eltLstLst.add(iTag, eltLst);
    }

    /**
     * Orders the content of the resulting document
     */
    for (int iTag = 0; iTag < tagsToSort.length; iTag++) {
      if (!((List) eltLstLst.get(iTag)).isEmpty()) {
        tempRoot.addContent((List) eltLstLst.get(iTag));
      }
    }

    /**
     * the result
     */
    return tempRoot;
  }

  /**
   * Changes the default encoding
   * @param encoding the standard name of the encoding
   * @since 1.0
   * @roseuid 3AAF4C6E027E
   */
  public void setOutputEncoding(java.lang.String encoding) {
    outputEncoding = encoding;
    setOutputter();

  }

  private String getOutputEncoding() {
    return outputEncoding;
  }

  /**
   * @return the document tree
   * @since 1.0
   * @roseuid 3AB0FA640395
   */
  public org.jdom.Document getDocument() {
    return underlyingDocument;
  }

  /**
   * @param doc
   * @since 1.0
   * @roseuid 3AB0FA640395
   */
  public void setDocument(org.jdom.Document doc) {
    underlyingDocument = doc;
  }

  /**
   * Gets the size of the resulting xml document
   * @return the size of the document in memory, given the encoding, <code>-1</code> if unknown.
   * @throws AppBuilderException
   */
  public long getDocumentSize() throws AppBuilderException {
    if (getDocument() != null) {
      long docSize;
      String docStr = getOutputter().outputString(getDocument());
      docSize = docStr.length();
      if (getOutputEncoding().startsWith("UTF-16")) {
        docSize *= 2;
      }
      return docSize;
    }
    return -1;
  }

  /**
   * For each element in tagsToFind returns the value of the specified attribute or the value of the
   * element if no attribute is found.
   * @param tagsToFind
   * @param attribute
   * @return For each element in tagsToFind returns the value of the specified attribute or the
   * value of the element if no attribute is found.
   * @throws AppBuilderException
   */
  public String[] getAttributeValues(String[] tagsToFind, String attribute)
      throws AppBuilderException {
    /**
     * gets the resulting document from the master document. Cloning the document is important. If
     * you clone or copy an element, the copy keeps his owner and, as a result, the element appears
     * twice in the document
     */
    org.jdom.Document resultDoc = (org.jdom.Document) getDocument().clone();
    Element root = resultDoc.getRootElement();

    int iTag;

    /**
     * Makes groups of elements by tag
     */
    List eltLstLst = new ArrayList(tagsToFind.length);
    for (iTag = 0; iTag < tagsToFind.length; iTag++) {
      List eltLst = root.getChildren(tagsToFind[iTag]);
      if (!eltLst.isEmpty()) {
        if (!root.removeChildren(tagsToFind[iTag])) {
          throw new AppBuilderException("Could not remove \""
              + tagsToFind[iTag] + "\" elements from \"" + getName() + "\"");
        }

      }
      eltLstLst.add(iTag, eltLst);
    }

    if (eltLstLst.isEmpty()) {
      return null;
    }

    String[] attributeValues = new String[eltLstLst.size()];
    for (int i = 0; i < eltLstLst.size(); i++) {
      List eltLst = (List) eltLstLst.get(i);
      for (int j = 0; j < eltLst.size(); j++) {
        Element e = (Element) eltLst.get(j);
        if (attribute != null) {
          attributeValues[i] = e.getAttributeValue(attribute);
        } else {
          attributeValues[i] = e.getText();
        }

      }
    }

    return attributeValues;
  }

  /**
   * Looks for all the elements with the given tag in the root element and its children. returns
   * <code>null</code> if nothing was found The values are unique in the array returned
   * @return the array of all the values found
   */
  public String[] getTagValues(String tagToFind) {
    Collection result = new HashSet();
    if (getDocument().getRootElement().getName().equals(tagToFind)) {
      result.add(getDocument().getRootElement().getText());
    }

    Iterator iChildren = getDocument().getRootElement().getChildren(tagToFind).iterator();
    while (iChildren.hasNext()) {
      result.add(((Element) iChildren.next()).getText());
    }

    if (result.isEmpty()) {
      return null;
    }

    return objectArray2StringArray(result.toArray());
  }

  /**
   * Looks for all the attributes with the given name in the root element and its children. returns
   * <code>null</code> if nothing was found. The values are unique in the array returned
   * @return the array of all the values found
   */
  public String[] getAttributeValues(String attributeToFind) {
    Collection result = new HashSet();
    if (getDocument().getRootElement().getAttribute(attributeToFind) != null) {
      result.add(getDocument().getRootElement().getAttributeValue(
          attributeToFind));
    }

    Iterator iChildren = getDocument().getRootElement().getChildren().iterator();
    Element currentElement = null;
    while (iChildren.hasNext()) {
      currentElement = (Element) iChildren.next();
      if (currentElement.getAttribute(attributeToFind) != null) {
        result.add(currentElement.getAttributeValue(attributeToFind));
      }

    }
    if (result.isEmpty()) {
      return null;
    }

    return objectArray2StringArray(result.toArray());
  }

  /**
   * Converts an array of objects into an array of strings
   */
  private String[] objectArray2StringArray(Object[] objectArray) {
    String[] result = new String[objectArray.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = (String) objectArray[i];
    }
    return result;
  }

  private XMLOutputter getOutputter() {
    return outputter;
  }

  private void setOutputter() {
    Format format = Format.getPrettyFormat();
    format.setTextMode(Format.TextMode.TRIM);
    format.setEncoding(outputEncoding);
    format.setIndent("    ");
    outputter = new XMLOutputter(format);
  }
}
