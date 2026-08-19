/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.File;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.test.LogOutput;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2UserActivityResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();

    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
    File logsDir = new File(ctx.lookup(InsightConfig.class).getSonatypeWork(), "logs");
    assertThat(logsDir.mkdirs() || logsDir.isDirectory()).isTrue();
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(false);
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
    return ctx.restRequest().path(PublicApiPaths.USER_ACTIVITY_RESOURCE_PATH);
  }

  private User createUserWithPermissions(Permission... permissions) {
    User user = ctx.tempEntity().newUser();
    Role role = ctx.tempEntity().newRole(false /* global */, permissions);
    ctx.tempEntity().newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), user.getUsername());
    return user;
  }

  @Test
  void testGetUserActivitySummary() throws Exception {
    User user = createUserWithPermissions(Permission.ACCESS_AUDIT_LOG);

    HttpResponse response = restRequest().auth(user)
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    assertAuditLog(AuditEvent.VIEW_AUDIT_LOG, null, user.getUsername());
  }

  @Test
  void testGetUserActivityDetail() throws Exception {
    User user = createUserWithPermissions(Permission.ACCESS_AUDIT_LOG);

    HttpResponse response = restRequest().auth(user)
        .path("/" + user.getUsername())
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    assertAuditLog(AuditEvent.VIEW_AUDIT_LOG, null, user.getUsername());
  }

  @Test
  void testGetUserActivitySummary_Unauthorized() throws Exception {
    // Create a user without ACCESS_AUDIT_LOG permission
    User user = createUserWithPermissions(Permission.READ);

    HttpResponse response = restRequest().auth(user)
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
    assertAuditLog(AuditEvent.VIEW_AUDIT_LOG, "unauthorized", user.getUsername());
  }

  @Test
  void testGetUserActivityDetail_Unauthorized() throws Exception {
    // Create a user without ACCESS_AUDIT_LOG permission
    User user = createUserWithPermissions(Permission.READ);

    HttpResponse response = restRequest().auth(user)
        .path("/testuser")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
    assertAuditLog(AuditEvent.VIEW_AUDIT_LOG, "unauthorized", user.getUsername());
  }

  @Test
  void testExportUserActivitySummary() throws Exception {
    User user = createUserWithPermissions(Permission.ACCESS_AUDIT_LOG);

    HttpResponse response = restRequest().auth(user)
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    assertAuditLog(AuditEvent.EXPORT_AUDIT_LOG, null, user.getUsername());
  }

  @Test
  void testExportUserActivitySummary_Unauthorized() throws Exception {
    // Create a user without ACCESS_AUDIT_LOG permission
    User user = createUserWithPermissions(Permission.READ);

    HttpResponse response = restRequest().auth(user)
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    ctx.assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
    assertAuditLog(AuditEvent.EXPORT_AUDIT_LOG, "unauthorized", user.getUsername());
  }

  @Test
  void testUserActivityAuditEventExists() {
    // Verify that the audit event for viewing audit logs is properly defined
    assertThat(AuditEvent.VIEW_AUDIT_LOG.getDomain()).isEqualTo("audit-log");
    assertThat(AuditEvent.VIEW_AUDIT_LOG.getType()).isEqualTo("view");

    // Verify that the audit event for exporting audit logs is properly defined
    assertThat(AuditEvent.EXPORT_AUDIT_LOG.getDomain()).isEqualTo("audit-log");
    assertThat(AuditEvent.EXPORT_AUDIT_LOG.getType()).isEqualTo("export");
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
