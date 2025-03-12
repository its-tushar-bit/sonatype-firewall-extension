/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO.WaiverReasonData;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAOTest.ACKNOWLEDGED_VIOLATION_WAIVER_REASON;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.brain.utils.DateConverter.toLocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyWaiverDAOTest
    extends AbstractDbDAOTest
{
  private final String associatedPackagedUrl = "pkg:maven/group/artifact@2.0?classifier=c1&type=jar";

  private PolicyViolationDAO policyViolationDAO;

  private PolicyWaiverDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    policyViolationDAO = daoFactory.createPolicyViolationDAO();
    dao = daoFactory.createPolicyWaiverDAO();
  }

  @Test
  public void testGetByIdNotNull() {
    assertThatThrownBy(() -> dao.getByIdNotNull("fake id")).isInstanceOf(NotFoundException.class)
        .hasMessage("PolicyWaiver with ID fake id does not exist.");
  }

  @Test
  public void testCRUD() {
    String hash = "123456789012345678901";
    assertThat(hash.length()).isGreaterThan(20);
    String truncatedHash = hash.substring(0, 20);
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "My comment";
    String associatedPackagedUrl = "pkg:maven/group/artifact@1.0?classifier=c1&type=jar";
    ComponentMatcherStrategyForWaiver componentMatcherStrategy = ComponentMatcherStrategyForWaiver.DEFAULT;

    // Create
    PolicyWaiver policyWaiver =
        new PolicyWaiver(hash, policyId, ownerId, associatedPackagedUrl, componentMatcherStrategy, comment);
    policyWaiver.setConstraintFacts(createRandomConstraintFacts());
    Date expiryTime = DateTime.now().plusWeeks(1).toDate();
    policyWaiver.setExpiryTime(expiryTime);
    assertThat(policyWaiver.getId()).isNull();
    Date beforeInsert = new Date();
    dao.insert(policyWaiver);
    Date afterInsert = new Date();
    assertThat(policyWaiver.getId()).isNotNull();
    assertThat(policyWaiver.getCreateTime()).isNotNull();
    Date createTime = policyWaiver.getCreateTime();
    assertThat(createTime).isAfterOrEqualTo(beforeInsert).isBeforeOrEqualTo(afterInsert);

    // Read
    policyWaiver = dao.getById(policyWaiver.getId());
    assertThat(policyWaiver).isNotNull();
    assertPolicyWaiver(
        truncatedHash,
        policyId,
        ownerId,
        associatedPackagedUrl,
        componentMatcherStrategy,
        comment,
        createTime,
        expiryTime,
        policyWaiver
    );

    // Update
    String updateComment = "Updated comment";
    policyWaiver.setComment(updateComment);
    dao.update(policyWaiver);

    // Read
    policyWaiver = dao.getById(policyWaiver.getId());
    assertThat(policyWaiver).isNotNull();
    assertPolicyWaiver(
        truncatedHash,
        policyId,
        ownerId,
        associatedPackagedUrl,
        componentMatcherStrategy,
        updateComment,
        createTime,
        expiryTime,
        policyWaiver
    );

    // Delete
    dao.delete(policyWaiver);

    policyWaiver = dao.getById(policyWaiver.getId());
    assertThat(policyWaiver).isNull();
  }

  private void assertPolicyWaiver(
      String hash,
      String policyId,
      String ownerId,
      String associatedPackagedUrl,
      ComponentMatcherStrategyForWaiver componentMatcherStrategy,
      String comment,
      Date createTime,
      Date expiryTime,
      PolicyWaiver actual)
  {
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getAssociatedPackageUrl()).isEqualTo(associatedPackagedUrl);
    assertThat(actual.getComponentMatchStrategy()).isEqualTo(componentMatcherStrategy);
    assertThat(actual.getComment()).isEqualTo(comment);
    assertThat(actual.getCreateTime()).isEqualTo(createTime);
    assertThat(actual.getExpiryTime()).isEqualTo(expiryTime);
  }

  private void assertPolicyWaiver(PolicyWaiver expected, PolicyWaiver actual) {
    assertThat(actual.getHash()).isEqualTo(expected.getHash());
    assertThat(actual.getPolicyId()).isEqualTo(expected.getPolicyId());
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
    assertThat(actual.getComment()).isEqualTo(expected.getComment());
  }

  @Test
  public void testInsert_Duplicate_ComponentLevel() {
    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(hash, policyId, ownerId, constraintFacts, comment);
    dao.insert(policyWaiver1);

    PolicyWaiver policyWaiver2 = new PolicyWaiver(hash, policyId, ownerId, constraintFacts, comment);
    assertThatThrownBy(() -> dao.insert(policyWaiver2)).isInstanceOf(BadRequestException.class)
        .hasMessage("This policy waiver already exists.");

    dao.delete(policyWaiver1);
  }

  @Test
  public void testInsert_Duplicate_PolicyLevel() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(null /* hash */, policyId, ownerId, constraintFacts, comment);
    dao.insert(policyWaiver1);

    PolicyWaiver policyWaiver2 = new PolicyWaiver(null /* hash */, policyId, ownerId, constraintFacts, comment);
    assertThatThrownBy(() -> dao.insert(policyWaiver2)).isInstanceOf(BadRequestException.class)
        .hasMessage("This policy waiver already exists.");

    dao.delete(policyWaiver1);
  }

  @Test
  public void testGetActiveApplicableByOwnerId() {
    Policy policyApp = tempEntity.newPolicy(application);
    Policy policyOrg = tempEntity.newPolicy(organization);
    Policy policyParentOrg = tempEntity.newPolicy(organization.getParentOrganizationId());

    PolicyWaiver policyWaiverParentOrg = tempEntity.newWaiver("0", policyParentOrg.getId(),
        organization.getParentOrganizationId());
    PolicyWaiver policyWaiverOrg = tempEntity.newWaiver("1", policyOrg.getId(), organization.getId());
    PolicyWaiver policyWaiverApp = tempEntity.newWaiver("2", policyApp.getId(), application.getId());

    // Assert for application
    List<PolicyWaiver> policyWaivers = dao.getActiveApplicableByOwnerId(application.getId());
    assertThat(policyWaivers).hasSize(3);
    assertPolicyWaiver(policyWaiverParentOrg, policyWaivers.get(0));
    assertPolicyWaiver(policyWaiverOrg, policyWaivers.get(1));
    assertPolicyWaiver(policyWaiverApp, policyWaivers.get(2));

    // Assert for organization
    policyWaivers = dao.getActiveApplicableByOwnerId(organization.getId());
    assertThat(policyWaivers).hasSize(2);
    assertPolicyWaiver(policyWaiverParentOrg, policyWaivers.get(0));
    assertPolicyWaiver(policyWaiverOrg, policyWaivers.get(1));

    // Assert for parent organization
    policyWaivers = dao.getActiveApplicableByOwnerId(organization.getParentOrganizationId());
    assertThat(policyWaivers).hasSize(1);
    assertPolicyWaiver(policyWaiverParentOrg, policyWaivers.get(0));
  }

  @Test
  public void testInsert_CommentTooLong() {
    String hash = "12345678901234567890";
    String policyId = "MyPolicyId";
    String ownerId = organization.getId();
    String comment = StringUtils.repeat("X", 1001);
    PolicyWaiver policyWaiver1 = new PolicyWaiver(hash, policyId, ownerId, comment);

    assertThatThrownBy(() -> dao.insert(policyWaiver1)).isInstanceOf(BadRequestException.class)
        .hasMessage("Comment length must not exceed 1000 characters.");

    dao.delete(policyWaiver1);
  }

  @Test
  public void testInsert_ComponentMatchStrategyNotProvided_HashNotNull() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiver policyWaiver = new PolicyWaiver("testHash", policy.getId(), organization.getId(), "test comment");
    assertThat(policyWaiver.getComponentMatchStrategy()).isNull();

    dao.insert(policyWaiver);
    try {
      policyWaiver = dao.getById(policyWaiver.getId());
      assertThat(policyWaiver.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT);
    }
    finally {
      dao.delete(policyWaiver);
    }
  }

  @Test
  public void testInsert_ComponentMatchStrategyNotProvided_HashNull() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiver policyWaiver = new PolicyWaiver(null /* hash */, policy.getId(), organization.getId(), "test comment");
    assertThat(policyWaiver.getComponentMatchStrategy()).isNull();

    dao.insert(policyWaiver);
    try {
      policyWaiver = dao.getById(policyWaiver.getId());
      assertThat(policyWaiver.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForWaiver.ALL_COMPONENTS);
    }
    finally {
      dao.delete(policyWaiver);
    }
  }

  @Test
  public void testUpdate_ComponentMatchStrategyNotProvided_HashNotNull() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiver policyWaiver = tempEntity.newWaiver("testHash", policy.getId(), organization.getId(), "test comment");
    assertThat(policyWaiver.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT);

    policyWaiver.setComponentMatchStrategy(null);
    dao.update(policyWaiver);
    policyWaiver = dao.getById(policyWaiver.getId());
    assertThat(policyWaiver.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT);
  }

  @Test
  public void testUpdate_ComponentMatchStrategyNotProvided_HashNull() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(null /* hash */, policy.getId(), organization.getId(), "test comment");
    assertThat(policyWaiver.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForWaiver.ALL_COMPONENTS);

    policyWaiver.setComponentMatchStrategy(null);
    dao.update(policyWaiver);
    policyWaiver = dao.getById(policyWaiver.getId());
    assertThat(policyWaiver.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForWaiver.ALL_COMPONENTS);
  }

  @Test
  public void testUpdate_CommentTooLong() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
    String comment = StringUtils.repeat("X", 1001);
    policyWaiver.setComment(comment);

    assertThatThrownBy(() -> dao.update(policyWaiver)).isInstanceOf(BadRequestException.class)
        .hasMessage("Comment length must not exceed 1000 characters.");

    comment = comment.substring(0, 1000);
    policyWaiver.setComment(comment);
    dao.update(policyWaiver);
    assertThat(dao.getById(policyWaiver.getId()).getComment()).isEqualTo(comment);
  }

  @Test
  public void testUpdate_Duplicate_ComponentLevel() {
    String hash1 = "11111111111111111111";
    String hash2 = "11111111111111111112";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    tempEntity.newWaiver(hash1, policyId, ownerId, constraintFacts, comment);
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver(hash2, policyId, ownerId, constraintFacts, comment);

    policyWaiver2.setHash(hash1);
    assertThatThrownBy(() -> dao.update(policyWaiver2)).isInstanceOf(BadRequestException.class)
        .hasMessage("A policy waiver for the same policy violation already exists.");
  }

  @Test
  public void testUpdate_Duplicate_PolicyLevel() {
    Policy policy1 = tempEntity.newPolicy(organization);
    Policy policy2 = tempEntity.newPolicy(organization);
    String policyId1 = policy1.getId();
    String policyId2 = policy2.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    tempEntity.newWaiver(null /* hash */, policyId1, ownerId, constraintFacts, comment);
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver(null /* hash */, policyId2, ownerId, constraintFacts, comment);

    policyWaiver2.setPolicyId(policyId1);
    assertThatThrownBy(() -> dao.update(policyWaiver2)).isInstanceOf(BadRequestException.class)
        .hasMessage("A policy waiver for the same policy violation already exists.");
  }

  @Test
  public void testGetByOwnerId() {
    DateTime now = DateTime.now();
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";
    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver(null, policyId, ownerId, null, comment,
        now.toDate(), null);
    PolicyWaiver expiringWaiver = tempEntity.newWaiver("expiring", policyId, ownerId, null, comment,
        now.toDate(), now.plusHours(1).toDate());
    PolicyWaiver expiredWaiver = tempEntity.newWaiver("expired", policyId, ownerId, null, comment,
        now.toDate(), now.toDate());

    assertThat(dao.getByOwnerId(ownerId)).extracting(PolicyWaiver::getId)
        .containsExactly(noExpiryWaiver.getId(), expiringWaiver.getId(), expiredWaiver.getId());
  }

  @Test
  public void testGetActiveByOwnerId() {
    DateTime now = DateTime.now();
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";
    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver(null, policyId, ownerId, null, comment,
        now.toDate(), null);
    PolicyWaiver expiringWaiver = tempEntity.newWaiver("expiring", policyId, ownerId, null, comment,
        now.toDate(), now.plusHours(1).toDate());
    tempEntity.newWaiver("expired", policyId, ownerId, null, comment,
        now.toDate(), now.toDate());

    assertThat(dao.getActiveByOwnerId(ownerId)).extracting(PolicyWaiver::getId)
        .containsExactly(noExpiryWaiver.getId(), expiringWaiver.getId());
  }

  @Test
  public void testGetApplicableAndExpiredByOwnerId() {
    DateTime now = DateTime.now();
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";
    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver(null, policyId, ownerId, null, comment,
        now.toDate(), null);
    PolicyWaiver expiringWaiver = tempEntity.newWaiver("expiring", policyId, ownerId, null, comment,
        now.toDate(), now.plusHours(1).toDate());
    PolicyWaiver expiredWaiver = tempEntity.newWaiver("expired", policyId, ownerId, null, comment,
        now.toDate(), now.minusMillis(1).toDate());

    assertThat(dao.getApplicableAndExpiredByOwnerId(ownerId)).extracting(PolicyWaiver::getId)
        .containsExactly(noExpiryWaiver.getId(), expiringWaiver.getId(), expiredWaiver.getId());
  }

  @Test
  public void testGetByOwnerIdAndHash() {
    String hash = "12345678901234567890";
    DateTime now = DateTime.now();
    Policy policy1 = tempEntity.newPolicy(organization);
    Policy policy2 = tempEntity.newPolicy(organization);
    Policy policy3 = tempEntity.newPolicy(organization);
    Policy policy4 = tempEntity.newPolicy(organization);
    String ownerId = organization.getId();
    String comment = "Just testing";

    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver(hash, policy1.getId(), ownerId, null, comment,
        now.toDate(), null);
    PolicyWaiver expiringWaiver = tempEntity.newWaiver(hash, policy2.getId(), ownerId, null, comment,
        now.toDate(), now.plusHours(1).toDate());
    PolicyWaiver expiredWaiver = tempEntity.newWaiver(hash, policy3.getId(), ownerId, null, comment,
        now.toDate(), now.toDate());

    // not expiring waiver for all components
    tempEntity.newWaiver(null, policy4.getId(), ownerId, null, comment,
        now.toDate(), null);

    // not expiring waiver for Root Org
    tempEntity.newWaiver(hash, policy4.getId(), Organization.ROOT_ORGANIZATION_ID, null, comment,
        now.toDate(), null);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      List<PolicyWaiver> waivers = dao.getByOwnerIdAndHash(tx, ownerId, hash);
      tx.commit();

      assertThat(waivers).extracting(PolicyWaiver::getId)
          .containsExactly(noExpiryWaiver.getId(), expiringWaiver.getId(), expiredWaiver.getId());
    }
  }

  @Test
  public void testGetActiveByOwnerIdAndHash() {
    String hash = "12345678901234567890";
    DateTime now = DateTime.now();
    Policy policy1 = tempEntity.newPolicy(organization);
    Policy policy2 = tempEntity.newPolicy(organization);
    Policy policy3 = tempEntity.newPolicy(organization);
    Policy policy4 = tempEntity.newPolicy(organization);
    String ownerId = organization.getId();
    String comment = "Just testing";

    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver(hash, policy1.getId(), ownerId, null,
        EXACT_COMPONENT, comment, now.toDate(), null);
    PolicyWaiver expiringWaiver = tempEntity.newWaiver(hash, policy2.getId(), ownerId, null,
        EXACT_COMPONENT, comment,
        now.toDate(), now.plusHours(1).toDate());

    // Expired waiver
    tempEntity.newWaiver(hash, policy3.getId(), ownerId, null, EXACT_COMPONENT, comment,
        now.toDate(), now.toDate());

    // not expiring waiver for all components
    tempEntity.newWaiver(null, policy4.getId(), ownerId, null, ALL_COMPONENTS, comment,
        now.toDate(), null);

    // not expiring waiver for Root Org
    tempEntity.newWaiver(hash, policy4.getId(), Organization.ROOT_ORGANIZATION_ID, null, EXACT_COMPONENT,
        comment, now.toDate(), null);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      List<PolicyWaiver> waivers = dao.getActiveByOwnerIdAndHash(tx, ownerId, hash, EXACT_COMPONENT);
      tx.commit();

      assertThat(waivers).extracting(PolicyWaiver::getId)
          .containsExactly(noExpiryWaiver.getId(), expiringWaiver.getId());
    }
  }

  @Test
  public void getApplicableToComponent() {
    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(hash, policyId, ownerId, comment);
    policyWaiver1.setComponentMatchStrategy(EXACT_COMPONENT);
    dao.insert(policyWaiver1);
    PolicyWaiver policyWaiver2 = new PolicyWaiver(policyId, ownerId, comment);
    policyWaiver2.setComponentMatchStrategy(ALL_COMPONENTS);
    dao.insert(policyWaiver2);

    List<PolicyWaiver> waivers = dao.getApplicableToComponent(ownerId, hash);
    dao.delete(policyWaiver1);
    dao.delete(policyWaiver2);

    assertThat(waivers).extracting(PolicyWaiver::getId).containsExactly(policyWaiver1.getId(), policyWaiver2.getId());
  }

  @Test
  public void getApplicableToComponent_TimeBasedWaivers() {
    Instant now = Instant.now();
    Date yesterday = Date.from(now.minus(1, ChronoUnit.DAYS));
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";

    PolicyWaiver expiringWaiver = new PolicyWaiver(hash, policyId, ownerId, comment);
    expiringWaiver.setComponentMatchStrategy(EXACT_COMPONENT);
    expiringWaiver.setExpiryTime(aWeekFromNow);
    dao.insert(expiringWaiver);

    PolicyWaiver expiredWaiver = new PolicyWaiver(policyId, ownerId, comment);
    expiringWaiver.setComponentMatchStrategy(ALL_COMPONENTS);
    expiredWaiver.setExpiryTime(yesterday);
    dao.insert(expiredWaiver);

    List<PolicyWaiver> waivers = dao.getApplicableToComponent(ownerId, hash);
    dao.delete(expiringWaiver);
    dao.delete(expiredWaiver);

    assertThat(waivers).extracting(PolicyWaiver::getId)
        .containsExactly(expiringWaiver.getId());
  }

  @Test
  public void testGetApplicableToComponentIncludingAllVersions() {
    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(hash, policyId, ownerId, comment);
    policyWaiver1.setComponentMatchStrategy(EXACT_COMPONENT);
    dao.insert(policyWaiver1);

    PolicyWaiver policyWaiver2 = new PolicyWaiver(policyId, ownerId, comment);
    policyWaiver2.setComponentMatchStrategy(ALL_COMPONENTS);
    dao.insert(policyWaiver2);

    PolicyWaiver policyWaiver3 = new PolicyWaiver(policyId, ownerId, comment);
    PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier
        .createMavenCoordinates("g1", "a1", "v1", "c1", "jar"));
    policyWaiver3.setComponentMatchStrategy(ALL_VERSIONS);
    policyWaiver3.setAssociatedPackageUrl(purl.getPackageUrl());
    dao.insert(policyWaiver3);

    List<PolicyWaiver> waivers = dao.getApplicableToComponentIncludingAllVersions(ownerId, hash, purl);
    dao.delete(policyWaiver1);
    dao.delete(policyWaiver2);
    dao.delete(policyWaiver3);

    assertThat(waivers).extracting(PolicyWaiver::getId)
        .containsExactly(policyWaiver1.getId(), policyWaiver2.getId(), policyWaiver3.getId());
  }

  @Test
  public void testGetApplicableToComponentOnlyAllVersions_noPurl() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiver policyWaiver = new PolicyWaiver(policy.getId(), organization.getId(), null);
    PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier
        .createMavenCoordinates("g1", "a1", "v1", "c1", "jar"));
    policyWaiver.setComponentMatchStrategy(ALL_VERSIONS);
    policyWaiver.setAssociatedPackageUrl(purl.getPackageUrl());
    dao.insert(policyWaiver);
    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> waivers = dao.getApplicableToComponentOnlyAllVersions(tx, organization.getId(), purl);
      assertThat(waivers).isNotEmpty();

      List<PolicyWaiver> waiversNoPurl = dao.getApplicableToComponentOnlyAllVersions(tx, organization.getId(), null);
      assertThat(waiversNoPurl).isEmpty();
    }
  }

  @Test
  public void testGetApplicableToComponentOnlyAllVersions_OnlySomeApply() {
    Policy policy1 = tempEntity.newPolicy(organization);
    Policy policy2 = tempEntity.newPolicy(organization);
    Policy policy3 = tempEntity.newPolicy(organization);

    PolicyWaiver policyWaiverExpected = new PolicyWaiver(policy1.getId(), application.getId(), null);
    PackageUrlIdentifier expectedPurl1 = PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier
        .createMavenCoordinates("g1", "a1", "v1", "c1", "jar"));
    policyWaiverExpected.setComponentMatchStrategy(ALL_VERSIONS);
    policyWaiverExpected.setAssociatedPackageUrl(expectedPurl1.getPackageUrl());
    dao.insert(policyWaiverExpected);

    PolicyWaiver policyWaiverExpected2 = new PolicyWaiver(policy2.getId(), application.getId(), null);
    PackageUrlIdentifier expectedPurl2 = PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier
        .createMavenCoordinates("g1", "a1", "v2", "c1", "jar"));
    policyWaiverExpected2.setComponentMatchStrategy(ALL_VERSIONS);
    policyWaiverExpected2.setAssociatedPackageUrl(expectedPurl2.getPackageUrl());
    dao.insert(policyWaiverExpected2);

    PolicyWaiver policyWaiverWrongPurl = new PolicyWaiver(policy3.getId(), application.getId(), null);
    PackageUrlIdentifier wrongPurl = PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier
        .createMavenCoordinates("gw", "aw", "vw", "c1", "jar"));
    policyWaiverWrongPurl.setComponentMatchStrategy(ALL_VERSIONS);
    policyWaiverWrongPurl.setAssociatedPackageUrl(wrongPurl.getPackageUrl());
    dao.insert(policyWaiverWrongPurl);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> waivers = dao.getApplicableToComponentOnlyAllVersions(tx, application.getId(), expectedPurl1);
      assertThat(waivers).hasSize(2);
      assertThat(waivers).extracting(PolicyWaiver::getId)
          .containsExactly(policyWaiverExpected.getId(), policyWaiverExpected2.getId());
    }
  }

  @Test
  public void testDeleteDoesNotCascadeToWaivedPolicyViolation() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("ababababab", policy.getId(), application.getId());
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID,
            "PolicyWaiverDAOTest");
    PolicyViolation waivedPolicyViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, policyWaiver);
    assertThat(policyViolationDAO.getById(waivedPolicyViolation.getId())).isNotNull();

    dao.delete(policyWaiver);
    assertThat(policyViolationDAO.getById(waivedPolicyViolation.getId())).isNotNull();
  }

  @Test
  public void testGetByPolicyIdAndOwnerIds() {
    Policy policy1 = tempEntity.newPolicy(application);
    Policy policy2 = tempEntity.newPolicy(organization);
    tempEntity.newWaiver(policy1.getId(), application.getId());
    PolicyWaiver waiver2 = tempEntity.newWaiver(policy2.getId(), application.getId());
    tempEntity.newWaiver(policy2.getId(), organization.getId());

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> waivers = dao.getByPolicyIdAndOwnerIds(tx, policy2.getId(),
          Collections.singleton(application.getId()));
      assertThat(waivers).extracting(PolicyWaiver::getId).containsExactly(waiver2.getId());
    }
  }

  @Test
  public void testGetCount() {
    assertThat(dao.getCount()).isEqualTo(0);
    Policy policy1 = tempEntity.newPolicy(application);
    Policy policy2 = tempEntity.newPolicy(organization);
    Policy policy3 = tempEntity.newPolicy(organization);
    tempEntity.newWaiver(policy1.getId(), application.getId());
    tempEntity.newWaiver(policy2.getId(), application.getId());
    tempEntity.newWaiver(policy3.getId(), application.getId());
    assertThat(dao.getCount()).isEqualTo(3);
  }

  @Test
  public void testGetActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts() {
    String hash = "hash";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(hash, policyId, ownerId, constraintFacts, null, comment);
    tempEntity.newWaiver(hash, policyId, ownerId, createRandomConstraintFacts(), comment);

    try (TransactionContext tx = dao.createTransactionContext()) {
      PolicyWaiver foundPolicyWaiver = dao.getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId,
          ownerId, constraintFacts, policyWaiver1.getAssociatedPackageUrl(), policyWaiver1.getComponentMatchStrategy());
      assertThat(foundPolicyWaiver.getId()).isEqualTo(policyWaiver1.getId());
    }
  }

  @Test
  public void testGetActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts_ExpiredWaiver() {
    String hash = "hash";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    DateTime now = DateTime.now();
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(hash, policyId, ownerId, constraintFacts, comment, now.toDate(), now.toDate());

    try (TransactionContext tx = dao.createTransactionContext()) {
      PolicyWaiver foundPolicyWaiver = dao.getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId,
          ownerId, constraintFacts, policyWaiver.getAssociatedPackageUrl(), policyWaiver.getComponentMatchStrategy());
      assertThat(foundPolicyWaiver).isNull();
    }
  }

  @Test
  public void testGetActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts_UnexpiredWaiver() {
    String hash = "hash";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    DateTime now = DateTime.now();
    PolicyWaiver unexpiredWaiver = tempEntity.newWaiver(hash, policyId, ownerId, constraintFacts, comment, now.toDate(),
        now.plusDays(1).toDate());

    try (TransactionContext tx = dao.createTransactionContext()) {
      PolicyWaiver foundPolicyWaiver = dao.getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId,
          ownerId, constraintFacts, unexpiredWaiver.getAssociatedPackageUrl(),
          unexpiredWaiver.getComponentMatchStrategy());
      assertThat(foundPolicyWaiver.getId()).isEqualTo(unexpiredWaiver.getId());
    }
  }

  @Test
  public void testGetActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts_NullConstraintFacts() {
    String hash = "hash";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 =
        tempEntity.newWaiver(hash, policyId, ownerId, null /* constraintFacts */, null, comment);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Get using null constraint facts
      PolicyWaiver foundPolicyWaiver = dao.getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId,
          ownerId, null /* constraintFacts */, policyWaiver1.getAssociatedPackageUrl(),
          policyWaiver1.getComponentMatchStrategy());
      assertThat(foundPolicyWaiver.getId()).isEqualTo(policyWaiver1.getId());

      // Get using not null constraint facts
      foundPolicyWaiver = dao.getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId, ownerId,
          createRandomConstraintFacts(), policyWaiver1.getAssociatedPackageUrl(),
          policyWaiver1.getComponentMatchStrategy());
      assertThat(foundPolicyWaiver).isNull();
    }
  }

  @Test
  public void testGetActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts_PURL() {
    String hash = "hash";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Get using not null match strategy
      PolicyWaiver foundPolicyWaiver =
          dao.getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId, ownerId, constraintFacts,
              associatedPackagedUrl, ComponentMatcherStrategyForWaiver.DEFAULT);
      assertThat(foundPolicyWaiver).isNull();
    }
  }

  @Test
  public void testGetActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts_MatchStrategy() {
    String hash = "hash";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "My comment";
    ComponentMatcherStrategyForWaiver componentMatchStrategy = ComponentMatcherStrategyForWaiver.DEFAULT;
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    PolicyWaiver policyWaiver1 =
        tempEntity.newWaiver(hash, policyId, ownerId, constraintFacts, componentMatchStrategy, comment);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Get using null associated packaged url
      PolicyWaiver foundPolicyWaiver = dao.getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId,
          ownerId, constraintFacts, null /* associatedPackagedUrl */, componentMatchStrategy);
      assertThat(foundPolicyWaiver.getId()).isEqualTo(policyWaiver1.getId());

      // Get using not null associated packaged url, ignores purl when not for all versions match strategy
      foundPolicyWaiver =
          dao.getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId, ownerId, constraintFacts,
              associatedPackagedUrl, componentMatchStrategy);
      assertThat(foundPolicyWaiver.getId()).isEqualTo(policyWaiver1.getId());
    }
  }

  @Test
  public void testGetActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts_PURL_MatchStrategy() {
    String hash = "hash";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "My comment";
    ComponentMatcherStrategyForWaiver componentMatchStrategy = ComponentMatcherStrategyForWaiver.DEFAULT;
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    PolicyWaiver policyWaiver1 =
        tempEntity.newWaiver(hash, policyId, ownerId, constraintFacts, associatedPackagedUrl, componentMatchStrategy,
            comment);

    try (TransactionContext tx = dao.createTransactionContext()) {
      PolicyWaiver foundPolicyWaiver = dao.getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId,
          ownerId, constraintFacts, associatedPackagedUrl, componentMatchStrategy);
      assertThat(foundPolicyWaiver.getId()).isEqualTo(policyWaiver1.getId());

      // Get using null associated packaged url and match strategy
      foundPolicyWaiver =
          dao.getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId, ownerId, constraintFacts,
              null /* associatedPackagedUrl */, null /* componentMatchStrategy */);
      assertThat(foundPolicyWaiver).isNull();
    }
  }

  private List<ConstraintFact> createRandomConstraintFacts() {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact.setTriggerJson(
        "{\"conditionIndex\":1,\"trigger\":{\"refId\":\"" + UUID.randomUUID() + "\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("constraint Id", "constraint Name",
        LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);
    return Collections.singletonList(constraintFact);
  }

  @Test
  public void getByIdAndOwnerIdNotNull() {
    Policy policy = tempEntity.newPolicy(organization);
    String hash = "hash";
    String policyId = policy.getId();
    String comment = "My comment";
    ComponentMatcherStrategyForWaiver componentMatchStrategy = ComponentMatcherStrategyForWaiver.DEFAULT;
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(hash, policyId, application.getId(), constraintFacts, componentMatchStrategy, comment);

    PolicyWaiver databaseWaiver =
        dao.getByIdAndOwnerIdNotNull(policyWaiver.getId(), application.getId());

    assertPolicyWaiver(policyWaiver, databaseWaiver);
  }

  @Test
  public void getByIdAndOwnerIdNotNull_throwsNotFound_whenNoWaiver() {
    assertThatThrownBy(
        () -> dao.getByIdAndOwnerIdNotNull("fake id", application.getId()))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a waiver with ID fake id for owner " + application.getId() + ".");
  }

  @Test
  public void getByIdAndOwnerIdNotNull_throwsNotFound_whenWrongOwner() {
    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());

    assertThatThrownBy(() -> dao.getByIdAndOwnerIdNotNull(policyWaiver.getId(), "ownerId"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a waiver with ID " + policyWaiver.getId() + " for owner ownerId.");
  }

  @Test
  public void testUpdate_dontAllowUpdatingExpiredWaiverWhenThereIsAnIdenticalActiveWaiver() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String hash1 = TemporaryEntity.uuid().substring(0, 8);

    PolicyWaiver expiredPolicyWaiver =
        new PolicyWaiver(hash1, policyId, ownerId, constraintFacts, TemporaryEntity.uuid().substring(0, 8));
    expiredPolicyWaiver.setExpiryTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));
    tempEntity.newWaiver(expiredPolicyWaiver);

    PolicyWaiver activePolicyWaiver =
        new PolicyWaiver(hash1, policyId, ownerId, constraintFacts, TemporaryEntity.uuid().substring(0, 5));
    activePolicyWaiver.setExpiryTime(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    tempEntity.newWaiver(activePolicyWaiver);

    String associatedPackagedUrl = TemporaryEntity.uuid().substring(0, 10);
    expiredPolicyWaiver.setAssociatedPackageUrl(associatedPackagedUrl);

    assertThatThrownBy(() -> dao.update(expiredPolicyWaiver)).isInstanceOf(BadRequestException.class)
        .hasMessage("A policy waiver for the same policy violation already exists.");
  }

  @Test
  public void testUpdate_throwExceptionWhenAttemptingToHaveMultipleIdenticalActiveWaivers() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String hash1 = TemporaryEntity.uuid().substring(0, 8);

    // initially add expiredPolicyWaiver with expiry date in the past.
    // Doing so will allow us to add second waiver with future expiry date.
    PolicyWaiver expiredPolicyWaiver =
        new PolicyWaiver(hash1, policyId, ownerId, constraintFacts, TemporaryEntity.uuid().substring(0, 5));
    expiredPolicyWaiver.setExpiryTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));
    tempEntity.newWaiver(expiredPolicyWaiver);

    PolicyWaiver activePolicyWaiver =
        new PolicyWaiver(hash1, policyId, ownerId, constraintFacts, TemporaryEntity.uuid().substring(0, 5));
    activePolicyWaiver.setExpiryTime(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    tempEntity.newWaiver(activePolicyWaiver);

    // now try to set expiry date with future value.
    // note that there is already an active waiver. i.e. expiring in future.
    expiredPolicyWaiver.setExpiryTime(Date.from(Instant.now().plus(21, ChronoUnit.DAYS)));

    assertThatThrownBy(() -> dao.update(expiredPolicyWaiver)).isInstanceOf(BadRequestException.class)
        .hasMessage("A policy waiver for the same policy violation already exists.");
  }

  // The tested method is deprecated
  @SuppressWarnings("deprecation")
  @Test
  public void testUpdateWithNoChecks() {
    // setup
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = application.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String hash = RandomStringUtils.randomAlphabetic(8);

    PolicyWaiver policyWaiver =
        new PolicyWaiver(hash, policyId, ownerId, constraintFacts, RandomStringUtils.randomAlphabetic(5));
    policyWaiver.setExpiryTime(Date.from(Instant.now().plus(5, ChronoUnit.DAYS)));

    tempEntity.newWaiver(policyWaiver);

    // act
    String newComment = RandomStringUtils.randomAlphabetic(10);
    policyWaiver.setComment(newComment);
    dao.updateWithNoChecks(policyWaiver);

    // verify
    PolicyWaiver updatedWaiver =
        dao.getByIdAndOwnerIdNotNull(policyWaiver.getId(), application.getId());

    assertThat(updatedWaiver.getComment())
        .as("The waiver should have updated comment.")
        .isEqualTo(newComment);
  }

  @Test
  public void testGetActiveByPolicyId() {
    Policy policy1 = tempEntity.newPolicy(application);
    Policy policy2 = tempEntity.newPolicy(organization);

    PolicyWaiver policy1Waiver1 = tempEntity.newWaiver("hash1", policy1.getId(), application.getId(), "test comment",
        Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    PolicyWaiver policy2ActiveWaiver1 =
        tempEntity.newWaiver("hash2", policy2.getId(), application.getId(), "test comment",
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    PolicyWaiver policy2ActiveWaiver2 =
        tempEntity.newWaiver("hash3", policy2.getId(), application.getId(), "test comment",
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    PolicyWaiver policy2ExpiredWaiver1 =
        tempEntity.newWaiver("hash4", policy2.getId(), application.getId(), "test comment",
            Date.from(Instant.now().minus(4, ChronoUnit.DAYS)));

    List<PolicyWaiver> waivers = dao.getActiveByPolicyId(policy2.getId());

    assertThat(waivers)
        .isNotEmpty()
        .as("It should not include expired waivers")
        .hasSize(2);

    assertThat(waivers)
        .extracting(PolicyWaiver::getId)
        .contains(policy2ActiveWaiver1.getId(), policy2ActiveWaiver2.getId())
        .doesNotContain(policy1Waiver1.getId(), policy2ExpiredWaiver1.getId());
  }

  @Test
  public void testGetByPolicyId() {
    Policy policy1 = tempEntity.newPolicy(application);
    Policy policy2 = tempEntity.newPolicy(organization);

    PolicyWaiver policy1Waiver1 = tempEntity.newWaiver("hash1", policy1.getId(), application.getId(), "test comment",
        Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    PolicyWaiver policy2ActiveWaiver1 =
        tempEntity.newWaiver("hash2", policy2.getId(), application.getId(), "test comment",
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    PolicyWaiver policy2ActiveWaiver2 =
        tempEntity.newWaiver("hash3", policy2.getId(), application.getId(), "test comment",
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    PolicyWaiver policy2ExpiredWaiver1 =
        tempEntity.newWaiver("hash4", policy2.getId(), application.getId(), "test comment",
            Date.from(Instant.now().minus(4, ChronoUnit.DAYS)));

    List<PolicyWaiver> waivers = dao.getByPolicyId(policy2.getId());

    assertThat(waivers)
        .isNotEmpty()
        .as("It should also include expired waivers")
        .hasSize(3);

    assertThat(waivers)
        .extracting(PolicyWaiver::getId)
        .contains(policy2ActiveWaiver1.getId(), policy2ActiveWaiver2.getId(), policy2ExpiredWaiver1.getId())
        .doesNotContain(policy1Waiver1.getId());
  }

  @Test
  public void testGetCountByOwnerIdAndDate() {
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date moreThanOneYearAgo = DateUtils.addDays(oneYearAgo, -1);

    Policy policy = tempEntity.newPolicy(repository);

    tempEntity.newWaiver("hash1", policy.getId(), repository.getId(), Collections.emptyList(), "now-waived-1", now);
    tempEntity.newWaiver("hash2", policy.getId(), repository.getId(), Collections.emptyList(), "now-waived-2", now);
    tempEntity.newWaiver("hash3", policy.getId(), repository.getId(), Collections.emptyList(), "1-year-ago-waived",
        oneYearAgo);
    tempEntity.newWaiver("hash4", policy.getId(), repository.getId(), Collections.emptyList(), "+1-year-ago-waived",
        moreThanOneYearAgo);

    Map<LocalDate, Long> results = dao.getCountByOwnerIdAndDate(repository.getId(), oneYearAgo);

    assertThat(results)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(toLocalDate(now), 2L, toLocalDate(oneYearAgo), 1L));
  }

  @Test
  public void testGetPolicyWaiverReasonMappings() {
    // given a set of policy waivers, only one of which has a reason
    final var policy = tempEntity.newPolicy(organization);
    final var expectedWaiverReason = ACKNOWLEDGED_VIOLATION_WAIVER_REASON;
    final var waiverWithReason = new PolicyWaiver();
    waiverWithReason.setOwnerId(application.getId());
    waiverWithReason.setPolicyId(policy.getId());
    waiverWithReason.setWaiverReasonId(expectedWaiverReason.getId());

    tempEntity.newWaiver(policy.getId(), organization.getId());
    tempEntity.newWaiver(waiverWithReason);

    // when: fetch the waiver/reason mappings
    List<WaiverReasonData> actualWaiverReasons = dao.getPolicyWaiverReasonMappings();

    // then: we only get the waiver with a reason
    assertThat(actualWaiverReasons).hasSize(1);
    final var actualWaiverReason = actualWaiverReasons.get(0);
    assertThat(actualWaiverReason.policyWaiverId()).isEqualTo(waiverWithReason.getId());
    assertThat(actualWaiverReason.reasonText()).isEqualTo(expectedWaiverReason.getReasonText());
  }

  @Test
  public void testGetPolicyWaiverReasonMappings_emptyResult() {
    // given a single waiver with no reason
    final var policy = tempEntity.newPolicy(organization);
    tempEntity.newWaiver(policy.getId(), organization.getId());

    // when: fetch the waiver/reason mappings
    List<WaiverReasonData> actualWaiverReasons = dao.getPolicyWaiverReasonMappings();

    // then: we get an empty list
    assertThat(actualWaiverReasons).isNotNull().isEmpty();
  }
}
