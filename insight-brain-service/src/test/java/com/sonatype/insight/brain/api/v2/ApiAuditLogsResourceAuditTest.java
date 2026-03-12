/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.apache.http.HttpStatus;
import org.junit.Test;

@Category(SlowTest.class)
public class ApiAuditLogsResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.AUDIT_LOGS_RESOURCE_PATH);
  }

  @Test
  public void testGetAuditLogs() throws Exception {
    User user = createUserWithPermissions(Permission.ACCESS_AUDIT_LOG);

    HttpResponse response = restRequest().auth(user)
        .query("startUtcDate", "2024-02-04")
        .query("endUtcDate", "2024-02-08")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertAuditLog(AuditEvent.EXPORT_AUDIT_LOG, null, user.getUsername());
  }

  @Test
  public void testGetAuditLogs_Unauthorized() throws Exception {
    // Create a user without ACCESS_AUDIT_LOG permission
    User user = createUserWithPermissions(Permission.READ);

    HttpResponse response = restRequest().auth(user)
        .query("startUtcDate", "2024-02-10")
        .query("endUtcDate", "2024-02-08")
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
    assertAuditLog(AuditEvent.EXPORT_AUDIT_LOG, "unauthorized", user.getUsername());
  }
}
