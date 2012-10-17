/**
 * Copyright (C) 2000 - 2009 Silverpeas
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
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.xml.xpath;

import org.jdom.Element;

import org.silverpeas.xml.XmlTreeHandler;

/**
 * Titre : Application Builder Description : Represents a simplified XPath as specified below.<br/>
 * W3C XPath 1.0 Recommendation (S=Simplified)<br/>
 * (S)[1] LocationPath ::= '/' RelativeLocationPath? | RelativeLocationPath<br/>
 * (S)[3] RelativeLocationPath ::= Step ('/' RelativeLocationPath)?<br/>
 * (S)[4] Step ::= '.' | '..' | '@' NCName | NCName Predicate?<br/>
 * (S)[8] Predicate ::= '[' PredicateExpr ']'<br/>
 * (S)[9] PredicateExpr ::= '@'? NCName '=' PrimaryExpr<br/>
 * (S)[15] PrimaryExpr ::= Literal | Number<br/>
 * Restriction : when a predicate is used, only the first matching element is selected.<br/>
 */

public class XPath {

  // ######### Constructors ##########

  public XPath() {
  }

  public XPath(String xpath) {
    this(null, xpath, XmlTreeHandler.MODE_SELECT);
  }

  public XPath(Element startingElement, String xpath) {
    this(startingElement, xpath, XmlTreeHandler.MODE_SELECT);
  }

  /**
   * @param Element startingElement the base element where XPath starts
   * @param String xpath the XPath string
   * @param char mode a static XmlTreeHandler constant
   */
  public XPath(Element startingElement, String xpath, char mode) {
    setMode(mode);
    setStartingElement(startingElement);
    setXPath(xpath);
  }

  // ######### getters and setters ##########

  public void setStartingElement(Element e) {
    getTreeHandler().setStartingElement(e);
    getTokenizer().reinitRead();
    eraseAll();
  }

  public Element getStartingElement() {
    return getTreeHandler().getStartingElement();
  }

  public void setXPath(String xpath) {
    getTokenizer().setXPath(xpath);
    getTreeHandler().returnToStartingElement();
    eraseAll();
  }

  public String getXPath() {
    return getTokenizer().getXPath();
  }

  /**
   * Use the constants defined in XmlTreeHandler
   * @see com.silverpeas.xml.XmlTreeHandler
   */
  public void setMode(char mode) {
    getTreeHandler().setMode(mode);
    getTreeHandler().returnToStartingElement();
    getTokenizer().reinitRead();
    eraseAll();
  }

  /**
   * @return a constant defined in XmlTreeHandler
   * @see com.silverpeas.xml.XmlTreeHandler
   */
  public char getMode() {
    return getTreeHandler().getMode();
  }

  // ######### XPathTokenizer ##########

  private XPathTokenizer getTokenizer() {
    if (_tokenizer == null) {
      _tokenizer = new XPathTokenizer();
    }
    return _tokenizer;
  }

  private XPathTokenizer _tokenizer = null;

  // ######### XmlTreeHandler ##########

  private XmlTreeHandler getTreeHandler() {
    if (_xmlTreeHandler == null) {
      _xmlTreeHandler = new XmlTreeHandler();
    }
    return _xmlTreeHandler;
  }

  private XmlTreeHandler _xmlTreeHandler = null;

  // ######### Does parsing do sth on XML ##########

  private void setXMLActionsToBePerformed(boolean doPerform) {
    _doPerformActions = doPerform;
  }

  private boolean areXMLActionsPerformed() {
    return _doPerformActions;
  }

  private boolean _doPerformActions;

  // ######### Syntactical analyser ##########

  public void parse() throws XPathParseException {
    // First, validate the syntax
    getTokenizer().reinitRead();
    setXMLActionsToBePerformed(false);
    analyse();
    // Then perform actions
    if (getTreeHandler().getStartingElement() != null) {
      getTokenizer().reinitRead();
      getTreeHandler().returnToStartingElement();
      setXMLActionsToBePerformed(true);
      analyse();
    }
  }

  /**
   * The main parse function
   */
  private void analyse() throws XPathParseException {
    getTokenizer().readNextToken();
    LocationPath();
    if (getTokenizer().getCurrentTokenType() != XPathTokenizer.END_OF_XPATH) {
      throw new XPathParseException("end of XPath expected", getXPath(),
          getTokenizer().getCurrentTokenPosition());
    }
    if (areXMLActionsPerformed()) {
      setExists(getTreeHandler().currentNodeExists());
    }
  }

