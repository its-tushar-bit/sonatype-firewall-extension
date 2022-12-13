/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseUtilTest
{
  @Test
  public void testGetDatabaseEngine_H2() {
    assertThat(DatabaseUtil.getDatabaseEngineFromName("h2")).isEqualTo(H2DatabaseEngine.INSTANCE);
  }

  @Test
  public void testGetDatabaseEngine_PostgreSQL() {
    assertThat(DatabaseUtil.getDatabaseEngineFromName("PostgreSQL")).isEqualTo(PostgresDatabaseEngine.INSTANCE);
  }
}
