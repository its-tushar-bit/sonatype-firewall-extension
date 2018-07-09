/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PolicyViolationGrandfatheringServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService;

  @Test
  public void testRevokeGrandfathering() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy("test");
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scanId1");
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation fixedGrandfatheredPolicyViolation = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation1,
        policy);
    fixedGrandfatheredPolicyViolation.setFixTime(new Date());
    policyViolationDAO.update(fixedGrandfatheredPolicyViolation);
    PolicyViolation grandfatheredPolicyViolation1 = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation1,
        policy);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "scanId2");
    PolicyViolation grandfatheredPolicyViolation2 = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation2,
        policy);

    policyViolationGrandfatheringService.revokeGrandfathering(app1.getPublicId());

    assertThat(policyViolationDAO.getById(fixedGrandfatheredPolicyViolation.getId()).isGrandfathered(), is(true));
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation1.getId()).isGrandfathered(), is(false));
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation2.getId()).isGrandfathered(), is(true));
  }
}
