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
package org.silverpeas.migration.jcr.service;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 *
 * @author ehugonnet
 */
public class ConverterUtilTest {

  /**
   * Test of extractLanguage method, of class ConverterUtil.
   */
  @Test
  public void testExtractLanguage() {
    String filename = "6961wysiwyg_de.txt";
    String result = ConverterUtil.extractLanguage(filename);
    assertThat(result, is("de"));
    filename = "6961wysiwyg_fr.txt";
    result = ConverterUtil.extractLanguage(filename);
    assertThat(result, is("fr"));
    filename = "6961wysiwyg.txt";
    result = ConverterUtil.extractLanguage(filename);
    assertThat(result, is(nullValue()));
    filename = "Node_1939wysiwyg_zk.txt";
    result = ConverterUtil.extractLanguage(filename);
    assertThat(result, is("zk"));
    filename = "Node_1939wysiwyg.txt";
    result = ConverterUtil.extractLanguage(filename);
    assertThat(result, is(nullValue()));
  }

  /**
   * Test of replaceLanguage method, of class ConverterUtil.
   */
  @Test
  public void testReplaceLanguage() {
    String filename = "6961wysiwyg_de.txt";
    String result = ConverterUtil.replaceLanguage(filename, "fr");
    assertThat(result, is("6961wysiwyg_fr.txt"));
    filename = "6961wysiwyg.txt";
    result = ConverterUtil.replaceLanguage(filename, "en");
    assertThat(result, is("6961wysiwyg_en.txt"));
    filename = "Node_1939wysiwyg_zk.txt";
    result = ConverterUtil.replaceLanguage(filename, "de");
    assertThat(result, is("Node_1939wysiwyg_de.txt"));
    filename = "Node_1939wysiwyg.txt";
    result = ConverterUtil.replaceLanguage(filename, "de");
    assertThat(result, is("Node_1939wysiwyg_de.txt"));
    filename = "Node_1939wysiwyg_zkx.txt";
    result = ConverterUtil.replaceLanguage(filename, "de");
    assertThat(result, is(nullValue()));
  }

  /**
   * Test of extractLanguage method, of class ConverterUtil.
   */
  @Test
  public void testExtractBaseName() {
    String filename = "6961wysiwyg_de.txt";
    String result = ConverterUtil.extractBaseName(filename);
    assertThat(result, is("6961wysiwyg"));
    filename = "6961wysiwyg_fr.txt";
    result = ConverterUtil.extractBaseName(filename);
    assertThat(result, is("6961wysiwyg"));
    filename = "6961wysiwyg.txt";
    result = ConverterUtil.extractBaseName(filename);
    assertThat(result, is("6961wysiwyg"));
    filename = "Node_1939wysiwyg_zk.txt";
    result = ConverterUtil.extractBaseName(filename);
    assertThat(result, is("Node_1939wysiwyg"));
    filename = "Node_1939wysiwyg_zkw.txt";
    result = ConverterUtil.extractBaseName(filename);
    assertThat(result, is(nullValue()));
    filename = "Node_1939wysiwygi_zkw.txt";
    result = ConverterUtil.extractBaseName(filename);
    assertThat(result, is(nullValue()));
  }

}
