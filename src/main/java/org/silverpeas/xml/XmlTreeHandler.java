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

package org.silverpeas.xml;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

public class XmlTreeHandler {

  /**
   * The consult mode
   */
  public static final char MODE_SELECT = 'S';
  /**
   * Every named node accessed is created after the existing ones
   */
  public static final char MODE_INSERT = 'I';
  /**
   * Every named node accessed is deleted
   */
  public static final char MODE_DELETE = 'D';
  /**
   * If the named node exists, it is accessed If the named node does not exist, it is created
   */
  public static final char MODE_UPDATE = 'U';
  /**
   * Every named node accessed becomes unique If it exists, the others ones are deleted If it does
   * not exist, it is created
   */
  public static final char MODE_UNIQUE = '1';

  public XmlTreeHandler() {
    this(null, MODE_SELECT);
  }

  public XmlTreeHandler(Element startingElement) {
    this(startingElement, MODE_SELECT);
  }

  public XmlTreeHandler(Element startingElement, char mode) {
    setStartingElement(startingElement);
    setMode(mode);
  }

  public void setStartingElement(Element startingElement) {
    removeAllStates();
    _startingElement = startingElement;
    eraseAttribute();
    eraseName();
    setElement(_startingElement);
    syncIElementsWithElementAndName();
    setCurrentElementAsCousinsAncestor();
    pushState();
  }

  public void setMode(char mode) {
    _mode = mode;
  }

  public char getMode() {
    return _mode;
  }

  public Element getStartingElement() {
    return _startingElement;
  }

  private Element _startingElement = null;
  private char _mode;

  // ###################################
  private void setName(String name) {
    _name = name;
  }

  private String getName() {
    return _name;
  }

  private boolean hasName() {
    return getName() != null;
  }

  private void eraseName() {
    if (hasName()) {
      setName(null);
    }
  }

  private String _name = null;

  // ###################################
  private void setElement(Element e) {
    _element = e;
  }

  private Element getElement() {
    return _element;
  }

  private boolean hasElement() {
    return getElement() != null;
  }

  private void eraseElement() {
    if (hasElement()) {
      setElement(null);
    }
  }

  private Element _element = null;

  // ###################################
  private void setIElements(Iterator iElements) {
    _iElements = iElements;
  }

  private Iterator getIElements() {
    return _iElements;
  }

  private boolean hasIElements() {
    return getIElements() != null;
  }

  private void eraseIElements() {
    if (hasIElements()) {
      _iElements = null;
    }
  }

  private Iterator _iElements = null;

  // ###################################
  private void setNextIElementsAsElement() {
    if (hasIElements()) {
      // all modes but MODE_SELECT do sth special only when a name is present
      switch (getMode()) {
        case MODE_UNIQUE:
        case MODE_DELETE:
          if (hasName()) {
            eraseAll();
            break;
          }
        case MODE_UPDATE:
          if (hasName()) {
            if (getIElements().hasNext()) {
              setElement((Element) getIElements().next());
              break;
            } // else MODE_INSERT
          }
        case MODE_INSERT:
          if (hasName()) {
            Element e = new Element(getName());
            getElement().getParentElement().getChildren(getName()).add(e);
            setElement(e);
            syncIElementsWithElementAndName();
            break;
          }
        case MODE_SELECT:
          if (getIElements().hasNext()) {
            setElement((Element) getIElements().next());
          } else {
            eraseAll();
          }
          break;
      }
    } else {
      eraseAll();
    }
  }

  private void syncIElementsWithElementAndName() {
    if (hasElement()) {
      if (getElement().isRootElement()) {
        eraseIElements();
      } else {
        List children = null;
        if (hasName()) {
          children = getElement().getParentElement().getChildren(getName());
        } else {
          children = getElement().getParentElement().getChildren();
        }
        setIElements(children.iterator());
        while (!((Object) getElement()).equals(getIElements().next())) {
        }
      }
    }
  }

