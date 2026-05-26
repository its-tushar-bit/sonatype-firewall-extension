/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.service.InsightConfig;
import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SupportServiceUnitTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void testGetAuditLog() throws Exception {
    InsightConfig config = createConfigWithWorkDir();
    File auditLog = tempDir.newFile("custom-audit.log");
    config.setAuditLogFilename(auditLog.getAbsolutePath());

    assertThat(SupportService.getAuditLog(config)).isEqualTo(auditLog);
  }

  @Test
  public void testGetAuditLog_NoAuditLogger() throws Exception {
    InsightConfig config = createConfigWithWorkDir();
    // No logs directory, so no audit log

    assertThat(SupportService.getAuditLog(config)).isNull();
  }

  @Test
  public void testGetAuditLog_FallsBackToWorkDirAuditLogWhenConfiguredPathMissing() throws Exception {
    InsightConfig config = createConfigWithWorkDir();
    File logsDir = new File(config.getSonatypeWork(), "logs");
    assertThat(logsDir.mkdirs()).isTrue();
    File fallbackAuditLog = new File(logsDir, "audit.log");
    assertThat(fallbackAuditLog.createNewFile()).isTrue();
    config.setAuditLogFilename(new File(tempDir.getRoot(), "missing/custom-audit.log").getAbsolutePath());

    assertThat(SupportService.getAuditLog(config)).isEqualTo(fallbackAuditLog);
  }

  @Test
  public void testGetPolicyViolationLog() throws Exception {
    InsightConfig config = createConfigWithWorkDir();
    File policyLog = tempDir.newFile("custom-policy-violation.log");
    config.setPolicyViolationLogFilename(policyLog.getAbsolutePath());

    assertThat(SupportService.getPolicyViolationLog(config)).isEqualTo(policyLog);
  }

  @Test
  public void testGetPolicyViolationLog_NoPolicyViolationLogger() throws Exception {
    InsightConfig config = createConfigWithWorkDir();
    // No logs directory, so no policy violation log

    assertThat(SupportService.getPolicyViolationLog(config)).isNull();
  }

  private InsightConfig createConfigWithWorkDir() throws IOException {
    File workDir = tempDir.newFolder("work");
    InsightConfig config = new InsightConfig();
    config.setSonatypeWork(workDir.getAbsolutePath());
    return config;
  }
}
