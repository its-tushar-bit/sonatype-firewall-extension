/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.backup.DbBackupTask;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.jupiter.api.Test;

/**
 * Postgres port of {@code DbBackupTaskTest#testBackup_NotH2Database}: the DB backup admin task
 * rejects non-embedded (i.e. Postgres) databases with a 400. The sibling H2-only method
 * ({@code testBackup_H2Database}) exercises a real embedded-H2 disk backup, which cannot run against
 * this Postgres-by-construction server, so it lives in {@code insight-brain-variant-test-h2}
 * ({@code IqH2DbBackupTaskTest}).
 */
@IqPostgresTest
class IqPostgresDbBackupTaskTest
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  @Test
  void testBackup_NotH2Database() throws Exception {
    InsightConfig insightConfig = ctx.lookup(InsightConfig.class);
    DatabaseConfig savedDatabaseConfig = insightConfig.getDatabase();

    try {
      insightConfig.setDatabase(new DatabaseConfig());

      HttpResponse response = ctx.adminRequest().path("tasks", DbBackupTask.PATH).post();
      ctx.assertResponseStatus(400, response);
      String message = response.getBodyText();
      assertThat(message).contains("The DB backup task is supported only for h2 databases.");
    }
    finally {
      insightConfig.setDatabase(savedDatabaseConfig);
    }
  }
}
