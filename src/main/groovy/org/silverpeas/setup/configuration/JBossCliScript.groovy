package org.silverpeas.setup.configuration

import org.apache.commons.io.FilenameUtils
import org.silverpeas.setup.api.AbstractScript

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
