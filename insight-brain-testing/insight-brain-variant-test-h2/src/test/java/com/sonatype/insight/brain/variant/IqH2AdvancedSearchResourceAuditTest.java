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
import com.sonatype.insight.brain.search.AdvancedSearchResource;
import com.sonatype.insight.brain.search.AdvancedSearchStatusDTO;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2AdvancedSearchResourceAuditTest
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
  void testSetStatus_Enabled() throws Exception {
    AdvancedSearchStatusDTO statusDTO = new AdvancedSearchStatusDTO();
    statusDTO.isEnabled = true;

    restRequest().body(statusDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ADVANCED_SEARCH, null);
    assertCustomData(auditDTO, "advancedSearch", "enabled");
  }

  @Test
  void testSetStatus_Disabled() throws Exception {
    AdvancedSearchStatusDTO statusDTO = new AdvancedSearchStatusDTO();
    statusDTO.isEnabled = false;

    restRequest().body(statusDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ADVANCED_SEARCH, null);
    assertCustomData(auditDTO, "advancedSearch", "disabled");
  }

  @Test
  void testSetStatus_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).body(new AdvancedSearchStatusDTO()).put();

    assertAuditLog(AuditEvent.CONFIGURE_ADVANCED_SEARCH, "unauthorized");
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(AdvancedSearchResource.RESOURCE_PATH, AdvancedSearchResource.STATUS_PATH);
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
