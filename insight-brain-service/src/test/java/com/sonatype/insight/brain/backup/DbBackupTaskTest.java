/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.backup;

import java.io.File;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.H2DatabaseBackup;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DbBackupTaskTest
    extends AbstractResourceTest
{
  @Before
  public void setup() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @After
  public void cleanup() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Test
  public void testBackup_H2Database() throws Exception {
    // H2 does not allow backups of in-memory databases, so we need an on-disk database for this test.
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    databaseConfig
        .setUrl("jdbc:h2:target/DbBackupTest/ods;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    OperationalDataStoreProvider.init(databaseConfig, false);

    HttpResponse response = adminRequest().path("tasks", DbBackupTask.PATH).post();
    assertResponseStatus(200, response);
    String message = response.getBodyText();
    assertThat(message).startsWith(DbBackupTask.RESPONSE_MESSAGE_PREFIX);
    File dbBackupDir = new File(message.substring(DbBackupTask.RESPONSE_MESSAGE_PREFIX.length()));
    assertThat(dbBackupDir).isDirectory();
    assertThat(dbBackupDir.getParentFile().getAbsolutePath())
        .isEqualTo(getCLMServer().getConfiguration().getDbBackupDir().getAbsolutePath());
    assertThat(new File(dbBackupDir, DatabaseName.ods + H2DatabaseBackup.BACKUP_FILENAME_SUFFIX)).isFile();
  }

  @Test
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
