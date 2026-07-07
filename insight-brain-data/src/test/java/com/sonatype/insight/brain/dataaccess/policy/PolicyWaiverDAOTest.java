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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO.PolicyContainerWaiverData;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO.WaiverReasonData;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
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

  private static final String HASH = "12345678901234567890";

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
    assertThat(policyWaiver.getHash()).isEqualTo(truncatedHash);

    // Read
    PolicyWaiver foundPolicyWaiver = dao.getById(policyWaiver.getId());
    JPA.assertEntityEquals(foundPolicyWaiver, policyWaiver);

    // Update
    String updateComment = "Updated comment";
    policyWaiver.setComment(updateComment);

    dao.update(policyWaiver);

    // Read
    foundPolicyWaiver = dao.getById(policyWaiver.getId());
    assertThat(policyWaiver).isNotNull();
    JPA.assertEntityEquals(foundPolicyWaiver, policyWaiver);

    // Delete
    dao.delete(policyWaiver);

    policyWaiver = dao.getById(policyWaiver.getId());
    assertThat(policyWaiver).isNull();
  }

  private void assertPolicyWaiver(PolicyWaiver expected, PolicyWaiver actual) {
    assertThat(actual.getHash()).isEqualTo(expected.getHash());
    assertThat(actual.getPolicyId()).isEqualTo(expected.getPolicyId());
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
    assertThat(actual.getComment()).isEqualTo(expected.getComment());
  }

  @Test
  public void testInsert_Duplicate_ComponentLevel() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(HASH, policyId, ownerId, constraintFacts, comment);
    dao.insert(policyWaiver1);

    PolicyWaiver policyWaiver2 = new PolicyWaiver(HASH, policyId, ownerId, constraintFacts, comment);
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
    String policyId = "MyPolicyId";
    String ownerId = organization.getId();
    String comment = StringUtils.repeat("X", 1001);
    PolicyWaiver policyWaiver1 = new PolicyWaiver(HASH, policyId, ownerId, comment);

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
    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver(null, policyId, ownerId, null, comment, now.toDate(), null);
    PolicyWaiver expiringWaiver = tempEntity.newWaiver("expiring", policyId, ownerId, null, comment,
        now.toDate(), now.plusHours(1).toDate());
    tempEntity.newWaiver("expired", policyId, ownerId, null, comment,
        now.toDate(), now.toDate());

    assertThat(dao.getActiveByOwnerId(ownerId)).extracting(PolicyWaiver::getId)
        .containsExactly(noExpiryWaiver.getId(), expiringWaiver.getId());
  }

  @Test
  public void testGetByOwnerIdAndPolicyId() {
    Date now = new Date();
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";
    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver(null, policyId, ownerId, null, comment, now, null);
    PolicyWaiver expiringWaiver = tempEntity.newWaiver("expiring", policyId, ownerId, null, comment,
        now, DateUtils.addHours(now, 1));
    PolicyWaiver expiredWaiver = tempEntity.newWaiver("expired", policyId, ownerId, null, comment,
        now, DateUtils.addHours(now, -1));

    // PolicyWaiver for unrelated owner
    tempEntity.newWaiver(null, policyId, application.getId(), null, comment, now, null);
    // PolicyWaiver for another policy
    tempEntity.newWaiver(null, tempEntity.newPolicy(organization).getId(), ownerId, null, comment, now, null);

    assertThat(dao.getByOwnerHierarchyAndPolicyId(organization, policyId)).extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrder(noExpiryWaiver.getId(), expiringWaiver.getId(), expiredWaiver.getId());
  }

  @Test
  public void testGetByOwnerIdAndPolicyId_excludeWaiversForContainerImage() {
    Date now = new Date();
    Policy policy = tempEntity.newPolicy(application);
    String policyId = policy.getId();
    String ownerId = application.getId();
    String comment = "Just testing";

    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver("hash", policyId, ownerId, null, comment, now, null);
    PolicyWaiver expiringWaiver =
        tempEntity.newWaiver("expiring", policyId, ownerId, null, comment, now, DateUtils.addHours(now, 1));
    PolicyWaiver expiredWaiver =
        tempEntity.newWaiver("expired", policyId, ownerId, null, comment, now, DateUtils.addHours(now, -1));

    PolicyWaiver policyWaiverForContainerImage =
        tempEntity.newWaiver(null, policyId, application.getId(), null, comment, now, null);
    policyWaiverForContainerImage.setForContainerImage(true);
    dao.update(policyWaiverForContainerImage);

    assertThat(dao.getByOwnerHierarchyAndPolicyId(application, policyId))
        .extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrder(noExpiryWaiver.getId(), expiringWaiver.getId(), expiredWaiver.getId());
  }

  @Test
  public void testGetAllForContainerImage() {
    Date now = new Date();
    Policy policy = tempEntity.newPolicy(application);
    String policyId = policy.getId();
    String ownerId = application.getId();
    String comment = "Just testing";

    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver("hash", policyId, ownerId, null, comment, now, null);
    PolicyWaiver expiringWaiver =
        tempEntity.newWaiver("expiring", policyId, ownerId, null, comment, now, DateUtils.addHours(now, 1));
    PolicyWaiver expiredWaiver =
        tempEntity.newWaiver("expired", policyId, ownerId, null, comment, now, DateUtils.addHours(now, -1));

    PolicyWaiver policyWaiverForContainerImage =
        tempEntity.newWaiver(null, policyId, application.getId(), null, comment, now, null);
    policyWaiverForContainerImage.setForContainerImage(true);
    dao.update(policyWaiverForContainerImage);

    PolicyWaiver policyWaiverForContainerImageComponent =
        tempEntity.newWaiver("component", policyId, application.getId(), null, comment, now, null);
    policyWaiverForContainerImageComponent.setForContainerImageComponent(true);
    dao.update(policyWaiverForContainerImageComponent);

    assertThat(dao.getAll())
        .extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrder(noExpiryWaiver.getId(), expiringWaiver.getId(), expiredWaiver.getId(),
            policyWaiverForContainerImage.getId(), policyWaiverForContainerImageComponent.getId());

    assertThat(dao.getAllForContainerImageByOwnerId(ownerId))
        .extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrder(policyWaiverForContainerImage.getId(),
            policyWaiverForContainerImageComponent.getId());
  }

  @Test
  public void testDeleteAllForContainerImage() {
    Date now = new Date();
    Policy policy = tempEntity.newPolicy(application);
    String policyId = policy.getId();
    String ownerId = application.getId();
    String comment = "Just testing";

    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver("hash", policyId, ownerId, null, comment, now, null);
    PolicyWaiver expiringWaiver =
        tempEntity.newWaiver("expiring", policyId, ownerId, null, comment, now, DateUtils.addHours(now, 1));
    PolicyWaiver expiredWaiver =
        tempEntity.newWaiver("expired", policyId, ownerId, null, comment, now, DateUtils.addHours(now, -1));

    PolicyWaiver policyWaiverForContainerImage =
        tempEntity.newWaiver(null, policyId, application.getId(), null, comment, now, null);
    policyWaiverForContainerImage.setForContainerImage(true);
    dao.update(policyWaiverForContainerImage);

    PolicyWaiver policyWaiverForContainerImageComponent =
        tempEntity.newWaiver("component", policyId, application.getId(), null, comment, now, null);
    policyWaiverForContainerImageComponent.setForContainerImageComponent(true);
    dao.update(policyWaiverForContainerImageComponent);

    assertThat(dao.getAll())
        .extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrder(noExpiryWaiver.getId(), expiringWaiver.getId(), expiredWaiver.getId(),
            policyWaiverForContainerImage.getId(), policyWaiverForContainerImageComponent.getId());

    dao.deleteAllForContainerImage(application.getId());

    assertThat(dao.getAll())
        .extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrder(noExpiryWaiver.getId(), expiringWaiver.getId(), expiredWaiver.getId());
  }

  @Test
  public void testGetByOwnerIdAndHash() {
    DateTime now = DateTime.now();
    Policy policy1 = tempEntity.newPolicy(organization);
    Policy policy2 = tempEntity.newPolicy(organization);
    Policy policy3 = tempEntity.newPolicy(organization);
    Policy policy4 = tempEntity.newPolicy(organization);
    String ownerId = organization.getId();
    String comment = "Just testing";

    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver(HASH, policy1.getId(), ownerId, null, comment, now.toDate(),
        null);
    PolicyWaiver expiringWaiver = tempEntity.newWaiver(HASH, policy2.getId(), ownerId, null, comment,
        now.toDate(), now.plusHours(1).toDate());
    PolicyWaiver expiredWaiver = tempEntity.newWaiver(HASH, policy3.getId(), ownerId, null, comment,
        now.toDate(), now.toDate());

    // not expiring waiver for all components
    tempEntity.newWaiver(null, policy4.getId(), ownerId, null, comment, now.toDate(), null);

    // not expiring waiver for Root Org
    tempEntity.newWaiver(HASH, policy4.getId(), Organization.ROOT_ORGANIZATION_ID, null, comment, now.toDate(), null);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      List<PolicyWaiver> waivers = dao.getByOwnerIdAndHash(tx, ownerId, HASH);
      tx.commit();

      assertThat(waivers).extracting(PolicyWaiver::getId)
          .containsExactly(noExpiryWaiver.getId(), expiringWaiver.getId(), expiredWaiver.getId());
    }
  }

  @Test
  public void testGetActiveByOwnerIdAndHash() {
    DateTime now = DateTime.now();
    Policy policy1 = tempEntity.newPolicy(organization);
    Policy policy2 = tempEntity.newPolicy(organization);
    Policy policy3 = tempEntity.newPolicy(organization);
    Policy policy4 = tempEntity.newPolicy(organization);
    String ownerId = organization.getId();
    String comment = "Just testing";

    PolicyWaiver noExpiryWaiver = tempEntity.newWaiver(HASH, policy1.getId(), ownerId, null,
        EXACT_COMPONENT, comment, now.toDate(), null);
    PolicyWaiver expiringWaiver = tempEntity.newWaiver(HASH, policy2.getId(), ownerId, null,
        EXACT_COMPONENT, comment,
        now.toDate(), now.plusHours(1).toDate());

    // Expired waiver
    tempEntity.newWaiver(HASH, policy3.getId(), ownerId, null, EXACT_COMPONENT, comment,
        now.toDate(), now.toDate());

    // not expiring waiver for all components
    tempEntity.newWaiver(null, policy4.getId(), ownerId, null, ALL_COMPONENTS, comment,
        now.toDate(), null);

    // not expiring waiver for Root Org
    tempEntity.newWaiver(HASH, policy4.getId(), Organization.ROOT_ORGANIZATION_ID, null, EXACT_COMPONENT,
        comment, now.toDate(), null);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      List<PolicyWaiver> waivers = dao.getActiveByOwnerIdAndHash(tx, ownerId, HASH, EXACT_COMPONENT);
      tx.commit();

      assertThat(waivers).extracting(PolicyWaiver::getId)
          .containsExactly(noExpiryWaiver.getId(), expiringWaiver.getId());
    }
  }

  @Test
  public void testGetByOwnerIdAndHashes_multipleHashes() {
    DateTime now = DateTime.now();
    Policy policy1 = tempEntity.newPolicy(organization);
    Policy policy2 = tempEntity.newPolicy(organization);
    String ownerId = organization.getId();
    String hash1 = "hash0000000000000001";
    String hash2 = "hash0000000000000002";
    String hash3 = "hash0000000000000003";

    PolicyWaiver waiver1 = tempEntity.newWaiver(hash1, policy1.getId(), ownerId, null, "comment", now.toDate(), null);
    PolicyWaiver waiver2a = tempEntity.newWaiver(hash2, policy1.getId(), ownerId, null, "comment", now.toDate(), null);
    // Second waiver for hash2 uses a different policy to avoid unique constraint violation
    PolicyWaiver waiver2b = tempEntity.newWaiver(hash2, policy2.getId(), ownerId, null, "comment2", now.toDate(), null);
    // hash3 has no waivers

    try (TransactionContext tx = dao.createTransactionContext()) {
      Map<String, List<PolicyWaiver>> result = dao.getByOwnerIdAndHashes(tx, ownerId, Set.of(hash1, hash2, hash3));

      assertThat(result).hasSize(2); // only hash1 and hash2 have waivers
      assertThat(result.get(hash1)).hasSize(1).extracting(PolicyWaiver::getId).containsExactly(waiver1.getId());
      assertThat(result.get(hash2)).hasSize(2)
          .extracting(PolicyWaiver::getId)
          .containsExactlyInAnyOrder(waiver2a.getId(), waiver2b.getId());
      assertThat(result).doesNotContainKey(hash3);
    }
  }

  @Test
  public void testGetByOwnerIdAndHashes_emptyHashes() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      Map<String, List<PolicyWaiver>> result =
          dao.getByOwnerIdAndHashes(tx, organization.getId(), Collections.emptySet());
      assertThat(result).isEmpty();
    }
  }

  @Test
  public void testGetByOwnerIdAndHashes_nullHashes() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      Map<String, List<PolicyWaiver>> result = dao.getByOwnerIdAndHashes(tx, organization.getId(), null);
      assertThat(result).isEmpty();
    }
  }

  @Test
  public void testGetByOwnerIdAndHashes_noMatchingWaivers() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      Map<String, List<PolicyWaiver>> result = dao.getByOwnerIdAndHashes(
          tx, organization.getId(), Set.of("nonexistent_hash_1", "nonexistent_hash_2"));
      assertThat(result).isEmpty();
    }
  }

  @Test
  public void testGetExpiredByOwnerIdAndPurl() {
    String ownerId = "owner123";
    String packageUrlString = "pkg:maven/com.sonatype.nexus/nexus-platform-api@1.0.0?type=jar";
    String packageUrlString1 = "pkg:maven/com.sonatype.nexus/insight-brain@1.0.0?type=jar";
    PackageUrlIdentifier purl = new PackageUrlIdentifier(packageUrlString);

    PolicyWaiver policyWaiver =
        insertPolicyWaiver(null, ownerId, ALL_VERSIONS, packageUrlString, DateTime.now().minusHours(1).toDate());
    insertPolicyWaiver(null, ownerId, ALL_VERSIONS, packageUrlString1, DateTime.now().minusHours(1).toDate());

    List<PolicyWaiver> result = dao.getExpiredToComponentIncludingAllVersions(ownerId, null, purl);

    assertThat(result).hasSize(1)
        .extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrder(policyWaiver.getId());
  }

  @Test
  public void testGetExpiredByOwnerIdAndHash() {
    String ownerId = organization.getId();
    DateTime now = DateTime.now();

    PolicyWaiver expiringWaiver = insertPolicyWaiver(HASH, ownerId, EXACT_COMPONENT, now.minusHours(1).toDate());
    PolicyWaiver expiringWaiver1 = insertPolicyWaiver(HASH, ownerId, EXACT_COMPONENT, now.toDate());

    List<PolicyWaiver> waivers = dao.getExpiredToComponentIncludingAllVersions(ownerId, HASH, null);

    assertThat(waivers).extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrder(expiringWaiver.getId(), expiringWaiver1.getId());
  }

  @Test
  public void testGetExpiredByOwnerIdAndHash_ReturnsExpiredWaivers() {
    String ownerId = organization.getId();
    DateTime now = DateTime.now();

    PolicyWaiver expiringWaiver = insertPolicyWaiver(HASH, ownerId, EXACT_COMPONENT, now.toDate());
    PolicyWaiver expiringWaiver1 = insertPolicyWaiver(HASH, ownerId, EXACT_COMPONENT, now.minusHours(1).toDate());
    insertPolicyWaiver(HASH, ownerId, EXACT_COMPONENT, now.plusHours(1).toDate());

    List<PolicyWaiver> waivers = dao.getExpiredToComponentIncludingAllVersions(ownerId, HASH, null);

    assertThat(waivers).extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrder(expiringWaiver.getId(), expiringWaiver1.getId());
  }

  @Test
  public void testGetExpiredByOwnerIdAndHash_CorrectMatchStrategy() {
    String ownerId = organization.getId();
    DateTime now = DateTime.now();
    String packageUrlString = "pkg:maven/com.sonatype.nexus/nexus-platform-api@1.0.0?type=jar";

    PolicyWaiver expiringWaiver =
        insertPolicyWaiver(null, ownerId, ALL_VERSIONS, packageUrlString, now.minusHours(1).toDate());

    List<PolicyWaiver> waivers =
        dao.getExpiredToComponentIncludingAllVersions(ownerId, null, new PackageUrlIdentifier(packageUrlString));

    assertThat(waivers).extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrder(expiringWaiver.getId());
  }

  @Test
  public void testGetExpiredByOwnerIdAndHash_NoMatchingWaivers() {
    String ownerId = organization.getId();
    DateTime now = DateTime.now();

    insertPolicyWaiver(HASH, ownerId, EXACT_COMPONENT, now.toDate());
    insertPolicyWaiver(HASH, ownerId, EXACT_COMPONENT, now.minusHours(1).toDate());
    insertPolicyWaiver(HASH, ownerId, EXACT_COMPONENT, now.minusHours(2).toDate());

    List<PolicyWaiver> waivers = dao.getExpiredToComponentIncludingAllVersions(ownerId, "9876", null);

    assertThat(waivers).isEmpty();
  }

  private PolicyWaiver insertPolicyWaiver(
      String hash,
      String ownerId,
      ComponentMatcherStrategyForWaiver matchingStrategy,
      Date expiryTime)
  {
    return tempEntity.newWaiver(hash, tempEntity.newPolicy(organization).getId(), ownerId, null,
        matchingStrategy, null, DateTime.now().toDate(), expiryTime);
  }

  private PolicyWaiver insertPolicyWaiver(
      String hash,
      String ownerId,
      ComponentMatcherStrategyForWaiver matchingStrategy,
      String associatedPackagedUrl,
      Date expiryTime)
  {
    return tempEntity.newWaiver(hash, tempEntity.newPolicy(organization).getId(), ownerId, null,
        associatedPackagedUrl, matchingStrategy, null, DateTime.now().toDate(), expiryTime);
  }

  @Test
  public void testGetApplicableToComponent() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(HASH, policyId, ownerId, comment);
    policyWaiver1.setComponentMatchStrategy(EXACT_COMPONENT);
    dao.insert(policyWaiver1);
    PolicyWaiver policyWaiver2 = new PolicyWaiver(policyId, ownerId, comment);
    policyWaiver2.setComponentMatchStrategy(ALL_COMPONENTS);
    dao.insert(policyWaiver2);

    List<PolicyWaiver> waivers = dao.getApplicableToComponent(ownerId, HASH);
    dao.delete(policyWaiver1);
    dao.delete(policyWaiver2);

    assertThat(waivers).extracting(PolicyWaiver::getId).containsExactly(policyWaiver1.getId(), policyWaiver2.getId());
  }

  @Test
  public void testGetApplicableToComponent_TimeBasedWaivers() {
    Instant now = Instant.now();
    Date yesterday = Date.from(now.minus(1, ChronoUnit.DAYS));
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));

    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";

    PolicyWaiver expiringWaiver = new PolicyWaiver(HASH, policyId, ownerId, comment);
    expiringWaiver.setComponentMatchStrategy(EXACT_COMPONENT);
    expiringWaiver.setExpiryTime(aWeekFromNow);
    dao.insert(expiringWaiver);

    PolicyWaiver expiredWaiver = new PolicyWaiver(policyId, ownerId, comment);
    expiringWaiver.setComponentMatchStrategy(ALL_COMPONENTS);
    expiredWaiver.setExpiryTime(yesterday);
    dao.insert(expiredWaiver);

    List<PolicyWaiver> waivers = dao.getApplicableToComponent(ownerId, HASH);
    dao.delete(expiringWaiver);
    dao.delete(expiredWaiver);

    assertThat(waivers).extracting(PolicyWaiver::getId)
        .containsExactly(expiringWaiver.getId());
  }

  @Test
  public void testGetApplicableToComponentIncludingAllVersions() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(HASH, policyId, ownerId, comment);
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

    List<PolicyWaiver> waivers = dao.getApplicableToComponentIncludingAllVersions(ownerId, HASH, purl);
    dao.delete(policyWaiver1);
    dao.delete(policyWaiver2);
    dao.delete(policyWaiver3);

    assertThat(waivers).extracting(PolicyWaiver::getId)
        .containsExactly(policyWaiver1.getId(), policyWaiver2.getId(), policyWaiver3.getId());
  }

  @Test
  public void testGetApplicableToComponentIncludingAllVersionsByOwnerIds_groupsByOwnerMatchingSingleOwnerLookup() {
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(organization);
    String comment = "Just testing";
    PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar"));

    PolicyWaiver orgExact = new PolicyWaiver(HASH, policy.getId(), organization.getId(), comment);
    orgExact.setComponentMatchStrategy(EXACT_COMPONENT);
    dao.insert(orgExact);
    PolicyWaiver orgAllComponents = new PolicyWaiver(policy.getId(), organization.getId(), comment);
    orgAllComponents.setComponentMatchStrategy(ALL_COMPONENTS);
    dao.insert(orgAllComponents);
    PolicyWaiver orgAllVersions = new PolicyWaiver(policy.getId(), organization.getId(), comment);
    orgAllVersions.setComponentMatchStrategy(ALL_VERSIONS);
    orgAllVersions.setAssociatedPackageUrl(purl.getPackageUrl());
    dao.insert(orgAllVersions);
    PolicyWaiver appExact = new PolicyWaiver(HASH, policy.getId(), application.getId(), comment);
    appExact.setComponentMatchStrategy(EXACT_COMPONENT);
    dao.insert(appExact);

    Map<String, List<PolicyWaiver>> grouped = dao.getApplicableToComponentIncludingAllVersionsByOwnerIds(
        List.of(application.getId(), organization.getId()), HASH, purl);
    List<PolicyWaiver> orgSingle = dao.getApplicableToComponentIncludingAllVersions(organization.getId(), HASH, purl);
    List<PolicyWaiver> appSingle = dao.getApplicableToComponentIncludingAllVersions(application.getId(), HASH, purl);
    dao.delete(orgExact);
    dao.delete(orgAllComponents);
    dao.delete(orgAllVersions);
    dao.delete(appExact);

    assertThat(grouped.get(organization.getId())).extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrderElementsOf(orgSingle.stream().map(PolicyWaiver::getId).toList());
    assertThat(grouped.get(application.getId())).extracting(PolicyWaiver::getId)
        .containsExactlyInAnyOrderElementsOf(appSingle.stream().map(PolicyWaiver::getId).toList());
    assertThat(grouped.get(organization.getId())).extracting(PolicyWaiver::getId)
        .contains(orgExact.getId(), orgAllComponents.getId(), orgAllVersions.getId());
    assertThat(grouped.get(application.getId())).extracting(PolicyWaiver::getId).contains(appExact.getId());
  }

  @Test
  public void testGetExpiredToComponentIncludingAllVersionsByOwnerIds_returnsOnlyExpiredGroupedByOwner() {
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(organization);
    Date yesterday = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    Date tomorrow = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));

    PolicyWaiver orgExpired = new PolicyWaiver(HASH, policy.getId(), organization.getId(), "Just testing");
    orgExpired.setComponentMatchStrategy(EXACT_COMPONENT);
    orgExpired.setExpiryTime(yesterday);
    dao.insert(orgExpired);
    PolicyWaiver appActive = new PolicyWaiver(HASH, policy.getId(), application.getId(), "Just testing");
    appActive.setComponentMatchStrategy(EXACT_COMPONENT);
    appActive.setExpiryTime(tomorrow);
    dao.insert(appActive);

    Map<String, List<PolicyWaiver>> grouped = dao.getExpiredToComponentIncludingAllVersionsByOwnerIds(
        List.of(application.getId(), organization.getId()), HASH, null);
    dao.delete(orgExpired);
    dao.delete(appActive);

    assertThat(grouped).containsOnlyKeys(organization.getId());
    assertThat(grouped.get(organization.getId())).extracting(PolicyWaiver::getId)
        .containsExactly(orgExpired.getId());
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
  public void testDelete_DoesNotCascadeToWaivedPolicyViolation() {
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
  public void testDelete_CascadesToPolicyWaiverRequests() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("ababababab", policy.getId(), application.getId());
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setPolicyId(policy.getId())
        .setOwnerId(application.getId())
        .setPolicyWaiverId(policyWaiver.getId())
        .setPolicyViolationId("policyViolationId"));

    // sanity check
    PolicyWaiverRequestDAO policyWaiverRequestDAO = daoFactory.createPolicyWaiverRequestDAO();
    assertThat(policyWaiverRequestDAO.getByOwnerId(application.getId())).hasSize(1);

    dao.delete(policyWaiver);

    assertThat(policyWaiverRequestDAO.getByOwnerId(application.getId())).isEmpty();
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
  public void testGetByIdAndOwnerIdNotNull() {
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
  public void testGetByIdAndOwnerIdNotNull_throwsNotFound_whenNoWaiver() {
    assertThatThrownBy(
        () -> dao.getByIdAndOwnerIdNotNull("fake id", application.getId()))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Cannot find a waiver with ID fake id for owner " + application.getId() + ".");
  }

  @Test
  public void testGetByIdAndOwnerIdNotNull_throwsNotFound_whenWrongOwner() {
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
  public void testUpdateForRenewal_bypassesDuplicateCheck() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String hash = TemporaryEntity.uuid().substring(0, 8);

    PolicyWaiver expiredWaiver =
        new PolicyWaiver(hash, policyId, ownerId, constraintFacts, TemporaryEntity.uuid().substring(0, 8));
    expiredWaiver.setExpiryTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));
    tempEntity.newWaiver(expiredWaiver);

    PolicyWaiver activeWaiver =
        new PolicyWaiver(hash, policyId, ownerId, constraintFacts, TemporaryEntity.uuid().substring(0, 5));
    activeWaiver.setExpiryTime(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    tempEntity.newWaiver(activeWaiver);

    Date newExpiry = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));
    expiredWaiver.setExpiryTime(newExpiry);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.updateForRenewal(tx, expiredWaiver);
      tx.commit();
    }

    PolicyWaiver renewed = dao.getByIdAndOwnerIdNotNull(expiredWaiver.getId(), ownerId);
    assertThat(renewed.getExpiryTime()).isEqualTo(newExpiry);
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
        .as("Should include active waivers from policy2")
        .contains(policy2ActiveWaiver1.getId(), policy2ActiveWaiver2.getId())
        .as("Should NOT include waivers from policy1, nor expired, requested, and rejected waivers from policy2")
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

  @Test
  public void testGetAllContainerPolicyWaivers_emptyResult() {
    List<PolicyContainerWaiverData> waivers = dao.getAllContainerPolicyWaivers(1, 10);

    assertThat(waivers).isEmpty();
  }

  @Test
  public void testGetAllContainerPolicyWaivers_returnsOneWaiver() {
    Policy policy = tempEntity.newPolicy(organization, 10);

    PolicyWaiver waiverContainerImage = new PolicyWaiver(null, policy.getId(), application.getId(), "comment");
    waiverContainerImage.setForContainerImage(true);
    tempEntity.newWaiver(waiverContainerImage);

    PolicyWaiver waiverContainerImageComponent =
        new PolicyWaiver("hash1", policy.getId(), application.getId(), "comment");
    waiverContainerImageComponent.setForContainerImageComponent(true);
    tempEntity.newWaiver(waiverContainerImageComponent);

    List<PolicyContainerWaiverData> waivers = dao.getAllContainerPolicyWaivers(1, 10);

    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).policyWaiverId()).isEqualTo(waiverContainerImage.getId());
    assertThat(waivers.get(0).ownerId()).isEqualTo(waiverContainerImage.getOwnerId());
  }

  @Test
  public void testGetAllContainerPolicyWaivers_returnsMoreThanOneWaiver() {
    Policy policy = tempEntity.newPolicy(organization, 10);

    PolicyWaiver waiverContainer01 = createContainerWaiver(policy, application.getId());
    createComponentWaiver(policy, application.getId(), "comp01-hash1");

    Application anotherApplication = tempEntity.newApplication("anotherContainerImage", organization.getId());
    PolicyWaiver waiverContainer02 = createContainerWaiver(policy, anotherApplication.getId());
    createComponentWaiver(policy, anotherApplication.getId(), "comp02-hash1");
    createComponentWaiver(policy, anotherApplication.getId(), "comp02-hash2");

    List<PolicyContainerWaiverData> waivers = dao.getAllContainerPolicyWaivers(1, 10);
    assertThat(waivers).hasSize(2);
    assertThat(waivers)
        .extracting(PolicyContainerWaiverData::policyWaiverId)
        .containsExactlyInAnyOrder(waiverContainer01.getId(), waiverContainer02.getId());

    // Test pagination - verify we get 1 result per page and all IDs are present
    waivers = dao.getAllContainerPolicyWaivers(1, 1);
    assertThat(waivers).hasSize(1);
    String firstPageWaiverId = waivers.get(0).policyWaiverId();
    assertThat(firstPageWaiverId).isIn(waiverContainer01.getId(), waiverContainer02.getId());

    waivers = dao.getAllContainerPolicyWaivers(2, 1);
    assertThat(waivers).hasSize(1);
    String secondPageWaiverId = waivers.get(0).policyWaiverId();
    assertThat(secondPageWaiverId).isIn(waiverContainer01.getId(), waiverContainer02.getId());

    // Verify pagination returns different waivers
    assertThat(firstPageWaiverId).isNotEqualTo(secondPageWaiverId);
  }

  @Test
  public void testGetContainerPolicyWaiversCount() {
    Policy policy = tempEntity.newPolicy(organization, 10);

    createContainerWaiver(policy, application.getId());
    createComponentWaiver(policy, application.getId(), "comp01-hash1");

    Application anotherApplication = tempEntity.newApplication("anotherContainerImage", organization.getId());
    createContainerWaiver(policy, anotherApplication.getId());
    createComponentWaiver(policy, anotherApplication.getId(), "comp02-hash1");
    createComponentWaiver(policy, anotherApplication.getId(), "comp02-hash2");

    long waivers = dao.getContainerPolicyWaiversCount();
    assertThat(waivers).isEqualTo(2);
  }

  private PolicyWaiver createContainerWaiver(Policy policy, String ownerId) {
    PolicyWaiver waiver = new PolicyWaiver(null, policy.getId(), ownerId, "comment");
    waiver.setForContainerImage(true);
    tempEntity.newWaiver(waiver);
    return waiver;
  }

  private void createComponentWaiver(Policy policy, String ownerId, String hash) {
    PolicyWaiver waiver = new PolicyWaiver(hash, policy.getId(), ownerId, "comment");
    waiver.setForContainerImageComponent(true);
    tempEntity.newWaiver(waiver);
  }

  @Test
  public void testGetAllContainerPolicyWaivers_withAccessibleOwnerIds_returnsFilteredWaivers() {
    Policy policy = tempEntity.newPolicy(organization, 10);

    Application app1 = tempEntity.newApplication("app1", organization.getId());
    Application app2 = tempEntity.newApplication("app2", organization.getId());
    Application app3 = tempEntity.newApplication("app3", organization.getId());

    createContainerWaiver(policy, app1.getId());
    createComponentWaiver(policy, app1.getId(), "hash1");
    createContainerWaiver(policy, app2.getId());
    createComponentWaiver(policy, app2.getId(), "hash2");
    createContainerWaiver(policy, app3.getId());
    createComponentWaiver(policy, app3.getId(), "hash3");

    Set<String> accessibleOwnerIds = new HashSet<>();
    accessibleOwnerIds.add(app1.getId());
    accessibleOwnerIds.add(app2.getId());

    List<PolicyContainerWaiverData> waivers =
        dao.getAllContainerPolicyWaivers(1, 10, accessibleOwnerIds);

    assertThat(waivers).hasSize(2);
    assertThat(waivers)
        .extracting(PolicyContainerWaiverData::ownerId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId());
  }

  @Test
  public void testGetAllContainerPolicyWaivers_withEmptyOwnerIds_returnsEmpty() {
    Policy policy = tempEntity.newPolicy(organization, 10);
    createContainerWaiver(policy, application.getId());

    List<PolicyContainerWaiverData> waivers =
        dao.getAllContainerPolicyWaivers(1, 10, Collections.emptySet());

    assertThat(waivers).isEmpty();
  }

  @Test
  public void testGetAllContainerPolicyWaivers_withOrgId_includesOrgWaiver() {
    Policy policy = tempEntity.newPolicy(organization, 10);
    createContainerWaiver(policy, organization.getId());
    createContainerWaiver(policy, application.getId());
    createComponentWaiver(policy, application.getId(), "hash1");

    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication("app2", org2.getId());
    createContainerWaiver(policy, app2.getId());
    createComponentWaiver(policy, app2.getId(), "hash2");

    Set<String> accessibleOwnerIds = Collections.singleton(organization.getId());
    List<PolicyContainerWaiverData> waivers =
        dao.getAllContainerPolicyWaivers(1, 10, accessibleOwnerIds);

    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).ownerId()).isEqualTo(organization.getId());
  }

  @Test
  public void testGetContainerPolicyWaiversCount_withAccessibleOwnerIds() {
    Policy policy = tempEntity.newPolicy(organization, 10);

    Application app1 = tempEntity.newApplication("app1", organization.getId());
    Application app2 = tempEntity.newApplication("app2", organization.getId());
    Application app3 = tempEntity.newApplication("app3", organization.getId());

    createContainerWaiver(policy, app1.getId());
    createComponentWaiver(policy, app1.getId(), "hash1");
    createContainerWaiver(policy, app2.getId());
    createComponentWaiver(policy, app2.getId(), "hash2");
    createContainerWaiver(policy, app3.getId());
    createComponentWaiver(policy, app3.getId(), "hash3");

    Set<String> accessibleOwnerIds = new HashSet<>();
    accessibleOwnerIds.add(app1.getId());
    accessibleOwnerIds.add(app2.getId());

    long count = dao.getContainerPolicyWaiversCount(accessibleOwnerIds);

    assertThat(count).isEqualTo(2);
  }

  @Test
  public void testGetContainerPolicyWaiversCount_withEmptyOwnerIds_returnsZero() {
    Policy policy = tempEntity.newPolicy(organization, 10);
    createContainerWaiver(policy, application.getId());

    long count = dao.getContainerPolicyWaiversCount(Collections.emptySet());

    assertThat(count).isEqualTo(0);
  }

  @Test
  public void testSelectCount_scopedToAccessibleOwners() {
    Policy policy = tempEntity.newPolicy(organization);
    Application app1 = tempEntity.newApplication("app1", organization.getId());
    Application app2 = tempEntity.newApplication("app2", organization.getId());

    tempEntity.newWaiver("hash1", policy.getId(), app1.getId());
    tempEntity.newWaiver("hash2", policy.getId(), app1.getId());
    tempEntity.newWaiver("hash3", policy.getId(), app2.getId());

    assertThat(dao.selectCount(Set.of(app1.getId()))).isEqualTo(2);
    assertThat(dao.selectCount(Collections.emptySet())).isZero();
  }

  @Test
  public void testGetAllContainerPolicyWaivers_paginationWithFiltering() {
    Application app1 = tempEntity.newApplication("app1", organization.getId());
    Application app2 = tempEntity.newApplication("app2", organization.getId());

    for (int i = 0; i < 5; i++) {
      // Threat levels must be 0-10, use valid range
      Policy uniquePolicy = tempEntity.newPolicy(organization, 5 + i);
      createContainerWaiver(uniquePolicy, app1.getId());
      createComponentWaiver(uniquePolicy, app1.getId(), "hash1-" + i);
    }
    for (int i = 0; i < 3; i++) {
      // Threat levels must be 0-10, use valid range
      Policy uniquePolicy = tempEntity.newPolicy(organization, 2 + i);
      createContainerWaiver(uniquePolicy, app2.getId());
      createComponentWaiver(uniquePolicy, app2.getId(), "hash2-" + i);
    }

    Set<String> app1Only = Collections.singleton(app1.getId());

    List<PolicyContainerWaiverData> page1 = dao.getAllContainerPolicyWaivers(1, 3, app1Only);
    List<PolicyContainerWaiverData> page2 = dao.getAllContainerPolicyWaivers(2, 3, app1Only);

    assertThat(page1).hasSize(3);
    assertThat(page2).hasSize(2);
    assertThat(dao.getContainerPolicyWaiversCount(app1Only)).isEqualTo(5);
    assertThat(page1).extracting(PolicyContainerWaiverData::ownerId).containsOnly(app1.getId());
    assertThat(page2).extracting(PolicyContainerWaiverData::ownerId).containsOnly(app1.getId());
  }

  @Test
  public void testGetFilteredForDashboard_withComponentNameFilter() {
    Policy policy = tempEntity.newPolicy(organization);
    String purl1 = "pkg:maven/com.example/component1@1.0.0?type=jar";
    String purl2 = "pkg:maven/com.example/other-component@2.0.0?type=jar";

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "comment");
    waiver1.setAssociatedPackageUrl(purl1);
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "comment");
    waiver2.setAssociatedPackageUrl(purl2);
    tempEntity.newWaiver(waiver2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component1",
          null,
          true,
          null,
          null,
          false,
          null,
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(results).hasSize(1);
      assertThat(results.get(0).getAssociatedPackageUrl()).isEqualTo(purl1);
    }
  }

  @Test
  public void testGetFilteredForDashboard_withComponentNameFilterCaseInsensitive() {
    Policy policy = tempEntity.newPolicy(organization);
    String purl = "pkg:maven/com.example/MyComponent@1.0.0?type=jar";

    PolicyWaiver waiver = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "comment");
    waiver.setAssociatedPackageUrl(purl);
    tempEntity.newWaiver(waiver);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "mycomponent",
          null,
          true,
          null,
          null,
          false,
          null,
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(results).hasSize(1);
    }
  }

  @Test
  public void testGetFilteredForDashboard_noFiltersReturnsAll() {
    Policy policy = tempEntity.newPolicy(organization);

    tempEntity.newWaiver("hash1", policy.getId(), organization.getId());
    tempEntity.newWaiver("hash2", policy.getId(), organization.getId());

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          null,
          null,
          true,
          null,
          null,
          false,
          null,
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(results).hasSize(2);
    }
  }

  @Test
  public void testGetFilteredForDashboard_emptyOwnerIdsReturnsEmpty() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.emptySet(),
          "component",
          null,
          true,
          null,
          null,
          false,
          null,
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(results).isEmpty();
    }
  }

  @Test
  public void testGetFilteredForDashboard_pagination() {
    Policy policy = tempEntity.newPolicy(organization);
    String purlPrefix = "pkg:maven/com.example/component";

    for (int i = 0; i < 5; i++) {
      PolicyWaiver waiver = new PolicyWaiver("hash" + i, policy.getId(), organization.getId(), "comment");
      waiver.setAssociatedPackageUrl(purlPrefix + i + "@1.0.0?type=jar");
      tempEntity.newWaiver(waiver);
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Page 0, pageSize 2 (returns 3 results = pageSize + 1)
      List<PolicyWaiver> page0 = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          true,
          null,
          null,
          false,
          null,
          0,
          2,
          "EXPIRATION_DATE");

      assertThat(page0).hasSize(3); // pageSize + 1

      // Page 1, pageSize 2 (returns 3 results)
      List<PolicyWaiver> page1 = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          true,
          null,
          null,
          false,
          null,
          1,
          2,
          "EXPIRATION_DATE");

      assertThat(page1).hasSize(3); // pageSize + 1
    }
  }

  @Test
  public void testGetFilteredForDashboard_withComponentNameContainingSqlWildcards() {
    Policy policy = tempEntity.newPolicy(organization);
    String purl = "pkg:maven/com.example/my_component@1.0.0?type=jar";
    String purlOther = "pkg:maven/com.example/other@1.0.0?type=jar";

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "comment");
    waiver1.setAssociatedPackageUrl(purl);
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "comment");
    waiver2.setAssociatedPackageUrl(purlOther);
    tempEntity.newWaiver(waiver2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Searching for "my_component" with SQL wildcard underscore — should only match purl1, not purl2
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "my_component",
          null,
          true,
          null,
          null,
          false,
          null,
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(results).hasSize(1);
      assertThat(results.get(0).getAssociatedPackageUrl()).isEqualTo(purl);
    }
  }

  @Test
  public void testGetFilteredForDashboard_activeOnly() {
    Policy policy = tempEntity.newPolicy(organization);

    // Active waiver
    PolicyWaiver activeWaiver = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "active");
    activeWaiver.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    tempEntity.newWaiver(activeWaiver);

    // Expired waiver
    PolicyWaiver expiredWaiver = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "expired");
    expiredWaiver.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    expiredWaiver.setExpiryTime(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
    tempEntity.newWaiver(expiredWaiver);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Active only = true
      List<PolicyWaiver> activeResults = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          true,
          null,
          null,
          false,
          null,
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(activeResults).hasSize(1);
      assertThat(activeResults.get(0).getComment()).isEqualTo("active");

      // Active only = false (returns all)
      List<PolicyWaiver> allResults = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(allResults).hasSize(2);
    }
  }

  @Test
  public void testGetFilteredForDashboard_filteredPolicyIdsExcludesNonMatchingPolicies() {
    Policy policy1 = tempEntity.newPolicy(organization);
    Policy policy2 = tempEntity.newPolicy(organization);

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy1.getId(), organization.getId(), "w1");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy2.getId(), organization.getId(), "w2");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    tempEntity.newWaiver(waiver2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Only policy1 in filtered set — waiver2 must be excluded at DB level
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          true,
          null,
          null,
          false,
          Collections.singleton(policy1.getId()),
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(results).hasSize(1);
      assertThat(results.get(0).getPolicyId()).isEqualTo(policy1.getId());
    }
  }

  // ========== Sort Tests ==========

  @Test
  public void testGetFilteredForDashboard_sortByExpirationDateAscending() {
    Policy policy = tempEntity.newPolicy(organization);
    Date now = new Date();
    Date expiry1 = Date.from(now.toInstant().plus(1, java.time.temporal.ChronoUnit.DAYS));
    Date expiry2 = Date.from(now.toInstant().plus(2, java.time.temporal.ChronoUnit.DAYS));
    Date expiry3 = Date.from(now.toInstant().plus(3, java.time.temporal.ChronoUnit.DAYS));

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "w1");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    waiver1.setExpiryTime(expiry3);
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "w2");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    waiver2.setExpiryTime(expiry1);
    tempEntity.newWaiver(waiver2);

    PolicyWaiver waiver3 = new PolicyWaiver("hash3", policy.getId(), organization.getId(), "w3");
    waiver3.setAssociatedPackageUrl("pkg:maven/com.example/component@3.0.0?type=jar");
    waiver3.setExpiryTime(expiry2);
    tempEntity.newWaiver(waiver3);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(results).hasSize(3);
      // Ascending order: waiver2 (expiry1) < waiver3 (expiry2) < waiver1 (expiry3)
      assertThat(results.get(0).getComment()).isEqualTo("w2");
      assertThat(results.get(1).getComment()).isEqualTo("w3");
      assertThat(results.get(2).getComment()).isEqualTo("w1");
    }
  }

  @Test
  public void testGetFilteredForDashboard_sortByExpirationDateDescending() {
    Policy policy = tempEntity.newPolicy(organization);
    Date now = new Date();
    Date expiry1 = Date.from(now.toInstant().plus(1, java.time.temporal.ChronoUnit.DAYS));
    Date expiry2 = Date.from(now.toInstant().plus(2, java.time.temporal.ChronoUnit.DAYS));
    Date expiry3 = Date.from(now.toInstant().plus(3, java.time.temporal.ChronoUnit.DAYS));

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "w1");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    waiver1.setExpiryTime(expiry3);
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "w2");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    waiver2.setExpiryTime(expiry1);
    tempEntity.newWaiver(waiver2);

    PolicyWaiver waiver3 = new PolicyWaiver("hash3", policy.getId(), organization.getId(), "w3");
    waiver3.setAssociatedPackageUrl("pkg:maven/com.example/component@3.0.0?type=jar");
    waiver3.setExpiryTime(expiry2);
    tempEntity.newWaiver(waiver3);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "-EXPIRATION_DATE");

      assertThat(results).hasSize(3);
      // Descending order: waiver1 (expiry3) > waiver3 (expiry2) > waiver2 (expiry1)
      assertThat(results.get(0).getComment()).isEqualTo("w1");
      assertThat(results.get(1).getComment()).isEqualTo("w3");
      assertThat(results.get(2).getComment()).isEqualTo("w2");
    }
  }

  @Test
  public void testGetFilteredForDashboard_sortByCreationDate() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "first");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    tempEntity.newWaiver(waiver1);

    // Small delay to ensure different create times
    try {
      Thread.sleep(10);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "middle");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    tempEntity.newWaiver(waiver2);

    try {
      Thread.sleep(10);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    PolicyWaiver waiver3 = new PolicyWaiver("hash3", policy.getId(), organization.getId(), "last");
    waiver3.setAssociatedPackageUrl("pkg:maven/com.example/component@3.0.0?type=jar");
    tempEntity.newWaiver(waiver3);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Ascending order (oldest first)
      List<PolicyWaiver> resultsAsc = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "CREATION_DATE");

      assertThat(resultsAsc).hasSize(3);
      assertThat(resultsAsc.get(0).getComment()).isEqualTo("first");
      assertThat(resultsAsc.get(1).getComment()).isEqualTo("middle");
      assertThat(resultsAsc.get(2).getComment()).isEqualTo("last");

      // Descending order (newest first)
      List<PolicyWaiver> resultsDesc = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "-CREATION_DATE");

      assertThat(resultsDesc).hasSize(3);
      assertThat(resultsDesc.get(0).getComment()).isEqualTo("last");
      assertThat(resultsDesc.get(1).getComment()).isEqualTo("middle");
      assertThat(resultsDesc.get(2).getComment()).isEqualTo("first");
    }
  }

  @Test
  public void testGetFilteredForDashboard_sortByComponentScope() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "z-package");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/zeta@1.0.0?type=jar");
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "a-package");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/alpha@1.0.0?type=jar");
    tempEntity.newWaiver(waiver2);

    PolicyWaiver waiver3 = new PolicyWaiver("hash3", policy.getId(), organization.getId(), "m-package");
    waiver3.setAssociatedPackageUrl("pkg:maven/com.example/middle@1.0.0?type=jar");
    tempEntity.newWaiver(waiver3);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Ascending alphabetical order
      List<PolicyWaiver> resultsAsc = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          null,
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "COMPONENT_SCOPE");

      assertThat(resultsAsc).hasSize(3);
      assertThat(resultsAsc.get(0).getComment()).isEqualTo("a-package");
      assertThat(resultsAsc.get(1).getComment()).isEqualTo("m-package");
      assertThat(resultsAsc.get(2).getComment()).isEqualTo("z-package");

      // Descending alphabetical order
      List<PolicyWaiver> resultsDesc = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          null,
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "-COMPONENT_SCOPE");

      assertThat(resultsDesc).hasSize(3);
      assertThat(resultsDesc.get(0).getComment()).isEqualTo("z-package");
      assertThat(resultsDesc.get(1).getComment()).isEqualTo("m-package");
      assertThat(resultsDesc.get(2).getComment()).isEqualTo("a-package");
    }
  }

  @Test
  public void testGetFilteredForDashboard_sortByPolicyName() {
    Policy policy1 = tempEntity.newPolicy(organization.getId(), "Alpha Security Policy", 5);
    Policy policy2 = tempEntity.newPolicy(organization.getId(), "Zeta Security Policy", 8);

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy1.getId(), organization.getId(), "w1");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy2.getId(), organization.getId(), "w2");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    tempEntity.newWaiver(waiver2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Ascending by policy name
      List<PolicyWaiver> resultsAsc = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "POLICY_NAME");

      assertThat(resultsAsc).hasSize(2);
      assertThat(resultsAsc.get(0).getComment()).isEqualTo("w1"); // Alpha policy
      assertThat(resultsAsc.get(1).getComment()).isEqualTo("w2"); // Zeta policy

      // Descending by policy name
      List<PolicyWaiver> resultsDesc = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "-POLICY_NAME");

      assertThat(resultsDesc).hasSize(2);
      assertThat(resultsDesc.get(0).getComment()).isEqualTo("w2"); // Zeta policy
      assertThat(resultsDesc.get(1).getComment()).isEqualTo("w1"); // Alpha policy
    }
  }

  @Test
  public void testGetFilteredForDashboard_sortByThreatLevel() {
    Policy policy1 = tempEntity.newPolicy(organization.getId(), "Low Threat Policy", 3);
    Policy policy2 = tempEntity.newPolicy(organization.getId(), "High Threat Policy", 9);

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy1.getId(), organization.getId(), "low");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy2.getId(), organization.getId(), "high");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    tempEntity.newWaiver(waiver2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Ascending by threat level (default)
      List<PolicyWaiver> resultsAsc = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "THREAT_LEVEL");

      assertThat(resultsAsc).hasSize(2);
      assertThat(resultsAsc.get(0).getComment()).isEqualTo("low"); // Threat level 3
      assertThat(resultsAsc.get(1).getComment()).isEqualTo("high"); // Threat level 9

      // Descending by threat level
      List<PolicyWaiver> resultsDesc = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "-THREAT_LEVEL");

      assertThat(resultsDesc).hasSize(2);
      assertThat(resultsDesc.get(0).getComment()).isEqualTo("high"); // Threat level 9
      assertThat(resultsDesc.get(1).getComment()).isEqualTo("low"); // Threat level 3
    }
  }

  @Test
  public void testGetFilteredForDashboard_nullOrderBy_defaultsToExpiryTimeAsc() {
    Policy policy = tempEntity.newPolicy(organization);
    Date now = new Date();
    Date expiry1 = Date.from(now.toInstant().plus(1, java.time.temporal.ChronoUnit.DAYS));
    Date expiry2 = Date.from(now.toInstant().plus(2, java.time.temporal.ChronoUnit.DAYS));

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "later");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    waiver1.setExpiryTime(expiry2);
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "earlier");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    waiver2.setExpiryTime(expiry1);
    tempEntity.newWaiver(waiver2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          null); // null orderBy

      assertThat(results).hasSize(2);
      // Default: ascending by expiry_time, nulls last
      assertThat(results.get(0).getComment()).isEqualTo("earlier");
      assertThat(results.get(1).getComment()).isEqualTo("later");
    }
  }

  @Test
  public void testGetFilteredForDashboard_unknownOrderBy_defaultsToExpiryTimeAsc() {
    Policy policy = tempEntity.newPolicy(organization);
    Date now = new Date();
    Date expiry1 = Date.from(now.toInstant().plus(1, java.time.temporal.ChronoUnit.DAYS));
    Date expiry2 = Date.from(now.toInstant().plus(2, java.time.temporal.ChronoUnit.DAYS));

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "later");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    waiver1.setExpiryTime(expiry2);
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "earlier");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    waiver2.setExpiryTime(expiry1);
    tempEntity.newWaiver(waiver2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "UNKNOWN_FIELD"); // Unknown orderBy

      assertThat(results).hasSize(2);
      // Default: ascending by expiry_time, nulls last
      assertThat(results.get(0).getComment()).isEqualTo("earlier");
      assertThat(results.get(1).getComment()).isEqualTo("later");
    }
  }

  // ========== Edge Cases: filteredPolicyIds ==========

  @Test
  public void testGetFilteredForDashboard_emptyFilteredPolicyIds_returnsAll() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "w1");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "w2");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    tempEntity.newWaiver(waiver2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Empty set should NOT filter - returns all matching waivers
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          Collections.emptySet(),
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(results).hasSize(2);
    }
  }

  @Test
  public void testGetFilteredForDashboard_nullFilteredPolicyIds_returnsAll() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "w1");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy.getId(), organization.getId(), "w2");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    tempEntity.newWaiver(waiver2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // null filteredPolicyIds should NOT filter - returns all matching waivers
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          null,
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(results).hasSize(2);
    }
  }

  @Test
  public void testGetFilteredForDashboard_nonExistentPolicyIds_returnsEmpty() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiver waiver = new PolicyWaiver("hash1", policy.getId(), organization.getId(), "w1");
    waiver.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    tempEntity.newWaiver(waiver);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Filter by non-existent policy IDs
      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          Collections.singleton("non-existent-policy-id"),
          0,
          100,
          "EXPIRATION_DATE");

      assertThat(results).isEmpty();
    }
  }

  // ========== Combined Filter + Sort Tests ==========

  @Test
  public void testGetFilteredForDashboard_combinedFilterAndSort() {
    Policy policy1 = tempEntity.newPolicy(organization.getId(), "Policy Alpha", 5);
    Policy policy2 = tempEntity.newPolicy(organization.getId(), "Policy Beta", 8);
    Policy policy3 = tempEntity.newPolicy(organization.getId(), "Policy Gamma", 3);

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy1.getId(), organization.getId(), "w1");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy2.getId(), organization.getId(), "w2");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    tempEntity.newWaiver(waiver2);

    PolicyWaiver waiver3 = new PolicyWaiver("hash3", policy3.getId(), organization.getId(), "w3");
    waiver3.setAssociatedPackageUrl("pkg:maven/com.example/component@3.0.0?type=jar");
    tempEntity.newWaiver(waiver3);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Filter by policy1 and policy3, sort by POLICY_NAME ascending
      Set<String> filteredPolicyIds = new HashSet<>();
      filteredPolicyIds.add(policy1.getId());
      filteredPolicyIds.add(policy3.getId());

      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          filteredPolicyIds,
          0,
          100,
          "POLICY_NAME");

      assertThat(results).hasSize(2);
      // Sorted by policy name: Alpha (w1) < Gamma (w3)
      assertThat(results.get(0).getComment()).isEqualTo("w1");
      assertThat(results.get(1).getComment()).isEqualTo("w3");
    }
  }

  @Test
  public void testGetFilteredForDashboard_filteredPolicyIdsWithThreatLevelSort() {
    Policy policy1 = tempEntity.newPolicy(organization.getId(), "Low", 2);
    Policy policy2 = tempEntity.newPolicy(organization.getId(), "Medium", 5);
    Policy policy3 = tempEntity.newPolicy(organization.getId(), "High", 9);

    PolicyWaiver waiver1 = new PolicyWaiver("hash1", policy1.getId(), organization.getId(), "w1");
    waiver1.setAssociatedPackageUrl("pkg:maven/com.example/component@1.0.0?type=jar");
    tempEntity.newWaiver(waiver1);

    PolicyWaiver waiver2 = new PolicyWaiver("hash2", policy2.getId(), organization.getId(), "w2");
    waiver2.setAssociatedPackageUrl("pkg:maven/com.example/component@2.0.0?type=jar");
    tempEntity.newWaiver(waiver2);

    PolicyWaiver waiver3 = new PolicyWaiver("hash3", policy3.getId(), organization.getId(), "w3");
    waiver3.setAssociatedPackageUrl("pkg:maven/com.example/component@3.0.0?type=jar");
    tempEntity.newWaiver(waiver3);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Filter by policy1 and policy3, sort by THREAT_LEVEL descending
      Set<String> filteredPolicyIds = new HashSet<>();
      filteredPolicyIds.add(policy1.getId());
      filteredPolicyIds.add(policy3.getId());

      List<PolicyWaiver> results = dao.getFilteredForDashboard(
          tx,
          Collections.singleton(organization.getId()),
          "component",
          null,
          false,
          null,
          null,
          false,
          filteredPolicyIds,
          0,
          100,
          "-THREAT_LEVEL");

      assertThat(results).hasSize(2);
      // Sorted by threat level descending: High (w3, level 9) > Low (w1, level 2)
      assertThat(results.get(0).getComment()).isEqualTo("w3");
      assertThat(results.get(1).getComment()).isEqualTo("w1");
    }
  }
}
