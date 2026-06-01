/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.db.H2DatabaseBackup;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import java.io.File;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

public class DbBackupTaskExecutionTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final InsightConfig config = new InsightConfig();

  private final Configuration configuration = mock(Configuration.class);

  private final OperationalDataStore operationalDataStore = mock(OperationalDataStore.class);

  private final H2DatabaseBackup h2DatabaseBackup = mock(H2DatabaseBackup.class);

  private final DataSource dataSource = mock(DataSource.class);

  private DbBackupTask dbBackupTask;

  @Before
  public void setUp() throws Exception {
    File workDir = temporaryFolder.newFolder("sonatype-work");
    File backupRoot = temporaryFolder.newFolder("db-backups");

    config.setSonatypeWork(workDir.getAbsolutePath());
    when(configuration.getDbBackupDir()).thenReturn(backupRoot.getAbsolutePath());
    when(operationalDataStore.getDataSource()).thenReturn(dataSource);

    dbBackupTask = new DbBackupTask(config, configuration, operationalDataStore, h2DatabaseBackup);
  }

  @Test
  public void shouldCreateBackupDirectoryAndReturnCreatedMessage() {
    String response = dbBackupTask.doBackup();

    assertThat(response).startsWith(DbBackupTask.RESPONSE_MESSAGE_PREFIX);
    File backupDir = new File(response.substring(DbBackupTask.RESPONSE_MESSAGE_PREFIX.length()));
    assertThat(backupDir).isDirectory();

    ArgumentCaptor<com.sonatype.insight.db.DatabaseConfig> databaseConfigCaptor =
        ArgumentCaptor.forClass(com.sonatype.insight.db.DatabaseConfig.class);
    verify(h2DatabaseBackup).backup(databaseConfigCaptor.capture(), same(dataSource), eq(backupDir));
    assertThat(databaseConfigCaptor.getValue().getUrl()).contains("/data/ods");
  }

  @Test
  public void shouldRejectNonH2Databases() {
    config.setDatabase(new DatabaseConfig());

    assertThatThrownBy(dbBackupTask::execute)
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The DB backup task is supported only for h2 databases.");

    verifyNoInteractions(h2DatabaseBackup);
  }
}
