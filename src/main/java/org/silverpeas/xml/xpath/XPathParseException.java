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

public class XPathParseException extends Exception {

  private Integer _errorIndex = null;
  private String _indexedErrorMsg = null;

  public XPathParseException() {
    super();
  }

  public XPathParseException(String msg) {
    super(msg);
  }

  public XPathParseException(String msg, String xpathStr, int errorIndex) {
    super(msg + " at column " + errorIndex);
    _errorIndex = new Integer(errorIndex);
    _indexedErrorMsg = xpathStr + "\n";
    char[] padding = new char[errorIndex - 1];
    Arrays.fill(padding, '-');
    _indexedErrorMsg += new String(padding) + '^';
  }

  public Integer getErrorIndex() {
    return _errorIndex;
  }

  public String getErrorLocationMessage() {
    return _indexedErrorMsg;
  }

  @Override
  public String getMessage() {
    String msg = "";
    if (super.getMessage() != null) {
      msg += super.getMessage();
    }
    if (getErrorLocationMessage() != null) {
      msg += "\n" + getErrorLocationMessage();
    }
    return msg.equals("") ? null : msg;
  }
}