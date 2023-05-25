/*
  Copyright (C) 2000 - 2022 Silverpeas

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  As a special exception to the terms and conditions of version 3.0 of
  the GPL, you may redistribute this Program in connection with Free/Libre
  Open Source Software ("FLOSS") applications as described in Silverpeas's
  FLOSS exception.  You should have received a copy of the text describing
  the FLOSS exception, and it is also available here:
  "https://www.silverpeas.org/docs/core/legal/floss_exception.html"

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.silverpeas.setup.api

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.jcr.Jcr
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore
import org.apache.jackrabbit.oak.plugins.document.mongo.MongoDocumentNodeStoreBuilder
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders
import org.apache.jackrabbit.oak.segment.file.FileStore
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.gradle.internal.impldep.com.google.common.util.concurrent.MoreExecutors

import javax.jcr.Repository
import java.nio.file.Path

/**
 * A factory to create instances of JCR Repository from configuration properties.
 * @author mmoquillon
 */
@Singleton(lazy = true)
class JcrRepositoryFactory {

  /**
   * Creates an instance mapping to the JCR repository used by Silverpeas.
   * @return a JCR repository instance.
   */
  @SuppressWarnings('GrMethodMayBeStatic')
  DisposableRepository createRepository(Map settings) {
    try {
      Properties properties = new Properties()
      properties.load(new FileInputStream("${settings.JCR_HOME}/silverpeas-oak.properties"))
      DisposableRepository repository
      switch (properties['storage']) {
        case 'segment':
          repository = getSegmentNodeStoreRepository("${settings.JCR_HOME}", properties)
          break
        case 'document':
          repository = getDocumentNodeStoreRepository(properties)
          break
        default:
          throw new RuntimeException("Storage ${properties['storage']} no supported!")
      }
      return repository
    } catch (Exception ex) {
      throw new RuntimeException(ex.getMessage(), ex)
    }
  }

  private static DisposableRepository getSegmentNodeStoreRepository(String jcrHomePath, Properties properties) {
    String repo =
        properties['segment.repository'] ? properties['segment.repository'] : 'segmentstore'
    Path repoPath = Path.of(repo)
    Path segmentStorePath =
        repoPath.isAbsolute() ? repoPath : Path.of(jcrHomePath).resolve(repoPath)

    FileStore fs = FileStoreBuilder.fileStoreBuilder(segmentStorePath.toFile()).build()
    NodeStore ns = SegmentNodeStoreBuilders.builder(fs).build()
    return new DisposableRepository(repository: new Jcr(new Oak(ns)).createRepository(), fs: fs)
  }

  private static DisposableRepository getDocumentNodeStoreRepository(Properties properties) {
    String uri =
        properties['document.uri'] ? properties['document.uri'] : 'mongodb://localhost:27017'
    String db = properties['document.db'] ? properties['document.db'] : 'oak'
    int blobCacheSize =
        properties['document.blobCacheSize'] ? properties['document.blobCacheSize'] as int : 16

    DocumentNodeStore ns = MongoDocumentNodeStoreBuilder.newMongoDocumentNodeStoreBuilder()
        .setExecutor(MoreExecutors.newDirectExecutorService())
        .setMongoDB(uri, db, blobCacheSize)
        .build()
    return new DisposableRepository(repository: new Jcr(new Oak(ns)).createRepository(), dns: ns)
  }

  static class DisposableRepository implements Repository {
    @Delegate
    Repository repository
    private FileStore fs
    private DocumentNodeStore dns

    /**
     * Disposes this repository. It frees all the allocated resources to access the JCR. It is
     * required to invoke this method once all the works with the JCR is done.
     */
    void dispose() {
      if (fs) {
        fs.close()
      } else if (dns) {
        dns.dispose()
      }
    }
  }
}
