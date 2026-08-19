/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.backup.DbBackupTask;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.H2DatabaseBackup;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.jupiter.api.Test;

/**
 * H2 port of {@code DbBackupTaskTest#testBackup_H2Database}: the DB backup admin task performs a real
 * embedded-H2 disk backup and writes the {@code ods} backup file. This is the H2-only sibling of the
 * Postgres {@code testBackup_NotH2Database} (which lives in {@code insight-brain-variant-test-pg}); it
 * belongs here because the reused {@code @IqH2Test} server is backed by an embedded H2 disk database.
 */
@IqH2Test
class IqH2DbBackupTaskTest
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  @Test
  void testBackup_H2Database() throws Exception {
    HttpResponse response = ctx.adminRequest().path("tasks", DbBackupTask.PATH).post();
    ctx.assertResponseStatus(200, response);
    String message = response.getBodyText();
    assertThat(message).startsWith(DbBackupTask.RESPONSE_MESSAGE_PREFIX);
    File dbBackupDir = new File(message.substring(DbBackupTask.RESPONSE_MESSAGE_PREFIX.length()).trim());
    assertThat(dbBackupDir).isDirectory();
    assertThat(dbBackupDir.getParentFile().getAbsolutePath()).isEqualTo(
        ctx.lookup(Configuration.class).getDbBackupDir());
    assertThat(new File(dbBackupDir, DatabaseName.ods + H2DatabaseBackup.BACKUP_FILENAME_SUFFIX)).isFile();
  }
}
