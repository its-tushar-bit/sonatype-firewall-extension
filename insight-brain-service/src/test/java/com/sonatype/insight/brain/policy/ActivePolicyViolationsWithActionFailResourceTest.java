/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ActivePolicyViolationsWithActionFailResourceTest
    extends AbstractResourceTest
{
  private PolicyViolationDAO policyViolationDAO;

  @Before
  public void setUp() {
    policyViolationDAO = lookup(PolicyViolationDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ActivePolicyViolationsWithActionFailResource.RESOURCE_PATH);
  }

  @Test
  public void testGetActiveViolationsWithActionFail__Success() throws Exception {
    final String STAGE_ID = BuildStageType.ID;

    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getPublicId());
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), STAGE_ID, "scan-1");

    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    openViolation.setActionTypeId(Action.ID_FAIL);
    policyViolationDAO.update(openViolation);

    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));

    tempEntity.newPolicyViolation(policyEvaluation, policy);
    HttpResponse response = restRequest()
        .parameter(application.getPublicId(), STAGE_ID)
        .get();

    assertResponseStatus(200, response);

    List<PolicyViolation> result = response.getBodyList();
    assertThat(result).hasSize(1);
  }

  @Test
  public void testGetActiveViolationsWithActionFail_Empty() throws Exception {
    final String STAGE_ID = BuildStageType.ID;

    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getPublicId());

    HttpResponse response = restRequest()
        .parameter(application.getPublicId(), STAGE_ID)
        .get();

    assertResponseStatus(200, response);

    List<PolicyViolation> result = response.getBodyList();
    assertThat(result).hasSize(0);
  }
}
