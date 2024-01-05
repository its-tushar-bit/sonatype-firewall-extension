/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.db.DatabaseConfigProvider;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.H2DatabaseEngine;

public class H2InMemoryDatabaseConfigProvider
    implements DatabaseConfigProvider
{
  @Override
  public DatabaseConfig getDatabaseConfig(final DatabaseName databaseName) {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setUrl("jdbc:h2:mem:functionalTestInMemoryDatabase;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1");
    databaseConfig.setDriverClassName("org.h2.Driver");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    return databaseConfig;
  }

  @Override
  public DatabaseEngine getDatabaseEngine() {
    return H2DatabaseEngine.INSTANCE;
  }
}
