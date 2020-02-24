/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() throws Exception {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTestScanId");

    PolicyViolationDAO dao = new PolicyViolationDAO();

    // Create
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1",
        "Version1");
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, "constraint data", "filename");
    assertThat(policyViolation.getId()).isNull();
    dao.insert(policyViolation);
    assertThat(policyViolation.getId()).isNotNull();

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNotNull();
    assertPolicyViolation(policyEvaluation.getApplicationId(), policyEvaluation.getStageTypeId(), policy.getId(),
        policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, "filename",
        policyEvaluation.getTime(), null /* actionTypeId */, policyViolation);

    policyViolation.setActionTypeId(Action.ID_FAIL);
    dao.update(policyViolation);

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNotNull();
    assertPolicyViolation(policyEvaluation.getApplicationId(), policyEvaluation.getStageTypeId(), policy.getId(),
        policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, "filename",
        policyEvaluation.getTime(), Action.ID_FAIL, policyViolation);

    // Delete
    dao.delete(policyViolation);

    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNull();
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
    assertThat(actual.getApplicationId()).isEqualTo(applicationId);
    assertThat(actual.getStageTypeId()).isEqualTo(stageTypeId);
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getPolicyName()).isEqualTo(policyName);
    assertThat(actual.getThreatLevel()).isEqualTo(threatLevel);
    assertThat(actual.getThreatCategory()).isEqualTo(threatCategory);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getFilename()).isEqualTo(filename);
    assertThat(actual.getOpenTime()).isEqualTo(openTime);
    assertThat(actual.getActionTypeId()).isEqualTo(actionTypeId);
  }

  @Test
  public void testGetByApplicationId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

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
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-3");
    PolicyViolation openViolation3 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-4");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getByApplicationId(applicationId);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation1.getId(),
        openViolation2.getId(), openViolation3.getId(), fixedViolation.getId(), waivedViolation.getId(),
        grandfatheredViolation.getId());
  }

  @Test
  public void testGetUnfixedByApplicationIdAndStageId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIdAndStageId(applicationId, BuildStageType.ID);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId(),
        waivedViolation.getId(), grandfatheredViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdAndStageId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageId(applicationId, BuildStageType.ID);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdAndStageIdAndHash() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    tempEntity.newPolicyViolation(policyEvaluation, policy, null, "other-hash", "reason");

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdAndHash(applicationId, BuildStageType.ID,
        openViolation.getHash());

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  @Test
  public void testGetUnfixedByApplicationIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIds(Arrays.asList(applicationId));

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation1.getId(),
        openViolation2.getId(), waivedViolation.getId(), grandfatheredViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIds(Arrays.asList(applicationId));

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation1.getId(),
        openViolation2.getId());
  }

  @Test
  public void testGetUnfixedByApplicationIdsAndStageIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);
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

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId(),
        waivedViolation.getId(), grandfatheredViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, tempEntity.newWaiver(policy.getId(), applicationId));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsAndStageIds(Arrays.asList(applicationId),
        Arrays.asList(BuildStageType.ID));

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  private String addViolation(PolicyViolationDAO dao,
                              String stageTypeId,
                              Date openTime,
                              Date grandfatherTime,
                              Date waiveTime,
                              Date fixTime)
  {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, stageTypeId,
        "scan-" + tempEntity.uuid(), openTime);
    PolicyViolation violation;
    if (waiveTime != null) {
      violation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
          tempEntity.newWaiver(policy.getId(), applicationId));
    }
    else {
      violation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    }
    violation.setGrandfatherTime(grandfatherTime);
    violation.setWaiveTime(waiveTime);
    violation.setFixTime(fixTime);
    dao.update(violation);

    return violation.getId();
  }

  @Test
  public void testGetActiveByApplicationIdsOpenedAfterDate() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Instant reference = Instant.now();
    Date openBefore = new Date(reference.minus(Duration.ofMinutes(1)).toEpochMilli());
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date grandfatherTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date waiveTime = new Date(reference.plus(Duration.ofMinutes(3)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());
    Date notGrandfathered = null;
    Date notWaived = null;
    Date notFixed = null;

    List<String> expectedIds = new ArrayList<>();

    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));

    addViolation(dao, BuildStageType.ID, cutoff, grandfatherTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, grandfatherTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openBefore, notGrandfathered, notWaived, notFixed);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsOpenedAfterDate(Arrays.asList(applicationId),
        cutoff);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void getUnfixedByApplicationIdsOpenedAfterDate() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Instant reference = Instant.now();
    Date openBefore = new Date(reference.minus(Duration.ofMinutes(1)).toEpochMilli());
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date grandfatherTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date waiveTime = new Date(reference.plus(Duration.ofMinutes(3)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());
    Date notGrandfathered = null;
    Date notWaived = null;
    Date notFixed = null;

    List<String> expectedIds = new ArrayList<>();

    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, grandfatherTime, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, waiveTime, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, cutoff, grandfatherTime, notWaived, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, cutoff, notGrandfathered, waiveTime, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, openAfter, notGrandfathered, waiveTime, notFixed));

    addViolation(dao, BuildStageType.ID, cutoff, grandfatherTime, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, grandfatherTime, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openBefore, notGrandfathered, notWaived, notFixed);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIdsOpenedAfterDate(Arrays.asList(applicationId),
        cutoff);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIdsOpenedAfterDate() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Instant reference = Instant.now();
    Date openBefore = new Date(reference.minus(Duration.ofMinutes(1)).toEpochMilli());
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date grandfatherTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date waiveTime = new Date(reference.plus(Duration.ofMinutes(3)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());
    Date notGrandfathered = null;
    Date notWaived = null;
    Date notFixed = null;

    List<String> expectedIds = new ArrayList<>();

    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));

    addViolation(dao, BuildStageType.ID, cutoff, grandfatherTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, grandfatherTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openBefore, notGrandfathered, notWaived, notFixed);
    addViolation(dao, ReleaseStageType.ID, openAfter, notGrandfathered, notWaived, notFixed);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsAndStageIdsOpenedAfterDate(
        Arrays.asList(applicationId), Arrays.asList(BuildStageType.ID), cutoff);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void getUnfixedByApplicationIdsAndStageIdsOpenedAfterDate() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Instant reference = Instant.now();
    Date openBefore = new Date(reference.minus(Duration.ofMinutes(1)).toEpochMilli());
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date grandfatherTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date waiveTime = new Date(reference.plus(Duration.ofMinutes(3)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());
    Date notGrandfathered = null;
    Date notWaived = null;
    Date notFixed = null;

    List<String> expectedIds = new ArrayList<>();

    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, grandfatherTime, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, waiveTime, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, grandfatherTime, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, notFixed));

    addViolation(dao, BuildStageType.ID, openBefore, notGrandfathered, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, fixTime);
    addViolation(dao, ReleaseStageType.ID, openAfter, notGrandfathered, notWaived, notFixed);
    addViolation(dao, ReleaseStageType.ID, openAfter, notGrandfathered, waiveTime, notFixed);
    addViolation(dao, ReleaseStageType.ID, openAfter, grandfatherTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, grandfatherTime, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, grandfatherTime, notWaived, fixTime);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIdsAndStageIdsOpenedAfterDate(
        Arrays.asList(applicationId), Arrays.asList(BuildStageType.ID), cutoff);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void testGetActiveByApplicationIdAndStageIdsAndTimeRange() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver waiver = tempEntity.newWaiver(policy.getId(), applicationId);
    Date to = new Date(System.currentTimeMillis() - 10 * 1000);
    Date from = new Date(to.getTime() - 60 * 1000);

    Date before = new Date(from.getTime() - 1000);
    Date during1 = new Date(from.getTime() + 1000);
    Date during2 = new Date(from.getTime() + 2000);

    PolicyEvaluation policyEvalBeforeDateRange = tempEntity
        .newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-0", before);
    // waived before time range
    tempEntity.newWaivedPolicyViolation(policyEvalBeforeDateRange, policy, waiver);
    // fixed before time range
    PolicyViolation fixedBefore = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    fixedBefore.setFixTime(before);
    dao.update(fixedBefore);
    // grandfathered before time range
    PolicyViolation grandfatheredBefore = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    grandfatheredBefore.setFixTime(before);
    dao.update(grandfatheredBefore);

    // opened before time range and still unresolved
    PolicyViolation openedBefore = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    // opened before time range and waived during time range
    PolicyViolation openedBeforeWaivedDuring = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    openedBeforeWaivedDuring.setWaiveTime(during1);
    dao.update(openedBeforeWaivedDuring);
    // opened before time range and fixed during time range
    PolicyViolation openedBeforeFixedDuring = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    openedBeforeFixedDuring.setFixTime(during1);
    dao.update(openedBeforeFixedDuring);
    // opened before time range and grandfathered during time range
    PolicyViolation openedBeforeGrandfatheredDuring = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    openedBeforeGrandfatheredDuring.setGrandfatherTime(during1);
    dao.update(openedBeforeGrandfatheredDuring);

    PolicyEvaluation policyEvalOnStartDateRange = tempEntity
        .newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1", from);
    // opened during time range and still unresolved
    PolicyViolation openedDuring = tempEntity.newPolicyViolation(policyEvalOnStartDateRange, policy);
    // opened during time range but immediately waived
    tempEntity.newWaivedPolicyViolation(policyEvalOnStartDateRange, policy, waiver);
    // opened during time range and waived after time range
    PolicyViolation openedDuringWaivedAfter = tempEntity.newPolicyViolation(policyEvalOnStartDateRange, policy);
    openedDuringWaivedAfter.setWaiveTime(to);
    dao.update(openedDuringWaivedAfter);
    // opened during time range and fixed after time range
    PolicyViolation openedDuringFixedAfter = tempEntity.newPolicyViolation(policyEvalOnStartDateRange, policy);
    openedDuringFixedAfter.setFixTime(to);
    dao.update(openedDuringFixedAfter);
    // opened during time range and grandfathered after time range
    PolicyViolation openedDuringGrandfatheredAfter = tempEntity.newPolicyViolation(policyEvalOnStartDateRange, policy);
    openedDuringGrandfatheredAfter.setGrandfatherTime(to);
    dao.update(openedDuringGrandfatheredAfter);

    PolicyEvaluation policyEvalInDateRange = tempEntity
        .newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-2", during1);
    // opened and waived during time range
    PolicyViolation openedAndWaivedDuring = tempEntity.newPolicyViolation(policyEvalInDateRange, policy);
    openedAndWaivedDuring.setWaiveTime(during2);
    dao.update(openedAndWaivedDuring);
    // opened and fixed during time range
    PolicyViolation openedAndFixedDuring = tempEntity.newPolicyViolation(policyEvalInDateRange, policy);
    openedAndFixedDuring.setFixTime(during2);
    dao.update(openedAndFixedDuring);
    // opened and grandfathered during time range
    PolicyViolation openedAndGrandfatheredDuring = tempEntity.newPolicyViolation(policyEvalInDateRange, policy);
    openedAndGrandfatheredDuring.setGrandfatherTime(during2);
    dao.update(openedAndGrandfatheredDuring);

    PolicyEvaluation policyEvalOnEndDateRange = tempEntity
        .newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-3", to);
    // opened after time range
    tempEntity.newPolicyViolation(policyEvalOnEndDateRange, policy);

    PolicyEvaluation policyEvalOnStartDateRangeOtherStage = tempEntity
        .newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-os", from);
    // matching app and time range but wrong stage
    tempEntity.newPolicyViolation(policyEvalOnStartDateRangeOtherStage, policy);

    PolicyEvaluation policyEvalOnStartDateRangeOtherApp = tempEntity
        .newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID, "scan-oa", from);
    // matching stage and time range but wrong app
    tempEntity.newPolicyViolation(policyEvalOnStartDateRangeOtherApp, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdsAndTimeRange(applicationId,
        Arrays.asList(BuildStageType.ID), from, to);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openedBefore.getId(),
        openedBeforeWaivedDuring.getId(), openedBeforeFixedDuring.getId(), openedBeforeGrandfatheredDuring.getId(),
        openedDuring.getId(), openedDuringWaivedAfter.getId(), openedDuringFixedAfter.getId(),
        openedDuringGrandfatheredAfter.getId(), openedAndWaivedDuring.getId(), openedAndFixedDuring.getId(),
        openedAndGrandfatheredDuring.getId());
  }

  @Test
  public void testGetUnfixedGrandfatheredByApplicationId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();

    Policy policy1 = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation unfixedGrandfatheredViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    unfixedGrandfatheredViolation1.setGrandfatherTime(policyEvaluation1.getTime());
    dao.update(unfixedGrandfatheredViolation1);
    PolicyViolation fixedGrandfatheredViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    fixedGrandfatheredViolation1.setFixTime(new Date());
    fixedGrandfatheredViolation1.setGrandfatherTime(new Date());
    dao.update(fixedGrandfatheredViolation1);

    Application application2 = tempEntity.newApplicationWithParent();
    Policy policy2 = tempEntity.newPolicy(application2);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    PolicyViolation unfixedGrandfatheredViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    unfixedGrandfatheredViolation2.setGrandfatherTime(new Date());
    dao.update(unfixedGrandfatheredViolation2);
    PolicyViolation fixedGrandfatheredViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    fixedGrandfatheredViolation2.setFixTime(new Date());
    fixedGrandfatheredViolation2.setGrandfatherTime(new Date());
    dao.update(fixedGrandfatheredViolation2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyViolation> violations1 = dao.getUnfixedGrandfatheredByApplicationId(tx, applicationId);
      assertThat(violations1).extracting(PolicyViolation::getId)
          .containsExactly(unfixedGrandfatheredViolation1.getId());

      List<PolicyViolation> violations2 = dao.getUnfixedGrandfatheredByApplicationId(tx, application2.getId());
      assertThat(violations2).extracting(PolicyViolation::getId)
          .containsExactly(unfixedGrandfatheredViolation2.getId());
    }
  }

  @Test
  public void testGetUnfixedByApplicationId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();

    Policy policy1 = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation unfixedViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation fixedViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    fixedViolation1.setFixTime(new Date());
    dao.update(fixedViolation1);

    Application application2 = tempEntity.newApplicationWithParent();
    Policy policy2 = tempEntity.newPolicy(application2);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan-2");
    PolicyViolation unfixedViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    PolicyViolation fixedViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    fixedViolation2.setFixTime(new Date());
    dao.update(fixedViolation2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyViolation> violations1 = dao.getUnfixedByApplicationId(tx, applicationId);
      assertThat(violations1).extracting(PolicyViolation::getId).containsExactly(unfixedViolation1.getId());

      List<PolicyViolation> violations2 = dao.getUnfixedByApplicationId(tx, application2.getId());
      assertThat(violations2).extracting(PolicyViolation::getId).containsExactly(unfixedViolation2.getId());
    }
  }

  @Test
  public void testReplacePolicyId() {
    Policy fromPolicy = tempEntity.newPolicy(application);
    Policy toPolicy = tempEntity.newPolicy(application);
    Policy otherPolicy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scanId");
    PolicyViolation fromPolicyViolation = tempEntity.newPolicyViolation(evaluation, fromPolicy);
    PolicyViolation toPolicyViolation = tempEntity.newPolicyViolation(evaluation, toPolicy);
    PolicyViolation otherPolicyViolation = tempEntity.newPolicyViolation(evaluation, otherPolicy);

    PolicyViolationDAO dao = new PolicyViolationDAO();
    dao.replacePolicyId(fromPolicy.getId(), toPolicy.getId());

    fromPolicyViolation = dao.getById(fromPolicyViolation.getId());
    assertThat(fromPolicyViolation.getPolicyId()).isEqualTo(toPolicy.getId());
    toPolicyViolation = dao.getById(toPolicyViolation.getId());
    assertThat(toPolicyViolation.getPolicyId()).isEqualTo(toPolicy.getId());
    otherPolicyViolation = dao.getById(otherPolicyViolation.getId());
    assertThat(otherPolicyViolation.getPolicyId()).isEqualTo(otherPolicy.getId());
  }

  @Test
  public void testReplacePolicyIdForApplication() {
    Policy fromPolicy = tempEntity.newPolicy(organization);
    Policy toPolicy = tempEntity.newPolicy(application);
    Policy otherPolicy = tempEntity.newPolicy(application);
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
    assertThat(fromPolicyViolation.getPolicyId()).isEqualTo(toPolicy.getId());
    otherPolicyViolation = dao.getById(otherPolicyViolation.getId());
    assertThat(otherPolicyViolation.getPolicyId()).isEqualTo(otherPolicy.getId());
    otherAppPolicyViolation = dao.getById(otherAppPolicyViolation.getId());
    assertThat(otherAppPolicyViolation.getPolicyId()).isEqualTo(fromPolicy.getId());
  }

  @Test
  public void testDeleteFixedByApplicationIdAndDate() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation evaluation0 = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(),
        BuildStageType.ID, "scan-1", new Date(System.currentTimeMillis() - 900));
    PolicyViolation violation0 = tempEntity.newPolicyViolation(evaluation0, policy);
    violation0.setFixTime(evaluation0.getTime());
    dao.update(violation0);
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1",
        new Date(System.currentTimeMillis() - 900));
    for (int i = 0; i < PolicyViolationDAO.DELETE_BATCH_SIZE + 2; i++) {
      tempEntity.newPolicyViolation(evaluation1, policy);
    }
    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-2",
        new Date(System.currentTimeMillis() - 500));
    for (PolicyViolation violation : dao.getByApplicationId(applicationId)) {
      violation.setFixTime(evaluation2.getTime());
      dao.update(violation);
    }
    PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation2, policy);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(evaluation2, policy);
    PolicyEvaluation evaluation3 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-3");
    violation2.setFixTime(evaluation3.getTime());
    dao.update(violation2);

    int deletedRows = dao.deleteFixedByApplicationIdAndDate(applicationId, evaluation3.getTime());

    assertThat(deletedRows).isEqualTo(PolicyViolationDAO.DELETE_BATCH_SIZE + 2);
    assertThat(dao.getByApplicationId(applicationId))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactlyInAnyOrder(violation1, violation2);
    assertThat(dao.getById(violation0.getId())).isNotNull();
  }

  @Test
  public void testGetCount() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    assertThat(dao.getCount()).isEqualTo(2);
  }
}
