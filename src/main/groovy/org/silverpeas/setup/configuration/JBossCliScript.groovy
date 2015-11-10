package org.silverpeas.setup.configuration

import org.apache.commons.io.FilenameUtils
import org.silverpeas.setup.api.AbstractScript
import org.silverpeas.setup.api.Logger

import java.nio.file.Files
import java.nio.file.Path

/**
 * A script with JBoss CLI statements. It is used to configure JBoss/Wildfly for Silverpeas.
 * @author mmoquillon
 */
class JBossCliScript extends AbstractScript {

  /**
   * Constructs a new JBoss CLI script for a CLI file located at the specified path.
   * @param path the absolute path of a JBoss CLI script file.
   */
  JBossCliScript(String path) {
    super(path)
  }

  /**
   * Uses the specified logger to trace the execution of this script.
   * @param logger a logger.
   * @return itself.
   */
  @Override
  JBossCliScript useLogger(final Logger logger) {
    this.log = logger
    return this
  }

  /**
   * Uses the specified settings to parameterize the execution of this script.
   * @param settings a collection of key-value pairs defining all the settings.
   * @return itself.
   */
  @Override
  JBossCliScript useSettings(final Map settings) {
    this.settings = settings
    return this
  }

  /**
   * Runs this script with the specified arguments.
   * @param args a Map of variables to pass to the scripts. The keys in the Map are the names of the
   * variables. Expected the following:
   * <ul>
   *  <li><em>iboss</em>: the running JBoss/Wildfly instance against which the script will be
   *  performed.</li>
   * </ul>
   * @throws RuntimeException if an error occurs during the execution of the script.
   */
  @Override
  void run(Map args) throws RuntimeException {
    try {
      JBossServer jboss = args.jboss
      String fileBaseName = FilenameUtils.getBaseName(script.name)
      Path cli = Files.createTempFile("${fileBaseName}-", '.cli')
      log.info "Prepare ${script.name} into ${cli.fileName}"
      new FileReader(script).transformLine(new FileWriter(cli.toString())) { line ->
        VariableReplacement.parseExpression(line, settings)
      }
      jboss.processCommandFile(cli.toFile())
    } catch (Exception ex) {
      throw new RuntimeException(ex)
    }
  }
}
