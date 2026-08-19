/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationRequest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.CPE_MATCHING_CONFIGURATION_RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2CpeMatchingConfigurationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() throws Exception {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    ctx.setFeatures(LicensedFeature.CPE_MATCHING);
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
  public com.sonatype.insight.brain.dataaccess.policy.PolicyDAO getPolicyDAO() {
    return ctx.lookup(com.sonatype.insight.brain.dataaccess.policy.PolicyDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(CPE_MATCHING_CONFIGURATION_RESOURCE_PATH);
  }

  @Test
  void testUpdateCpeMatchingConfiguration() throws Exception {
    Application app1 = ctx.tempEntity().newApplicationWithParent();
    CpeMatchingConfigurationRequest requestDTO = new CpeMatchingConfigurationRequest();
    requestDTO.enabled = true;
    HttpResponse response = restRequest().parameter("application", app1.getId())
        .body(requestDTO)
        .put();
    ctx.assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_CPE_MATCHING_CONFIGURATION, null);
    assertCustomData(auditDTO, "enabled", true);
  }

  @Test
  void testUpdateCpeMatchingConfiguration_unauthorized() throws Exception {
    Application app1 = ctx.tempEntity().newApplicationWithParent();
    CpeMatchingConfigurationRequest requestDTO = new CpeMatchingConfigurationRequest();
    requestDTO.enabled = true;
    HttpResponse response = restRequest().parameter("application", app1.getId())
        .body(requestDTO)
        .with(httpRequest -> httpRequest.auth(unauthorizedUser))
        .put();
    ctx.assertResponseStatus(403, response);
    assertAuditLog(AuditEvent.UPDATE_CPE_MATCHING_CONFIGURATION, "unauthorized");
  }

  @Test
  void testUpdateCpeMatchingConfiguration_noRequestObjectError() throws Exception {
    Application app1 = ctx.tempEntity().newApplicationWithParent();
    HttpResponse response = restRequest().parameter("application", app1.getId()).put();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("CPE matching configuration cannot be null");
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_CPE_MATCHING_CONFIGURATION, "bad-request");
    assertCustomData(auditDTO, "enabled", null);
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
