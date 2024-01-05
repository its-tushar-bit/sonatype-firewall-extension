/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseException;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DatabaseUtilTest
{
  @Test
  public void testGetDatabaseEngine_H2() {
    assertThat(DatabaseUtil.getDatabaseEngine("h2")).isEqualTo(H2DatabaseEngine.INSTANCE);
  }

  @Test
  public void testGetDatabaseEngine_PostgreSQL() {
    assertThat(DatabaseUtil.getDatabaseEngine("PostgreSQL")).isEqualTo(PostgresDatabaseEngine.INSTANCE);
  }

  @Test
  public void testGetDatabaseEngine_ByConfig_H2() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    assertThat(DatabaseUtil.getDatabaseEngine(databaseConfig)).isEqualTo(H2DatabaseEngine.INSTANCE);
  }

  @Test
  public void testGetDatabaseEngine_ByConfig_PostgreSQL() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.postgresql.Driver");
    assertThat(DatabaseUtil.getDatabaseEngine(databaseConfig)).isEqualTo(PostgresDatabaseEngine.INSTANCE);
  }

  @Test
  public void testGetDatabaseEngine_ByConfig_Unknown() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("not a real driver");
    assertThatExceptionOfType(DatabaseException.class).isThrownBy(() -> DatabaseUtil.getDatabaseEngine(databaseConfig));
  }

  @Test
  public void testIsDatabaseEmbedded_H2() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    assertThat(DatabaseUtil.isDatabaseEmbedded(databaseConfig)).isTrue();
  }

  @Test
  public void testIsDatabaseEmbedded_Postgres() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.postgresql.Driver");
    assertThat(DatabaseUtil.isDatabaseEmbedded(databaseConfig)).isFalse();
  }
}
