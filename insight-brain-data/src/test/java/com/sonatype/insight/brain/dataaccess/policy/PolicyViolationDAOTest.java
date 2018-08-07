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
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
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
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan-3");
    PolicyViolation openViolation3 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-4");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getByApplicationId(applicationId);

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation1.getId(), openViolation2.getId(), openViolation3.getId(),
            fixedViolation.getId(), waivedViolation.getId(), grandfatheredViolation.getId()));
  }

  @Test
  public void testGetUnfixedByApplicationIdAndStageId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

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

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId(), waivedViolation.getId(), grandfatheredViolation.getId()));
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
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

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

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation1.getId(), openViolation2.getId(), waivedViolation.getId(),
            grandfatheredViolation.getId()));
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
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setGrandfatherTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

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
  public void testGetUnfixedByApplicationIdsAndStageIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");

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

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId(), waivedViolation.getId(), grandfatheredViolation.getId()));
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

    assertThat(violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openViolation.getId()));
  }

  private String addViolation(PolicyViolationDAO dao,
                              String stageTypeId,
                              Date openTime,
                              Date grandfatherTime,
                              Date waiveTime,
                              Date fixTime)
  {
    Policy policy = tempEntity.newPolicy(applicationId, "name" + tempEntity.uuid());

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

    List<String> violationIds = dao.getActiveByApplicationIdsOpenedAfterDate(Arrays.asList(applicationId), cutoff)
        .stream().map(PolicyViolation::getId).collect(toList());

    assertThat(violationIds, containsInAnyOrder(expectedIds.stream().toArray()));
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

    List<String> violationIds = dao.getUnfixedByApplicationIdsOpenedAfterDate(Arrays.asList(applicationId), cutoff)
        .stream().map(PolicyViolation::getId).collect(toList());

    assertThat(violationIds, containsInAnyOrder(expectedIds.stream().toArray()));
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

    List<String> violationIds = dao.getActiveByApplicationIdsAndStageIdsOpenedAfterDate(Arrays.asList(applicationId),
        Arrays.asList(BuildStageType.ID), cutoff).stream().map(PolicyViolation::getId).collect(toList());

    assertThat(violationIds, containsInAnyOrder(expectedIds.stream().toArray()));
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

    List<String> violationIds = dao.getUnfixedByApplicationIdsAndStageIdsOpenedAfterDate(Arrays.asList(applicationId),
        Arrays.asList(BuildStageType.ID), cutoff).stream().map(PolicyViolation::getId).collect(toList());

    assertThat(violationIds, containsInAnyOrder(expectedIds.stream().toArray()));
  }

  @Test
  public void testGetActiveByApplicationIdAndStageIdsAndTimeRange() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(applicationId, "name");
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

    assertThat(violations.toString(), violations.stream().map(PolicyViolation::getId).collect(toSet()),
        containsInAnyOrder(openedBefore.getId(), openedBeforeWaivedDuring.getId(), openedBeforeFixedDuring.getId(),
            openedBeforeGrandfatheredDuring.getId(), openedDuring.getId(), openedDuringWaivedAfter.getId(),
            openedDuringFixedAfter.getId(), openedDuringGrandfatheredAfter.getId(), openedAndWaivedDuring.getId(),
            openedAndFixedDuring.getId(), openedAndGrandfatheredDuring.getId()));
  }

  @Test
  public void testGetUnfixedGrandfatheredByApplicationId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();

    Policy policy1 = tempEntity.newPolicy(applicationId, "policy-1");
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation unfixedGrandfatheredViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    unfixedGrandfatheredViolation1.setGrandfatherTime(policyEvaluation1.getTime());
    dao.update(unfixedGrandfatheredViolation1);
    PolicyViolation fixedGrandfatheredViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    fixedGrandfatheredViolation1.setFixTime(new Date());
    fixedGrandfatheredViolation1.setGrandfatherTime(new Date());
    dao.update(fixedGrandfatheredViolation1);

    String applicationId2 = tempEntity.newApplicationWithParent().getId();
    Policy policy2 = tempEntity.newPolicy(applicationId2, "policy-2");
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(applicationId2, BuildStageType.ID, "scan-2");
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
      assertThat(violations1, hasSize(1));
      assertThat(violations1.get(0).getId(), is(unfixedGrandfatheredViolation1.getId()));

      List<PolicyViolation> violations2 = dao.getUnfixedGrandfatheredByApplicationId(tx, applicationId2);
      assertThat(violations2, hasSize(1));
      assertThat(violations2.get(0).getId(), is(unfixedGrandfatheredViolation2.getId()));
    }
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
