/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2ApiJiraConfigurationResourceAuditTest
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

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.JIRA_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testSetConfiguration() throws Exception {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    dto.username = "username";
    dto.password = "password".toCharArray();
    dto.customFields = Maps.newHashMap("field", "value");

    HttpResponse response = restRequest().body(dto).put();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_JIRA, null);
    assertAuditData(auditDTO, dto.url, dto.username);
  }

  @Test
  void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_JIRA, "bad-request");
  }

  @Test
  void testDeleteConfiguration() throws Exception {
    JiraConfiguration config = ctx.tempEntity().newJiraConfiguration();

    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_JIRA, null);
    assertAuditData(auditDTO, config.getUrl(), config.getUsername());
  }

  @Test
  void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(404, response);
    assertAuditLog(AuditEvent.DELETE_JIRA, "not-found");
  }

  private void assertAuditData(
      AuditDTO auditDTO,
      String url,
      String username)
  {
    assertCustomData(auditDTO, "url", url);
    assertCustomData(auditDTO, "username", username);
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
