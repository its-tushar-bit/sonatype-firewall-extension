/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.version.VersionService;

import org.junit.Before;
import org.junit.Test;

public class ApplicationLifecycleAuditTest
    extends AbstractAuditTest
{
  private ApplicationLifecycle lifecycle;

  @Before
  public void before() {
    lifecycle = getCLMServer().getInjector().getInstance(ApplicationLifecycle.class);
  }

  @Test
  public void testBoot() throws Exception {
    lifecycle.boot();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.START_SERVER, null, SYSTEM_USER);
    assertLifecycleAuditData(auditDTO);
  }

  @Test
  public void testStop() throws Exception {
    lifecycle.stop();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.STOP_SERVER, null, SYSTEM_USER);
    assertLifecycleAuditData(auditDTO);
  }

  private void assertLifecycleAuditData(final AuditDTO auditDTO) {
    assertCustomData(auditDTO, "serverInstanceId", InsightBrainService.getInstanceId());
    assertCustomData(auditDTO, "serverConfigurationFile", InsightBrainService.getConfigFile().toString());
    assertCustomData(auditDTO, "serverRelease", new VersionService().getLogDisplayVersion());
    assertCustomData(auditDTO, "processOwner", System.getProperty("user.name"));
  }
}
