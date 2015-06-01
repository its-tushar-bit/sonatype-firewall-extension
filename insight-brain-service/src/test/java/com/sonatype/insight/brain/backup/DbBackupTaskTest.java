/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.backup;

import java.io.File;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.H2DatabaseBackup;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.db.DatabaseConfig;

import com.ning.http.client.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

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
  public void testBackup() throws Exception {
    // H2 does not allow backups of in-memory databases, so we need an on-disk database for this test.
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    databaseConfig
        .setUrl("jdbc:h2:target/DbBackupTest/ods;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    OperationalDataStoreProvider.init(databaseConfig);

    Response response = AuthedRestAccess.post(getAdminBaseUrl() + "tasks/" + DbBackupTask.PATH, null /* body */);
    assertResponseStatus(200, response);
    String message = response.getResponseBody();
    assertThat(message, startsWith(DbBackupTask.RESPONSE_MESSAGE_PREFIX));
    File dbBackupDir = new File(message.substring(DbBackupTask.RESPONSE_MESSAGE_PREFIX.length()));
    assertThat(dbBackupDir.isDirectory(), is(true));
    assertThat(dbBackupDir.getParentFile().getAbsolutePath(), is(getCLMServer().getConfiguration().getDbBackupDir()
        .getAbsolutePath()));
    assertThat(new File(dbBackupDir, "ods" + H2DatabaseBackup.BACKUP_FILENAME_SUFFIX).isFile(), is(true));
  }
}
