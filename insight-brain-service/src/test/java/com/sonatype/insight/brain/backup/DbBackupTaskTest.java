/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.backup;

import java.io.File;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.H2DatabaseBackup;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

public class DbBackupTaskTest
    extends AbstractResourceTest
{
  @Test
  @H2DiskTest
  @Category(SlowTest.class)
  public void testBackup_H2Database() throws Exception {
    HttpResponse response = adminRequest().path("tasks", DbBackupTask.PATH).post();
    assertResponseStatus(200, response);
    String message = response.getBodyText();
    assertThat(message).startsWith(DbBackupTask.RESPONSE_MESSAGE_PREFIX);
    File dbBackupDir = new File(message.substring(DbBackupTask.RESPONSE_MESSAGE_PREFIX.length()));
    assertThat(dbBackupDir).isDirectory();
    assertThat(dbBackupDir.getParentFile().getAbsolutePath()).isEqualTo(
        getCLMServer().getInstance(Configuration.class).getDbBackupDir());
    assertThat(new File(dbBackupDir, DatabaseName.ods + H2DatabaseBackup.BACKUP_FILENAME_SUFFIX)).isFile();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testBackup_NotH2Database() throws Exception {
    InsightConfig insightConfig = getCLMServer().getConfiguration();
    com.sonatype.insight.brain.service.DatabaseConfig savedDatabaseConfig = insightConfig.getDatabase();

    try {
      insightConfig.setDatabase(new com.sonatype.insight.brain.service.DatabaseConfig());

      HttpResponse response = adminRequest().path("tasks", DbBackupTask.PATH).post();
      assertResponseStatus(500, response);
      String message = response.getBodyText();
      assertThat(message).contains("The DB backup task is supported only for h2 databases.");
    }
    finally {
      insightConfig.setDatabase(savedDatabaseConfig);
    }
  }
}
