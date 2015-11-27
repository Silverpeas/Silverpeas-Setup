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
import java.util.concurrent.TimeoutException
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

  private long timeout = 60000

  private def logger = Logger.getLogger(getClass().getSimpleName(), System.out)

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
   * Waits for the JBoss/Wildfly is running. If the server requires to be reloaded, then a reload
   * is performed silently before checking it is running. Be caution, this method can never returns
   * in some peculiar state of the server.
   */
  private void waitUntilRunning() {
    logger.formatInfo(' %s', '.')
    String status = this.status()
    long start = System.currentTimeMillis();
    while(status != 'running') {
      if (status == 'reload-required') {
        this.reload()
      }
      Thread.sleep(1000)
      if (System.currentTimeMillis() - start > this.timeout) {
        logger.error "JBoss doesn't respond. Stop all"
        throw new TimeoutException("JBoss doesn't respond. Stop all")
      }
      logger.formatInfo('%s', '.')
      status = this.status()
    }
  }

  /**
   * Ensures the JBoss/Wildfly instance is running before performing the specified action.
   * @param action the action to run against a running JBoss/Wildfly instance.
   * @return the result value of the closure if any.
   */
  def doWhenRunning(Closure action) {
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
   * The logger to use with this JBoss/Wildfly instance to output any traces about its actions.
   * @param logger the logger to use.
   * @return itself.
   */
  JBossServer useLogger(logger) {
    this.logger = logger
    return this
  }

  /**
   * The JBoss/Wildfly starting timeout in milliseconds. Whether JBoss/Wildfly is not running before
   * the expected time, then an exception is thrown. By default, the timeout is set to 1 minute.
   * @param timeout the JBoss/Wildfly starting timeout in milliseconds. If null or equals to 0,
   * nothing is set.
   * @return itself.
   */
  JBossServer withStartingTimeout(long timeout) {
    if (this.timeout!= null && this.timeout > 0) {
      this.timeout = timeout
    }
    return this
  }

  /**
   * Gets the name and the version of the JBoss/Wildfly application server referred by the
   * JBOSS_HOME environment variable.
   * A JBoss/Wildfly instance should be running.
   * @return the server name and version.
   */
  String about() {
    def proc = """${cli} --connect --commands=:read-attribute(name=product-name),:read-attribute(name=product-version)""".execute()
    proc.waitFor()
    String about = ''
    if (proc.exitValue() == 0) {
      String output = proc.in.text
      Matcher matcher = output =~ /"result" => "(.+)"/
        matcher.each { token ->
          about += token[1] + ' '
        }
    }
    return (about.empty ? 'Unsupported or unknown application server' : about.trim())
  }

  /**
   * Starts an instance of the JBoss/Wildfly server in a standalone mode (full JEE profile).
   * If an instance of JBoss/Wildfly is already running, then nothing is done.
   * @param params named parameters. Parameters supported are:
   * <ul>
   *   <li>adminOnly: starts JBoss/Wildfly to perform only administration tasks</li>
   * </ul>
   */
  void start(Map params = null) {
    boolean adminOnly = (params != null && params.adminOnly ? params.adminOnly:false)
    if (!isStartingOrRunning()) {
      ProcessBuilder process
      if (adminOnly) {
        process = new ProcessBuilder(starter, '-c', 'standalone-full.xml', '--admin-only')
      } else {
        process = new ProcessBuilder(starter, '-c', 'standalone-full.xml', '-b', '0.0.0.0')
      }
      process.directory(new File(jbossHome))
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
   * Reloads the configuration. Usually any changes in the system settings require a reload of
   * the server.
   * A JBoss/Wildfly instance should be running.
   */
  void reload() {
    def proc = """${cli} --connect --command=:reload""".execute()
    proc.waitFor()
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
      Matcher matcher = output.toString() =~ /"result" => "(.+)"/
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
   * A JBoss/Wildfly instance should be running.
   * @param artifactsPath
   * @throws RuntimeException if the deployment of the artifact failed.
   */
  void deploy(String artifactsPath) throws RuntimeException {
    String artifact = Paths.get(artifactsPath).fileName
    if (!isDeployed(artifact)) {
      Process proc =
          new ProcessBuilder(cli, '--connect', "deploy ${artifactsPath}")
              .redirectErrorStream(true)
              .start()
      proc.waitFor()
      if (proc.exitValue() != 0 || !isDeployed(artifact)) {
        throw new RuntimeException(proc.in.text)
      }
    } else {
      logger.info "${artifact} is already deployed in JBoss"
    }
  }

  /**
   * Undeploys the specified artifact from this JBoss server by using the Management API.
   * A JBoss/Wildfly instance should be running.
   * @param artifactName
   * @throws RuntimeException if the deployment of the artifact failed.
   */
  void undeploy(String artifact) throws RuntimeException {
    if (isDeployed(artifact)) {
      Process proc =
          new ProcessBuilder(cli, '--connect', "undeploy ${artifact}")
              .redirectErrorStream(true)
              .start()
      proc.waitFor()
      if (proc.exitValue() != 0 || isDeployed(artifact)) {
        throw new RuntimeException(proc.in.text)
      }
    } else {
      logger.info "${artifact} isn't deployed in JBoss"
    }
  }

  /**
   * Is the specified artifact is deployed?
   * A JBoss/Wildfly instance should be running.
   * @param artifact the artifact to check its deployment status.
   * @return true if the artifact is deployed, false otherwise.
   */
  boolean isDeployed(String artifact) {
    Process proc =
        new ProcessBuilder(cli, '--connect', "ls deployment")
            .start()
    proc.waitFor()
    return proc.in.text.contains(artifact)
  }

  /**
   * Configures JBoss by running the JBoss CLI statements in the specified commands file.
   * A JBoss/Wildfly instance should be running.
   * @param commandsFile the commands file to eat.
   * commands file.
   * @throws Exception if an error occurs while processing the specified commands file.
   */
  void processCommandFile(File commandsFile) throws Exception {
    try {
      logger.info "${commandsFile.name} processing..."
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
    } catch (InvalidObjectException e) {
      logger.info "${commandsFile.name} processing: [WARNING]"
      logger.warn "Invalid resource. ${e.message}"
    } catch (AssertionError | Exception e) {
      logger.info "${commandsFile.name} processing: [FAILURE]"
      throw e
    }
  }

}