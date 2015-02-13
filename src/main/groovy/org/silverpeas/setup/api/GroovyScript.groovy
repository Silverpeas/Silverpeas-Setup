package org.silverpeas.setup.api

import java.nio.file.Path
import java.nio.file.Paths

/**
 * A Groovy script. It wraps the actual referred script file and it manages its execution.
 * @author mmoquillon
 */
class GroovyScript implements Script {

  private static GroovyScriptEngine engine = new GroovyScriptEngine('')

  protected Path script
  protected Logger log

  /**
   * Constructs a new GroovyScript instance that refers the script located at the specified path.
   * @param path the absolute path of a Groovy script.
   */
  GroovyScript(String path) {
    script = Paths.get(path)
  }

  @Override
  GroovyScript useLogger(Logger logger) {
    this.log = logger
    return this
  }

  /**
   * Runs this script with the specified arguments.
   * @param args a Map of variables to pass to the scripts. The keys in the Map are the names of the
   * variables.
   * @throws RuntimeException if an error occurs during the execution of the script.
  */
  @Override
  void run(def args) throws RuntimeException {
    log.info "${script.fileName} processing..."
    Binding parameters = new Binding()
    args.each { key, value ->
      parameters.setVariable(key, value)
    }
    String status = '[OK]'
    try {
      engine.run(script.toUri().toString(), parameters)
    } catch (Exception ex) {
      status = '[FAILURE]'
      throw ex
    } finally {
      log.info "${script.fileName} processing: ${status}"
    }
  }
}
