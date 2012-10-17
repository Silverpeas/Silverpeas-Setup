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

package org.silverpeas.tests;

import java.io.File;

import org.jdom.Element;

import org.silverpeas.applicationbuilder.XmlDocument;
import org.silverpeas.xml.XmlTreeHandler;

/**
 * Titre : Application Builder Description : Copyright : Copyright (c) 2001 Société : Stratélia
 * @author Jean-Christophe Carry
 * @version 2.0
 */

public class TestXmlTreeHandler {

  private XmlTreeHandler t = null;

  private void test_SELECT_ELEMENT_NOTNAMED(Element e) {
    t.setStartingElement(e);
    if (t.hasParent()) {
      t.gotoParent();
      System.out.println(t.getCurrentElement());
      while (t.hasNextSibling()) {
        t.gotoNextSibling();
        System.out.println(t.getCurrentElement());
      }
      t.returnToStartingElement();
    } else {
      System.out.println("NO PARENTS");
    }
    System.out.println("\tSTART " + t.getCurrentElement());
    if (t.hasNextSibling()) {
      while (t.hasNextSibling()) {
        t.gotoNextSibling();
        System.out.println("\t" + t.getCurrentElement());
      }
    } else {
      System.out.println("\tNO SIBLINGS");
    }
    t.returnToStartingElement();
    if (t.hasChildren()) {
      t.gotoFirstChild();
      System.out.println("\t\t" + t.getCurrentElement());
      if (t.hasChildren()) {
        t.gotoFirstChild();
        System.out.println("\t\t\t" + t.getCurrentElement());
        while (t.hasNextSibling()) {
          t.gotoNextSibling();
          System.out.println("\t\t\t" + t.getCurrentElement());
        }
        t.gotoParent();
      } else {
        System.out.println("\t\t\tNO GRAND CHILDREN");
      }
      while (t.hasNextSibling()) {
        t.gotoNextSibling();
        System.out.println("\t\t" + t.getCurrentElement());
        if (t.hasChildren()) {
          t.gotoFirstChild();
          System.out.println("\t\t\t" + t.getCurrentElement());
          while (t.hasNextSibling()) {
            t.gotoNextSibling();
            System.out.println("\t\t\t" + t.getCurrentElement());
          }
          t.gotoParent();
        } else {
          System.out.println("\t\t\tNO GRAND CHILDREN");
        }
      }
      t.gotoParent();
    } else {
      System.out.println("\t\tNO CHILDREN");
    }
  }

  private int level = 0;

  private void printLevel() {
    for (int i = 1; i < level; i++) {
      System.out.print("\t");
    }
    System.out.println(t.getCurrentElement());
  }

  private boolean test_SELECT_ELEMENT_NAMED(String elementName) {
    boolean result = false;

    if (t.hasChildren(elementName)) {
      result = true;
      level++;
      t.gotoFirstChild(elementName);
      printLevel();
      while (t.hasNextSibling()) {
        t.gotoNextSibling();
        printLevel();
      }
      level--;
      t.gotoParent();
    } else if (t.hasChildren()) {
      level++;
      t.gotoFirstChild();
      if (result = test_SELECT_ELEMENT_NAMED(elementName)) {
        printLevel();
      }
      level--;
      t.gotoParent();
    }
    if (result) {
      printLevel();
    }
    if (t.hasNextSibling()) {
      t.gotoNextSibling();
      result = test_SELECT_ELEMENT_NAMED(elementName);
    }
    return result;
  }

