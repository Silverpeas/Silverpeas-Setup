package org.silverpeas.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * A JUnit rule to bootstrap a Spring context for each test to run in a given test class.
 * @author mmoquillon
 */
public class SpringContext implements TestRule {

  private ConfigurableApplicationContext context;

  /**
   * Constructs a new Spring context that will be started before each running and that will be
   * stopped after each test execution.
   * @param test the test class instance that has to be took in charge by the Spring context.
   * @param contextConfig the classpath of the XML Spring context configuration file.
   */
  public SpringContext(Object test, String... contextConfig) {
    context = new ClassPathXmlApplicationContext(contextConfig);
    context.getAutowireCapableBeanFactory().autowireBean(test);
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        context.start();
        try {
          base.evaluate();
        } finally {
          context.close();
        }
      }
    };
  }

  public <T> T getBean(Class<T> beanClass) {
    return context.getBean(beanClass);
  }
}
