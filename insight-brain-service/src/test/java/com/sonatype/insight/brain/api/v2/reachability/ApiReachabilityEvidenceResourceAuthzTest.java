/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.reachability;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for {@link ApiReachabilityEvidenceResource}.
 * Verifies that READ permission scoped to the application is required.
 */
@Category(SlowTest.class)
public class ApiReachabilityEvidenceResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private String buildPath() {
    return String.format(
        "/api/v2/applications/%s/reports/someReportId/vulnerabilities/CVE-2023-35116/reachability-evidence",
        app.getPublicId());
  }

  @Test
  public void testGetEvidence_Unauthenticated_Returns401() throws Exception {
    HttpResponse response = restRequest().path(buildPath()).anon().get();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testGetEvidence_Unauthorized_Returns403() throws Exception {
    HttpResponse response = restRequest().path(buildPath()).auth(unauthorized).get();
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testGetEvidence_WithReadPermission_DoesNotReturn401Or403() throws Exception {
    grantReadPermission(app.getId());

    HttpResponse response = restRequest().path(buildPath()).auth(authorized).get();
    // Will be 404 (no evidence data) but NOT 401/403
    assertThat(response.getStatusCode()).isNotIn(401, 403);
  }
}