  private void test_SELECT_ELEMENT_COUSIN() throws Exception {
    doc = new XmlDocument(new File("XML4tests"), "config4cousin.xml");
    doc.load();
    t.setStartingElement(doc.getDocument().getRootElement());
    System.out.println("START");
    t.gotoFirstChild("cousin");
    t.setCurrentElementAsCousinsAncestor();
    System.out.println("\t" + t.getCurrentElement());
    t.gotoFirstChild("brother");
    System.out.println("\t\t" + t.getCurrentElement());
    t.gotoFirstChild("LDAPRealm");
    System.out.print("\t\t\t" + t.getCurrentElement() + " : ");
    t.pushState();
    t.gotoAttribute("Name");
    System.out.println(t.getCurrentNodeValue());
    t.popState();
    System.out.println("pivot : cousin");
    while (t.hasNextCousin()) {
      t.gotoNextCousin();
      System.out.print("\t\t\t" + t.getCurrentElement() + " : ");
      t.pushState();
      t.gotoAttribute("Name");
      System.out.println(t.getCurrentNodeValue());
      t.popState();
      t.pushState();
      int level = 3;
      while (t.hasParent()) {
        t.gotoParent();
        level--;
        for (int i = 1; i <= level; i++) {
          System.out.print("\t");
        }
        System.out.println(t.getCurrentElement());
      }
      t.popState();
    }
    System.out.println("START");
    t.returnToStartingElement();
    t.gotoFirstChild("cousin");
    System.out.println("\t" + t.getCurrentElement());
    t.gotoFirstChild("brother");
    System.out.println("\t\t" + t.getCurrentElement());
    t.gotoFirstChild("LDAPRealm");
    System.out.print("\t\t\t" + t.getCurrentElement() + " : ");
    t.pushState();
    t.gotoAttribute("Name");
    System.out.println(t.getCurrentNodeValue());
    t.popState();
    System.out.println("pivot : root");
    while (t.hasNextCousin()) {
      t.gotoNextCousin();
      System.out.print("\t\t\t" + t.getCurrentElement() + " : ");
      t.pushState();
      t.gotoAttribute("Name");
      System.out.println(t.getCurrentNodeValue());
      t.popState();
      t.pushState();
      int level = 3;
      while (t.hasParent()) {
        t.gotoParent();
        level--;
        for (int i = 1; i <= level; i++) {
          System.out.print("\t");
        }
        System.out.println(t.getCurrentElement());
      }
      t.popState();
    }
  }

  private void test_SELECT_ATTRIBUTE(String name) {
    t.returnToStartingElement();
    System.out.print(t.getCurrentElement() + ":");
    t.pushState();
    t.gotoAttribute(name);
    System.out.println(t.getCurrentAttribute());
    t.popState();
    t.gotoFirstChild();
    System.out.print("\t" + t.getCurrentElement() + ":");
    t.pushState();
    t.gotoAttribute(name);
    System.out.println(t.getCurrentAttribute());
    t.popState();
    t.gotoNextSibling();
    System.out.print("\t" + t.getCurrentElement() + ":");
    t.pushState();
    t.gotoAttribute(name);
    System.out.println(t.getCurrentAttribute());
    t.popState();
    t.gotoParent();
    System.out.print(t.getCurrentElement() + ":");
    t.gotoAttribute(name);
    System.out.println(t.getCurrentAttribute());
  }

  private XmlDocument doc = null;

  private void test_UPDATE_ATTRIBUTE(String name) throws Exception {
    doc = new XmlDocument(new File("XML4tests"), "config.xml");
    doc.load();
    t.setStartingElement(doc.getDocument().getRootElement().getChild("Server"));
    test_SELECT_ATTRIBUTE(name);
    doc.setName("UPDATE_ATTRIBUTE(" + name + ")_" + doc.getName());
    doc.save();
  }

  private void test_UPDATE_ELEMENT(String name, boolean fromfirstchild)
      throws Exception {
    doc = new XmlDocument(new File("XML4tests"), "config.xml");
    doc.load();
    t.setStartingElement(doc.getDocument().getRootElement());
    if (fromfirstchild)
      t.gotoFirstChild();
    System.out.println(t.getCurrentElement());
    t.gotoFirstChild(name);
    System.out.println("\t" + t.getCurrentElement());
    t.gotoNextSibling();
    System.out.println("\t" + t.getCurrentElement());
    t.gotoNextSibling();
    System.out.println("\t" + t.getCurrentElement());
    t.gotoParent();
    System.out.println(t.getCurrentElement());
    doc.setName("UPDATE_ELEMENT(" + name + ")_"
        + (fromfirstchild ? "Server" : "root") + "_" + doc.getName());
    doc.save();
  }

  private void test_DELETE_ATTRIBUTE(String name) throws Exception {
    doc = new XmlDocument(new File("XML4tests"), "config.xml");
    doc.load();
    t.setStartingElement(doc.getDocument().getRootElement().getChild("Server"));
    test_SELECT_ATTRIBUTE(name);
    doc.setName("DELETE_ATTRIBUTE(" + name + ")_" + doc.getName());
    doc.save();
  }