  // ###################################
  private void setAttribute(Attribute a) {
    _attribute = a;
  }

  private Attribute getAttribute() {
    return _attribute;
  }

  private boolean hasAttribute() {
    return getAttribute() != null;
  }

  private void eraseAttribute() {
    if (hasAttribute()) {
      setAttribute(null);
    }
  }

  private Attribute _attribute = null;

  // ###################################
  private void setCousinsAncestor(Element ancestor) {
    _cousinsAncestor = ancestor;
  }

  private Element getCousinsAncestor() {
    return _cousinsAncestor;
  }

  private void eraseCousinsAncestor() {
    _cousinsAncestor = null;
  }

  private boolean hasCousinsAncestor() {
    return _cousinsAncestor != null;
  }

  private Element _cousinsAncestor = null;

  // ###################################
  private boolean isElementCousinsAncestor() {
    if (!hasElement()) {
      return false;
    }
    if (!hasCousinsAncestor()) {
      return getElement().isRootElement();
    }
    return ((Object) getCousinsAncestor()).equals(getElement());
  }

  private boolean isParentCousinsAncestor() {
    if (!hasElement()) {
      return false;
    }
    if (!hasCousinsAncestor()) {
      return hasParent() && getElement().getParentElement().isRootElement();
    }
    Element a = getCousinsAncestor();
    Element p = getElement().getParentElement();
    return ((Object) a).equals(p);
  }

  private void setElementCousinsAncestor() {
    if (hasElement()) {
      setCousinsAncestor(getElement());
    } else {
      eraseCousinsAncestor();
    }
  }

  private void setParentCousinsAncestor() {
    if (hasElement() && hasParent()
        && !getElement().getParentElement().isRootElement()) {
      setCousinsAncestor(getElement().getParentElement());
    } else {
      eraseCousinsAncestor();
    }
  }

  // ###################################
  private void eraseAll() {
    eraseAttribute();
    eraseName();
    eraseElement();
    eraseIElements();
    eraseCousinsAncestor();
  }

  // ###################################
  public boolean isElement() {
    return hasElement();
  }

  public boolean isAttribute() {
    return hasAttribute();
  }

  public Element getCurrentElement() {
    if (isElement()) {
      return getElement();
    }
    return null;
  }

  public Attribute getCurrentAttribute() {
    if (isAttribute()) {
      return getAttribute();
    }
    return null;
  }

  public boolean currentNodeExists() {
    return hasElement() || hasAttribute();
  }

  public Object getCurrentNode() {
    if (isElement()) {
      return getElement();
    }
    if (isAttribute()) {
      return getAttribute();
    }
    return null;
  }

  public String getCurrentNodeValue() {
    if (isElement()) {
      return getElement().getText();
    }
    if (isAttribute()) {
      return getAttribute().getValue();
    }
    return null;
  }

  public void setCurrentNodeValue(String value) {
    if (isElement()) {
      getElement().setText(value);
    }
    if (isAttribute()) {
      getAttribute().setValue(value);
    }
  }

  public boolean hasCurrentNodeValue() {
    String value = getCurrentNodeValue();
    return value != null && !value.trim().equals("");
  }

  // ###################################
  public boolean isCurrentElementRoot() {
    if (!isElement()) {
      return false;
    }
    return getCurrentElement().isRootElement();
  }

  public void gotoRoot() {
    if (getStartingElement() != null) {
      eraseAll();
      setElement(getStartingElement().getDocument().getRootElement());
      syncIElementsWithElementAndName();
    }
  }

  public void returnToStartingElement() {
    returnToStartingState();
  }

  public void setCurrentElementAsStartingElement() {
    removeAllStates();
    if (hasElement()) {
      _startingElement = getElement();
      setCurrentElementAsCousinsAncestor();
    } else {
      eraseAll();
      _startingElement = null;
    }
    pushState();
  }

