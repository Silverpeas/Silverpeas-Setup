/*
  Copyright (C) 2000 - 2024 Silverpeas

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  As a special exception to the terms and conditions of version 3.0 of
  the GPL, you may redistribute this Program in connection with Free/Libre
  Open Source Software ("FLOSS") applications as described in Silverpeas's
  FLOSS exception.  You should have received a copy of the text describing
  the FLOSS exception, and it is also available here:
  "https://www.silverpeas.org/legal/floss_exception.html"

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.silverpeas.setup.api

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.SystemUtils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeoutException
import java.util.regex.Matcher

/**
 * It wraps an existing installation of a JBoss application server. It provides functions to
 * interact with the JBoss AS (either Wildfly or JBoss EAP) in order to start/stop it or to
 * configure it. The configuration is done through command files with JBoss CLI statements.
 * </p>
 * The features supported by this class are compatible to any Wildfly or JBoss EAP built upon
 * Wildfly version superior than 9.0
 * @author mmoquillon
 */
class JBossServer {

  static long DEFAULT_TIMEOUT = 300000l

  private String cli

  private String starter

  final String jbossHome

  private boolean serverManagementAllIP

  private File redirection = null

  private long timeout = DEFAULT_TIMEOUT

  private FileLogger logger = FileLogger.getLogger(getClass().getSimpleName(), System.out)

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

