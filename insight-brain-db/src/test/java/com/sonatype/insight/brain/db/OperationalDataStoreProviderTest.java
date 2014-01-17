/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;

import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Test;

public class OperationalDataStoreProviderTest
    extends AbstractDatabaseProviderTest
{
  @Test
  public void verifyDatabaseCreation_InMemory() throws Exception {
    verifyDatabaseCreation(null /* databaseConfig */);
  }

  @Test
  public void verifyDatabaseCreation_OnDisk() throws Exception {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    databaseConfig
        .setUrl("jdbc:h2:target/OperationalDataStoreProviderTest/test;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    databaseConfig.setMaxConnections(50);
    File databaseDir = new File("target/OperationalDataStoreProviderTest");

    verifyDatabaseCreation_OnDisk(databaseConfig, databaseDir);
  }
}
