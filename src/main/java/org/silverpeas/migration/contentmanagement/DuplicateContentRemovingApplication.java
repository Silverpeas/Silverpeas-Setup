/*
 * Copyright (C) 2000 - 2012 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection withWriter Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.migration.contentmanagement;

import java.io.IOException;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.silverpeas.util.Console;
import org.silverpeas.dbbuilder.sql.ConnectionFactory;

/**
 * Application that can be ran in an explicit way without running DBBuilder for that.
 */
public class DuplicateContentRemovingApplication {

  private static Console console;

  public static void main(String[] args) {
    try {
      init();
      DuplicateContentRemoving migration = new DuplicateContentRemoving();
      migration.setConsole(console);
      migration.setConnection(ConnectionFactory.getConnection());
      migration.migrate();
    } catch (Exception ex) {
      console.printError(ex.getMessage(), ex);
    } finally {
      console.close();
    }
  }

  private static void init() throws IOException {
    new ClassPathXmlApplicationContext("classpath:/spring-jdbc-datasource.xml");
    console = new Console("duplicateContentRemoving.log", DuplicateContentRemovingApplication.class);
  }
}