  public void setNameFromCurrentElement() {
    if (hasElement()) {
      setName(getElement().getName());
      syncIElementsWithElementAndName();
    }
  }

  // ###################################
  public boolean hasParent() {
    return isElement() && !isCurrentElementRoot();
  }

  public void gotoParent() {
    if (hasParent()) {
      eraseName();
      if (isElementCousinsAncestor()) {
        setParentCousinsAncestor();
      }
      setElement(getCurrentElement().getParentElement());
      syncIElementsWithElementAndName();
    } else {
      eraseAll();
    }
  }

  // ###################################
  public boolean hasNextSibling() {
    if (hasIElements()) {
      return getIElements().hasNext();
    }
    return false;
  }

  public void gotoNextSibling() {
    boolean changeCousinsAncestor = isElementCousinsAncestor();
    setNextIElementsAsElement();
    if (changeCousinsAncestor && hasElement()) {
      setCousinsAncestor(getElement());
    }
  }

  // ###################################
  private List getNamedChildren() {
    if (!isElement()) {
      return null;
    }
    List children = null;
    if (hasName()) {
      children = getCurrentElement().getChildren(getName());
      switch (getMode()) {
        case MODE_SELECT:
          break;
        case MODE_UNIQUE:
          // no children => MODE_UPDATE => MODE_INSERT
          if (!children.isEmpty()) {
            if (children.size() > 1) {
              Iterator i = children.iterator();
              boolean passedFirst = false;
              while (i.hasNext()) {
                Element e = (Element) i.next();
                if (passedFirst) {
                  getCurrentElement().removeContent(e);
                } else {
                  passedFirst = true;
                }
              }
            }
            break;
          }
        case MODE_UPDATE:
          // if no children exist => MODE_INSERT
          if (!children.isEmpty()) {
            break;
          }
        case MODE_INSERT:
          children.add(new Element(getName()));
          break;
        case MODE_DELETE:
          // actually removes the children
          getCurrentElement().removeChildren(getName());
          // to tell that there is no children
          children.clear();
          break;
      }
    } else {
      children = getCurrentElement().getChildren();
    }
    if (children.isEmpty()) {
      return null;
    }
    return children;
  }

  private void setFirstNamedChild() {
    List children = getNamedChildren();
    if (children == null) {
      eraseAll();
    } else {
      setElement((Element) children.iterator().next());
      syncIElementsWithElementAndName();
    }
  }

  // ###################################
  public boolean hasChildren() {
    if (isElement()) {
      return getCurrentElement().getChildren().isEmpty();
    }
    return false;
  }

  public boolean hasChildren(String name) {
    if (hasChildren()) {
      List children = getElement().getChildren(name);
      return (children != null) && (!children.isEmpty());
    }
    return false;
  }

  public void gotoFirstChild() {
    eraseName();
    setFirstNamedChild();
  }

  public void gotoFirstChild(String name) {
    setName(name);
    setFirstNamedChild();
  }

  // ###################################
  public void gotoAttribute(String name) {
    if (isElement()) {
      Attribute a = getCurrentElement().getAttribute(name);
      switch (getMode()) {
        case MODE_SELECT:
          break;
        case MODE_UNIQUE:
        case MODE_UPDATE:
        case MODE_INSERT:
          if (a == null) {
            a = new Attribute(name, "PLEASE GIVE ME A VALUE");
            getCurrentElement().setAttribute(a);
          }
          break;
        case MODE_DELETE:
          if (a != null) {
            getCurrentElement().removeAttribute(name);
            a = null;
          }
          break;
      }
      if (a == null) {
        eraseAll();
      } else {
        eraseName();
        eraseIElements();
        eraseElement();
        setAttribute(a);
      }
    } else {
      eraseAll();
    }
  }

  // ###################################
  public static final char TYPE_ATTRIBUTE = 'A';
  public static final char TYPE_ELEMENT = 'E';

