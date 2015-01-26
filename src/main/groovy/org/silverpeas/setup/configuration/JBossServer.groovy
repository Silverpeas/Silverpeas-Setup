/*
    Copyright (C) 2000 - 2015 Silverpeas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    As a special exception to the terms and conditions of version 3.0 of
    the GPL, you may redistribute this Program in connection with Free/Libre
    Open Source Software ("FLOSS") applications as described in Silverpeas's
    FLOSS exception.  You should have recieved a copy of the text describing
    the FLOSS exception, and it is also available here:
    "http://www.silverpeas.org/docs/core/legal/floss_exception.html"

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.setup.configuration
/**
 * It wraps an existing installation of a JBoss application server. It provides functions to
 * interact with the JBoss AS (either Wildfly or JBoss EAP) in order to start/stop it or to
 * configure it. The configuration is done through command files with JBoss CLI statements.
 * @author mmoquillon
 */
class JBossServer {

  private String cli

  private String starter

  private String jbossHome

  private File redirection = null

  /**
   * Constructs a new instance of a JBossServer wrapping the specified JBoss/Wildfly installation.
   * By default, the new instance is set up to work with a full JEE profile of a JBoss's standalone
   * mode.
   * @param jbossHome the path of the JBoss/Wildfly home directory.
   */
  JBossServer(String jbossHome) {
    this.jbossHome = jbossHome
    if (System.getProperty('os.name').toLowerCase().indexOf('win') >= 0) {
      this.cli = "${jbossHome}/bin/jboss-cli.bat"
      this.starter = "${jbossHome}/bin/standalone.bat"
    } else {
      this.cli = "${jbossHome}/bin/jboss-cli.sh"
      this.starter = "${jbossHome}/bin/standalone.sh"
    }
  }

  private void assertJBossIsRunning() {
    if (!isRunning()) {
      throw new AssertionError('JBoss not running')
    }
  }

  private void assertCommandSucceeds(command) throws AssertionError, InvalidObjectException {
    if (command.exitValue() != 0) {
      String message = command.err.text
      if (!message) {
        throw new InvalidObjectException(command.in.text)
      }
      throw new AssertionError(message)
    }
  }

  /**
   * Asks for the redirection of JBoss/Wildfly outputs to the specified log file.
   * @param log the file into which the JBoss/Wildfly will be redirected.
   * @return itself.
   */
  JBossServer redirectOutputTo(File log) {
    this.redirection = log
    return this
  }

  /**
   * Starts an instance of the JBoss/Wildfly server in a standalone mode (full JEE profile).
   * If an instance of JBoss/Wildfly is already running, then nothing is done.
   */
  void start() {
    if (!isRunning()) {
      ProcessBuilder process =
          new ProcessBuilder(starter, '-c', 'standalone-full.xml', '-b', '0.0.0.0')
              .directory(new File(jbossHome))
              .redirectErrorStream(true)
      if (redirection != null) {
        process.redirectOutput(redirection)
      } else {
        process.inheritIO()
        System.println(process.redirectOutput())
      }
      process.start()
      while (!isRunning()) {
        sleep(1000);
      }
    } else {
      println 'A JBoss instance is already started'
    }
  }

  /**
   * Starts in debug an instance of the JBoss/Wildfly server in a standalone mode (full JEE profile).
   * If an instance of JBoss/Wildfly is already running, then nothing is done.
   */
  void debug() {
    if (!isRunning()) {
      ProcessBuilder process =
          new ProcessBuilder(starter, '-debug', '-c', 'standalone-full.xml', '-b', '0.0.0.0')
              .directory(new File(jbossHome))
              .redirectErrorStream(true)
      if (redirection != null) {
        process.redirectOutput(redirection)
      } else {
        process.inheritIO()
        System.println(process.redirectOutput())
      }
      process.start()
      while (!isRunning()) {
        sleep(1000);
      }
    } else {
      println 'A JBoss instance is already started'
    }
  }

  /**
   * Stops a running JBoss/Wildfly instance. If no JBoss/Wildfly instance is running, then nothing
   * is done.
   */
  void stop() {
    if (isRunning()) {
      def proc = """${cli} --connect --command=:shutdown""".execute()
      proc.waitFor()
    } else {
      println 'No JBoss instance running'
    }
  }

  /**
   * Is a JBoss/Wildfly instance running?
   * @return true if an instance of JBoss/Wildfly is running, false otherwise.
   */
  boolean isRunning() {
    def proc = """${cli} --connect --command=:read-attribute(name=server-state)""".execute()
    proc.waitFor()
    return proc.exitValue() == 0
  }

  /**
   * Is JBoss is already configured for Silverpeas?
   * @return true if JBoss/Wildfly is already configured for Silverpeas, false otherwise. This
   * method returns true even if the configuration for Silverpeas isn't complete.
   */
  boolean isAlreadyConfigured() {
    return new File("${jbossHome}/standalone/configuration/standalone-full.xml")
        .text.contains('silverpeas')
  }

  /**
   * Configures JBoss by running the JBoss CLI statements in the specified commands file.
   * @param commandsFile the commands file to eat.
   */
  void processCommandFile(File commandsFile, File output) {
    assertJBossIsRunning()
    try {
      println "${commandsFile.name} processing..."
      ProcessBuilder command = new ProcessBuilder(cli, '--connect', "--file=${commandsFile.path}")
          .redirectErrorStream(true)
      if (output != null) {
        command.redirectOutput(output)
      } else {
        command.inheritIO()
        System.println(command.redirectOutput())
      }
      def proc = command.start()
      //def proc = """${cli} --file=${commandsFile.path}""".execute()
      proc.waitFor()
      assertCommandSucceeds(proc)
      println "${commandsFile.name} processing: [OK]"
    } catch (InvalidObjectException e) {
      println "${commandsFile.name} processing: [WARNING]"
      println e.message
    } catch (AssertionError | Exception e) {
      println "${commandsFile.name} processing: [FAILURE]"
      println e.message
    }
  }
}