  private static void assertCommandSucceeds(command) throws AssertionError, InvalidObjectException {
    String result = command.in.text
    if (command.exitValue() != 0 || result.contains('"outcome" => "failed"')) {
      boolean rollBacked = result.contains('"rolled-back" => true')
      String msg = "Execution Output: \n${result}"
      if (!rollBacked) {
        throw new InvalidObjectException(msg)
      }
      throw new AssertionError(msg)
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
    while(!running(status)) {
      if (reloadRequired(status)) {
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
  void doWhenRunning(Closure action) {
    String status = status()
    if (stopped(status)) {
      logger.info 'JBoss not started, so start it'
      start()
    } else if (starting(status)) {
      logger.info 'JBoss is starting, so wait for it is running'
      waitUntilRunning()
    } else if (reloadRequired(status)) {
      logger.info 'JBoss asks for reload, so reloads it'
      def proc = """${cli} --connect --command=:reload""".execute()
      proc.waitFor()
      waitUntilRunning()
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
    if (timeout > 0) {
      this.timeout = timeout
    }
    return this
  }

  /**
   * By default, the management console is only available on the machine of the running wildfly
   * at IP 127.0.0.1 and port 9990.
   * This method allows the management console to be accessible from any server IP. This is useful
   * with a client server which does not provide WEB browser (which is generally the case).
   * By default, false (that means default wildfly behavior).
   * This parameter only affects start and debug methods.
   * @param isAllIp true to make the management accessible from any client.
   * @return itself.
   */
  JBossServer setServerManagementAllIP(boolean isAllIp) {
    this.serverManagementAllIP = isAllIp
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
    int managementPort = 9990
    boolean adminOnly = (params != null && params.adminOnly ? params.adminOnly:false)
    if (reloadRequired()) {
      reload()
      stop()
    }
    if (!isStartingOrRunning()) {
      ProcessBuilder process
      if (adminOnly) {
        process = new ProcessBuilder(starter, '-c', 'standalone-full.xml', '--admin-only')
      } else if (serverManagementAllIP) {
        process = new ProcessBuilder(starter, '-c', 'standalone-full.xml', '-bmanagement', '0.0.0.0', '-b', '0.0.0.0')
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
      if (serverManagementAllIP) {
        println ''
        println "Silverpeas management console is accessible at server IP on port ${managementPort}"
      }
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
    if (reloadRequired()) {
      reload()
      stop()
    }
    if (!isStartingOrRunning()) {
      int managementPort = 9990
      String p = (port <= 1000 ? '5005':String.valueOf(port))
      ProcessBuilder process
      if (serverManagementAllIP) {
        process =
            new ProcessBuilder(starter, '-c', 'standalone-full.xml', '-bmanagement', '0.0.0.0', '-b', '0.0.0.0', '--debug', p)
                .directory(new File(jbossHome))
                .redirectErrorStream(true)
      } else {
        process =
            new ProcessBuilder(starter, '-c', 'standalone-full.xml', '-b', '0.0.0.0', '--debug', p)
                .directory(new File(jbossHome))
                .redirectErrorStream(true)
      }
      if (redirection != null) {
        process.redirectOutput(redirection)
      } else {
        process.inheritIO()
        System.println(process.redirectOutput())
      }
      process.start()
      waitUntilRunning()
      println ''
      if (serverManagementAllIP) {
        println "Silverpeas management console is accessible at server IP on port ${managementPort}"
      }
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

  private boolean running(String status) {
    return status == 'running'
  }

  private boolean starting(String status) {
    return status == 'starting'
  }

  private boolean stopped(String status) {
    return status == 'stopped'
  }

  private boolean reloadRequired(String status) {
    return status == 'reload-required'
  }

  /**
   * Is a JBoss/Wildfly instance running?
   * @return true if an instance of JBoss/Wildfly is running, false otherwise.
   */
  boolean isRunning() {
    return running(status())
  }

  /**
   * Is a JBoss/Wildfly instance being starting?
   * @return true if an instance of JBoss/Wildfly is being starting, false otherwise.
   */
  boolean isStarting() {
    return starting(status())
  }

  /**
   * Is a JBoss/Wildfly instance running or being starting?
   * @return true if an instance of JBoss/Wildfly is either running or being starting,
   * false otherwise.
   */
  boolean isStartingOrRunning() {
    String status = status()
    return starting(status) || running(status)
  }

  /**
   * Is a JBoss/Wildfly instance stopped?
   * @return true if an instance of JBoss/Wildfly is stopped, false otherwise.
   */
  boolean isStopped() {
    return stopped(status())
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
   * Adds the artifact located at the specified path into the deployment repository of this JBoss
   * server and deploys it by using the Management API. The unique name with which the artifact
   * will be identified is the name of the artifact itself. The name of the runtime context under
   * which the artifact could be accessed once deployed is the name of the artifact itself.
   * If it is already registered, nothing is done.
   * Once added, the artifact can be deployed and undeployed again on demand.
   * A JBoss/Wildfly instance should be running.
   * @param artifactPath the path of the artifact to add.
   * @throws RuntimeException if the adding of the artifact failed.
   */
  void add(String artifactPath) throws RuntimeException {
    String artifactName = Paths.get(artifactPath).fileName
    add(artifactPath, artifactName)
  }

  /**
   * Adds the artifact located at the specified path into the deployment repository of this JBoss
   * server under the specified runtime context by using the Management API. The unique name with
   * which the artifact will be identified is the name of the artifact itself.
   * If it is already  registered, nothing is done.
   * Once added, the artifact can be undeployed and deployed again on demand.
   * A JBoss/Wildfly instance should be running.
   * @param artifactPath the path of the artifact to add.
   * @param context the runtime context under which the artifact could be accessed once deployed.
   * @throws RuntimeException if the adding of the artifact failed.
   */
  void add(String artifactPath, String context) throws RuntimeException {
    String artifactName = FilenameUtils.getName(artifactPath)
    add(artifactPath, artifactName, context)
  }

  /**
   * Adds the artifact located at the specified path into the deployment repository of this JBoss
   * server under the specified runtime context and with the specified unique name by using the
   * Management API. If it
   * is already  registered, nothing is done.
   * Once added, the artifact can be undeployed and deployed again on demand.
   * A JBoss/Wildfly instance should be running.
   * @param artifactPath the path of the artifact to add.
   * @param context the runtime context under which the artifact will be deployed into this server.
   * @throws RuntimeException if the adding of the artifact failed.
   */
  void add(String artifactPath, String name, String context) throws RuntimeException {
    String normalizedArtifactPath = FilenameUtils.separatorsToUnix(artifactPath)
    Path artifactTruePath = Paths.get(normalizedArtifactPath)
    boolean archive = Files.isRegularFile(artifactTruePath)
    if (!isInDeployments(name)) {
      Process proc = executeCliCommand(
          "/deployment=${name}:add(runtime-name=${context},content=[{path=>${normalizedArtifactPath},archive=${archive}}])",
          SystemUtils.IS_OS_WINDOWS)
      proc.waitFor()
      if (proc.exitValue() != 0 || !isInDeployments(name)) {
        throw new RuntimeException("Adding of ${name} in JBoss failed with exit code " +
            "${proc.exitValue()} and message ${proc.in.text}")
      }
    } else {
      logger.info "${name} is already added in JBoss"
    }
  }

  /**
   * Removes the specified artifact from the deployment repository of this JBoss server by using
   * the Management API. If it isn't in this JBoss server, then nothing is done. If the artifact
   * is deployed, then it is undeployed before being removed.
   * A JBoss/Wildfly instance should be running.
   * @param artifact the name of the artifact to remove.
   * @throws RuntimeException if the removing of the artifact failed.
   */
  void remove(String artifact) throws RuntimeException {
    if (isInDeployments(artifact)) {
      Process proc = executeCliCommand("undeploy ${artifact}")
      proc.waitFor()
      if (proc.exitValue() != 0 || isInDeployments(artifact)) {
        throw new RuntimeException("Remove of ${artifact} from JBoss failed with exit code " +
            "${proc.exitValue()} and message ${proc.in.text}")
      }
    } else {
      logger.info "${artifact} isn't deployed in JBoss"
    }
  }

  /**
   * Deploys the specified artifact by using the Management API. It should be first added into
   * this JBoss server. If the artifact is already deployed, then nothing is done.
   * A JBoss/Wildfly instance should be running.
   * @param artifact the name of the artifact to deploy.
   * @throws RuntimeException if the deployment of the artifact failed.
   */
  void deploy(String artifact) throws RuntimeException {
    Process proc = executeCliCommand("/deployment=${artifact}:deploy()")
    proc.waitFor()
    if (proc.exitValue() != 0 || !isDeployed(artifact)) {
      throw new RuntimeException("Deployment of ${artifact} failed with exit code " +
          "${proc.exitValue()} and message ${proc.in.text}")
    }
  }

  /**
   * Undeploys the specified artifact from this JBoss server by using the Management API. It should
   * be added into this JBoss server. If the artifact is already undeployed, then nothing is done.
   * A JBoss/Wildfly instance should be running.
   * @param artifact the name of the artifact to undeploy.
   * @throws RuntimeException if the deployment of the artifact failed.
   */
  void undeploy(String artifact) throws RuntimeException {
    Process proc = executeCliCommand("/deployment=${artifact}:undeploy()")
    proc.waitFor()
    if (proc.exitValue() != 0 || isDeployed(artifact)) {
      throw new RuntimeException("Deployment of ${artifact} failed with exit code " +
          "${proc.exitValue()} and message ${proc.in.text}")
    }
  }

  /**
   * Is the specified artifact deployed?
   * A JBoss/Wildfly instance should be running.
   * @param artifact the name of the artifact to check its deployment status.
   * @return true if the artifact is deployed, false otherwise.
   */
  boolean isDeployed(String artifact) {
    Process proc = executeCliCommand("/deployment=${artifact}:read-attribute(name=enabled)")
    proc.waitFor()
    return proc.in.text.contains('"result" => true')
  }

 /**
  * Is the specified artifact added as a deployment in this JBoss/Wildfly?
  * A JBoss/Wildfly instance should be running.
  * @param artifact the name of the artifact to check it is added into this JBoss/Wildfly.
  * @return true if the artifact is among the deployments, false otherwise.
  */
  boolean isInDeployments(String artifact) {
    Process proc = executeCliCommand("ls deployment")
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
      logger.error e.message
      throw e
    }
  }

  /**
   * Centralizes the process creation corresponding to the execution of a CLI command.
   * @param commands the CLI commands.
   * @param wrapIntoDoubleQuotes if true, the commands are wrapped into double quotes.
   * @return the instance of the process which handles the CLI command execution.
   */
  Process executeCliCommand(String commands, boolean wrapIntoDoubleQuotes = false) {
    String finalCommands = commands
    if (wrapIntoDoubleQuotes) {
      finalCommands = "\"" + finalCommands + "\""
    }
    return new ProcessBuilder(cli, '--connect', finalCommands)
        .redirectErrorStream(true)
        .start()
  }
}