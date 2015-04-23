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
import org.silverpeas.setup.api.SilverpeasSetupService
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

  private int debugging = 0

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

  private void assertJBossIsRunning()  {
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
   * Waits for the JBoss/Wildfly is running. Be caution, this method can never returns if it is
   * invoked while no JBoss/Wildfly instance is started.
   */
  private void waitUntilRunning() {
    logger.formatInfo(' %s', '.')
    while(!isRunning()) {
      Thread.sleep(1000)
      logger.formatInfo('%s', '.')
    }
  }

  /**
   * Ensures the JBoss/Wildfly instance is running before performing the specified action.
   * @param action the action to run against a running JBoss/Wildfly instance.
   * @return the result value of the closure if any.
   */
  private def doWhenRunning(Closure action) {
    switch (status()) {
      case 'stopped':
        logger.info 'JBoss not started, so start it'
        start()
        break
      case 'starting':
        logger.info 'JBoss is starting, so wait for it is running'
        waitUntilRunning()
        break
      case 'reload-required':
        logger.info 'JBoss asks for reload, so reloads it'
        def proc = """${cli} --connect --command=:reload""".execute()
        proc.waitFor()
        waitUntilRunning()
        break
    }
    action.call()
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
   * The logger to use with this JBossService instance to output any traces about its actions.
   * @param logger the logger to use.
   * @return itself.
   */
  JBossServer useLogger(logger) {
    this.logger = logger
    return this
  }

  /**
   * Asks JBoss to run by default in debugging mode when its start command is invoked.
   * @param port the debugging port. If lesser or equal to 1000 or not set, the default port 5005
   * will be used.
   * @return itself.
   */
  JBossServer forceDebugMode(int port = 5005) {
    this.debugging = (port <= 1000 ? 5005:port)
    return this
  }

  /**
   * Starts an instance of the JBoss/Wildfly server in a standalone mode (full JEE profile).
   * If an instance of JBoss/Wildfly is already running, then nothing is done.
   * If the debug mode is forced, then it starts in debug.
   */
  void start() {
    if (debugging >= 1000) {
      debug(debugging)
    } else {
      if (!isStartingOrRunning()) {
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
        waitUntilRunning()
      } else {
        logger.info 'A JBoss instance is already started'
      }
    }
  }

  /**
   * Starts in debug an instance of the JBoss/Wildfly server in a standalone mode (full JEE profile).
   * If an instance of JBoss/Wildfly is already running, then nothing is done.
   * @param port the debugging port. If lesser or equal to 1000 or not specified, the default port
   * 5005 will be used.
   */
  void debug(int port = 5005) {
    if (!isStartingOrRunning()) {
      String p = (port <= 1000 ? '5005':String.valueOf(port))
      ProcessBuilder process =
          new ProcessBuilder(starter, '-c', 'standalone-full.xml', '-b', '0.0.0.0', '--debug', p)
              .directory(new File(jbossHome))
              .redirectErrorStream(true)
      if (redirection != null) {
        process.redirectOutput(redirection)
      } else {
        process.inheritIO()
        System.println(process.redirectOutput())
      }
      process.start()
      waitUntilRunning()
      println "Silverpeas Debugging Port is ${p}"
    } else {
      logger.info 'A JBoss instance is already started'
    }
  }

  /**
   * Stops a running JBoss/Wildfly instance. If no JBoss/Wildfly instance is running, then nothing
   * is done.
   */
  void stop() {
    if (isStartingOrRunning()) {
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
    return status() == 'running'
  }

  /**
   * Is a JBoss/Wildfly instance being starting?
   * @return true if an instance of JBoss/Wildfly is being starting, false otherwise.
   */
  boolean isStarting() {
    return status() == 'starting'
  }

  /**
   * Is a JBoss/Wildfly instance running or being starting?
   * @return true if an instance of JBoss/Wildfly is either running or being starting,
   * false otherwise.
   */
  boolean isStartingOrRunning() {
    String status = status()
    return status == 'starting' || status == 'running'
  }

  /**
   * Is a JBoss/Wildfly instance stopped?
   * @return true if an instance of JBoss/Wildfly is stopped, false otherwise.
   */
  boolean isStopped() {
    return status() == 'stopped'
  }

  /**
   * Gets the status of this JBoss/Wildfly instance: running, starting, stopped, ...
   * @return the status of the JBoss/Wildfly instance wrapped by this object as a String value.
   */
  String status() {
    StringBuilder output = new StringBuilder()
    def proc = """${cli} --connect --command=:read-attribute(name=server-state)""".execute()
    proc.waitForProcessOutput(output, output)
    String status = 'stopped'
    if (proc.exitValue() == 0) {
      Matcher matcher = output.toString() =~ /"result" => "(\w+)"/
      matcher.each { token ->
        status = token[1]
      }
    }
    return status
  }

  /**
   * Is JBoss is already configured for Silverpeas?
   * @return true if JBoss/Wildfly is already configured for Silverpeas, false otherwise. This
   * method returns true even if the configuration for Silverpeas isn't complete.
   */
  boolean isAlreadyConfigured() {
    String config = new File("${jbossHome}/standalone/configuration/standalone-full.xml").text
    return config.contains('java:/datasources/DocumentStore') &&
        config.contains('java:/datasources/silverpeas')
  }

  /**
   * Deploys the artifacts at the specified path into this JBoss server by using the Management API.
   * If the server isn't running, then it is started before.
   * @param artifactsPath
   * @throws RuntimeException if the deployment of the artifact failed.
   */
  void deploy(String artifactsPath) throws RuntimeException {
    doWhenRunning {
      String artifact = Paths.get(artifactsPath).fileName
      if (!isDeployed(artifact)) {
        def proc = """${cli} --connect --command="deploy ${artifactsPath}" """.execute()
        proc.waitFor()
        if (proc.exitValue() != 0 || !isDeployed(artifact)) {
          throw new RuntimeException(proc.in.text)
        }
      } else {
        logger.info "${artifact} is already deployed in JBoss"
      }
    }
  }

  /**
   * Undeploys the specified artifact from this JBoss server by using the Management API.
   * If the server isn't running, then it is started before.
   * @param artifactName
   * @throws RuntimeException if the deployment of the artifact failed.
   */
  void undeploy(String artifact) throws RuntimeException {
    doWhenRunning {
      if (isDeployed(artifact)) {
        def proc = """${cli} --connect --command="undeploy ${artifact}" """.execute()
        proc.waitFor()
        if (proc.exitValue() != 0 || isDeployed(artifact)) {
          throw new RuntimeException(proc.in.text)
        }
      } else {
        logger.info "${artifact} isn't deployed in JBoss"
      }
    }
  }

  /**
   * Is the specified artifact is deployed?
   * @param artifact the artifact to check its deployment status.
   * @return true if the artifact is deployed, false otherwise.
   */
  boolean isDeployed(String artifact) {
    return doWhenRunning {
      StringBuilder output = new StringBuilder()
      def proc = """${
        cli
      } --connect --command=:read-children-names(child-type=deployment)""".execute()
      proc.waitForProcessOutput(output, output)
      return output.contains('"outcome" => "success"') && output.contains(artifact)
    }
  }

  /**
   * Prints out information about the deployment status of artifacts in this JBoss server. It an
   * artifact doesn't appear in the output, then it isn't deployed.
   * If the server isn't running, then it is started before.
   */
  void printDeployedArtifacts() {
    doWhenRunning {
      def proc = """${cli} --connect --command=deployment-info""".execute()
      proc.waitFor()
    }
  }

  /**
   * Configures JBoss by running the JBoss CLI statements in the specified commands file.
   * If the server isn't running, then it is started before.
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
   * @throws Exception if an error occurs while processing the specified commands file.
   */
  void processCommandFile(File commandsFile) throws Exception {
    doWhenRunning {
      try {
        logger.info "${commandsFile.name} processing..."

        if (shouldBeProcessed(commandsFile)) {
          logger.info "${commandsFile.name} processing: [DONE]"
        } else {
          def proc = """${cli} --connect --file=${commandsFile.path}""".execute()
          proc.waitFor()
          assertCommandSucceeds(proc)
          if (status() == 'starting') {
            logger.info "${commandsFile.name} processing: JBoss/Wildfly reloading..."
            // case of a reload, wait for the instance is running
            while (!isRunning()) {
              sleep(1000)
            }
            logger.info "${commandsFile.name} processing: JBoss/Wildfly reloaded"
          }
          logger.info "${commandsFile.name} processing: [OK]"
        }
      } catch (InvalidObjectException e) {
        logger.info "${commandsFile.name} processing: [WARNING]"
        logger.warn "Invalid resource. ${e.message}"
      } catch (AssertionError | Exception e) {
        logger.info "${commandsFile.name} processing: [FAILURE]"
        throw e
      }
    }
  }

  private boolean shouldBeProcessed(File commandsFile) {
    boolean processed = false
    List<String> lines = commandsFile.readLines()
    for (int i = 0; i < lines.size() && !processed; i++) {
      // the commands file should be processed if and only if some given properties are set
      Matcher matcher = lines.get(i) =~ /\s*#isDefined:\s+(.+)/
      boolean matched = false
      matcher.each { token ->
        matched = true
        processed = true
        token[1].split('( )+').each { property ->
          if (!SilverpeasSetupService.currentSettings[property]) {
            processed = false
          }
        }
      }
      if (matched && !processed) {
        break
      }

      // the commands file should be processed if and only if the resource referred by the file
      // isn't already defined in JBoss/Wildfly
      matcher = lines.get(i)  =~ /\s*#check:\s+(.+)/
      matcher.each { token ->
        StringBuilder output = new StringBuilder()
        def proc = """${cli} --connect --command="${token[1]}:read-resource" """.execute()
        proc.waitForProcessOutput(output, output)
        if (proc.exitValue() == 0 && output.contains('"outcome" => "success"')) {
          processed = true
        }
      }
    }
    return processed
  }

}