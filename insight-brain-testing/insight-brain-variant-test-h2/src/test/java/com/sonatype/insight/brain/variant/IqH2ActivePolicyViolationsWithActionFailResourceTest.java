/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.ActivePolicyViolationsWithActionFailResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ActivePolicyViolationsWithActionFailResourceTest
{
  private IqTestContext ctx;

  private PolicyViolationDAO policyViolationDAO;

  @BeforeEach
  void setUp() {
    policyViolationDAO = ctx.lookup(PolicyViolationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ActivePolicyViolationsWithActionFailResource.RESOURCE_PATH);
  }

  @Test
  void testGetActiveViolationsWithActionFail__Success() throws Exception {
    final String STAGE_ID = BuildStageType.ID;

    Organization organization = ctx.tempEntity().newOrganization();
    Application application = ctx.tempEntity().newApplication(organization.getPublicId());
    Policy policy = ctx.tempEntity().newPolicy(application);

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(application.getId(), STAGE_ID, "scan-1");

    PolicyViolation openViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);
    openViolation.setActionTypeId(Action.ID_FAIL);
    policyViolationDAO.update(openViolation);

    ctx.tempEntity()
        .newWaivedPolicyViolation(policyEvaluation, policy,
            ctx.tempEntity().newWaiver(policy.getId(), application.getId()));

    ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);
    HttpResponse response = restRequest()
        .parameter(application.getPublicId(), STAGE_ID)
        .get();

    ctx.assertResponseStatus(200, response);

    List<PolicyViolation> result = response.getBodyList();
    assertThat(result).hasSize(1);
  }

  @Test
  void testGetActiveViolationsWithActionFail_Empty() throws Exception {
    final String STAGE_ID = BuildStageType.ID;

    Organization organization = ctx.tempEntity().newOrganization();
    Application application = ctx.tempEntity().newApplication(organization.getPublicId());

    HttpResponse response = restRequest()
        .parameter(application.getPublicId(), STAGE_ID)
        .get();

    ctx.assertResponseStatus(200, response);

    List<PolicyViolation> result = response.getBodyList();
    assertThat(result).hasSize(0);
  }
}
