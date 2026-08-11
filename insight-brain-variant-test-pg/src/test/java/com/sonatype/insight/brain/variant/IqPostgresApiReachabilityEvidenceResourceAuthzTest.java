/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for {@code ApiReachabilityEvidenceResource}.
 * Verifies that READ permission scoped to the application is required.
 */
@IqPostgresTest
class IqPostgresApiReachabilityEvidenceResourceAuthzTest
{
  private IqTestContext ctx;

  private Application app;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void createEntities() {
    var org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();
  }

  private void grantReadPermission(String contextId) {
    Role role = ctx.tempEntity().newRole(false /* global */, Permission.READ);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon();
  }

  private String buildPath() {
    return String.format(
        "/api/v2/applications/%s/reports/someReportId/vulnerabilities/CVE-2023-35116/reachability-evidence",
        app.getPublicId());
  }

  @Test
  void testGetEvidence_Unauthenticated_Returns401() throws Exception {
    HttpResponse response = restRequest().path(buildPath()).anon().get();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testGetEvidence_Unauthorized_Returns403() throws Exception {
    HttpResponse response = restRequest().path(buildPath()).auth(unauthorized).get();
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testGetEvidence_WithReadPermission_DoesNotReturn401Or403() throws Exception {
    grantReadPermission(app.getId());

    HttpResponse response = restRequest().path(buildPath()).auth(authorized).get();
    // Will be 404 (no evidence data) but NOT 401/403
    assertThat(response.getStatusCode()).isNotIn(401, 403);
  }
}
