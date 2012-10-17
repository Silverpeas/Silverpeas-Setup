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

import java.util.Arrays;
import org.jdom.IllegalNameException;

/**
 * Titre : Application Builder Description : implements a simplified XPath parser that retrieves the
 * different token composing a XPath Tokens are named * by the public fields STEP_SEPARATOR ('/')
 * PREDICATE_OPEN ('[') PREDICATE_CLOSE (']') EQUALITY ('=') DOT ('.' the abreviated step)
 * ABREV_ATTRIBAXIS ('@' the abreviated attribute axis specifier) PARENT_STEP ("..") LITERAL (string
 * enclosed by quotation marks or apostrophes) INTEGER (greater or equal to zero) REAL (greater or
 * equal to zero) NAME (a valid Name as specified in W3C XML 1.0 Recommendation)
 */
public class XPathTokenizer {

  // ######### Constructors ##########
  public XPathTokenizer() {
  }

  public XPathTokenizer(String xpath) {
    setXPath(xpath);
  }

  // ######### getters and setters ##########
  public void reinitRead() {
    // removeStates();
    setIndex(0);
    setIndexAsPosition();
    setCurrentToken(null);
    setCurrentTokenType(UNDEFINED);
  }

  public void setXPath(String xpath) {
    _xpath = xpath;
    reinitRead();
  }

  public String getXPath() {
    return _xpath;
  }

