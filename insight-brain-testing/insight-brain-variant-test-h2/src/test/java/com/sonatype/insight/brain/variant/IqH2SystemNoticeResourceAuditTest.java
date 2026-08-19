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
import com.sonatype.insight.brain.configuration.SystemNoticeResource;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2SystemNoticeResourceAuditTest
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
  void after() throws Exception {
    // system_notice is a singleton row the reused server persists across tests. SystemNoticeDAO is update-only
    // and TemporaryEntity does not track it, so a test that enables the notice would leak into later @IqH2Test
    // classes in the fork. Restore the default (disabled) notice before tearing down log capture.
    SystemNotice reset = new SystemNotice();
    reset.setEnabled(false);
    systemNoticeRequest().body(reset).put();
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
  void testUpdateSystemNotice_Enabled() throws Exception {
    SystemNotice notice = new SystemNotice();
    notice.setEnabled(true);
    notice.setMessage("notice");
    systemNoticeRequest().body(notice).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SYSTEM_NOTICE, null);
    assertCustomData(auditDTO, "systemNoticeDisplay", "enabled");
    assertCustomData(auditDTO, "systemNoticeText", notice.getMessage());
  }

  @Test
  void testUpdateSystemNotice_Disabled() throws Exception {
    SystemNotice notice = new SystemNotice();
    notice.setEnabled(false);
    systemNoticeRequest().body(notice).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SYSTEM_NOTICE, null);
    assertCustomData(auditDTO, "systemNoticeDisplay", "disabled");
  }

  @Test
  void testUpdateSystemNotice_Unauthorized() throws Exception {
    systemNoticeRequest().with(unauthorizedUser()).body(new SystemNotice()).put();

    assertAuditLog(AuditEvent.CONFIGURE_SYSTEM_NOTICE, "unauthorized");
  }

  private HttpRequest systemNoticeRequest() {
    return ctx.restRequest().path(SystemNoticeResource.RESOURCE_PATH);
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
