/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.27
 */
class DbDiagnostics
{
  private static final Logger log = LoggerFactory.getLogger(DbDiagnostics.class);

  static String getDBFileInfo() throws IOException {
    log.trace("getting db file info");
    final StringBuilder result = new StringBuilder("");

    final DatabaseConfig databaseConfig = OperationalDataStoreProvider.getDatabaseConfig();
    if (databaseConfig == null) {
      result.append("Null DatabaseConfig.");
      return result.toString();
    }

    final File ods = H2DatabaseUtil.getDatabasePath(databaseConfig);
    final File h2 = new File(ods.getPath() + ".h2.db");

    if (!h2.isFile()) {
      result.append("Found no database file at " + h2.getCanonicalPath() + "\n");
    }
    else {
      result.append("-- Database Diagnostics --\n");
      result.append("Database path: " + ods.getCanonicalPath() + "\n");
      result.append("Total database size: " + h2.length() + " bytes\n");

      final int version = DatabaseUtil
          .getDatabaseSchemaVersion(OperationalDataStoreProvider.getDataSource(), OperationalDataStoreProvider.ID);
      result.append("Schema version: " + version + "\n");
    }
    return result.toString();
  }
}
