/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@H2DiskTest
@Category(SlowTest.class)
public class ResetAdminCommandAuditTest
    extends AbstractDataTest
    implements AuditTestSupport
{
  @Rule
  public LogOutput logOutput = new LogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private InsightConfig insightConfig;

  @Before
  public void before() throws Exception {
    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(temporaryFolder.newFolder().getAbsolutePath());
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return daoFactory.createPolicyDAO();
  }

  @Test
  public void testRun() throws Exception {
    runTest();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RESET_USER_PASSWORD, null, SYSTEM_USER);
    assertCustomData(auditDTO, "username", ResetAdminCommand.DEFAULT_ADMIN.getUsername());
  }

  @Test
  public void testRun_Exception() {
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> new ResetAdminCommand().run((InsightConfig) null));
    assertAuditLog(AuditEvent.RESET_USER_PASSWORD, "server-error", SYSTEM_USER);
  }

  private void runTest() {
    new ResetAdminCommand()
    {
      // Use the provided OperationalDataStore from DatabaseRule
      @Override
      protected OperationalDataStore getOperationalDataStore(final DatabaseConfig databaseConfig) {
        return databaseRule.getOperationalDataStore();
      }
    }.run(insightConfig);
  }
}