  /**
   * W3C XPath 1.0 Recommendation (S=Simplified) (S)[1] LocationPath ::= '/' RelativeLocationPath? |
   * RelativeLocationPath
   */
  private void LocationPath() throws XPathParseException {
    if (getTokenizer().getCurrentTokenType() == XPathTokenizer.STEP_SEPARATOR) {
      if (areXMLActionsPerformed() && getTreeHandler().currentNodeExists()) {
        getTreeHandler().gotoRoot();
      }
      setIsAbsolute(true);
      if (getTokenizer().readNextToken() != XPathTokenizer.END_OF_XPATH) {
        RelativeLocationPath();
      }
    } else {
      if (areXMLActionsPerformed() && getTreeHandler().currentNodeExists()) {
        getTreeHandler().setCurrentElementAsCousinsAncestor();
      }
      setIsAbsolute(false);
      RelativeLocationPath();
    }
  }

  /**
   * W3C XPath 1.0 Recommendation (S=Simplified) (S)[3] RelativeLocationPath ::= Step ('/'
   * RelativeLocationPath)?
   */
  private void RelativeLocationPath() throws XPathParseException {
    Step();
    if (getTokenizer().getCurrentTokenType() == XPathTokenizer.STEP_SEPARATOR) {
      getTokenizer().readNextToken();
      RelativeLocationPath();
    }
  }

  /**
   * W3C XPath 1.0 Recommendation (S=Simplified) (S)[4] Step ::= '.' | '..' | '@' NCName | NCName
   * Predicate?
   */
  private void Step() throws XPathParseException {
    switch (getTokenizer().getCurrentTokenType()) {
      case XPathTokenizer.DOT:
        // the current element
        getTokenizer().readNextToken();
        break;
      case XPathTokenizer.PARENT_STEP:
        // the parent element
        if (areXMLActionsPerformed() && getTreeHandler().currentNodeExists()) {
          getTreeHandler().gotoParent();
          getTreeHandler().setNameFromCurrentElement();
        }
        getTokenizer().readNextToken();
        break;
      case XPathTokenizer.ABREV_ATTRIBAXIS:
        // attribute name expected
        if (getTokenizer().readNextToken() != XPathTokenizer.NAME) {
          throw new XPathParseException("XML Name expected", getXPath(),
              getTokenizer().getCurrentTokenPosition());
        }

        setIsAttribute(true);
        setIsElement(false);

        // now we search for the named attribute
        if (areXMLActionsPerformed() && getTreeHandler().currentNodeExists()) {
          getTreeHandler().gotoFirstNephewNode(XmlTreeHandler.TYPE_ATTRIBUTE,
              getTokenizer().getCurrentToken());
        }

        // ready for next analyse step
        getTokenizer().readNextToken();
        break;
      case XPathTokenizer.NAME:
        setIsElement(true);
        setIsAttribute(false);
        String elementName = getTokenizer().getCurrentToken();

        if (getTokenizer().readNextToken() == XPathTokenizer.PREDICATE_OPEN) {
          Predicate(elementName);
        } else {
          if (areXMLActionsPerformed() && getTreeHandler().currentNodeExists()) {
            getTreeHandler().gotoFirstNephewNode(XmlTreeHandler.TYPE_ELEMENT,
                elementName);
          }
        }
        break;
      default:
        throw new XPathParseException("'.', '..', '@' or XML Name expected",
            getXPath(), getTokenizer().getCurrentTokenPosition());
    }
  }

  /**
   * W3C XPath 1.0 Recommendation (S=Simplified) (S)[8] Predicate ::= '[' PredicateExpr ']'
   */
  private void Predicate(String elementName) throws XPathParseException {
    if (getTokenizer().getCurrentTokenType() != XPathTokenizer.PREDICATE_OPEN) {
      throw new XPathParseException("'[' expected", getXPath(), getTokenizer()
          .getCurrentTokenPosition());
    }
    getTokenizer().readNextToken();
    PredicateExpr(elementName);
    if (areXMLActionsPerformed() && getTreeHandler().currentNodeExists()) {
      getTreeHandler().setCurrentElementAsCousinsAncestor();
    }
    if (getTokenizer().getCurrentTokenType() != XPathTokenizer.PREDICATE_CLOSE) {
      throw new XPathParseException("']' expected", getXPath(), getTokenizer()
          .getCurrentTokenPosition());
    }
    getTokenizer().readNextToken();
  }

