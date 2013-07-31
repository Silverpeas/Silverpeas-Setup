/*
 * Copyright (C) 2000-2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Writer Free/Libre
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

import org.silverpeas.migration.jcr.service.model.SimpleDocument;

/**
 * It represents the process of migrating a given document into the JCR.
 *
 * @author mmoquillon
 */
public interface DocumentMigration {

  /**
   * Migrates the specified document and returns the actual number of document that has been
   * migrated.
   *
   * @param document the metadata about the document to migrate into the JCR.
   * @return the actual number of documents that has been migrated. If the document is a versioned
   * one with more than one version, the returned number will indicate the number of versions that
   * has been migrated for the specified document.
   */
  public long migrate(final SimpleDocument document) throws Exception;
}
