/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.List;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiPolicyViolationWaiverResourceTest
{
  private IqTestContext ctx;

  private PolicyWaiverDAO policyWaiverDAO;

  private Organization org;

  private Application app;

  private Policy policy;

  private PolicyViolation policyViolation;

  @BeforeEach
  void setUpPolicyViolation() {
    policyWaiverDAO = ctx.lookup(PolicyWaiverDAO.class);

    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    policy = ctx.tempEntity().newPolicy(org);

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
  }

  @Test
  void testAddPolicyWaiver_Application() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_WAIVER_PATH)
        .parameter(policyViolation.getId(), OwnerType.APPLICATION.toString())
        .body("waiver comment", MediaType.TEXT_PLAIN)
        .post();

    ctx.assertResponseStatus(204, response);
    assertPolicyWaiver(app.getId(), "waiver comment");
  }

  @Test
  void testAddPolicyWaiver_Organization() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_WAIVER_PATH)
        .parameter(policyViolation.getId(), OwnerType.ORGANIZATION.toString())
        .body("waiver comment", MediaType.TEXT_PLAIN)
        .post();

    ctx.assertResponseStatus(204, response);
    assertPolicyWaiver(org.getId(), "waiver comment");
  }

  private void assertPolicyWaiver(String ownerId, String comment) {
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(ownerId);
    assertThat(policyWaivers).hasSize(1);
    PolicyWaiver policyWaiver = policyWaivers.get(0);
    assertThat(policyWaiver.getId()).isNotNull();
    assertThat(policyWaiver.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiver.getHash()).isEqualTo(policyViolation.getHash());
    assertThat(policyWaiver.getComment()).isEqualTo(comment);
    assertThat(policyWaiver.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiver.getCreateTime()).isNotNull();
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(policyViolation.getConstraintFactsJson());
  }
}