  private void test_DELETE_ELEMENT(String name, boolean fromfirstchild)
      throws Exception {
    doc = new XmlDocument(new File("XML4tests"), "config.xml");
    doc.load();
    t.setStartingElement(doc.getDocument().getRootElement());
    if (fromfirstchild)
      t.gotoFirstChild();
    System.out.println(t.getCurrentElement());
    t.gotoFirstChild(name);
    System.out.println("\t" + t.getCurrentElement());
    t.gotoNextSibling();
    System.out.println("\t" + t.getCurrentElement());
    t.gotoNextSibling();
    System.out.println("\t" + t.getCurrentElement());
    t.gotoParent();
    System.out.println(t.getCurrentElement());
    doc.setName("DELETE_ELEMENT(" + name + ")_"
        + (fromfirstchild ? "Server" : "root") + "_" + doc.getName());
    doc.save();
  }

  private void test_INSERT_ATTRIBUTE(String name) throws Exception {
    doc = new XmlDocument(new File("XML4tests"), "config.xml");
    doc.load();
    t.setStartingElement(doc.getDocument().getRootElement().getChild("Server"));
    test_SELECT_ATTRIBUTE(name);
    doc.setName("INSERT_ATTRIBUTE(" + name + ")_" + doc.getName());
    doc.save();
  }

  private void test_INSERT_ELEMENT(String name, boolean fromfirstchild)
      throws Exception {
    doc = new XmlDocument(new File("XML4tests"), "config.xml");
    doc.load();
    t.setStartingElement(doc.getDocument().getRootElement());
    if (fromfirstchild)
      t.gotoFirstChild();
    System.out.println(t.getCurrentElement());
    t.gotoFirstChild(name);
    System.out.println("\t" + t.getCurrentElement());
    t.gotoNextSibling();
    System.out.println("\t" + t.getCurrentElement());
    t.gotoNextSibling();
    System.out.println("\t" + t.getCurrentElement());
    t.gotoParent();
    System.out.println(t.getCurrentElement());
    doc.setName("INSERT_ELEMENT(" + name + ")_"
        + (fromfirstchild ? "Server" : "root") + "_" + doc.getName());
    doc.save();
  }

  private void test_UNIQUE_ATTRIBUTE(String name) throws Exception {
    doc = new XmlDocument(new File("XML4tests"), "config.xml");
    doc.load();
    t.setStartingElement(doc.getDocument().getRootElement().getChild("Server"));
    test_SELECT_ATTRIBUTE(name);
    doc.setName("UNIQUE_ATTRIBUTE(" + name + ")_" + doc.getName());
    doc.save();
  }

  private void test_UNIQUE_ELEMENT(String name, boolean fromfirstchild)
      throws Exception {
    doc = new XmlDocument(new File("XML4tests"), "config.xml");
    doc.load();
    t.setStartingElement(doc.getDocument().getRootElement());
    if (fromfirstchild)
      t.gotoFirstChild();
    System.out.println(t.getCurrentElement());
    t.gotoFirstChild(name);
    System.out.println("\t" + t.getCurrentElement());
    t.gotoNextSibling();
    System.out.println("\t" + t.getCurrentElement());
    t.gotoNextSibling();
    System.out.println("\t" + t.getCurrentElement());
    t.gotoParent();
    System.out.println(t.getCurrentElement());
    doc.setName("UNIQUE_ELEMENT(" + name + ")_"
        + (fromfirstchild ? "Server" : "root") + "_" + doc.getName());
    doc.save();
  }

