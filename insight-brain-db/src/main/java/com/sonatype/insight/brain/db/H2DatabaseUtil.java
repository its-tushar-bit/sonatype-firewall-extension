/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;

import com.sonatype.insight.db.DatabaseConfig;

/**
 * @since 1.15.0
 */
public class H2DatabaseUtil
{
  private static final String H2_URL_PREFIX = "jdbc:h2:";

  public static File getDatabasePath(DatabaseConfig databaseConfig) {
    String url = databaseConfig.getUrl();
    if (!url.startsWith(H2_URL_PREFIX)) {
      throw new IllegalArgumentException(
          "Cannot process non-H2 database: " + url + ". " +
              "Check database configuration and ensure the database is properly initialized.");
    }

    String databaseDir = url.substring(H2_URL_PREFIX.length());
    int at = databaseDir.indexOf(';');
    if (at > 0) {
      databaseDir = databaseDir.substring(0, at);
    }
    return new File(databaseDir);
  }

  public static File getDatabaseVersionFile(File databasePath) {
    return new File(databasePath.getAbsolutePath() + ".ver");
  }
}
