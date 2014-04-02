/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class PolicyViolationAdapterTest
{

  private PolicyViolationAdapter policyViolationAdapter;

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Before
  public void setup() {
    policyViolationAdapter = new PolicyViolationAdapter(new PolicyViolationDAO());
  }

  @Test
  public void testCreatePolicyViolationDTO() {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org.getId(), "build-policy");
    Application app = tempEntity.newApplication(org.getId());

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    PolicyViolation violation = tempEntity.newPolicyViolation(policyEvaluation.getId(), policy);

    PolicyViolationDTO dto = policyViolationAdapter.createPolicyViolationDTO(app, violation);

    assertNotNull(dto);
    assertPolicyViolationDTO(Lists.newArrayList(dto), violation, app, policy);
  }

  @Test
  public void testCreatePolicyViolationDTOsWithPolicyEvaluations() {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org.getId(), "build-policy");
    Application app = tempEntity.newApplication(org.getId());

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    PolicyViolation evaluation1violation1 = tempEntity.newPolicyViolation(policyEvaluation1.getId(), policy);
    PolicyViolation evaluation1violation2 = tempEntity.newPolicyViolation(policyEvaluation1.getId(), policy);

    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app.getId(), StageReleaseStageType.ID,
        "scan-id");
    PolicyViolation evaluation2violation1 = tempEntity.newPolicyViolation(policyEvaluation2.getId(), policy);
    PolicyViolation evaluation2violation2 = tempEntity.newPolicyViolation(policyEvaluation2.getId(), policy);

    List<PolicyViolationDTO> dtos = policyViolationAdapter.createPolicyViolationDTOs(app,
        Lists.newArrayList(policyEvaluation1, policyEvaluation2));

    assertNotNull(dtos);
    assertThat(dtos, hasSize(4));
    assertPolicyViolationDTO(dtos, evaluation1violation1, app, policy);
    assertPolicyViolationDTO(dtos, evaluation1violation2, app, policy);
    assertPolicyViolationDTO(dtos, evaluation2violation1, app, policy);
    assertPolicyViolationDTO(dtos, evaluation2violation2, app, policy);
  }

  @Test
  public void testCreatePolicyViolationDTOsWithPolicyEvaluation() {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org.getId(), "build-policy");
    Application app = tempEntity.newApplication(org.getId());

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    PolicyViolation evaluation1violation1 = tempEntity.newPolicyViolation(policyEvaluation1.getId(), policy);
    PolicyViolation evaluation1violation2 = tempEntity.newPolicyViolation(policyEvaluation1.getId(), policy);

    List<PolicyViolationDTO> dtos = policyViolationAdapter.createPolicyViolationDTOs(app, policyEvaluation1);

    assertNotNull(dtos);
    assertThat(dtos, hasSize(2));
    assertPolicyViolationDTO(dtos, evaluation1violation1, app, policy);
    assertPolicyViolationDTO(dtos, evaluation1violation2, app, policy);
  }

  @Test
  public void testCreatePolicyViolationDTOsWithDeletedPolicy() {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org.getId(), "build-policy");
    Application app = tempEntity.newApplication(org.getId());

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");
    tempEntity.newPolicyViolation(policyEvaluation1.getId(), policy);
    tempEntity.newPolicyViolation(policyEvaluation1.getId(), policy);

    PolicyDAO policyDAO = new PolicyDAO();
    policyDAO.delete(policy);

    List<PolicyViolationDTO> dtos = policyViolationAdapter.createPolicyViolationDTOs(app,
        Lists.newArrayList(policyEvaluation1));

    assertNotNull(dtos);
    assertThat(dtos, empty());
  }

  @Test
  public void testCreatePolicyViolationDTOsWithNullPolicyEvaluations() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    List<PolicyEvaluation> evaluations = null;
    List<PolicyViolationDTO> dtos = policyViolationAdapter.createPolicyViolationDTOs(app, evaluations);

    assertNotNull(dtos);
    assertThat(dtos, empty());
  }

  @Test
  public void testCreatePolicyViolationDTOsWithNullPolicyEvaluation() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    PolicyEvaluation evaluation = null;
    List<PolicyViolationDTO> dtos = policyViolationAdapter.createPolicyViolationDTOs(app, evaluation);

    assertNotNull(dtos);
    assertThat(dtos, empty());
  }

  @Test
  public void testCreatePolicyViolationDTOsWithNoPolicyViolations() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-id");

    List<PolicyViolationDTO> dtos = policyViolationAdapter.createPolicyViolationDTOs(app,
        Lists.newArrayList(policyEvaluation1));

    assertNotNull(dtos);
    assertThat(dtos, empty());
  }
}
