/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.test.LogOutput;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ResetAdminCommandAuditTest
    implements AuditTestSupport
{
  @Rule
  public LogOutput logOutput = new LogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @After
  public void after() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Test
  public void testRun() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(temporaryFolder.newFolder().getAbsolutePath());
    OperationalDataStoreProvider
        .initWithoutMigration(new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods));

    new ResetAdminCommand().run(null, null, insightConfig);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RESET_USER_PASSWORD, null, SYSTEM_USER);
    assertCustomData(auditDTO, "username", ResetAdminCommand.DEFAULT_ADMIN.getUsername());
  }

  @Test
  public void testRun_Exception() throws Exception {
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> new ResetAdminCommand().run(null, null, null));
    assertAuditLog(AuditEvent.RESET_USER_PASSWORD, "server-error", SYSTEM_USER);
  }
}
