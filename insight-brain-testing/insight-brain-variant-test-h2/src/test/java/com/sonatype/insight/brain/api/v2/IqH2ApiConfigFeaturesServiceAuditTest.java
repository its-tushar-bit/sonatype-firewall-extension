/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;

@IqH2Test
class IqH2ApiConfigFeaturesServiceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private SystemConfigurationPropertyDAO configurationPropertyDAO;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    configurationPropertyDAO = ctx.lookup(SystemConfigurationPropertyDAO.class);
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
    return ctx.restRequest().path(PublicApiPaths.CONFIG_FEATURES_PATH);
  }

  @Test
  void testEnableFeature() throws Exception {
    configurationPropertyDAO.set(SystemConfigurationProperty.API_PAGE, "false");
    HttpResponse response = restRequest().path(SystemConfigurationProperty.API_PAGE).post();
    ctx.assertResponseStatus(NO_CONTENT_204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SET_FEATURES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.API_PAGE, "null");
  }

  @Test
  void testEnableFeature_enabledWhenAbsent() throws Exception {
    configurationPropertyDAO.set(SystemConfigurationProperty.DASHBOARD_DISABLED, "true");
    HttpResponse response = restRequest().path(ApiConfigFeaturesService.FEATURE_DASHBOARD).post();

    ctx.assertResponseStatus(NO_CONTENT_204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.SET_FEATURES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.DASHBOARD_DISABLED, "null");
  }

  @Test
  void testEnableFeature_Error() throws Exception {
    HttpResponse response = restRequest().path(SystemConfigurationProperty.API_PAGE).post();

    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.SET_FEATURES, "bad-request");
  }

  @Test
  void testDisableFeature() throws Exception {
    HttpResponse response = restRequest().path(SystemConfigurationProperty.API_PAGE).delete();
    ctx.assertResponseStatus(NO_CONTENT_204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UNSET_FEATURES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.API_PAGE, "false");
  }

  @Test
  void testDisableFeature_enabledWhenAbsent() throws Exception {
    HttpResponse response = restRequest().path(ApiConfigFeaturesService.FEATURE_DASHBOARD).delete();
    ctx.assertResponseStatus(NO_CONTENT_204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UNSET_FEATURES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.DASHBOARD_DISABLED, "true");
  }

  @Test
  void testDisableFeature_Error() throws Exception {
    configurationPropertyDAO.set(SystemConfigurationProperty.API_PAGE, "false");
    HttpResponse response = restRequest().path(SystemConfigurationProperty.API_PAGE).delete();

    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.UNSET_FEATURES, "bad-request");
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
