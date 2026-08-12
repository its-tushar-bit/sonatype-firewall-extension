/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Converted from the legacy {@code ApiVersionEvaluationWindowResourceAuditTest}. Kept in the original package
 * because {@link ApiVersionEvaluationWindowResource#OWNER_PATH} is package-private.
 */
@IqH2Test
class IqH2ApiVersionEvaluationWindowResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Organization org;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    org = ctx.tempEntity().newOrganization();
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

  private HttpRequest restRequest() {
    return ctx.restRequest()
        .path(PublicApiPaths.VERSION_EVALUATION_WINDOW_RESOURCE_PATH, ApiVersionEvaluationWindowResource.OWNER_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  void testSetConfiguration() throws Exception {
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    restRequest().body(dto).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_VERSION_EVALUATION_WINDOW, null);
    assertOwnerData(auditDTO, org);
    assertCustomData(auditDTO, "contextId", "context1");
    assertCustomData(auditDTO, "maxVersions", 10);
    assertCustomData(auditDTO, "maxAgeInDays", 30);
  }

  @Test
  void testSetConfiguration_Unauthorized() throws Exception {
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    restRequest().with(unauthorizedUser()).body(dto).put();

    assertAuditLog(AuditEvent.CONFIGURE_VERSION_EVALUATION_WINDOW, "unauthorized");
  }

  @Test
  void testDeleteConfiguration() throws Exception {
    ctx.tempEntity().newVersionEvaluationWindow(org.getId(), "context1", 10, 30);

    restRequest().query("contextId", "context1").delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_VERSION_EVALUATION_WINDOW, null);
    assertOwnerData(auditDTO, org);
    assertCustomData(auditDTO, "contextId", "context1");
  }

  @Test
  void testDeleteConfiguration_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).query("contextId", "context1").delete();

    assertAuditLog(AuditEvent.DELETE_VERSION_EVALUATION_WINDOW, "unauthorized");
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
