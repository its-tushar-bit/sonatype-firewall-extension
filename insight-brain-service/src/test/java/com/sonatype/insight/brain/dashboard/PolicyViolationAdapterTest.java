/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

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
    Policy policy = tempEntity.newPolicy(app.getId(), "build-policy");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    PolicyViolation violation = tempEntity.newPolicyViolation(policyEvaluation.getId(), policy);

    PolicyViolationDTO dto = policyViolationAdapter.createPolicyViolationDTO(app, violation);

    assertNotNull(dto);
    assertPolicyViolationDTO(Lists.newArrayList(dto), violation, app, policy);
  }

  @Test
  public void testCreatePolicyViolationDTOsWithPolicyViolations() {
    Application app = tempEntity.newApplicationWithParent("test-application");
    Policy policy = tempEntity.newPolicy(app.getId(), "build-policy");

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    PolicyViolation evaluation1violation1 = tempEntity.newPolicyViolation(policyEvaluation1.getId(), policy);
    PolicyViolation evaluation1violation2 = tempEntity.newPolicyViolation(policyEvaluation1.getId(), policy);

    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app.getId(), StageReleaseStageType.ID,
        "scan-id");
    PolicyViolation evaluation2violation1 = tempEntity.newPolicyViolation(policyEvaluation2.getId(), policy);
    PolicyViolation evaluation2violation2 = tempEntity.newPolicyViolation(policyEvaluation2.getId(), policy);

    List<PolicyViolation> violations = Lists.newArrayList();
    PolicyViolationDAO violationDAO = new PolicyViolationDAO();
    violations.addAll(violationDAO.getByEvaluationId(policyEvaluation1.getId()));
    violations.addAll(violationDAO.getByEvaluationId(policyEvaluation2.getId()));

    List<PolicyViolationDTO> dtos = policyViolationAdapter.createPolicyViolationDTOs(app, violations);

    assertNotNull(dtos);
    assertThat(dtos, hasSize(4));
    assertPolicyViolationDTO(dtos, evaluation1violation1, app, policy);
    assertPolicyViolationDTO(dtos, evaluation1violation2, app, policy);
    assertPolicyViolationDTO(dtos, evaluation2violation1, app, policy);
    assertPolicyViolationDTO(dtos, evaluation2violation2, app, policy);
  }

  @Test
  public void testCreatePolicyViolationDTOsWithDeletedPolicy() {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org.getId(), "build-policy");
    Application app = tempEntity.newApplication(org.getId());

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1.getId(), policy);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1.getId(), policy);

    PolicyDAO policyDAO = new PolicyDAO();
    policyDAO.delete(policy);

    List<PolicyViolationDTO> dtos = policyViolationAdapter.createPolicyViolationDTOs(app,
        new PolicyViolationDAO().getByEvaluationId(policyEvaluation1.getId()));

    assertNotNull(dtos);
    assertThat(dtos, hasSize(2));
    assertThat(policyViolation1.getPolicyName(), is(policy.getName()));
    assertThat(policyViolation1.getPolicyId(), is(policy.getId()));

    assertThat(policyViolation2.getPolicyName(), is(policy.getName()));
    assertThat(policyViolation2.getPolicyId(), is(policy.getId()));
  }

  @Test
  public void testCreatePolicyViolationDTOsWithNullPolicyViolations() {
    Application app = tempEntity.newApplicationWithParent("test-application");

    List<PolicyViolationDTO> dtos = policyViolationAdapter.createPolicyViolationDTOs(app, null);

    assertNotNull(dtos);
    assertThat(dtos, empty());
  }

  @Test
  public void testCreatePolicyViolationDTOsWithNoPolicyViolations() {
    Application app = tempEntity.newApplicationWithParent("test-application");

    List<PolicyViolation> emptyList = Collections.emptyList();
    List<PolicyViolationDTO> dtos = policyViolationAdapter.createPolicyViolationDTOs(app, emptyList);

    assertNotNull(dtos);
    assertThat(dtos, empty());
  }
}
