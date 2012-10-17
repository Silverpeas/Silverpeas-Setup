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

package org.silverpeas.tests.xpath;

import java.io.File;

import org.silverpeas.applicationbuilder.XmlDocument;
import org.silverpeas.xml.XmlTreeHandler;
import org.silverpeas.xml.xpath.XPath;
import org.silverpeas.xml.xpath.XPathParseException;

/**
 * Titre : Application Builder Description : Copyright : Copyright (c) 2001 Société : Stratélia
 * @author Jean-Christophe Carry
 * @version 2.0
 */

public class TestXPath {

  private XPath xpathObj = new XPath();
  private XmlDocument doc = null;

  public TestXPath() throws Exception {
    System.out.println();
    System.out.println("###############################");
    System.out.println("######### SYNTAX ##############");
    System.out.println("###############################");
    System.out.println();
    System.out.println("##### ERRORS ######");
    System.out.println();
    System.out.println("# empty path");
    testSyntax("");
    System.out.println("# é");
    testSyntax("/Domain/Sérver/@NativeIOEnabled");
    System.out.println("# mauvais prédicat");
    testSyntax("/WAAddressbooks/param[param-name'Home']");
    System.out.println("# &");
    testSyntax("param&value");
    System.out.println("# litéral mal délimité");
    testSyntax("./WAAddressbooks[name=\"shmilblick\"]/param[param-name=Home']");
    System.out.println();
    System.out.println("##### RIGHT ONES ######");
    System.out.println();
    testSyntax("/Domain/Server/@NativeIOEnabled");
    testSyntax("/WAAddressbooks/param[param-name='Home']");
    testSyntax("param-value");
    testSyntax("./WAAddressbooks[name=\"shmilblick\"]/param[param-name='Home']");
    System.out.println();
    System.out.println("################################");
    System.out.println("######### ACTIONS ##############");
    System.out.println("################################");
    System.out.println();
    System.out.println("######### SELECT ##############");
    doc = new XmlDocument(new File("XML4tests"), "config4parentof.xml");
    doc.load();
    xpathObj.setStartingElement(doc.getDocument().getRootElement());
    xpathObj.setMode(XmlTreeHandler.MODE_SELECT);
    testACTION("/@Name");
    testACTION("/Server/@ListenPort");
    testACTION("/Server");
    testACTION("/Server/SSL/@ServerKeyFileName");
    testACTION("/cousin/brother/LDAPRealm[@Name='defaultLDAPRealmForNetscapeDirectoryServer']/bidon");
    testACTION("/cousin/brother/LDAPRealm[@Name='defaultLDAPRealmForNovellDirectoryServices']/bidon");
    System.out.println();
    System.out.println("######### UNIQUE ##############");
    xpathObj.setMode(XmlTreeHandler.MODE_UNIQUE);
    testACTION("/JMSServer[@Name='SilverpeasJMSServer']");
    xpathObj.setNodeAsStart();
    testACTION("@Targets");
    xpathObj.setValue("Serveur1");
    testACTION("/JMSServer[@Name='SilverpeasJMSServer']/JMSQueue[@Name='com.stratelia.silverpeas.backbone.soap.jms.soapQueue']");
    xpathObj.setNodeAsStart();
    testACTION("@JNDIName");
    xpathObj.setValue("com.stratelia.silverpeas.backbone.soap.jms.soapQueue");
    testACTION("@StoreEnabled");
    xpathObj.setValue("default");
    testACTION("/JMSConnectionFactory[@Name='com.stratelia.silverpeas.backbone.soap.jms.QueueConnectionFactory']");
    xpathObj.setNodeAsStart();
    testACTION("@JNDIName");
    xpathObj
        .setValue("com.stratelia.silverpeas.backbone.soap.jms.QueueConnectionFactory");
    testACTION("@Targets");
    xpathObj.setValue("Serveur1");
    doc.setName("XPATH_UNIQUE.xml");
    doc.save();
    System.out.println("######### DELETE ##############");
    doc = new XmlDocument(new File("XML4tests"), "config.xml");
    doc.load();
    xpathObj.setStartingElement(doc.getDocument().getRootElement());
    xpathObj.setMode(XmlTreeHandler.MODE_DELETE);
    testACTION("/LDAPRealm[@Name='defaultLDAPRealmForMicrosoftSiteServer']");
    doc.setName("XPATH_DELETE.xml");
    doc.save();
    System.out.println("######### INSERT ##############");
    doc = new XmlDocument(new File("XML4tests"), "data-sources.xml");
    doc.load();
    xpathObj.setStartingElement(doc.getDocument().getRootElement());
    xpathObj.setMode(XmlTreeHandler.MODE_INSERT);
    testACTION("/data-source[@Name='SilverpeasDataSource']");
    xpathObj.setNodeAsStart();
    testACTION("@url");
    xpathObj.setValue("jdbc:inetdae7:stratdev4?database=SilverpeasBeaujolais");
    testACTION("@connection-driver");
    xpathObj.setValue("com.inet.tds.TdsDriver");
    doc.setName("XPATH_INSERT.xml");
    doc.save();
    System.out.println("######### UPDATE ##############");
    doc = new XmlDocument(new File("XML4tests"), "data-sources.xml");
    doc.load();
    xpathObj.setStartingElement(doc.getDocument().getRootElement());
    xpathObj.setMode(XmlTreeHandler.MODE_UPDATE);
    testACTION("/data-source[@name='SilverpeasDataSource']");
    xpathObj.setNodeAsStart();
    testACTION("@url");
    xpathObj.setValue("jdbc:inetdae7:pc00062?database=SilverpeasProduction");
    testACTION("@username");
    xpathObj.setValue("silverdbuser");
    testACTION("@password");
    xpathObj.setValue("very secret passwd");
    doc.setName("XPATH_UPDATE.xml");
    doc.save();
  }

  private void testSyntax(String xpath) {
    System.out.print("'" + xpath + "'");
    xpathObj.setXPath(xpath);
    try {
      xpathObj.parse();
      System.out.println(" successfull (abs:" + xpathObj.isAbsolute()
          + ";attrib:" + xpathObj.isAttribute() + ")");
    } catch (XPathParseException ex) {
      System.out.println(" ERROR : " + ex.getMessage());
    }
  }

  private void testACTION(String xpath) {
    xpathObj.setXPath(xpath);
    System.out.println("'" + xpath + "'");
    try {
      xpathObj.parse();
      System.out.print("\tSUCCESS\n\t");
      Boolean b = xpathObj.exists();
      if (b != null && b.booleanValue()) {
        System.out.println("EXISTS : " + xpathObj.getNode() + " "
            + xpathObj.getValue());
      } else {
        System.out.println("DOESN'T EXIST");
      }
    } catch (XPathParseException ex) {
      System.out.println(" ERROR : " + ex.getMessage());
    }
  }

  public static void main(String[] args) throws Exception {
    TestXPath t = new TestXPath();
  }
}