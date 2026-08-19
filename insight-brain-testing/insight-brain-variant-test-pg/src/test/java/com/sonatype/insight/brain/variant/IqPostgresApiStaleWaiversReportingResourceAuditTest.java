/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiStaleWaiversReportingResource;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * IQ Server on PostgreSQL — audit logging for {@link ApiStaleWaiversReportingResource}, converted
 * from the legacy {@code ApiStaleWaiversReportingResourceAuditTest}. No base class; state comes
 * from the injected {@link IqTestContext}.
 */
@IqPostgresTest
class IqPostgresApiStaleWaiversReportingResourceAuditTest
    implements AuditTestSupport
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
  }

  @AfterEach
  void tearDown() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiStaleWaiversReportingResource.PATH);
  }

  @Test
  void testGetStaleWaivers_NoValues() throws Exception {
    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_STALE_WAIVERS, null);
    assertCustomData(auditDTO, "numberOfStaleWaivers", 0);
  }

  @Test
  void testGetStaleWaivers() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    Policy policy = ctx.tempEntity().newPolicy();
    Policy expiredWaiverPolicy = ctx.tempEntity().newPolicy();

    ctx.tempEntity().newWaiver("hash1", policy.getId(), app.getId(), "stale waiver comment1");

    Repository repo = ctx.tempEntity().newRepository();

    ctx.tempEntity().newWaiver("hash2", policy.getId(), repo.getId(), null, "stale waiver comment2");

    Date expiredTime = Date.from(Instant.now().minus(Duration.ofHours(10)));
    ctx.tempEntity()
        .newWaiver("hash3", expiredWaiverPolicy.getId(), app.getId(), null, "expired waiver", null,
            expiredTime);

    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_STALE_WAIVERS, null);
    assertCustomData(auditDTO, "numberOfStaleWaivers", 2);
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
