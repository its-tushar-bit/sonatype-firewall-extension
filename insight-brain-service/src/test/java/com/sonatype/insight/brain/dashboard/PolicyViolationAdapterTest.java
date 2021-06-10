/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationAdapterTest
{
  private PolicyViolationAdapter policyViolationAdapter;

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Before
  public void setup() {
    policyViolationAdapter = new PolicyViolationAdapter();
  }

  @Test
  public void testCreatePolicyViolationDTO() {
    Application app = tempEntity.newApplicationWithParent("test-application");
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    PolicyViolation violation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    PolicyViolationDTO dto = policyViolationAdapter.createPolicyViolationDTO(app, policyEvaluation, violation);

    assertThat(dto).isNotNull();
    assertPolicyViolationDTO(Collections.singletonList(dto), violation, app, policyEvaluation, policy);
  }

  @Test
  public void testCreatePolicyViolationDTO_WithDeletedPolicy() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    new PolicyDAO().delete(policy);

    PolicyViolationDTO dto = policyViolationAdapter.createPolicyViolationDTO(app, policyEvaluation, policyViolation);

    assertThat(dto).isNotNull();
    assertPolicyViolationDTO(Collections.singletonList(dto), policyViolation, app, policyEvaluation, policy);
  }
}
