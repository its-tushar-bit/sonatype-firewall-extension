/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2ApiAuditLogsResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
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
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.AUDIT_LOGS_RESOURCE_PATH);
  }

  private User createUserWithPermissions(Permission... permissions) {
    User user = ctx.tempEntity().newUser();
    Role role = ctx.tempEntity().newRole(false /* global */, permissions);
    ctx.tempEntity().newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), user.getUsername());
    return user;
  }

  @Test
  void testGetAuditLogs() throws Exception {
    User user = createUserWithPermissions(Permission.ACCESS_AUDIT_LOG);

    HttpResponse response = restRequest().auth(user)
        .query("startUtcDate", "2024-02-04")
        .query("endUtcDate", "2024-02-08")
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    assertAuditLog(AuditEvent.EXPORT_AUDIT_LOG, null, user.getUsername());
  }

  @Test
  void testGetAuditLogs_Unauthorized() throws Exception {
    // Create a user without ACCESS_AUDIT_LOG permission
    User user = createUserWithPermissions(Permission.READ);

    HttpResponse response = restRequest().auth(user)
        .query("startUtcDate", "2024-02-10")
        .query("endUtcDate", "2024-02-08")
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
    assertAuditLog(AuditEvent.EXPORT_AUDIT_LOG, "unauthorized", user.getUsername());
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