  /*
   * private void test_PARENTOF(char nodeType, String nodeName, String nodeValue, boolean
   * pivotCousin) throws Exception { doc = new XmlDocument(new
   * File("XML4tests"),"config4parentof.xml"); doc.load();
   * t.setStartingElement(doc.getDocument().getRootElement()); char bakMode=t.getMode();
   * t.setMode(XmlTreeHandler.MODE_SELECT); System.out.println("START"); t.gotoFirstChild("cousin");
   * if (pivotCousin) { t.setCurrentElementAsCousinsAncestor(); }
   * System.out.println("\t"+t.getCurrentElement()); t.gotoFirstChild("brother");
   * System.out.println("\t\t"+t.getCurrentElement()); t.gotoFirstChild("LDAPRealm");
   * System.out.print("\t\t\t"+t.getCurrentElement()+" : "); t.setMode(bakMode); t.pushState();
   * t.setMode(XmlTreeHandler.MODE_SELECT); t.gotoFirstChildNode(nodeType, nodeName);
   * System.out.println(t.getCurrentNode()); t.setMode(bakMode); t.popState(); if (pivotCousin) {
   * System.out.println("pivot : cousin"); } else { System.out.println("pivot : root"); }
   * t.setMode(bakMode); t.gotoSelfOrCousinParentOf(nodeType, nodeName, nodeValue); t.pushState();
   * t.setMode(XmlTreeHandler.MODE_SELECT); t.gotoFirstChildNode(nodeType, nodeName);
   * System.out.println(t.getCurrentNode()); t.setMode(bakMode); t.popState();
   * t.gotoNextCousinParentOf(nodeType, nodeName, nodeValue); t.pushState();
   * t.setMode(XmlTreeHandler.MODE_SELECT); t.gotoFirstChildNode(nodeType, nodeName);
   * System.out.println(t.getCurrentNode()); t.setMode(bakMode); t.popState();
   * t.gotoNextCousinParentOf(nodeType, nodeName, nodeValue); t.pushState();
   * t.setMode(XmlTreeHandler.MODE_SELECT); t.gotoFirstChildNode(nodeType, nodeName);
   * System.out.println(t.getCurrentNode()); t.setMode(bakMode); t.popState(); // save result String
   * action=null; switch (t.getMode()) { case XmlTreeHandler.MODE_DELETE : action="DELETE"; break;
   * case XmlTreeHandler.MODE_INSERT : action="INSERT"; break; case XmlTreeHandler.MODE_UNIQUE :
   * action="UNIQUE"; break; case XmlTreeHandler.MODE_UPDATE : action="UPDATE"; break; } String
   * node=null; switch (nodeType) { case XmlTreeHandler.TYPE_ATTRIBUTE : node="ATTRIBUTE"; break;
   * case XmlTreeHandler.TYPE_ELEMENT : node="ELEMENT"; break; } if
   * (t.getMode()!=XmlTreeHandler.MODE_SELECT) { doc.setName("PARENTOF_"+node+"("
   * +nodeName+(nodeValue==null?"":"("+nodeValue+
   * ")")+")_"+action+"_"+(pivotCousin?"cousin":"root")+"_"+doc.getName()); doc.save(); } }
   */
  public TestXmlTreeHandler() throws Exception {
    t = new XmlTreeHandler();
    doc = new XmlDocument(new File("XML4tests"), "config.xml");
    doc.load();
    System.out.println("########################################");
    System.out.println("############## SELECT #################");
    System.out.println("########################################");
    t.setMode(XmlTreeHandler.MODE_SELECT);
    System.out.println();

    System.out.println("#### NAVIGATE ####");
    System.out.println("FROM ROOT");
    test_SELECT_ELEMENT_NOTNAMED(doc.getDocument().getRootElement());
    System.out.println("FROM FIRST CHILD");
    test_SELECT_ELEMENT_NOTNAMED(doc.getDocument().getRootElement().getChild(
        "Server"));

    System.out.println("#### SEARCH ####");
    t.setStartingElement(doc.getDocument().getRootElement());
    System.out.println("FROM ROOT");
    System.out.println("FOR 'SSL'");
    test_SELECT_ELEMENT_NAMED("SSL");
    System.out.println("FOR 'WebAppComponent'");
    test_SELECT_ELEMENT_NAMED("WebAppComponent");

    System.out.println("#### ATTRIBUTE ####");
    System.out.println("FROM FIRST CHILD");
    t.setStartingElement(doc.getDocument().getRootElement().getChild("Server"));
    System.out.println("ATTIBUTE 'ListenPort'");
    test_SELECT_ATTRIBUTE("ListenPort");
    System.out.println("ATTIBUTE 'LogFileName'");
    test_SELECT_ATTRIBUTE("LogFileName");
    System.out.println("ATTIBUTE 'ServerKeyFileName'");
    test_SELECT_ATTRIBUTE("ServerKeyFileName");

    System.out.println("#### COUSIN ####");
    test_SELECT_ELEMENT_COUSIN();

    System.out.println();
    System.out.println("########################################");
    System.out.println("############## UPDATE ##################");
    System.out.println("########################################");
    t.setMode(XmlTreeHandler.MODE_UPDATE);
    System.out.println();
    System.out.println("#### ATTRIBUTE ####");
    System.out.println("ATTIBUTE 'ListenPort'");
    test_UPDATE_ATTRIBUTE("ListenPort");
    System.out.println("ATTIBUTE 'LogFileName'");
    test_UPDATE_ATTRIBUTE("LogFileName");
    System.out.println("ATTIBUTE 'ServerKeyFileName'");
    test_UPDATE_ATTRIBUTE("ServerKeyFileName");

    System.out.println("#### ELEMENT ####");
    System.out.println("FROM ROOT");
    System.out.println("ELEMENT 'SSL'");
    test_UPDATE_ELEMENT("SSL", false);
    System.out.println("ELEMENT 'WebServer'");
    test_UPDATE_ELEMENT("WebServer", false);
    System.out.println("FROM FIRST CHILD");
    System.out.println("ELEMENT 'SSL'");
    test_UPDATE_ELEMENT("SSL", true);
    System.out.println("ELEMENT 'WebServer'");
    test_UPDATE_ELEMENT("WebServer", true);

    System.out.println();
    System.out.println("########################################");
    System.out.println("############## DELETE ##################");
    System.out.println("########################################");
    t.setMode(XmlTreeHandler.MODE_DELETE);
    System.out.println();
    System.out.println("#### ATTRIBUTE ####");
    System.out.println("ATTIBUTE 'ListenPort'");
    test_DELETE_ATTRIBUTE("ListenPort");
    System.out.println("ATTIBUTE 'LogFileName'");
    test_DELETE_ATTRIBUTE("LogFileName");
    System.out.println("ATTIBUTE 'ServerKeyFileName'");
    test_DELETE_ATTRIBUTE("ServerKeyFileName");

    System.out.println("#### ELEMENT ####");
    System.out.println("FROM ROOT");
    System.out.println("ELEMENT 'SSL'");
    test_DELETE_ELEMENT("SSL", false);
    System.out.println("ELEMENT 'WebServer'");
    test_DELETE_ELEMENT("WebServer", false);
    System.out.println("FROM FIRST CHILD");
    System.out.println("ELEMENT 'SSL'");
    test_DELETE_ELEMENT("SSL", true);
    System.out.println("ELEMENT 'WebServer'");
    test_DELETE_ELEMENT("WebServer", true);
    System.out.println();
    System.out.println("########################################");
    System.out.println("############## INSERT ##################");
    System.out.println("########################################");
    t.setMode(XmlTreeHandler.MODE_INSERT);
    System.out.println();
    System.out.println("#### ATTRIBUTE ####");
    System.out.println("ATTIBUTE 'ListenPort'");
    test_INSERT_ATTRIBUTE("ListenPort");
    System.out.println("ATTIBUTE 'LogFileName'");
    test_INSERT_ATTRIBUTE("LogFileName");
    System.out.println("ATTIBUTE 'ServerKeyFileName'");
    test_INSERT_ATTRIBUTE("ServerKeyFileName");

    System.out.println("#### ELEMENT ####");
    System.out.println("FROM ROOT");
    System.out.println("ELEMENT 'SSL'");
    test_INSERT_ELEMENT("SSL", false);
    System.out.println("ELEMENT 'WebServer'");
    test_INSERT_ELEMENT("WebServer", false);
    System.out.println("FROM FIRST CHILD");
    System.out.println("ELEMENT 'SSL'");
    test_INSERT_ELEMENT("SSL", true);
    System.out.println("ELEMENT 'WebServer'");
    test_INSERT_ELEMENT("WebServer", true);

    System.out.println();
    System.out.println("########################################");
    System.out.println("############## UNIQUE ##################");
    System.out.println("########################################");
    t.setMode(XmlTreeHandler.MODE_UNIQUE);
    System.out.println();
    System.out.println("#### ATTRIBUTE ####");
    System.out.println("ATTIBUTE 'ListenPort'");
    test_UNIQUE_ATTRIBUTE("ListenPort");
    System.out.println("ATTIBUTE 'LogFileName'");
    test_UNIQUE_ATTRIBUTE("LogFileName");
    System.out.println("ATTIBUTE 'ServerKeyFileName'");
    test_UNIQUE_ATTRIBUTE("ServerKeyFileName");

    System.out.println("#### ELEMENT ####");
    System.out.println("FROM ROOT");
    System.out.println("ELEMENT 'SSL'");
    test_UNIQUE_ELEMENT("SSL", false);
    System.out.println("ELEMENT 'LDAPRealm'");
    test_UNIQUE_ELEMENT("LDAPRealm", false);
    System.out.println("FROM FIRST CHILD");
    System.out.println("ELEMENT 'SSL'");
    test_UNIQUE_ELEMENT("SSL", true);
    System.out.println("ELEMENT 'LDAPRealm'");
    test_UNIQUE_ELEMENT("LDAPRealm", true);

  }

  public static void main(String[] args) throws Exception {
    TestXmlTreeHandler testXmlTreeHandler1 = new TestXmlTreeHandler();
  }
}