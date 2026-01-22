/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ActivePolicyViolationsWithActionFailServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ActivePolicyViolationsWithActionFailService activePolicyViolationsWithActionFailService;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Test
  public void testGetActiveViolationsWithActionFail_Success() {
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

    List<PolicyViolationWithoutConstraintFactsDTO> result =
            activePolicyViolationsWithActionFailService.getActiveViolationsWithActionFail(
                    application.getPublicId(), STAGE_ID);

    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
  }

  @Test
  public void testGetActiveViolationsWithActionFail_Fail() {
    final String STAGE_ID = BuildStageType.ID;

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      activePolicyViolationsWithActionFailService.getActiveViolationsWithActionFail(
              "non-existent-application-id", STAGE_ID);
    }).withMessage("Could not find an application with public ID non-existent-application-id.");
  }
}
