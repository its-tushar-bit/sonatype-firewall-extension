/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Test;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID,
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

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1",
        new Date(System.currentTimeMillis() - 2000));
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-2");
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-3");
    PolicyViolation openViolation3 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-4");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getByApplicationId(application.getId());

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation1.getId(),
        openViolation2.getId(), openViolation3.getId(), fixedViolation.getId(), waivedViolation.getId(),
        grandfatheredViolation.getId());
  }

  @Test
  public void testGetByApplicationIdAndPolicyAndHash() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1",
        new Date(System.currentTimeMillis() - 2000));
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID,
        "scan-2", new Date(System.currentTimeMillis() - 1000));

    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-3");
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-4");
    PolicyViolation openViolation3 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-4");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    tempEntity.newPolicyViolation(policyEvaluation2, tempEntity.newPolicy(application));
    tempEntity.newPolicyViolation(policyEvaluation2, tempEntity.newPolicy(application));

    List<PolicyViolation> violations = dao.getByApplicationIdAndPolicyIdAndHash(application.getId(),
        policy.getId(), openViolation1.getHash());

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation1.getId(),
        openViolation2.getId(), openViolation3.getId(), fixedViolation.getId(), waivedViolation.getId(),
        grandfatheredViolation.getId());
  }

  @Test
  public void testGetByApplicationIdAndPolicyAndHash_NullHash() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis() - 2000));
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID,
        "scan-2", new Date(System.currentTimeMillis() - 1000));

    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy, null, null);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, null, null);
    // Another violation with different hash, for same policy and evaluation
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-3");
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy, null, null);

    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, null,
        null, tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, null, null);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-4");
    PolicyViolation openViolation3 = tempEntity.newPolicyViolation(policyEvaluation, policy, null, null);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-4");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    tempEntity.newPolicyViolation(policyEvaluation2, tempEntity.newPolicy(application));
    tempEntity.newPolicyViolation(policyEvaluation2, tempEntity.newPolicy(application));

    List<PolicyViolation> violations = dao.getByApplicationIdAndPolicyIdAndHash(application.getId(),
        policy.getId(), null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation1.getId(),
        openViolation2.getId(), openViolation3.getId(), fixedViolation.getId(), waivedViolation.getId(),
        grandfatheredViolation.getId());
  }

  @Test
  public void testGetUnfixedByApplicationIdAndStageId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIdAndStageId(application.getId(), BuildStageType.ID);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId(),
        waivedViolation.getId(), grandfatheredViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdAndStageId() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageId(application.getId(), BuildStageType.ID);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdAndStageIdAndHash() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    tempEntity.newPolicyViolation(policyEvaluation, policy, null, "other-hash", "reason");

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations =
        dao.getActiveByApplicationIdAndStageIdAndHash(application.getId(), BuildStageType.ID,
        openViolation.getHash());

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  @Test
  public void testGetUnfixedByApplicationIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIds(Collections.singletonList(application.getId()));

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation1.getId(),
        openViolation2.getId(), waivedViolation.getId(), grandfatheredViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIds(Collections.singletonList(application.getId()));

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation1.getId(),
        openViolation2.getId());
  }

  @Test
  public void testGetUnfixedByApplicationIdsAndStageIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, null, null, null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId(),
        waivedViolation.getId(), grandfatheredViolation.getId());
  }

  @Test
  public void testGetUnfixedBy_threatLevel() {
    PolicyViolationDAO dao = new PolicyViolationDAO();

    Policy policyFive = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan1");
    PolicyViolation openViolationFive = tempEntity.newPolicyViolation(policyEvaluation, policyFive);

    Policy policyThreatLevelZero = tempEntity.newPolicy(application.getId(), "Low", 0);
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan3");
    PolicyViolation violationZero = tempEntity.newPolicyViolation(policyEvaluation, policyThreatLevelZero);

    Policy policyThreatLevelTen = tempEntity.newPolicy(application.getId(), "Critical", 10);
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan2");
    PolicyViolation violationTen = tempEntity.newPolicyViolation(policyEvaluation, policyThreatLevelTen);

    List<PolicyViolation> violations;

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 4, 6, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(openViolationFive.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 4, 5, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(openViolationFive.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 5, 6, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(openViolationFive.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 0, 10, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationZero.getId(), violationTen.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, 10, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationZero.getId(), violationTen.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 0, null, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationZero.getId(), violationTen.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationZero.getId(), violationTen.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 1, 10, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationTen.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 0, 9, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationZero.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 1, 9, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 0, 1, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(violationZero.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 9, 10, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(violationTen.getId());
  }

  @Test
  public void testGetUnfixedBy_threatCategory() {
    PolicyViolationDAO dao = new PolicyViolationDAO();

    Policy security = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan1");
    PolicyViolation violationSecurity = tempEntity.newPolicyViolation(policyEvaluation, security);

    Policy license = tempEntity.newPolicy(application.getId(), "license",
        new Condition(LicenseConditionType.ID, "is not", "GPL-2.0"));
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan2");
    PolicyViolation violationLicense = tempEntity.newPolicyViolation(policyEvaluation, license);

    Set<PolicyThreatCategory> policyThreatCategorySet = new HashSet<>();

    List<PolicyViolation> violations;
    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactly(violationSecurity.getId(), violationLicense.getId());

    policyThreatCategorySet.add(PolicyThreatCategory.SECURITY);
    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, policyThreatCategorySet, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(violationSecurity.getId());

    policyThreatCategorySet.clear();
    policyThreatCategorySet.add(PolicyThreatCategory.LICENSE);
    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, policyThreatCategorySet, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(violationLicense.getId());

    policyThreatCategorySet.clear();
    policyThreatCategorySet.add(PolicyThreatCategory.SECURITY);
    policyThreatCategorySet.add(PolicyThreatCategory.LICENSE);
    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, policyThreatCategorySet, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(violationLicense.getId(), violationSecurity.getId());

    policyThreatCategorySet.clear();
    policyThreatCategorySet.add(PolicyThreatCategory.QUALITY);
    policyThreatCategorySet.add(PolicyThreatCategory.OTHER);
    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, policyThreatCategorySet, null, null, null);
    assertThat(violations).isEmpty();
  }

  @Test
  public void testGetUnfixedByApplicationIdsAndStageIds_policyViolationState() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan1");

    PolicyViolation openPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedPolicyViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, policyWaiver);
    PolicyViolation grandfatheredPolicyViolation = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation, policy);

    List<String> applicationIds = Collections.singletonList(application.getId());
    List<String> stageTypeIds = Collections.singletonList(BuildStageType.ID);
    List<PolicyViolation> violations;

    // All violation states
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, true, true, true);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openPolicyViolation.getId(), waivedPolicyViolation.getId(),
            grandfatheredPolicyViolation.getId());

    // None violation states (Equal to all)
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, false, false, false);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openPolicyViolation.getId(), waivedPolicyViolation.getId(),
            grandfatheredPolicyViolation.getId());

    // Only Open
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, true, false, false);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openPolicyViolation.getId());

    // Only Waived
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, false, true, false);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(waivedPolicyViolation.getId());

    // Only Grandfathered
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, false, false, true);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(grandfatheredPolicyViolation.getId());

    // Open and Waived
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, true, true, false);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openPolicyViolation.getId(),
        waivedPolicyViolation.getId());

    // Open and Grandfathered
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, true, false, true);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openPolicyViolation.getId(),
        grandfatheredPolicyViolation.getId());

    // Waived and Grandfathered
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, false, true, true);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(waivedPolicyViolation.getId(),
        grandfatheredPolicyViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsAndStageIds(
        Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdsAndPolicyIds() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy1 = tempEntity.newPolicy(application);
    Policy policy2 = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy1);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy1,
        tempEntity.newWaiver(policy1.getId(), application.getId()));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy1);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation grandfatheredViolation = tempEntity.newPolicyViolation(policyEvaluation, policy1);
    grandfatheredViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(grandfatheredViolation);
    // Open policy violation for a different policy
    tempEntity.newPolicyViolation(policyEvaluation, policy2);

    // Policy violation for a different application
    policyEvaluation =
        tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy1);

    List<PolicyViolation> violations =
        dao.getActiveByApplicationIdsAndPolicyIds(Collections.singletonList(application.getId()),
            Collections.singletonList(policy1.getId()), null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdsAndPolicyIds_BeforeDate() {
    String appId = application.getId();
    Policy policy = tempEntity.newPolicy(application);

    PolicyViolationDAO dao = new PolicyViolationDAO();

    PolicyEvaluation eval;
    eval = tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "scan-" + tempEntity.uuid(), asDate("2023-01-01"));
    String jan01 = tempEntity.newPolicyViolation(eval, policy).getId();

    eval = tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "scan-" + tempEntity.uuid(), asDate("2023-01-15"));
    String jan15 = tempEntity.newPolicyViolation(eval, policy).getId();

    List<String> app = asList(appId);
    List<String> policyId = asList(policy.getId());

    // Test openTimeAfter and openTimeBefore both null
    List<PolicyViolation> result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, null, null);
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01, jan15);

    // Test openTimeAfter
    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-01"), null);
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01, jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-02"), null);
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-15"), null);
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-16"), null);
    assertThat(result).isEmpty();

    // Test openTimeBefore
    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, null, asDate("2023-01-15"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01, jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, null, asDate("2023-01-14"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, null, asDate("2023-01-01"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, null, asDate("2022-12-31"));
    assertThat(result).extracting(PolicyViolation::getId).isEmpty();

    // Test openTimeAfter AND openTimeBefore
    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-01"), asDate("2023-01-15"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01, jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-02"), asDate("2023-01-15"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-01"), asDate("2023-01-14"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-02"), asDate("2023-01-14"));
    assertThat(result).isEmpty();
  }

  private Date asDate(String dateString) {
    return org.joda.time.Instant.parse(dateString).toDate();
  }

  private String addViolation(
      PolicyViolationDAO dao,
      String stageTypeId,
      Date openTime,
      Date legacyViolationTime,
      Date waiveTime,
      Date fixTime,
      int threatLevel,
      Condition condition)
  {
    Policy policy = tempEntity.newPolicy(application.getId(), ("" + UUID.randomUUID()).replace("-", ""), threatLevel);

    if (condition != null) {
      Constraint constraint = new Constraint();
      constraint.setConditions(Collections.singletonList(condition));
      policy.setConstraints(Collections.singletonList(constraint));
    }

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId,
        "scan-" + tempEntity.uuid(), openTime);
    PolicyViolation violation;
    if (waiveTime != null) {
      violation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
          tempEntity.newWaiver(policy.getId(), application.getId()));
    }
    else {
      violation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    }
    violation.setLegacyViolationTime(legacyViolationTime);
    violation.setWaiveTime(waiveTime);
    violation.setFixTime(fixTime);
    dao.update(violation);

    return violation.getId();
  }

  private String addViolation(PolicyViolationDAO dao,
                              String stageTypeId,
                              Date openTime,
                              Date legacyViolationTime,
                              Date waiveTime,
                              Date fixTime)
  {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId,
        "scan-" + tempEntity.uuid(), openTime);
    PolicyViolation violation;
    if (waiveTime != null) {
      violation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
          tempEntity.newWaiver(policy.getId(), application.getId()));
    }
    else {
      violation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    }
    violation.setLegacyViolationTime(legacyViolationTime);
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
    Date legacyViolationTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date waiveTime = new Date(reference.plus(Duration.ofMinutes(3)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());
    Date notGrandfathered = null;
    Date notWaived = null;
    Date notFixed = null;

    List<String> expectedIds = new ArrayList<>();

    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));

    addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, legacyViolationTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openBefore, notGrandfathered, notWaived, notFixed);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsOpenedAfterDate(
        Collections.singletonList(application.getId()),
        cutoff, null, null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void testGetActiveByApplicationIdsOpenedAfterDate_ThreatLevel() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Instant reference = Instant.now();
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date legacyViolationTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());

    List<String> expectedIds = new ArrayList<>();

    // matches by date and threat level
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, null, null, null, 10, null));
    // matches by date but not threat level
    addViolation(dao, BuildStageType.ID, openAfter, null, null, null, 2, null);
    // matches by threat level but not date
    addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, null, null, 10, null);
    // matches neither by date nor by threat level
    addViolation(dao, BuildStageType.ID, cutoff, null, null, fixTime, 2, null);
    List<PolicyViolation> violations = dao.getActiveByApplicationIdsOpenedAfterDate(
        Collections.singletonList(application.getId()),
        cutoff, 8, 10, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void testGetActiveByApplicationIdsOpenedAfterDate_ThreatCategory() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Instant reference = Instant.now();
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date legacyViolationTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());

    List<String> expectedIds = new ArrayList<>();

    Condition licenseCondition = new Condition(LicenseConditionType.ID, "is not", "GPL-2.0");

    // matches by date and threat category
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, null, null, null, 10, null));
    // matches by date but not threat category
    addViolation(dao, BuildStageType.ID, openAfter, null, null, null, 10, licenseCondition);
    // matches by threat category but not date
    addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, null, null, 10, null);
    // matches neither by date nor by threat category
    addViolation(dao, BuildStageType.ID, cutoff, null, null, fixTime, 10, licenseCondition);

    Set<PolicyThreatCategory> policyThreatCategories = new HashSet<>();
    policyThreatCategories.add(PolicyThreatCategory.SECURITY);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsOpenedAfterDate(
        Collections.singletonList(application.getId()),
        cutoff, null, null, policyThreatCategories);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void getUnfixedByApplicationIdsOpenedAfterDate() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Instant reference = Instant.now();
    Date openBefore = new Date(reference.minus(Duration.ofMinutes(1)).toEpochMilli());
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date legacyViolationTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date waiveTime = new Date(reference.plus(Duration.ofMinutes(3)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());
    Date notGrandfathered = null;
    Date notWaived = null;
    Date notFixed = null;

    List<String> expectedIds = new ArrayList<>();

    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, waiveTime, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, cutoff, legacyViolationTime, notWaived, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, cutoff, notGrandfathered, waiveTime, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, openAfter, notGrandfathered, waiveTime, notFixed));

    addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, legacyViolationTime, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openBefore, notGrandfathered, notWaived, notFixed);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIdsOpenedAfterDate(
        Collections.singletonList(application.getId()),
        cutoff, null, null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIdsOpenedAfterDate() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Instant reference = Instant.now();
    Date openBefore = new Date(reference.minus(Duration.ofMinutes(1)).toEpochMilli());
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date legacyViolationTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date waiveTime = new Date(reference.plus(Duration.ofMinutes(3)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());
    Date notGrandfathered = null;
    Date notWaived = null;
    Date notFixed = null;

    List<String> expectedIds = new ArrayList<>();

    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, notFixed));

    addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notGrandfathered, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, legacyViolationTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notGrandfathered, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openBefore, notGrandfathered, notWaived, notFixed);
    addViolation(dao, ReleaseStageType.ID, openAfter, notGrandfathered, notWaived, notFixed);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsAndStageIdsOpenedAfterDate(
        Collections.singletonList(application.getId()), Collections.singletonList(BuildStageType.ID), cutoff, null,
        null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void testGetActiveByApplicationIdAndStageIdsAndTimeRange() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver waiver = tempEntity.newWaiver(policy.getId(), application.getId());
    Date to = new Date(System.currentTimeMillis() - 10 * 1000);
    Date from = new Date(to.getTime() - 60 * 1000);

    Date before = new Date(from.getTime() - 1000);
    Date during1 = new Date(from.getTime() + 1000);
    Date during2 = new Date(from.getTime() + 2000);

    PolicyEvaluation policyEvalBeforeDateRange = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-0", before);
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
    openedBeforeGrandfatheredDuring.setLegacyViolationTime(during1);
    dao.update(openedBeforeGrandfatheredDuring);

    PolicyEvaluation policyEvalOnStartDateRange = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1", from);
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
    openedDuringGrandfatheredAfter.setLegacyViolationTime(to);
    dao.update(openedDuringGrandfatheredAfter);

    PolicyEvaluation policyEvalInDateRange = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-2", during1);
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
    openedAndGrandfatheredDuring.setLegacyViolationTime(during2);
    dao.update(openedAndGrandfatheredDuring);

    PolicyEvaluation policyEvalOnEndDateRange = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-3", to);
    // opened after time range
    tempEntity.newPolicyViolation(policyEvalOnEndDateRange, policy);

    PolicyEvaluation policyEvalOnStartDateRangeOtherStage = tempEntity
        .newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-os", from);
    // matching app and time range but wrong stage
    tempEntity.newPolicyViolation(policyEvalOnStartDateRangeOtherStage, policy);

    PolicyEvaluation policyEvalOnStartDateRangeOtherApp = tempEntity
        .newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID, "scan-oa", from);
    // matching stage and time range but wrong app
    tempEntity.newPolicyViolation(policyEvalOnStartDateRangeOtherApp, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdsAndTimeRange(application.getId(),
        Collections.singletonList(BuildStageType.ID), from, to);

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
    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation unfixedGrandfatheredViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    unfixedGrandfatheredViolation1.setLegacyViolationTime(policyEvaluation1.getTime());
    dao.update(unfixedGrandfatheredViolation1);
    PolicyViolation fixedGrandfatheredViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    fixedGrandfatheredViolation1.setFixTime(new Date());
    fixedGrandfatheredViolation1.setLegacyViolationTime(new Date());
    dao.update(fixedGrandfatheredViolation1);

    Application application2 = tempEntity.newApplicationWithParent();
    Policy policy2 = tempEntity.newPolicy(application2);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    PolicyViolation unfixedGrandfatheredViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    unfixedGrandfatheredViolation2.setLegacyViolationTime(new Date());
    dao.update(unfixedGrandfatheredViolation2);
    PolicyViolation fixedGrandfatheredViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    fixedGrandfatheredViolation2.setFixTime(new Date());
    fixedGrandfatheredViolation2.setLegacyViolationTime(new Date());
    dao.update(fixedGrandfatheredViolation2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyViolation> violations1 = dao.getUnfixedGrandfatheredByApplicationId(tx, application.getId());
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
    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
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
      List<PolicyViolation> violations1 = dao.getUnfixedByApplicationId(tx, application.getId());
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
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId");
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
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId");
    PolicyViolation fromPolicyViolation = tempEntity.newPolicyViolation(evaluation, fromPolicy);
    PolicyViolation otherPolicyViolation = tempEntity.newPolicyViolation(evaluation, otherPolicy);
    PolicyEvaluation otherAppEvaluation = tempEntity.newPolicyEvaluation(tempEntity
        .newApplication(organization.getId()).getId(), BuildStageType.ID, "scanId");
    PolicyViolation otherAppPolicyViolation = tempEntity.newPolicyViolation(otherAppEvaluation, fromPolicy);

    PolicyViolationDAO dao = new PolicyViolationDAO();
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.replacePolicyId(tx, application.getId(), fromPolicy.getId(), toPolicy.getId());
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
  public void testDeleteFixedByApplicationIdAndDate_H2() {
    testDeleteFixedByApplicationIdAndDate(true);
  }

  @Test
  public void testDeleteFixedByApplicationIdAndDate_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();

    try (PostgresServer postgres = new PostgresServer()) {
      // Create a postgres ODS database
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);

      testDeleteFixedByApplicationIdAndDate(false);
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testDeleteFixedByApplicationIdAndDate(boolean isDatabaseEmbedded) {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    assertThat(dao.isDatabaseEmbedded()).isEqualTo(isDatabaseEmbedded);

    Application app = tempEntity.newApplicationWithParent();

    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation evaluation0 = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(),
        BuildStageType.ID, "scan-1", new Date(System.currentTimeMillis() - 900));
    PolicyViolation violation0 = tempEntity.newPolicyViolation(evaluation0, policy);
    violation0.setFixTime(evaluation0.getTime());
    dao.update(violation0);
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-1",
        new Date(System.currentTimeMillis() - 900));
    for (int i = 0; i < PolicyViolationDAO.DELETE_BATCH_SIZE + 2; i++) {
      tempEntity.newPolicyViolation(evaluation1, policy);
    }
    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-2",
        new Date(System.currentTimeMillis() - 500));
    for (PolicyViolation violation : dao.getByApplicationId(app.getId())) {
      violation.setFixTime(evaluation2.getTime());
      dao.update(violation);
    }
    PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation2, policy);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(evaluation2, policy);
    PolicyEvaluation evaluation3 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-3");
    violation2.setFixTime(evaluation3.getTime());
    dao.update(violation2);

    int deletedRows = dao.deleteFixedByApplicationIdAndDate(app.getId(), evaluation3.getTime());

    assertThat(deletedRows).isEqualTo(PolicyViolationDAO.DELETE_BATCH_SIZE + 2);
    assertThat(dao.getByApplicationId(app.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactlyInAnyOrder(violation1, violation2);
    assertThat(dao.getById(violation0.getId())).isNotNull();
  }

  @Test
  public void testGetCount() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    assertThat(dao.getCount()).isEqualTo(2);
  }

  @Test
  public void testDeleteByApplicationId_H2() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    assertThat(dao.isDatabaseEmbedded()).isTrue();

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    dao.deleteByApplicationId(null /* TransactionContext */, application.getId());

    assertThat(dao.getByApplicationId(application.getId())).isEmpty();
  }

  @Test
  public void testDeleteByApplicationId_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();

    try (PostgresServer postgres = new PostgresServer()) {
      // Create a postgres ODS database
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);

      PolicyViolationDAO dao = new PolicyViolationDAO();
      assertThat(dao.isDatabaseEmbedded()).isFalse();

      application = tempEntity.newApplicationWithParent();

      PolicyEvaluation policyEvaluation =
          tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
      Policy policy = tempEntity.newPolicy(application);
      tempEntity.newPolicyViolation(policyEvaluation, policy);
      tempEntity.newPolicyViolation(policyEvaluation, policy);
      assertThat(dao.getByApplicationId(application.getId())).hasSize(2);

      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        dao.deleteByApplicationId(tx, application.getId());
        tx.commit();
      }

      assertThat(dao.getByApplicationId(application.getId())).isEmpty();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }
}
