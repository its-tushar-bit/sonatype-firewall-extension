/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsConfigurationDTO;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsResource;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2SuccessMetricsResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  void testUpdate_Enabled() throws Exception {
    SuccessMetricsConfigurationDTO configuration = new SuccessMetricsConfigurationDTO();
    configuration.enabled = true;
    successMetricsRequest().body(configuration).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SUCCESS_METRICS, null);
    assertCustomData(auditDTO, "successMetricsFeature", "enabled");
  }

  @Test
  void testUpdate_Disabled() throws Exception {
    successMetricsRequest().body(new SuccessMetricsConfigurationDTO()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SUCCESS_METRICS, null);
    assertCustomData(auditDTO, "successMetricsFeature", "disabled");
  }

  @Test
  void testUpdate_Unauthorized() throws Exception {
    successMetricsRequest().with(unauthorizedUser()).body(new SuccessMetricsConfigurationDTO()).put();

    assertAuditLog(AuditEvent.CONFIGURE_SUCCESS_METRICS, "unauthorized");
  }

  private HttpRequest successMetricsRequest() {
    return ctx.restRequest().path(SuccessMetricsResource.RESOURCE_PATH);
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
