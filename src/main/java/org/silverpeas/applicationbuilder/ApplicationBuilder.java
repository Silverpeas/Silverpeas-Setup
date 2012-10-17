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

package org.silverpeas.applicationbuilder;

import org.silverpeas.applicationbuilder.maven.MavenContribution;
import org.silverpeas.applicationbuilder.maven.MavenRepository;
import java.io.File;

import org.silverpeas.installedtree.DirectoryLocator;

/**
 * The main class of the ApplicationBuilder tool. Controls the overall sequence of the process.
 * Holds the general information about the installed application structure.
 * @author Silverpeas
 * @version 1.0/B
 * @since 1.0/B
 */
public class ApplicationBuilder {

  private static final String APP_BUILDER_VERSION = "Application Builder V5.0";
  private static final String APPLICATION_NAME = "Silverpeas";
  private static final String APPLICATION_DESCRIPTION = "Collaborative portal organizer";
  private static final String APPLICATION_ROOT = "silverpeas";
  private static String extRepositoryPath = null;
  private EAR theEAR = null;
  private MavenRepository theRepository = null;
  private MavenRepository theExternalRepository = null;

  public ApplicationBuilder() throws AppBuilderException {
    boolean errorFound = false;
    // instantiates source and target objects
    try {
      theRepository = new MavenRepository();
      if (extRepositoryPath != null) {
        DirectoryLocator.setRepositoryHome(extRepositoryPath);
        theExternalRepository = new MavenRepository();
      }
    } catch (AppBuilderException abe) {
      Log.add(abe);
      errorFound = true;
    }
    try {
      theEAR = new EAR(new File(DirectoryLocator.getLibraryHome()));
    } catch (AppBuilderException abe) {
      Log.add(abe);
      errorFound = true;
    }
    if (errorFound) {
      throw new AppBuilderException();
    }
  }

  /**
   * Gets the Repository object
   * @return the repository object
   * @since 1.0/B
   * @roseuid 3AAF75C6001A
   */
  public MavenRepository getRepository() {
    return theRepository;
  }

  /**
   * Gets the External Repository object
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
   * @return the Client object
   * @since 1.0/B
   * @roseuid 3AAFA81703A2
   */
  /*
   * public Client getClient() { return theClient; }
   */
  /**
   * The unique method that provides the application name
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

  private static void makeArchivesToDeploy() throws AppBuilderException {
    Log.echo("CHECKING REPOSITORY");
    ApplicationBuilder appBuilder = new ApplicationBuilder();
    Log.add("Repository OK");

    Log.echo("GENERATING APPLICATION FOR JBOSS");
    Log.setEchoAsDotEnabled(true);
    // get the contributions
    MavenContribution[] lesContributions = appBuilder.getRepository().getContributions();

    // loop the contributions
    for (MavenContribution maContrib : lesContributions) {
      // identify the contribution
      Log.add("");
      Log.add("ADDING \"" + maContrib.getPackageName() + "\" of type \"" +
          maContrib.getPackageType() + "\"");
      // libraries
      if (maContrib.getLibraries() != null) {
        Log.add("merging libraries");
        appBuilder.getEAR().addLibraries(maContrib.getLibraries());
      }
      // client
      if (maContrib.getClientPart() != null) {
        Log.add("merging client part");
        appBuilder.getEAR().addLibrary(maContrib.getClientPart());
      }

      // WAR
      if (maContrib.getWARPart() != null) {
        Log.add("merging WAR part");
        appBuilder.getEAR().getWAR().mergeWARPart(maContrib.getWARPart());
      }
      // EJBs
      if (maContrib.getEJBs() != null) {
        Log.add("adding EJBs");
        appBuilder.getEAR().addEJBs(maContrib.getEJBs());
      }

      if (maContrib.getExternals() != null) {
        Log.add("adding External Wars");
        appBuilder.getEAR().addExternalWars(maContrib.getExternals());
      }

    }

    if (appBuilder.getExternalRepository() != null) {
      // loop the external contributions
      MavenContribution[] lesContributionsExternes =
          appBuilder.getExternalRepository().getContributions();
      for (MavenContribution maContrib : lesContributionsExternes) {
        // identify the contribution
        Log.add("");
        Log.add("ADDING \"" + maContrib.getPackageName() + "\" of type \"" +
            maContrib.getPackageType() + "\"");
        // client
        if (maContrib.getClientPart() != null) {
          Log.add("merging client part");
        }
        // libraries
        if (maContrib.getLibraries() != null) {
          Log.add("merging libraries");
        }
        // WAR
        if (maContrib.getWARPart() != null) {
          Log.add("merging WAR part");
          appBuilder.getEAR().getWAR().mergeWARPart(maContrib.getWARPart());
        }
        // EJBs
        if (maContrib.getEJBs() != null) {
          Log.add("adding EJBs");
          appBuilder.getEAR().addEJBs(maContrib.getEJBs());
        }
        // External WARs
        if (maContrib.getExternals() != null) {
          Log.add("adding External Wars");
          appBuilder.getEAR().addExternalWars(maContrib.getExternals());
        }

      }
    }

    appBuilder.getEAR().close();
    Log.add("");
    Log.setEchoAsDotEnabled(false);
    Log.echo("OK : \"" + appBuilder.getEAR().getName() + "\" successfully builded");
    Log.echo("Please find them in \"" + DirectoryLocator.getLibraryHome() + "\"");
    System.out.println(
        "Full log is available in \"" + DirectoryLocator.getLogHome() + File.separator +
        Log.getName() + "\"");
  }

  private static void endLoggingWithErrors() {
    Log.setEchoAsDotEnabled(false);
    Log.add("");
    Log.echo("ERRORS encountered : build aborted", System.err);
    System.err.println("see \"" + DirectoryLocator.getLogHome()
        + File.separator + Log.getName() + "\" for details");
  }

  /**
   * @roseuid 3AB1EC140270
   */
  public static void main(String[] args) {
    try {
      Log.echo("___ " + APP_BUILDER_VERSION + " ___");
      Log.add("");
      makeArchivesToDeploy();
    } catch (AppBuilderException abe) {
      Log.echo(abe);
      endLoggingWithErrors();
    } catch (Throwable t) {
      Log.echo(t);
      endLoggingWithErrors();
    } finally {
      Log.close();
    }
  }
}