  private String _xpath = null;
  // ######### lexical analyser ##########
  // used as lexical elements and as lexical element types
  public static final char STEP_SEPARATOR = '/';
  public static final char PREDICATE_OPEN = '[';
  public static final char PREDICATE_CLOSE = ']';
  public static final char EQUALITY = '=';
  public static final char DOT = '.';
  public static final char ABREV_ATTRIBAXIS = '@';
  // lexical element types
  public static final char PARENT_STEP = 'P';
  public static final char LITERAL = 'L';
  public static final char INTEGER = 'I';
  public static final char REAL = 'R';
  public static final char NAME = 'N';
  public static final char END_OF_XPATH = 'E';
  public static final char UNDEFINED = 'U';
  // used to parse literals
  private static final char LITERAL_DELIM_SINGLE = '\'';
  private static final char LITERAL_DELIM_DOUBLE = '"';
  // used to parse numbers
  private static char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
      '9' };

  static {
    Arrays.sort(DIGITS);
  }

  /**
   * If null, the token is the element type
   */
  public String getCurrentToken() {
    return _token;
  }

  /**
   * the token type and, for single characters, the token itself
   */
  public char getCurrentTokenType() {
    return _tokenType;
  }

  /**
   * @return the position of the current token
   */
  public int getCurrentTokenPosition() {
    return _tokenPosition;
  }

  private int _iXpath;
  private int _tokenPosition;
  private String _token = null;
  private char _tokenType = UNDEFINED;

  /**
   * @return the the current index
   */
  private int getIndex() {
    return _iXpath;
  }

  /**
   * sets the index of the current token
   */
  private void setIndex(int newIndex) {
    _iXpath = newIndex;
  }

  /**
   * sets the current token
   */
  private void setCurrentToken(String token) {
    _token = token;
  }

  /**
   * sets the current token type
   */
  private void setCurrentTokenType(char tokenType) {
    _tokenType = tokenType;
  }

  /**
   * sets the position of the current token
   */
  private void setIndexAsPosition() {
    _tokenPosition = getIndex() + 1;
  }

  /**
   * Sets the token found
   * @return the lexical element type. If getCurrentToken() is null, it is also the element itself.
   */
  public char readNextToken() throws XPathParseException {
    setIndexAsPosition();
    setCurrentTokenType(UNDEFINED);

    // no path
    if (getXPath() == null) {
      throw new XPathParseException("someone forgot to set my XPath !");
    }

    // empty path
    if (getXPath().length() == 0) {
      throw new XPathParseException("please give me a non empty XPath");
    }

    // end of read
    if (getIndex() == getXPath().length()) {
      setCurrentTokenType(END_OF_XPATH);
      return _tokenType;
    }

    switch (getXPath().charAt(getIndex())) {
      // Step separator
      case STEP_SEPARATOR:
        setCurrentTokenType(STEP_SEPARATOR);
        setCurrentToken(null);
        setIndex(getIndex() + 1);
        break;
      // Abreviated steps or Number start
      case DOT:
        if (getXPath().length() > getIndex() + 1
            && getXPath().charAt(getIndex() + 1) == DOT) {
          setCurrentTokenType(PARENT_STEP);
          setCurrentToken(new String(new char[] { DOT, DOT }));
          setIndex(getIndex() + 2);
        } else {
          int iStartStepOrNumber = getIndex();
          setIndex(getIndex() + 1);
          if (readDigits()) {
            setCurrentTokenType(REAL);
            setCurrentToken(getXPath()
                .substring(iStartStepOrNumber, getIndex()));
          } else {
            setCurrentToken(null);
            setCurrentTokenType(DOT);
          }
        }
        break;
      case ABREV_ATTRIBAXIS:
        setCurrentTokenType(ABREV_ATTRIBAXIS);
        setCurrentToken(null);
        setIndex(getIndex() + 1);
        break;
      case PREDICATE_OPEN:
        setCurrentTokenType(PREDICATE_OPEN);
        setCurrentToken(null);
        setIndex(getIndex() + 1);
        break;
      case PREDICATE_CLOSE:
        setCurrentTokenType(PREDICATE_CLOSE);
        setCurrentToken(null);
        setIndex(getIndex() + 1);
        break;
      case EQUALITY:
        setCurrentTokenType(EQUALITY);
        setCurrentToken(null);
        setIndex(getIndex() + 1);
        break;
      case LITERAL_DELIM_SINGLE:
        setIndex(getIndex() + 1);
        int iDelimSingle = getXPath().indexOf(LITERAL_DELIM_SINGLE, getIndex());
        if (iDelimSingle == -1) {
          throw new XPathParseException("literal not closed", getXPath(),
              getIndex());
        }
        setCurrentTokenType(LITERAL);
        setCurrentToken(getXPath().substring(getIndex(), iDelimSingle));
        setIndex(iDelimSingle + 1);
        break;
      case LITERAL_DELIM_DOUBLE:
        setIndex(getIndex() + 1);
        int iDelimDouble = getXPath().indexOf(LITERAL_DELIM_DOUBLE, getIndex());
        if (iDelimDouble == -1) {
          throw new XPathParseException("literal not closed", getXPath(),
              getIndex());
        }
        setCurrentTokenType(LITERAL);
        setCurrentToken(getXPath().substring(getIndex(), iDelimDouble));
        setIndex(iDelimDouble + 1);
        break;
      // Number
      // starting with '.' : see DOT case
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        setCurrentTokenType(INTEGER);
        int iStartNumber = getIndex();
        readDigits();
        if (getIndex() < getXPath().length()
            && getXPath().charAt(getIndex()) == DOT) {
          setCurrentTokenType(REAL);
          setIndex(getIndex() + 1);
          readDigits();
        }
        setCurrentToken(getXPath().substring(iStartNumber, getIndex()));
        break;
      // XML Name
      default:
        int iStartName = getIndex();
        boolean nameFound = false;
        setIndex(getIndex() + 1);
        try {
          while (getIndex() <= getXPath().length()
              && isValidName(getXPath().substring(iStartName, getIndex()))) {
            nameFound = true;
            setIndex(getIndex() + 1);
          }
        } catch (RuntimeException re) {
          throw new XPathParseException("unauthorised char in XML name",
              getXPath(), getIndex());
        }
        if (nameFound) {
          setCurrentTokenType(NAME);
          setIndex(getIndex() - 1);
          setCurrentToken(getXPath().substring(iStartName, getIndex()));
        } else {
          throw new XPathParseException("unauthorised char", getXPath(),
              iStartName + 1);
        }
        break;
    }

    return getCurrentTokenType();
  }

  private boolean readDigits() {
    boolean found = false;
    while (getIndex() < getXPath().length()
        && Arrays.binarySearch(DIGITS, getXPath().charAt(getIndex())) >= 0) {
      found = true;
      setIndex(getIndex() + 1);
    }
    return found;
  }

  protected boolean isValidName(String name) {
    try {
      new org.jdom.Element(name);
      return true;
    } catch (IllegalNameException e) {
      return false;
    }
  }
}