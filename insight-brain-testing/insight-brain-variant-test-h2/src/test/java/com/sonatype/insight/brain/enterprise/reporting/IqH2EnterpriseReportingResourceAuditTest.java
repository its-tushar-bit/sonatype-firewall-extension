/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionAcquire;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kept in the {@code com.sonatype.insight.brain.enterprise.reporting} package (not the default
 * {@code com.sonatype.insight.brain.variant} package) because it references
 * {@link EnterpriseReportingService#clearEnterpriseReportingConfigDTOBaseUrlSupplierForTests()}, which is
 * package-private. Reproduces the {@code AbstractAuditTest} log-capture scaffolding that the legacy
 * {@code EnterpriseReportingResourceAuditTest} inherited from its base class.
 */
@IqH2Test
class IqH2EnterpriseReportingResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    clearLookerConfigCache();
  }

  @AfterEach
  void after() {
    clearLookerConfigCache();
    logOutput.tearDown();
  }

  private void clearLookerConfigCache() {
    ctx.lookup(EnterpriseReportingService.class)
        .clearEnterpriseReportingConfigDTOBaseUrlSupplierForTests();
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
    return ctx.restRequest().path(EnterpriseReportingResource.RESOURCE_PATH);
  }

  @Test
  void testAcquireEmbedSession() throws Exception {
    EmbedCookielessSessionAcquire expectedResponse =
        new EmbedCookielessSessionAcquire("authTokenResponse", 300, "navTokenResponse", 400, "apiTokenResponse", 500,
            "sessionTokenResponse", 600);
    ctx.hdsRespondWith(expectedResponse).atUri("rest/enterpriseReporting/acquireEmbedSession");
    String encodedEmbedDomain = "http%3A%2F%2Flocalhost%3A8070";

    restRequest().path(EnterpriseReportingResource.ACQUIRE_EMBED_SESSION)
        .query("dashboardId", "dashboardIdParam")
        .query("embedDomain", encodedEmbedDomain)
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_INTEGRATED_ENTERPRISE_REPORTING_DASHBOARD, null);
    assertCustomData(auditDTO, "dashboard", "dashboardIdParam");
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
