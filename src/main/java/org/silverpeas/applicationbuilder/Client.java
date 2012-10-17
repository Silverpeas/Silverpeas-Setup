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

//Source file: R:\\StraProduct\\Pkg1.0\\Dev\\SrcJava\\Java\\ApplicationBuilder\\JBuilderEnv\\src\\com\\silverpeas\\applicationbuilder\\Client.java

package org.silverpeas.applicationbuilder;

import java.io.File;

/**
 * Represent the archive containing the client part of the application. It is filled with parts from
 * contributions. The filling can be achieved on the fly by the means of streams.
 * @author Silverpeas
 * @version 1.0/B
 * @since 1.0/B
 */
public class Client extends WriteOnlyArchive {

  /**
   * The name of the client archive to build
   * @since 1.0
   */
  private static final String NAME = "silverpeas-client.jar";

  public Client(File directory) throws AppBuilderException {
    super(directory, NAME);
  }

  /**
   * @roseuid 3AAE3D800345
   */
  public void mergeClientPart(ReadOnlyArchive clientPart)
      throws AppBuilderException {
    mergeWith(clientPart, MANIFEST_PATH);
  }

  /**
   * @roseuid 3AAE3D800345
   */
  public void mergeLibraries(ReadOnlyArchive[] libraries)
      throws AppBuilderException {
    for (int iLib = 0; iLib < libraries.length; iLib++) {
      try {
        mergeWith(libraries[iLib], MANIFEST_PATH);
      } catch (AppBuilderException abe) {
        Log
            .add("Could not merge library \"" + libraries[iLib].getName()
            + "\"");
        throw abe;
      }
    }
  }
}