  public void gotoFirstChildNode(char nodeType, String nodeName) {
    switch (nodeType) {
      case TYPE_ATTRIBUTE:
        gotoAttribute(nodeName);
        break;
      case TYPE_ELEMENT:
        gotoFirstChild(nodeName);
        break;
      default:
        eraseAll();
        break;
    }
  }

  public void gotoNextSiblingNode() {
    if (isElement()) {
      gotoNextSibling();
    } else {
      eraseAll();
    }
  }

  public boolean hasNextSiblingNode() {
    boolean found;
    char bakMode = getMode();
    pushState();
    gotoNextSiblingNode();
    found = currentNodeExists();
    popState();
    setMode(bakMode);
    return found;
  }

  // #####################################
  public void pushState() {
    if (_stateStack == null) {
      _stateStack = new LinkedList();
    }
    _stateStack.addFirst(new Object[] { getAttribute(), getElement(),
        getName(), getStartingElement(), getCousinsAncestor() });
  }

  public void popState() {
    if (_stateStack != null && _stateStack.size() >= 1) {
      setAttribute((Attribute) ((Object[]) _stateStack.getFirst())[0]);
      setElement((Element) ((Object[]) _stateStack.getFirst())[1]);
      setName((String) ((Object[]) _stateStack.getFirst())[2]);
      syncIElementsWithElementAndName();
      _startingElement = (Element) ((Object[]) _stateStack.getFirst())[3];
      setCousinsAncestor((Element) ((Object[]) _stateStack.getFirst())[4]);
      _stateStack.removeFirst();
    }
  }

  private void removeAllStates() {
    if (_stateStack != null && _stateStack.size() > 0) {
      while (_stateStack.size() >= 1) {
        _stateStack.removeFirst();
      }
    }
  }

  private void returnToStartingState() {
    if (_stateStack != null && _stateStack.size() > 0) {
      while (_stateStack.size() > 1) {
        _stateStack.removeFirst();
      }
      popState();
      pushState();
    }
  }

  private LinkedList _stateStack = null;

  // ######### Search ##########
  public void setCurrentElementAsCousinsAncestor() {
    setElementCousinsAncestor();
  }

  /**
   * move to the next element with the same name and with ancestors with the same names
   */
  public void gotoNextCousin() {
    if (hasElement() && hasName()) {
      switch (getMode()) {
        case MODE_UPDATE:
          if (hasNextCousin()) {
            setMode(MODE_SELECT);
            gotoNextCousin();
            setMode(MODE_UPDATE);
            break;
          }
          // if no next, INSERT
        case MODE_INSERT:
          char bakMode = getMode();
          // from UPDATE mode, there is no next
          if (getMode() == MODE_INSERT) {
            setMode(MODE_SELECT);
            while (hasNextCousin()) {
              gotoNextCousin();
            }
            setMode(bakMode);
          }
          gotoNextSibling();
          break;
        case MODE_DELETE:
        case MODE_UNIQUE:
          gotoNextSibling();
          break;
        case MODE_SELECT:
          if (!isElement() || isCurrentElementRoot() || !hasName()) {
            eraseAll();
          } else if (hasNextSibling()) {
            gotoNextSibling();
          } else if (isParentCousinsAncestor()) {
            eraseAll();
          } else {
            boolean changeCousinsAncestor = isElementCousinsAncestor();
            String cousinName = getName();
            setElement(getElement().getParentElement());
            setName(getElement().getName());
            syncIElementsWithElementAndName();
            gotoNextCousin();
            if (hasElement()) {
              gotoFirstChild(cousinName);
              if (hasElement() && changeCousinsAncestor) {
                setCousinsAncestor(getElement());
              }
            } else {
              eraseAll();
            }
          }
          break;
      }
    } else {
      eraseAll();
    }
  }

