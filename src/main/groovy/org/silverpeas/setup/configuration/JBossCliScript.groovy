package org.silverpeas.setup.configuration

import org.silverpeas.setup.api.Logger
import org.silverpeas.setup.api.Script
import org.silverpeas.setup.api.SilverpeasSetupService

import java.nio.file.Files
import java.nio.file.Path

/**
 * A script with JBoss CLI statements. It is used to configure JBoss/Wildfly for Silverpeas.
 * @author mmoquillon
 */
class JBossCliScript implements Script {

  private File script
  private Logger log

  /**
   * Constructs a new JBoss CLI script for a CLI file located at the specified path.
   * @param path the absolute path of a JBoss CLI script file.
   */
  JBossCliScript(String path) {
    this.script = new File(path)
  }

  /**
   * Uses the specified logger to trace this script execution.
   * @param logger a logger.
   * @return itself.
   */
  @Override
  JBossCliScript useLogger(final Logger logger) {
    this.log = logger
    return this
  }

  /**
   * Runs this script with the specified arguments.
   * @param args a Map of variables to pass to the scripts. The keys in the Map are the names of the
   * variables. Expected the following:
   * <ul>
   *  <li><em>settings</em>: the settings of Silverpeas;</li>
   *  <li><em>jboss</em>: the JBossServer instance.</li>
   * </ul>
   * @throws RuntimeException if an error occurs during the execution of the script.
   */
  @Override
  void run(def args) throws RuntimeException {
    try {
      log.info "Prepare JBoss configuration from ${script.name}"
      JBossServer jboss = args.jboss
      def settings = args.settings
      Path cli = Files.createTempFile(script.name, '')
      new FileReader(script).transformLine(new FileWriter(cli.toString())) { line ->
        VariableReplacement.parseExpression(line, settings)
      }
      jboss.processCommandFile(cli.toFile())
    } catch (Exception ex) {
      throw new RuntimeException(ex)
    }
  }
}
