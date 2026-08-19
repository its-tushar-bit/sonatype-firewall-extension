/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRequestPolicyWaiverDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH;
import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresApiPolicyWaiverResourceAuthzTest
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private Organization org;

  private Application app;

  private User authorized;

  @BeforeEach
  void createEntities() {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon();
  }

  @Test
  void requestPolicyWaiver_unauthenticated() throws Exception {
    HttpRequest request =
        restRequest().path(PublicApiPaths.POLICY_WAIVER_PATH, REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH)
            .parameter("policyViolationId")
            .body(new ApiRequestPolicyWaiverDTO())
            .anon();
    HttpResponse response = request.post();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void requestPolicyWaiver_authenticated() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy(app);
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), StageTypes.BUILD.getName(), "scanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    ApiRequestPolicyWaiverDTO dto = new ApiRequestPolicyWaiverDTO();
    dto.comment = "waiver comment";
    dto.policyViolationLink = "policyViolationLink.com";
    dto.addWaiverLink = "addWaiverLink.com";

    HttpRequest request =
        restRequest().path(PublicApiPaths.POLICY_WAIVER_PATH, REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH)
            .parameter(policyViolation.getId())
            .body(dto)
            .auth(authorized);
    HttpResponse response = request.post();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }
}