  /**
   * W3C XPath 1.0 Recommendation (S=Simplified) (S)[9] PredicateExpr ::= '@'? NCName '='
   * PrimaryExpr
   */
  private void PredicateExpr(String elementName) throws XPathParseException {
    char nodeType = 'X';
    String nodeName = null;
    switch (getTokenizer().getCurrentTokenType()) {
      case XPathTokenizer.ABREV_ATTRIBAXIS:
        nodeType = XmlTreeHandler.TYPE_ATTRIBUTE;
        // attribute name expected
        if (getTokenizer().readNextToken() != XPathTokenizer.NAME) {
          throw new XPathParseException("XML Name expected", getXPath(),
              getTokenizer().getCurrentTokenPosition());
        }
        nodeName = getTokenizer().getCurrentToken();
        break;
      case XPathTokenizer.NAME:
        nodeType = XmlTreeHandler.TYPE_ELEMENT;
        nodeName = getTokenizer().getCurrentToken();
        break;
      default:
        throw new XPathParseException("'@' or XML Name expected", getXPath(),
            getTokenizer().getCurrentTokenPosition());
    }
    if (getTokenizer().readNextToken() != XPathTokenizer.EQUALITY) {
      throw new XPathParseException("'=' expected", getXPath(), getTokenizer()
          .getCurrentTokenPosition());
    }
    getTokenizer().readNextToken();
    String value2find = PrimaryExpr();

    // search
    if (areXMLActionsPerformed() && getTreeHandler().currentNodeExists()) {
      getTreeHandler().gotoFirstNephewParentOf(elementName, nodeType, nodeName,
          value2find);
    }
  }

  /**
   * W3C XPath 1.0 Recommendation (S=Simplified) (S)[15] PrimaryExpr ::= Literal | Number
   */
  private String PrimaryExpr() throws XPathParseException {
    String value = null;
    switch (getTokenizer().getCurrentTokenType()) {
      case XPathTokenizer.INTEGER:
      case XPathTokenizer.REAL:
      case XPathTokenizer.LITERAL:
        value = getTokenizer().getCurrentToken();
        break;
      default:
        throw new XPathParseException("Number or Literal expected", getXPath(),
            getTokenizer().getCurrentTokenPosition());
    }
    getTokenizer().readNextToken();
    return value;
  }

  // ######### Observers & result getters ##########

  // ######### is it an attribute ##########

  /**
   * @return true if the XPath points to an attribute
   */
  public Boolean isAttribute() {
    return _isAttribute;
  }

  private void setIsAttribute(boolean isAttribute) {
    _isAttribute = new Boolean(isAttribute);
  }

  private void eraseIsAttribute() {
    _isAttribute = null;
  }

  private Boolean _isAttribute = null;

  // ######### is it an element ##########

  /**
   * @return true if the XPath points to an element
   */
  public Boolean isElement() {
    return _isElement;
  }

  private void setIsElement(boolean isElement) {
    _isElement = new Boolean(isElement);
  }

  private void eraseIsElement() {
    _isElement = null;
  }

  private Boolean _isElement = null;

  // ######### does it exist in the XML document ##########

  /**
   * @return null if not parsed or if no starting element is available. True if the XPath points to
   * an existing node
   */
  public Boolean exists() {
    return _exists;
  }

  private void setExists(boolean doesExist) {
    _exists = new Boolean(doesExist);
  }

  private void eraseExists() {
    _exists = null;
  }

  private Boolean _exists = null;

  // ######### Absolute path indicator ##########

  /**
   * sets the absolute XPath indicator
   */
  private void setIsAbsolute(boolean isAbsolute) {
    _isAbsolute = new Boolean(isAbsolute);
  }

  private void eraseIsAbsolute() {
    _isAbsolute = null;
  }

  /**
   * @return true if the XPath is absolute
   */
  public Boolean isAbsolute() {
    return _isAbsolute;
  }

  private Boolean _isAbsolute;

  // ######### init all indicators ##########

  private void eraseAll() {
    eraseExists();
    eraseIsAbsolute();
    eraseIsAttribute();
    eraseIsElement();
  }

  // ######### Value of the denoted node ##########

  public void setValue(String value) {
    getTreeHandler().setCurrentNodeValue(value);
  }

  /**
   * @return null if not parsed or if no starting element is available.
   */
  public String getValue() {
    return getTreeHandler().getCurrentNodeValue();
  }

  /**
   * @return null if not parsed or if no starting element is available.
   */
  public Object getNode() {
    return getTreeHandler().getCurrentNode();
  }

  public void setNodeAsStart() {
    getTreeHandler().setCurrentElementAsStartingElement();
  }
}