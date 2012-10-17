/*
 * Copyright (C) 2000 - 2012 Silverpeas
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
package org.silverpeas.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author ehugonnet
 */
public class ClasspathEntityResolver implements EntityResolver {

  private EntityResolver defaultResolver;

  public ClasspathEntityResolver(EntityResolver defaultResolver) {
    this.defaultResolver = defaultResolver;
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException,
      IOException {
    if (defaultResolver != null) {
      try {
        return defaultResolver.resolveEntity(publicId, systemId);
      } catch (IOException ioex) {
        return resolveInClasspath(publicId, systemId);
      }
    }
    return resolveInClasspath(publicId, systemId);
  }

  private InputSource resolveInClasspath(String publicId, String systemId) throws SAXException,
      IOException {
    InputSource result = resolveInClasspath(publicId);
    if (result == null) {
      result = resolveInClasspath(systemId);
    }
    if (result == null) {
      result = new InputSource(new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".
          getBytes("UTF-8")));
    }
    return result;
  }

  private InputSource resolveInClasspath(String id) throws SAXException, IOException {
    if (id != null && !id.isEmpty()) {
      String schemaName = id.substring(id.lastIndexOf('/'));
      InputStream resource = this.getClass().getClassLoader().getResourceAsStream(
          "/META-INF" + schemaName);
      if (resource != null) {
        return new InputSource(resource);
      }
    }
    return null;

  }
}