  /**
   * @return true if another element exists with the same path (same name and ancestors with same
   * names) in the document
   */
  public boolean hasNextCousin() {
    boolean found;
    if (hasElement() && hasName()) {
      pushState();
      char bakMode = getMode();
      setMode(MODE_SELECT);
      gotoNextCousin();
      setMode(bakMode);
      found = currentNodeExists();
      popState();
    } else {
      found = false;
    }
    return found;
  }

  /**
   * @return true if the current element is parent of the subnode of the given type, name and value.
   */
  public boolean isParentOf(char nodeType, String nodeName, String nodeValue) {
    boolean found = false;
    if (hasElement()) {
      pushState();
      char bakMode = getMode();
      setMode(MODE_SELECT);
      gotoFirstChildNode(nodeType, nodeName);
      found = currentNodeExists();
      if (nodeValue != null) {
        if (found) {
          found = getCurrentNodeValue() != null;
        }
        if (found) {
          found = getCurrentNodeValue().trim().equals(nodeValue);
        }
      }
      if (!found) {
        while (!found && hasNextSiblingNode()) {
          gotoNextSiblingNode();
          found = currentNodeExists();
          if (nodeValue != null) {
            if (found) {
              found = getCurrentNodeValue() != null;
            }
            if (found) {
              found = getCurrentNodeValue().trim().equals(nodeValue);
            }
          }
        }
      }
      setMode(bakMode);
      popState();
    }
    return found;
  }

  /**
   * @return true if the current element is parent of the subnode of the given type and name.
   */
  public boolean isParentOf(char nodeType, String nodeName) {
    return isParentOf(nodeType, nodeName, null);
  }

  public void gotoFirstNephewNode(char nodeType, String nodeName) {
    if (hasElement()) {
      boolean found = false;
      char bakMode = getMode();
      found = isParentOf(nodeType, nodeName);
      if (!found) {
        setMode(MODE_SELECT);
        while (!found && hasNextCousin()) {
          gotoNextCousin();
          found = isParentOf(nodeType, nodeName);
        }
        setMode(bakMode);
      }
      gotoFirstChildNode(nodeType, nodeName);
    } else {
      eraseAll();
    }
  }

  /**
   */
  public void gotoFirstNephewParentOf(String nephewName, char nodeType,
      String nodeName, String nodeValue) {
    if (hasElement()) {
      boolean found;
      boolean nephewExists;
      char bakMode = getMode();
      setMode(MODE_SELECT);
      pushState();
      gotoFirstNephewNode(TYPE_ELEMENT, nephewName);
      nephewExists = currentNodeExists();
      popState();
      if (nephewExists) {
        gotoFirstNephewNode(TYPE_ELEMENT, nephewName);
        found = isParentOf(nodeType, nodeName, nodeValue);
        if (!found) {
          while (!found && hasNextCousin()) {
            gotoNextCousin();
            found = isParentOf(nodeType, nodeName, nodeValue);
          }
        }
      } else {
        found = false;
      }
      setMode(bakMode);
      switch (getMode()) {
        case MODE_DELETE:
          if (nephewExists && found) {
            if (hasParent()) {
              Element e = getCurrentElement();
              gotoParent();
              getCurrentElement().removeContent(e);
            }
          }
          eraseAll();
          break;
        case MODE_UNIQUE:
          if (!nephewExists) {
            gotoFirstChild(nephewName);
          } else {
            if (!found) {
              setMode(MODE_SELECT);
              while (hasNextSiblingNode()) {
                gotoNextSiblingNode();
              }
              setMode(bakMode);
              gotoNextSiblingNode();
            }
          }
          if (!found) {
            pushState();
            gotoFirstChildNode(nodeType, nodeName);
            if (nodeValue != null) {
              setCurrentNodeValue(nodeValue);
            }
            popState();
          }
          Element e = getCurrentElement();
          Element p = e.getParentElement();
          List l = p.getChildren(getName());
          if (e.getParentElement().getChildren(getName()).size() > 1) {
            pushState();
            String uniqname = getName();
            gotoParent();
            getCurrentElement().removeChildren(uniqname);
            getCurrentElement().addContent(e);
            popState();
          }
          break;
        case MODE_SELECT:
          if (!found) {
            eraseAll();
          }
          break;
        case MODE_UPDATE:
          if (found) {
            break;
          } // else INSERT
        case MODE_INSERT:
          if (!nephewExists) {
            gotoFirstChild(nephewName);
          }
          if (found) {
            setMode(MODE_SELECT);
            while (hasNextCousin()) {
              gotoNextCousin();
            }
            setMode(bakMode);
          }
          if (nephewExists) {
            gotoNextCousin();
          }
          pushState();
          gotoFirstChildNode(nodeType, nodeName);
          if (nodeValue != null) {
            setCurrentNodeValue(nodeValue);
          }
          popState();
          break;
      }
    } else {
      eraseAll();
    }
  }

