/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import java.io.File;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserActivityResourceAuditTest
    extends AbstractAuditTest
{
  @Before
  public void before() {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
    File logsDir = new File(getCLMServer().getConfiguration().getSonatypeWork(), "logs");
    assertThat(logsDir.mkdirs() || logsDir.isDirectory()).isTrue();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.USER_ACTIVITY_RESOURCE_PATH);
  }

  @Test
  public void testGetUserActivitySummary() throws Exception {
    User user = createUserWithPermissions(Permission.ACCESS_AUDIT_LOG);

    HttpResponse response = restRequest().auth(user)
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertAuditLog(AuditEvent.VIEW_AUDIT_LOG, null, user.getUsername());
  }

  @Test
  public void testGetUserActivityDetail() throws Exception {
    User user = createUserWithPermissions(Permission.ACCESS_AUDIT_LOG);

    HttpResponse response = restRequest().auth(user)
        .path("/" + user.getUsername())
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertAuditLog(AuditEvent.VIEW_AUDIT_LOG, null, user.getUsername());
  }

  @Test
  public void testGetUserActivitySummary_Unauthorized() throws Exception {
    // Create a user without ACCESS_AUDIT_LOG permission
    User user = createUserWithPermissions(Permission.READ);

    HttpResponse response = restRequest().auth(user)
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
    assertAuditLog(AuditEvent.VIEW_AUDIT_LOG, "unauthorized", user.getUsername());
  }

  @Test
  public void testGetUserActivityDetail_Unauthorized() throws Exception {
    // Create a user without ACCESS_AUDIT_LOG permission
    User user = createUserWithPermissions(Permission.READ);

    HttpResponse response = restRequest().auth(user)
        .path("/testuser")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
    assertAuditLog(AuditEvent.VIEW_AUDIT_LOG, "unauthorized", user.getUsername());
  }

  @Test
  public void testExportUserActivitySummary() throws Exception {
    User user = createUserWithPermissions(Permission.ACCESS_AUDIT_LOG);

    HttpResponse response = restRequest().auth(user)
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertAuditLog(AuditEvent.EXPORT_AUDIT_LOG, null, user.getUsername());
  }

  @Test
  public void testExportUserActivitySummary_Unauthorized() throws Exception {
    // Create a user without ACCESS_AUDIT_LOG permission
    User user = createUserWithPermissions(Permission.READ);

    HttpResponse response = restRequest().auth(user)
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
    assertAuditLog(AuditEvent.EXPORT_AUDIT_LOG, "unauthorized", user.getUsername());
  }

  @Test
  public void testUserActivityAuditEventExists() {
    // Verify that the audit event for viewing audit logs is properly defined
    assertThat(AuditEvent.VIEW_AUDIT_LOG.getDomain()).isEqualTo("audit-log");
    assertThat(AuditEvent.VIEW_AUDIT_LOG.getType()).isEqualTo("view");

    // Verify that the audit event for exporting audit logs is properly defined
    assertThat(AuditEvent.EXPORT_AUDIT_LOG.getDomain()).isEqualTo("audit-log");
    assertThat(AuditEvent.EXPORT_AUDIT_LOG.getType()).isEqualTo("export");
  }
}
