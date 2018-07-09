/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PolicyViolationGrandfatheringResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyViolationGrandfatheringResource.RESOURCE_PATH);
  }

  @Test
  public void testRevokeGrandfathering() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scanId");
    Policy policy = tempEntity.newPolicy("test");
    PolicyViolation policyViolation = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation, policy);

    HttpResponse response = restRequest().path(PolicyViolationGrandfatheringResource.REVOKE_PATH)
        .parameter(application.getPublicId()).put();
    assertResponseStatus(204, response);
    policyViolation = new PolicyViolationDAO().getById(policyViolation.getId());
    assertThat(policyViolation.isGrandfathered(), is(false));
  }
}
