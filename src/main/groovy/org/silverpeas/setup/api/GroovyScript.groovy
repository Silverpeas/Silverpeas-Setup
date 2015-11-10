package org.silverpeas.setup.api
/**
 * A Groovy script. It wraps the actual referred script file and it manages its execution.
 * @author mmoquillon
 */
class GroovyScript extends AbstractScript {

  private static GroovyScriptEngine engine = new GroovyScriptEngine('')

  /**
   * Constructs a new GroovyScript instance that refers the script located at the specified path.
   * @param path the absolute path of a Groovy script.
   */
  GroovyScript(String path) {
    super(path)
  }

  /**
   * Runs this script with the specified arguments.
   * @param args a Map of variables to pass to the scripts. The keys in the Map are the names of the
   * variables.
   * @throws RuntimeException if an error occurs during the execution of the script.
  */
  @Override
  void run(Map args) throws RuntimeException {
    log.info "${script.name} processing..."
    Binding parameters = new Binding()
    parameters.setVariable('settings', settings)
    parameters.setVariable('log', log)
    args.each { key, value ->
      parameters.setVariable(key, value)
    }
    String status = '[OK]'
    try {
      engine.run(script.toURI().toString(), parameters)
    } catch (Exception ex) {
      status = '[FAILURE]'
      throw ex
    } finally {
      log.info "${script.name} processing: ${status}"
    }
  }


}
