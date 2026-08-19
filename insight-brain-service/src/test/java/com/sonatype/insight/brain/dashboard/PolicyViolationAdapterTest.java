/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationAdapterTest
    extends AbstractDataTest
{
  private PolicyDAO policyDAO;

  @BeforeEach
  public void setUp() {
    policyDAO = daoFactory.createPolicyDAO();
  }

  @Test
  public void testCreatePolicyViolationDTO() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    PolicyViolation violation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    PolicyViolationDTO dto = PolicyViolationAdapter.createPolicyViolationDTO(app, policyEvaluation, violation);

    assertThat(dto).isNotNull();
    assertPolicyViolationDTO(Collections.singletonList(dto), violation, app, policyEvaluation, policy);
  }

  @Test
  public void testCreatePolicyViolationDTO_WithDeletedPolicy() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyDAO.delete(policy);

    PolicyViolationDTO dto = PolicyViolationAdapter.createPolicyViolationDTO(app, policyEvaluation, policyViolation);

    assertThat(dto).isNotNull();
    assertPolicyViolationDTO(Collections.singletonList(dto), policyViolation, app, policyEvaluation, policy);
  }
}
