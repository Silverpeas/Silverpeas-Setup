/**
 * Copyright (C) 2000 - 2013 Silverpeas
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
package org.silverpeas.applicationbuilder;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import org.silverpeas.applicationbuilder.maven.MavenContribution;
import org.silverpeas.applicationbuilder.maven.MavenRepository;
import org.silverpeas.util.Console;
import org.silverpeas.util.file.DirectoryLocator;

/**
 * The main class of the ApplicationBuilder tool. Controls the overall sequence of the process.
 * Holds the general information about the installed application structure.
 *
 * @author Silverpeas
 * @version 1.0/B
 * @since 1.0/B
 */
public class ApplicationBuilder {

  private static final String APP_BUILDER_VERSION = "Application Builder "
      + ResourceBundle.getBundle("messages").getString("silverpeas.version");
  private static final String APPLICATION_NAME = "Silverpeas";
  private static final String APPLICATION_DESCRIPTION = "Collaborative portal organizer";
  private static final String APPLICATION_ROOT = ResourceBundle.getBundle("messages").getString(
      "application.root.context");
  private EAR theEAR = null;
  private MavenRepository theRepository = null;
  private MavenRepository theExternalRepository = null;
  private final Console console;

  public ApplicationBuilder() throws AppBuilderException, IOException {
    console = new Console(ApplicationBuilder.class);
    boolean errorFound = false;

    // instantiates source and target objects
    try {
      theRepository = new MavenRepository(console);
    } catch (AppBuilderException abe) {
      console.printError("", abe);
      errorFound = true;
    }
    try {
      theEAR = new EAR(new File(DirectoryLocator.getLibraryHome()), console);
    } catch (AppBuilderException abe) {
      console.printError("", abe);
      errorFound = true;
    }
    if (errorFound) {
      throw new AppBuilderException();
    }
  }

  /**
   * Gets the Repository object
   *
   * @return the repository object
   * @since 1.0/B
   * @roseuid 3AAF75C6001A
   */
  public MavenRepository getRepository() {
    return theRepository;
  }

  /**
   * Gets the External Repository object
   *
   * @return the repository object
   * @since 1.0/B
   * @roseuid 3AAF75C6001A
   */
  public MavenRepository getExternalRepository() {
    return theExternalRepository;
  }

  /**
   * @return the EAR object
   * @since 1.0/B
   * @roseuid 3AAF989A0256
   */
  public EAR getEAR() {
    return theEAR;
  }

  /**
   * The unique method that provides the application name
   *
   * @roseuid 3AAF9A5300BF
   */
  public static String getApplicationName() {
    return APPLICATION_NAME;
  }

  /**
   * The unique method that provides the application description
   */
  public static String getApplicationDescription() {
    return APPLICATION_DESCRIPTION;
  }

  /**
   * @return the root used to access the application with a browser
   */
  public static String getApplicationRoot() {
    return APPLICATION_ROOT;
  }

  private void makeArchivesToDeploy() throws AppBuilderException, IOException {
    console.printMessage("CHECKING REPOSITORY");
    console.printMessage("Repository OK");

    console.printMessage("GENERATING APPLICATION FOR JBOSS");
    console.setEchoAsDotEnabled(true);
    // get the contributions
    MavenContribution[] contributions = getRepository().getContributions();
    for (MavenContribution contribution : contributions) {
      console.printMessage("ADDING \"" + contribution.getPackageName() + "\" of type \""
          + contribution.
          getPackageType() + '"');
      if (null != contribution.getLibraries()) {
        console.printTrace("merging libraries");
        getEAR().addLibraries(contribution.getLibraries());
      }
      if (null != contribution.getClientPart()) {
        console.printTrace("merging client part");
        getEAR().addLibrary(contribution.getClientPart());
      }
      if (null != contribution.getWARPart()) {
        console.printTrace("merging WAR part");
        getEAR().getWAR().mergeWARPart(contribution.getWARPart());
      }
      if (null != contribution.getEJBs()) {
        console.printTrace("adding EJBs");
        getEAR().addEJBs(contribution.getEJBs());
      }
      if (null != contribution.getExternals()) {
        console.printTrace("adding External Wars");
        getEAR().addExternalWars(contribution.getExternals());
      }

    }

    if (null != getExternalRepository()) {
      MavenContribution[] lesContributionsExternes = getExternalRepository().getContributions();
      for (MavenContribution maContrib : lesContributionsExternes) {
        console.printMessage("ADDING \"" + maContrib.getPackageName() + "\" of type \"" + maContrib.
            getPackageType() + '"');
        if (null != maContrib.getClientPart()) {
          console.printTrace("merging client part");
        }
        if (null != maContrib.getLibraries()) {
          console.printTrace("merging libraries");
        }
        if (null != maContrib.getWARPart()) {
          console.printTrace("merging WAR part");
          getEAR().getWAR().mergeWARPart(maContrib.getWARPart());
        }
        if (null != maContrib.getEJBs()) {
          console.printTrace("adding EJBs");
          getEAR().addEJBs(maContrib.getEJBs());
        }
        if (null != maContrib.getExternals()) {
          console.printTrace("adding External Wars");
          getEAR().addExternalWars(maContrib.getExternals());
        }

      }
    }
    getEAR().close();
    console.setEchoAsDotEnabled(false);
    console.printMessage("OK : \"" + getEAR().getName() + "\" successfully builded");
    console.printMessage("Please find them in \"" + DirectoryLocator.getLibraryHome() + '"');
    System.out.println("\r\nFull log is available in \"" + DirectoryLocator.getLogHome()
        + File.separatorChar + "application_build.log \"");
  }

  private void endLoggingWithErrors() {
    console.setEchoAsDotEnabled(false);
    console.printError("ERRORS encountered : build aborted");
    System.err.println("see \"" + DirectoryLocator.getLogHome() + File.separatorChar
        + "application_build.log \" for details");
    console.close();
  }

  public static void main(String[] args) throws IOException, AppBuilderException {
    ApplicationBuilder appBuilder = new ApplicationBuilder();
    try {
      appBuilder.console.printMessage("___ " + APP_BUILDER_VERSION + " ___");
      System.out.println("___ " + APP_BUILDER_VERSION + " ___");
      appBuilder.makeArchivesToDeploy();
    } catch (AppBuilderException abe) {
      appBuilder.console.printError(abe.getMessage(), abe);
      appBuilder.endLoggingWithErrors();
      System.exit(1);
    } catch (Exception t) {
      appBuilder.console.printError(t.getMessage(), t);
      appBuilder.endLoggingWithErrors();
      System.exit(1);
    } finally {
      appBuilder.console.close();
    }
  }
}
