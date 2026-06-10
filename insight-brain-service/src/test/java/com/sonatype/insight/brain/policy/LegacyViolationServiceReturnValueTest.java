/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LegacyViolationServiceReturnValueTest
    extends AbstractComponentTest
{
  @Inject
  private LegacyViolationService legacyViolationService;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  private Policy newPolicyAllowingLegacyViolations() {
    Policy policy = tempEntity.newPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);
    return policy;
  }

  @Test
  public void revoke_returnsCountOfClearedLegacyViolations() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policyForLegacyViolation = tempEntity.newPolicy();
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId1");
    tempEntity.newLegacyPolicyViolation(evaluation, policyForLegacyViolation);
    tempEntity.newLegacyPolicyViolation(evaluation, policyForLegacyViolation);

    int count = legacyViolationService.revokeLegacyViolationStatus(app.getPublicId());

    assertThat(count).isEqualTo(2);
  }

  @Test
  public void revoke_returnsZeroWhenNoLegacyViolations() {
    Application app = tempEntity.newApplicationWithParent();
    int count = legacyViolationService.revokeLegacyViolationStatus(app.getPublicId());
    assertThat(count).isEqualTo(0);
  }

  @Test
  public void grant_returnsCountOfNewlyMarkedViolations() {
    Application app = tempEntity.newApplicationWithParent();
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);

    Policy buildPolicy = newPolicyAllowingLegacyViolations();
    Policy proxyPolicy = newPolicyAllowingLegacyViolations();

    PolicyEvaluation buildEvaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId1");
    tempEntity.newPolicyViolation(buildEvaluation, buildPolicy);
    tempEntity.newPolicyViolation(buildEvaluation, buildPolicy);

    PolicyEvaluation proxyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_PROXY, "scanId2");
    tempEntity.newPolicyViolation(proxyEvaluation, proxyPolicy);

    int count = legacyViolationService.grantLegacyViolationStatus(app.getPublicId());

    assertThat(count).isEqualTo(2);
  }
}
