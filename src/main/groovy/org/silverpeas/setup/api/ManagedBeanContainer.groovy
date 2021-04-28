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

  static <T> T get(String name, Class<T> type) {
    def bean = beans.get(name)
    if (!bean) {
      throw new RuntimeException("The bean ${name} isn't found!")
    }
    return type.cast(bean)
  }

  static <T> T get(Class<T> type) {
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
