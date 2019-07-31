/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiPolicyWaiverServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiPolicyWaiverService apiPolicyWaiverService;

  private Policy policy;

  private PolicyViolation policyViolation;

  private Application app;

  private Organization org;

  @Before
  public void setUpPolicyViolation() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy(org.getId());

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
  }

  @Test
  public void testAddPolicyWaiver_Application() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
    assertPolicyWaiver(app.getId(), "waiver comment");
  }

  @Test
  public void testAddPolicyWaiver_Organization() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
    assertPolicyWaiver(org.getId(), "waiver comment");
  }

  @Test
  public void testAddPolicyWaiver_AcceptsNoComment() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, null);
    assertPolicyWaiver(app.getId(), null);
  }

  @Test
  public void testAddPolicyWaiver_InvalidPolicyViolationId() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiver("invalid-policyViolationId", OwnerType.APPLICATION, "waiver comment")
    ).isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find policy violation with ID invalid-policyViolationId.");
  }

  @Test
  public void testAddPolicyWaiver_InvalidOwnerType() {
    assertThatThrownBy(() ->
        apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.REPOSITORY, "waiver comment")
    ).isInstanceOf(IllegalStateException.class)
        .hasMessage("Unknown owner type: repository");
  }

  private void assertPolicyWaiver(String ownerId, String comment) {
    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getByOwnerId(ownerId);
    assertThat(policyWaivers).hasSize(1);
    PolicyWaiver policyWaiver = policyWaivers.get(0);
    assertThat(policyWaiver).isNotNull();
    assertThat(policyWaiver.getId()).isNotNull();
    assertThat(policyWaiver.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiver.getHash()).isEqualTo(policyViolation.getHash());
    assertThat(policyWaiver.getComment()).isEqualTo(comment);
    assertThat(policyWaiver.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiver.getCreateTime()).isNotNull();
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(policyViolation.getConstraintFactsJson());
  }
}
