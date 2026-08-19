/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;

public interface DatabaseConfigProvider
{
  DatabaseConfig getDatabaseConfig(DatabaseName databaseName);

  DatabaseEngine getDatabaseEngine();
}