  public void gotoFirstNephewParentOf(String nephewName, char nodeType,
      String nodeName) {
    gotoFirstNephewParentOf(nephewName, nodeType, null);
  }

  /**
   * finds and goes to the first cousin which has a subnode of the given type, name and value.
   */
  public void gotoNextCousinParentOf(char nodeType, String nodeName,
      String nodeValue) {
    if (hasElement() && hasName()) {
      char bakMode = getMode();
      setMode(MODE_SELECT);
      boolean found = false;
      while (!found && hasNextCousin()) {
        gotoNextCousin();
        found = isParentOf(nodeType, nodeName, nodeValue);
      }
      setMode(bakMode);
      switch (getMode()) {
        case MODE_DELETE:
          if (found) {
            if (hasParent()) {
              Element e = getCurrentElement();
              gotoParent();
              getCurrentElement().removeContent(e);
            }
          }
          eraseAll();
          break;
        case MODE_UNIQUE:
          if (!found) {
            gotoNextSiblingNode();
            pushState();
            gotoFirstChildNode(nodeType, nodeName);
            if (nodeValue != null) {
              setCurrentNodeValue(nodeValue);
            }
            popState();
          }
          Element e = getCurrentElement();
          if (e.getParentElement().getChildren(getName()).size() > 1) {
            pushState();
            String uniqname = getName();
            gotoParent();
            getCurrentElement().removeChildren(uniqname);
            getCurrentElement().addContent(e);
            popState();
          }
          break;
        case MODE_SELECT:
          if (!found) {
            eraseAll();
          }
          break;
        case MODE_UPDATE:
          if (found) {
            break;
          } // else INSERT
        case MODE_INSERT:
          if (found) {
            setMode(MODE_SELECT);
            while (hasNextCousin()) {
              gotoNextCousin();
            }
            setMode(bakMode);
          }
          gotoNextCousin();
          pushState();
          gotoFirstChildNode(nodeType, nodeName);
          if (nodeValue != null) {
            setCurrentNodeValue(nodeValue);
          }
          popState();
          break;
      }
    } else {
      eraseAll();
    }
  }

  public void gotoNextCousinParentOf(char nodeType, String nodeName) {
    gotoNextCousinParentOf(nodeType, nodeName, null);
  }

  /**
   * finds and goes to the first cousin which has a subnode of the given type, name and value.
   */
  public boolean hasNextCousinParentOf(char nodeType, String nodeName,
      String nodeValue) {
    boolean found;
    if (hasElement()) {
      pushState();
      char bakMode = getMode();
      setMode(MODE_SELECT);
      gotoNextCousinParentOf(nodeType, nodeName, nodeValue);
      setMode(bakMode);
      found = currentNodeExists();
      popState();
    } else {
      found = false;
    }
    return found;
  }

  public boolean hasNextCousinParentOf(char nodeType, String nodeName) {
    return hasNextCousinParentOf(nodeType, nodeName, null);
  }
}