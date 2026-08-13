/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.ide.IdeResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2IdeResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Application app;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    app = ctx.tempEntity().newApplicationWithParent();
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

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  void testDoScan() throws Exception {
    restRequest("simple").get();

    assertAuditLog(null);
  }

  @Test
  void testPostScan() throws Exception {
    restRequest("enhanced").post();

    assertAuditLog(null);
  }

  @Test
  void testDoScan_Unauthorized() throws Exception {
    restRequest("simple").with(unauthorizedUser()).get();

    assertAuditLog("unauthorized");
  }

  @Test
  void testPostScan_Unauthorized() throws Exception {
    restRequest("enhanced").with(unauthorizedUser()).post();

    assertAuditLog("unauthorized");
  }

  @Test
  void testDoCoordinatesScan() throws Exception {
    restCoordinatesRequest().get();

    assertAuditLog(null);
  }

  @Test
  void testDoCoordinatesScan_Unauthorized() throws Exception {
    restCoordinatesRequest().with(unauthorizedUser()).get();

    assertAuditLog("unauthorized");
  }

  private void assertAuditLog(String error) {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_PROJECT, error);
    assertApplicationData(auditDTO, app);
  }

  private HttpRequest restRequest(String scanType) {
    String hash = "abababababababababab";
    HttpRequest request =
        ctx.restRequest().path(IdeResource.RESOURCE_PATH).path("scan", scanType, app.getPublicId(), hash);

    String hdsUrl = "rest/ide/scan/" + scanType + "/" + hash;
    ctx.hdsRespondWithResource("/IdeResourceAuditTest/SimpleMatch_abababababababababab.json").atUri(hdsUrl);

    return request;
  }

  private HttpRequest restCoordinatesRequest() {
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "", "jar");
    HttpRequest request = ctx.restRequest()
        .path(IdeResource.RESOURCE_PATH)
        .path(IdeResource.COORDINATES_SCAN_PATH)
        .parameter(app.getPublicId())
        .query("componentIdentifier", identifier);

    String hdsUrl = request.getUrl().replaceFirst("(.*/)(rest/ide/scan/coordinates)(/[^?]+)(.*)", "$2$4");
    ctx.hdsRespondWithResource("/IdeResourceAuditTest/Coordinates.json").atUri(hdsUrl);

    return request;
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
