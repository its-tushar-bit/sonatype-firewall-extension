/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyWaiverDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testGetByIdNotNull() {
    assertThatThrownBy(() -> {
      new PolicyWaiverDAO().getByIdNotNull("fake id");
    }).isInstanceOf(NotFoundException.class).hasMessage("Cannot find a policy waiver with ID fake id.");
  }

  @Test
  public void testCRUD() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash = "123456789012345678901";
    assertThat(hash.length()).isGreaterThan(20);
    String truncatedHash = hash.substring(0, 20);
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "My comment";

    // Create
    PolicyWaiver policyWaiver = new PolicyWaiver(hash, policyId, ownerId, comment);
    policyWaiver.setConstraintFacts(createRandomConstraintFacts());
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
    assertPolicyWaiver(truncatedHash, policyId, ownerId, comment, createTime, policyWaiver);

    // Update
    String updateComment = "Updated comment";
    policyWaiver.setComment(updateComment);
    dao.update(policyWaiver);

    // Read
    policyWaiver = dao.getById(policyWaiver.getId());
    assertThat(policyWaiver).isNotNull();
    assertPolicyWaiver(truncatedHash, policyId, ownerId, updateComment, createTime, policyWaiver);

    // Delete
    dao.delete(policyWaiver);

    policyWaiver = dao.getById(policyWaiver.getId());
    assertThat(policyWaiver).isNull();
  }

  private void assertPolicyWaiver(String hash,
                                  String policyId,
                                  String ownerId,
                                  String comment,
                                  Date createTime,
                                  PolicyWaiver actual)
  {
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getComment()).isEqualTo(comment);
    assertThat(actual.getCreateTime()).isEqualTo(createTime);
  }

  private void assertPolicyWaiver(PolicyWaiver expected, PolicyWaiver actual) {
    assertThat(actual.getHash()).isEqualTo(expected.getHash());
    assertThat(actual.getPolicyId()).isEqualTo(expected.getPolicyId());
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
    assertThat(actual.getComment()).isEqualTo(expected.getComment());
  }

  @Test
  public void testInsert_Duplicate_ComponentLevel() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(hash, policyId, ownerId, constraintFacts, comment);
    dao.insert(policyWaiver1);

    PolicyWaiver policyWaiver2 = new PolicyWaiver(hash, policyId, ownerId, constraintFacts, comment);
    assertThatThrownBy(() -> {
      dao.insert(policyWaiver2);
    }).isInstanceOf(BadRequestException.class).hasMessage("This policy waiver already exists.");

    dao.delete(policyWaiver1);
  }

  @Test
  public void testInsert_Duplicate_PolicyLevel() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(null /* hash */, policyId, ownerId, constraintFacts, comment);
    dao.insert(policyWaiver1);

    PolicyWaiver policyWaiver2 = new PolicyWaiver(null /* hash */, policyId, ownerId, constraintFacts, comment);
    assertThatThrownBy(() -> {
      dao.insert(policyWaiver2);
    }).isInstanceOf(BadRequestException.class).hasMessage("This policy waiver already exists.");

    dao.delete(policyWaiver1);
  }

  @Test
  public void testGetApplicableByOwnerId() {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();
    Policy policyApp = tempEntity.newPolicy(application);
    Policy policyOrg = tempEntity.newPolicy(organization);
    Policy policyParentOrg = tempEntity.newPolicy(organization.getParentOrganizationId());

    PolicyWaiver policyWaiverParentOrg = tempEntity.newWaiver("0", policyParentOrg.getId(),
        organization.getParentOrganizationId());
    PolicyWaiver policyWaiverOrg = tempEntity.newWaiver("1", policyOrg.getId(), organization.getId());
    PolicyWaiver policyWaiverApp = tempEntity.newWaiver("2", policyApp.getId(), application.getId());

    // Assert for application
    List<PolicyWaiver> policyWaivers = dao.getApplicableByOwnerId(application.getId());
    assertThat(policyWaivers).hasSize(3);
    assertPolicyWaiver(policyWaiverParentOrg, policyWaivers.get(0));
    assertPolicyWaiver(policyWaiverOrg, policyWaivers.get(1));
    assertPolicyWaiver(policyWaiverApp, policyWaivers.get(2));

    // Assert for organization
    policyWaivers = dao.getApplicableByOwnerId(organization.getId());
    assertThat(policyWaivers).hasSize(2);
    assertPolicyWaiver(policyWaiverParentOrg, policyWaivers.get(0));
    assertPolicyWaiver(policyWaiverOrg, policyWaivers.get(1));

    // Assert for parent organization
    policyWaivers = dao.getApplicableByOwnerId(organization.getParentOrganizationId());
    assertThat(policyWaivers).hasSize(1);
    assertPolicyWaiver(policyWaiverParentOrg, policyWaivers.get(0));
  }

  @Test
  public void testInsert_CommentTooLong() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash = "12345678901234567890";
    String policyId = "MyPolicyId";
    String ownerId = organization.getId();
    String comment = StringUtils.repeat("X", 1001);
    PolicyWaiver policyWaiver1 = new PolicyWaiver(hash, policyId, ownerId, comment);

    assertThatThrownBy(() -> {
      dao.insert(policyWaiver1);
    }).isInstanceOf(BadRequestException.class).hasMessage("Comment length must not exceed 1000 characters.");

    dao.delete(policyWaiver1);
  }

  @Test
  public void testUpdate_CommentTooLong() throws Exception {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
    PolicyWaiverDAO dao = new PolicyWaiverDAO();
    String comment = StringUtils.repeat("X", 1001);
    policyWaiver.setComment(comment);

    assertThatThrownBy(() -> {
      dao.update(policyWaiver);
    }).isInstanceOf(BadRequestException.class).hasMessage("Comment length must not exceed 1000 characters.");

    comment = comment.substring(0, 1000);
    policyWaiver.setComment(comment);
    dao.update(policyWaiver);
    assertThat(dao.getById(policyWaiver.getId()).getComment()).isEqualTo(comment);
  }

  @Test
  public void testUpdate_Duplicate_ComponentLevel() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

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
    assertThatThrownBy(() -> {
      dao.update(policyWaiver2);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("A policy waiver for the same policy violation already exists.");
  }

  @Test
  public void testUpdate_Duplicate_PolicyLevel() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

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
    assertThatThrownBy(() -> {
      dao.update(policyWaiver2);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("A policy waiver for the same policy violation already exists.");
  }

  @Test
  public void testGetByOwnerIdAndHash() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "Just testing";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(hash, policyId, ownerId, comment);
    dao.insert(policyWaiver1);
    PolicyWaiver policyWaiver2 = new PolicyWaiver(policyId, ownerId, comment);
    dao.insert(policyWaiver2);

    List<PolicyWaiver> waivers = dao.getByOwnerIdAndHash(ownerId, hash);
    dao.delete(policyWaiver1);
    dao.delete(policyWaiver2);

    assertThat(waivers).extracting(PolicyWaiver::getId).containsExactly(policyWaiver1.getId(), policyWaiver2.getId());
  }

  @Test
  public void testDeleteDoesNotCascadeToWaivedPolicyViolation() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("ababababab", policy.getId(), applicationId);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyWaiverDAOTest");
    PolicyViolation waivedPolicyViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, policyWaiver);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    assertThat(policyViolationDAO.getById(waivedPolicyViolation.getId())).isNotNull();

    new PolicyWaiverDAO().delete(policyWaiver);
    assertThat(policyViolationDAO.getById(waivedPolicyViolation.getId())).isNotNull();
  }

  @Test
  public void testGetByPolicyIdAndOwnerIds() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

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
    PolicyWaiverDAO dao = new PolicyWaiverDAO();
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
  public void testGetByHashAndPolicyIdAndOwnerIdAndConstraintFacts() {
    String hash = "hash";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(hash, policyId, ownerId, constraintFacts, comment);
    tempEntity.newWaiver(hash, policyId, ownerId, createRandomConstraintFacts(), comment);

    PolicyWaiverDAO dao = new PolicyWaiverDAO();
    try (TransactionContext tx = dao.createTransactionContext()) {
      PolicyWaiver foundPolicyWaiver = dao.getByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId, ownerId,
          constraintFacts);
      assertThat(foundPolicyWaiver.getId()).isEqualTo(policyWaiver1.getId());
    }
  }

  @Test
  public void testGetByHashAndPolicyIdAndOwnerIdAndConstraintFacts_NullConstraintFacts() {
    String hash = "hash";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(hash, policyId, ownerId, null /* constraintFacts */, comment);

    PolicyWaiverDAO dao = new PolicyWaiverDAO();
    try (TransactionContext tx = dao.createTransactionContext()) {
      // Get using null constraint facts
      PolicyWaiver foundPolicyWaiver = dao.getByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId, ownerId,
          null /* constraintFacts */);
      assertThat(foundPolicyWaiver.getId()).isEqualTo(policyWaiver1.getId());

      // Get using not null constraint facts
      foundPolicyWaiver = dao.getByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, hash, policyId, ownerId,
          createRandomConstraintFacts());
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
}
