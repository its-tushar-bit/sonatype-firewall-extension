/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() throws Exception {
    Policy policy = tempEntity.newPolicy(applicationId, "testCascadeDeleteToPolicyViolations");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTestScanId");

    PolicyViolationDAO dao = new PolicyViolationDAO();

    // Create
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1",
        "Version1");
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, "constraint data", "filename");
    assertThat(policyViolation.getId(), is(nullValue()));
    dao.insert(policyViolation);
    assertThat(policyViolation.getId(), is(notNullValue()));

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation, is(notNullValue()));
    assertPolicyViolation(policyEvaluation.getApplicationId(), policyEvaluation.getStageTypeId(), policy.getId(),
        policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, "filename",
        policyEvaluation.getTime(), null /* actionTypeId */, policyViolation);

    policyViolation.setActionTypeId(Action.ID_FAIL);
    dao.update(policyViolation);

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation, is(notNullValue()));
    assertPolicyViolation(policyEvaluation.getApplicationId(), policyEvaluation.getStageTypeId(), policy.getId(),
        policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, "filename",
        policyEvaluation.getTime(), Action.ID_FAIL, policyViolation);

    // Delete
    dao.delete(policyViolation);

    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation, is(nullValue()));
  }

  private void assertPolicyViolation(String applicationId,
                                     String stageTypeId,
                                     String policyId,
                                     String policyName,
                                     int threatLevel,
                                     PolicyThreatCategory threatCategory,
                                     String hash,
                                     ComponentIdentifier componentIdentifier,
                                     String filename,
                                     Date openTime,
                                     String actionTypeId,
                                     PolicyViolation actual)
  {
    assertThat(actual.getApplicationId(), is(applicationId));
    assertThat(actual.getStageTypeId(), is(stageTypeId));
    assertThat(actual.getPolicyId(), is(policyId));
    assertThat(actual.getPolicyName(), is(policyName));
    assertThat(actual.getThreatLevel(), is(threatLevel));
    assertThat(actual.getThreatCategory(), is(threatCategory));
    assertThat(actual.getHash(), is(hash));
    assertThat(actual.getComponentIdentifier(), is(componentIdentifier));
    assertThat(actual.getFilename(), is(filename));
    assertThat(actual.getOpenTime(), is(openTime));
    assertThat(actual.getActionTypeId(), is(actionTypeId));
  }

  @Test
  public void testGetByApplicationId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1",
        new Date(System.currentTimeMillis() - 2000));
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-2");
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), applicationId));

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-3");
    PolicyViolation openViolation3 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-4");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getByApplicationId(applicationId);

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation1.getId(), openViolation2.getId(), openViolation3.getId(),
            fixedViolation.getId(), waivedViolation.getId()));
  }

  @Test
  public void testGetUnfixedByApplicationIdAndStageId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIdAndStageId(applicationId, BuildStageType.ID);

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId(), waivedViolation.getId()));
  }

  @Test
  public void testGetActiveByApplicationIdAndStageId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageId(applicationId, BuildStageType.ID);

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId()));
  }

  @Test
  public void testGetActiveByApplicationIdAndStageIdAndHash() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    tempEntity.newPolicyViolation(policyEvaluation, policy, null, "other-hash", "reason");

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdAndHash(applicationId, BuildStageType.ID,
        openViolation.getHash());

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId()));
  }

  @Test
  public void testGetUnfixedByApplicationIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIds(Arrays.asList(applicationId));

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation1.getId(), openViolation2.getId(), waivedViolation.getId()));
  }

  @Test
  public void testGetUnfixedByApplicationIds_InOperatorOptimizationForH2() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(),
        ReleaseStageType.ID, "scan-2");
    PolicyViolation otherAppViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    Collection<String> applicationIds = new ArrayList<>();
    applicationIds.add(otherAppViolation.getApplicationId());
    while (applicationIds.size() < PolicyViolationDAO.IN_OPERATOR_THRESHOLD) {
      applicationIds.add(tempEntity.uuid());
    }
    applicationIds.add(applicationId);
    List<PolicyViolation> violations = dao.getUnfixedByApplicationIds(applicationIds);

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId(), waivedViolation.getId(), otherAppViolation.getId()));
  }

  @Test
  public void testGetActiveByApplicationIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIds(Arrays.asList(applicationId));

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation1.getId(), openViolation2.getId()));
  }

  @Test
  public void testGetActiveByApplicationIds_InOperatorOptimizationForH2() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(),
        ReleaseStageType.ID, "scan-2");
    PolicyViolation otherAppViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    Collection<String> applicationIds = new ArrayList<>();
    applicationIds.add(otherAppViolation.getApplicationId());
    while (applicationIds.size() < PolicyViolationDAO.IN_OPERATOR_THRESHOLD) {
      applicationIds.add(tempEntity.uuid());
    }
    applicationIds.add(applicationId);
    List<PolicyViolation> violations = dao.getActiveByApplicationIds(applicationIds);

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId(), otherAppViolation.getId()));
  }

  @Test
  public void testGetUnfixedByApplicationIdsAndStageIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIdsAndStageIds(Arrays.asList(applicationId),
        Arrays.asList(BuildStageType.ID));

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId(), waivedViolation.getId()));
  }

  @Test
  public void testGetUnfixedByApplicationIdsAndStageIds_InOperatorOptimizationForH2() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    PolicyViolation otherAppViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    Collection<String> applicationIds = new ArrayList<>();
    applicationIds.add(otherAppViolation.getApplicationId());
    while (applicationIds.size() < PolicyViolationDAO.IN_OPERATOR_THRESHOLD) {
      applicationIds.add(tempEntity.uuid());
    }
    applicationIds.add(applicationId);
    List<PolicyViolation> violations = dao.getUnfixedByApplicationIdsAndStageIds(applicationIds,
        Arrays.asList(BuildStageType.ID));

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId(), waivedViolation.getId(), otherAppViolation.getId()));
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsAndStageIds(Arrays.asList(applicationId),
        Arrays.asList(BuildStageType.ID));

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId()));
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIds_InOperatorOptimizationForH2() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    PolicyViolation otherAppViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    Collection<String> applicationIds = new ArrayList<>();
    applicationIds.add(otherAppViolation.getApplicationId());
    while (applicationIds.size() < PolicyViolationDAO.IN_OPERATOR_THRESHOLD) {
      applicationIds.add(tempEntity.uuid());
    }
    applicationIds.add(applicationId);
    List<PolicyViolation> violations = dao.getActiveByApplicationIdsAndStageIds(applicationIds,
        Arrays.asList(BuildStageType.ID));

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId(), otherAppViolation.getId()));
  }

  @Test
  public void testReplacePolicyId() {
    Policy fromPolicy = tempEntity.newPolicy(applicationId, "From Policy");
    Policy toPolicy = tempEntity.newPolicy(applicationId, "To Policy");
    Policy otherPolicy = tempEntity.newPolicy(applicationId, "Other Policy");
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scanId");
    PolicyViolation fromPolicyViolation = tempEntity.newPolicyViolation(evaluation, fromPolicy);
    PolicyViolation toPolicyViolation = tempEntity.newPolicyViolation(evaluation, toPolicy);
    PolicyViolation otherPolicyViolation = tempEntity.newPolicyViolation(evaluation, otherPolicy);

    PolicyViolationDAO dao = new PolicyViolationDAO();
    dao.replacePolicyId(fromPolicy.getId(), toPolicy.getId());

    fromPolicyViolation = dao.getById(fromPolicyViolation.getId());
    assertThat(fromPolicyViolation.getPolicyId(), is(toPolicy.getId()));
    toPolicyViolation = dao.getById(toPolicyViolation.getId());
    assertThat(toPolicyViolation.getPolicyId(), is(toPolicy.getId()));
    otherPolicyViolation = dao.getById(otherPolicyViolation.getId());
    assertThat(otherPolicyViolation.getPolicyId(), is(otherPolicy.getId()));
  }

  @Test
  public void testReplacePolicyIdForApplication() {
    Policy fromPolicy = tempEntity.newPolicy(organization.getId(), "From Policy");
    Policy toPolicy = tempEntity.newPolicy(applicationId, "To Policy");
    Policy otherPolicy = tempEntity.newPolicy(applicationId, "Other Policy");
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scanId");
    PolicyViolation fromPolicyViolation = tempEntity.newPolicyViolation(evaluation, fromPolicy);
    PolicyViolation otherPolicyViolation = tempEntity.newPolicyViolation(evaluation, otherPolicy);
    PolicyEvaluation otherAppEvaluation = tempEntity.newPolicyEvaluation(tempEntity
        .newApplication(organization.getId()).getId(), BuildStageType.ID, "scanId");
    PolicyViolation otherAppPolicyViolation = tempEntity.newPolicyViolation(otherAppEvaluation, fromPolicy);

    PolicyViolationDAO dao = new PolicyViolationDAO();
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.replacePolicyId(tx, applicationId, fromPolicy.getId(), toPolicy.getId());
      tx.commit();
    }

    fromPolicyViolation = dao.getById(fromPolicyViolation.getId());
    assertThat(fromPolicyViolation.getPolicyId(), is(toPolicy.getId()));
    otherPolicyViolation = dao.getById(otherPolicyViolation.getId());
    assertThat(otherPolicyViolation.getPolicyId(), is(otherPolicy.getId()));
    otherAppPolicyViolation = dao.getById(otherAppPolicyViolation.getId());
    assertThat(otherAppPolicyViolation.getPolicyId(), is(fromPolicy.getId()));
  }
}
