/*
 * Copyright (C) 2000 - 2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection withWriter Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.util;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 *
 * @author ehugonnet
 */
public class ConfigurationHolderTest {

  public ConfigurationHolderTest() {
    
  }
  

  /**
   * Test of getHome method, of class ConfigurationHolder.
   */
  @Test
  public void testGetHome() throws Exception {   
    setEnvironmentVariable("SILVERPEAS_HOME", "/opt/silverpeas");
    ConfigurationHolder.reloadConfiguration();
    String expResult = "/opt/silverpeas";
    String result = ConfigurationHolder.getHome();
    assertThat(result, is(expResult));
  }

  /**
   * Test of getDataHome method, of class ConfigurationHolder.
   */
  @Test
  public void testGetDataHome() throws Exception {
    setEnvironmentVariable("SILVERPEAS_HOME", "/opt/silverpeas");
    ConfigurationHolder.reloadConfiguration();
    String expResult = "/opt/silverpeas/data";
    String result = ConfigurationHolder.getDataHome();
    assertThat(result, is(expResult));
  }

  /**
   * Test of getDataHome method, of class ConfigurationHolder.
   */
  @Test
  public void testGetDataHomeForEnv() throws Exception {
    setEnvironmentVariable("SILVERPEAS_HOME", "/opt/silverpeas");    
    setEnvironmentVariable("SILVERPEAS_DATA_HOME", "/var/data/silverpeas");
    ConfigurationHolder.reloadConfiguration();
    String expResult = "/var/data/silverpeas";
    String result = ConfigurationHolder.getDataHome();
    assertThat(result, is(expResult));
  }

  
  private static void setEnvironmentVariable(String name, String value) throws ClassNotFoundException,
      NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    System.setProperty("silverpeas.home", "");
    Class<?> cl = Class.forName("java.util.Collections$UnmodifiableMap");
    Field field = cl.getDeclaredField("m");
    field.setAccessible(true);
    Map<String, String> env = (Map<String, String>) field.get(System.getenv());
    env.put(name, value);
  }
}