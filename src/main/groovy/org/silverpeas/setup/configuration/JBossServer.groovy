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

import org.silverpeas.setup.api.Logger
import org.silverpeas.setup.api.SystemWrapper

import java.nio.file.Paths
import java.util.regex.Matcher

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

  private def logger = Logger.getLogger(getClass().getSimpleName())

  /**
   * Constructs a new instance of a JBossServer wrapping the specified JBoss/Wildfly installation.
   * By default, the new instance is set up to work with a full JEE profile of a JBoss's standalone
   * mode.
   * @param jbossHome the path of the JBoss/Wildfly home directory.
   */
  JBossServer(String jbossHome) {
    this.jbossHome = jbossHome
    if (SystemWrapper.getProperty('os.name').toLowerCase().indexOf('win') >= 0) {
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

  JBossServer useLogger(logger) {
    this.logger = logger
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
      logger.info 'A JBoss instance is already started'
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
      logger.info 'A JBoss instance is already started'
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
      logger.info 'No JBoss instance running'
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
    String config = new File("${jbossHome}/standalone/configuration/standalone-full.xml").text
    return config.contains('java:/datasources/DocumentStore') &&
        config.contains('java:/datasources/Silverpeas')
  }

  /**
   * Deploys the artifacts at the specified path into this JBoss server by using the Management API.
   * If the server isn't running, then it is started before.
   * @param artifactsPath
   * @throws RuntimeException if the deployment of the artifact failed.
   */
  void deploy(String artifactsPath) throws RuntimeException {
    if (!isRunning()) {
      start()
    }
    String artifact = Paths.get(artifactsPath).fileName
    if (!isDeployed(artifact)) {
      def proc = """${cli} --connect --command="deploy ${artifactsPath}" """.execute()
      proc.waitFor()
      if (proc.exitValue() != 0) {
        throw new RuntimeException(proc.in.text)
      }
    } else {
      logger.info "${artifact} is already deployed in JBoss"
    }
  }

  /**
   * Undeploys the specified artifact from this JBoss server by using the Management API.
   * If the server isn't running, then it is started before.
   * @param artifactName
   * @throws RuntimeException if the deployment of the artifact failed.
   */
  void undeploy(String artifact) throws RuntimeException {
    if (!isRunning()) {
      start()
    }
    if (isDeployed(artifact)) {
      def proc = """${cli} --connect --command="undeploy ${artifact}" """.execute()
      proc.waitFor()
      if (proc.exitValue() != 0) {
        throw new RuntimeException(proc.in.text)
      }
    } else {
      logger.info "${artifact} isn't deployed in JBoss"
    }
  }

  /**
   * Is the specified artifact is deployed?
   * @param artifact the artifact to check its deployment status.
   * @return true if the artifact is deployed, false otherwise.
   */
  boolean isDeployed(String artifact) {
    if (!isRunning()) {
      start()
    }
    def proc = """${cli} --connect /deployment=${artifact}:read-attribute(name=enabled)""".execute()
    proc.waitFor()
    return proc.exitValue() == 0 && proc.text.contains('"result" => true')
  }

  /**
   * Prints out information about the deployment status of artifacts in this JBoss server. It an
   * artifact doesn't appear in the output, then it isn't deployed.
   */
  void printDeployedArtifacts() {
    if (!isRunning()) {
      start()
    }
    def proc = """${cli} --connect --command=deployment-info""".execute()
    proc.waitFor()
  }

  /**
   * Configures JBoss by running the JBoss CLI statements in the specified commands file.
   * <p>
   * If the command file contains the following line:</p>
   * <pre><code>#check: SUBSYSTEM_RESOURCE</code></pre>
   * <p>with SUBSYSTEM_RESOURCE as the path of a resource in a subsystem of JBoss/Wildfly, then
   * the method will check the specified resource exists before executing the command file;
   * the command file wil be processed only if the specified resource doesn't already exist in
   * JBoss/Wildfly. For example:</p>
   * <pre><code>#check: /subsystem=datasources/data-source=Silverpeas</code></pre>
   * <p>specified to check the datasource Silverpeas already exists in JBoss/Wildfly.</p>
   * @param commandsFile the commands file to eat.
   * @param output the file into which outputs of the command file processing will be traced. If null,
   * the output of the processing will output to the standard output.
   */
  void processCommandFile(File commandsFile, File output) {
    assertJBossIsRunning()
    try {
      logger.info "${commandsFile.name} processing..."

      if (isAlreadyProcessed(commandsFile)) {
        logger.info "${commandsFile.name} processing: [ALREADY DONE]"
      } else {
        ProcessBuilder command = new ProcessBuilder(cli, '--connect', "--file=${commandsFile.path}")
            .redirectErrorStream(true)
        if (output != null) {
          command.redirectOutput(output)
        } else {
          command.inheritIO()
          logger.info command.redirectOutput()
        }
        def proc = command.start()
        proc.waitFor()
        assertCommandSucceeds(proc)
        logger.info "${commandsFile.name} processing: [OK]"
      }
    } catch (InvalidObjectException e) {
      logger.info "${commandsFile.name} processing: [WARNING]"
      logger.warn "Invalid resource. ${e.message}"
    } catch (AssertionError | Exception e) {
      logger.info "${commandsFile.name} processing: [FAILURE]"
      logger.error e
    }
  }

  private boolean isAlreadyProcessed(File commandsFile) {
    boolean processed = false
    List<String> lines = commandsFile.readLines()
    for (int i = 0; i < lines.size() && !processed; i++) {
      Matcher matcher = lines.get(i)  =~ /\s*#check:\s+(.+)/
      matcher.each { token ->
        def proc = """${cli} --connect --command="${token[1]}:read-resource" """.execute()
        proc.waitFor()
        if (proc.exitValue() == 0 && proc.in.text.contains('"outcome" => "success"')) {
          processed = true
        }
      }
    }
    return processed
  }
}