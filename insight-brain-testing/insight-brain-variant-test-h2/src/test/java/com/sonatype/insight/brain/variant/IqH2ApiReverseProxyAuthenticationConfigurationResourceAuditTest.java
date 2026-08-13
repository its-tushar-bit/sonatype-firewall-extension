/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiReverseProxyAuthenticationConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Converted from the legacy {@code ApiReverseProxyAuthenticationConfigurationResourceAuditTest}.
 */
@IqH2Test
class IqH2ApiReverseProxyAuthenticationConfigurationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

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

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.REVERSE_PROXY_AUTHENTICATION_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testSetConfiguration() throws Exception {
    ApiReverseProxyAuthenticationConfigurationDTO dto = new ApiReverseProxyAuthenticationConfigurationDTO();
    dto.enabled = true;
    dto.usernameHeader = "usernameHeader";
    dto.csrfProtectionDisabled = true;
    dto.logoutUrl = "logoutUrl";

    HttpResponse response = restRequest().body(dto).put();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REVERSE_PROXY_AUTHENTICATION, null);
    assertAuditData(auditDTO, dto.enabled, dto.usernameHeader, dto.csrfProtectionDisabled, dto.logoutUrl);
  }

  @Test
  void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_REVERSE_PROXY_AUTHENTICATION, "bad-request");
  }

  @Test
  void testDeleteConfiguration() throws Exception {
    ReverseProxyAuthenticationConfiguration config = ctx.tempEntity().newReverseProxyAuthenticationConfiguration();

    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REVERSE_PROXY_AUTHENTICATION, null);
    assertAuditData(auditDTO, config.isEnabled(), config.getUsernameHeader(), config.isCsrfProtectionDisabled(),
        config.getLogoutUrl());
  }

  @Test
  void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(404, response);
    assertAuditLog(AuditEvent.DELETE_REVERSE_PROXY_AUTHENTICATION, "not-found");
  }

  private void assertAuditData(
      AuditDTO auditDTO,
      boolean enabled,
      String usernameHeader,
      boolean csrfProtectionDisabled,
      String logoutUrl)
  {
    assertCustomData(auditDTO, "enabled", enabled);
    assertCustomData(auditDTO, "usernameHeader", usernameHeader);
    assertCustomData(auditDTO, "csrfProtectionDisabled", csrfProtectionDisabled);
    assertCustomData(auditDTO, "logoutUrl", logoutUrl);
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
