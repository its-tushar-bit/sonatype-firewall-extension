/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.PolicyWaiverResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IQ Server on PostgreSQL — converted from {@code PolicyWaiverResourceAuthzTest}, exercising the
 * authorization boilerplate ({@code AbstractResourceAuthzTest}) inline against
 * {@link PolicyWaiverResource} via the shared, reused server.
 */
@IqPostgresTest
class IqPostgresPolicyWaiverResourceAuthzTest
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private Organization org;

  private Application app;

  private User unauthorized;

  private User authorized;

  private Policy policy;

  @BeforeEach
  public void init() {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();

    policy = ctx.tempEntity().newPolicy(app);
  }

  private void grantReadPermission(String contextId) {
    Role role = ctx.tempEntity().newRole(false /* global */, Permission.READ);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  private void assertStatus(HttpResponse response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    }
    else {
      assertThat(response.getStatusCode()).isEqualTo(status);
    }
  }

  private HttpResponse testAuthzGet(HttpRequest request) throws Exception {
    HttpResponse response = request.auth(unauthorized).get();
    assertStatus(response, 403);

    response = request.auth(authorized).get();
    assertStatus(response, null);
    return response;
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(PolicyWaiverResource.RESOURCE_PATH);
  }

  @Test
  void testGetPolicyWaiversByHash() throws Exception {
    HttpRequest request = restRequest().path("component/hash");

    grantReadPermission(app.getId());

    testAuthzGet(request.parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());

    testAuthzGet(request.parameter(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  void testGetApplicableContexts() throws Exception {
    grantReadPermission(app.getId());

    testAuthzGet(restRequest().path("applicable/context/{policyId}")
        .parameter(OwnerType.APPLICATION,
            app.getPublicId(), policy.getId()));
  }
}
