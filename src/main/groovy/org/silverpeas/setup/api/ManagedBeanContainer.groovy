package org.silverpeas.setup.api

/**
 * A provider of beans that were constructed and initialized at plugin application to a Grandle
 * project.
 * @author mmoquillon
 */
class ManagedBeanContainer {

  private static Map<String, Object> beans = [:]

  private ManagedBeanContainer() {

  }

  static Registry registry() {
    return new Registry()
  }

  static def get(String name, Class type) {
    def bean = beans.get(name)
    if (!bean) {
      throw new RuntimeException("The bean ${name} isn't found!")
    }
    return type.cast(bean)
  }

  static def get(Class type) {
    get(type.simpleName, type)
  }

  static class Registry {

    Registry register(def bean, String name) {
      beans.putIfAbsent(name, bean)
      return this
    }

    Registry register(Object bean) {
      register(bean, bean.class.simpleName)
    }
  }
}
