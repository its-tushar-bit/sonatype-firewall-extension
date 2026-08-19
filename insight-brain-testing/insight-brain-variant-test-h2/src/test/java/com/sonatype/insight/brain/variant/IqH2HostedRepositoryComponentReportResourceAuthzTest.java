/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.report.HostedRepositoryComponentReportResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization coverage for {@link HostedRepositoryComponentReportResource}.
 * <p>
 * Re-evaluation is a write operation on an individual component, so it is gated on
 * {@link Permission#EVALUATE_COMPONENT} — the same permission the hosted-scan branch of
 * {@code ReportResource#reevaluatePolicy} requires. {@link Permission#WRITE} ("Edit IQ Elements")
 * is a separately assignable permission that must not by itself authorize an evaluation.
 * <p>
 * Kept in the {@code com.sonatype.insight.brain.variant} package; reproduces the {@code AbstractResourceAuthzTest}
 * fixture (authorized/unauthorized users) and its {@code testAuthzPost} helper that the legacy
 * {@code HostedRepositoryComponentReportResourceAuthzTest} inherited from its base class.
 */
@IqH2Test
class IqH2HostedRepositoryComponentReportResourceAuthzTest
{
  private IqTestContext ctx;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void createEntities() {
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();
  }

  private void grantPermission(String contextId, Permission permission) {
    Role role = ctx.tempEntity().newRole(false /* global */, permission);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(HostedRepositoryComponentReportResource.RESOURCE_PATH);
  }

  private void assertStatus(HttpResponse response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    }
    else {
      assertThat(response.getStatusCode()).isEqualTo(status);
    }
  }

  private HttpResponse testAuthzPost(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    HttpResponse response = request.auth(unauthorized).post();
    assertStatus(response, 403);

    response = request.auth(authorized).post();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  @Test
  void testReevaluatePolicy_requiresEvaluateComponentPermission() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    HostedRepositoryComponent hrc = ctx.tempEntity().newHostedRepositoryComponent(repository);

    grantPermission(hrc.getId(), Permission.EVALUATE_COMPONENT);

    // The granted user reaches the handler; the unauthorized user is rejected with 403 before it.
    // A 404 from the granted user is an acceptable success status here: authorization passed and the
    // handler then found no policy_evaluation for this scanId. What matters is that it is not 403.
    HttpRequest request = restRequest().path(HostedRepositoryComponentReportResource.REEVALUATE_PATH)
        .parameter(hrc.getId(), "no-such-scan");
    testAuthzPost(request, 404);
  }

  @Test
  void testReevaluatePolicy_writePermissionAloneIsNotSufficient() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    HostedRepositoryComponent hrc = ctx.tempEntity().newHostedRepositoryComponent(repository);

    // WRITE is what the read handlers on this resource use. Granting only WRITE must not authorize
    // triggering an evaluation — that is the distinction this endpoint's @Authorize encodes.
    grantPermission(hrc.getId(), Permission.WRITE);

    HttpRequest request = restRequest().path(HostedRepositoryComponentReportResource.REEVALUATE_PATH)
        .parameter(hrc.getId(), "no-such-scan");
    assertThat(request.auth(authorized).post().getStatusCode()).isEqualTo(403);
  }
}
