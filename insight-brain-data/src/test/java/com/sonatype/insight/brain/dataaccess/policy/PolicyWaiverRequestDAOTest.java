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
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus.REJECTED;
import static com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus.REQUESTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyWaiverRequestDAOTest
    extends AbstractDbDAOTest
{
  private PolicyWaiverRequestDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createPolicyWaiverRequestDAO();
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
    String reviewerId = "reviewerId";
    String reviewerName = "reviewerName";
    String requesterId = "requesterId";
    String requesterName = "requesterName";
    Date reviewTime = new Date();
    String noteToReviewer = "note to reviewer";
    String rejectionReason = "";

    ComponentMatcherStrategyForWaiver componentMatcherStrategy = ComponentMatcherStrategyForWaiver.DEFAULT;

    // Create
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest(hash, policyId, ownerId, associatedPackagedUrl, componentMatcherStrategy, comment);
    policyWaiverRequest.setConstraintFacts(createRandomConstraintFacts());
    Date expiryTime = DateTime.now().plusWeeks(1).toDate();
    policyWaiverRequest.setExpiryTime(expiryTime);
    policyWaiverRequest.setReviewerId(reviewerId);
    policyWaiverRequest.setReviewerName(reviewerName);
    policyWaiverRequest.setRequesterId(requesterId);
    policyWaiverRequest.setRequesterName(requesterName);
    policyWaiverRequest.setReviewTime(reviewTime);
    policyWaiverRequest.setNoteToReviewer(noteToReviewer);
    policyWaiverRequest.setRejectionReason(rejectionReason);
    policyWaiverRequest.setPolicyViolationId("policyViolationId");

    Date beforeInsert = new Date();
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest);
    Date afterInsert = new Date();
    assertThat(policyWaiverRequest.getId()).isNotNull();
    assertThat(policyWaiverRequest.getRequestTime()).isNotNull();
    Date createTime = policyWaiverRequest.getRequestTime();
    assertThat(createTime).isAfterOrEqualTo(beforeInsert).isBeforeOrEqualTo(afterInsert);
    assertThat(policyWaiverRequest.getHash()).isEqualTo(truncatedHash);

    // Read
    PolicyWaiverRequest foundPolicyWaiverRequest = dao.getById(policyWaiverRequest.getId());
    JPA.assertEntityEquals(foundPolicyWaiverRequest, policyWaiverRequest);

    // Update
    String updateComment = "Updated comment";
    policyWaiverRequest.setComment(updateComment);
    PolicyWaiverRequestStatus status = PolicyWaiverRequestStatus.REJECTED;
    reviewerId = "reviewerId2";
    reviewerName = "reviewerName2";
    requesterId = "requesterId2";
    requesterName = "requesterName2";
    reviewTime = DateTime.now().minusMinutes(5).toDate();
    policyWaiverRequest.setStatus(status);
    policyWaiverRequest.setReviewerId(reviewerId);
    policyWaiverRequest.setReviewerName(reviewerName);
    policyWaiverRequest.setRequesterId(requesterId);
    policyWaiverRequest.setRequesterName(requesterName);
    policyWaiverRequest.setReviewTime(reviewTime);
    policyWaiverRequest.setRejectionReason(rejectionReason);

    dao.update(policyWaiverRequest);

    // Read
    foundPolicyWaiverRequest = dao.getById(policyWaiverRequest.getId());
    JPA.assertEntityEquals(foundPolicyWaiverRequest, policyWaiverRequest);

    // Delete
    dao.delete(policyWaiverRequest);

    policyWaiverRequest = dao.getById(policyWaiverRequest.getId());
    assertThat(policyWaiverRequest).isNull();
  }

  private List<ConstraintFact> createRandomConstraintFacts() {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact.setTriggerJson(
        "{\"conditionIndex\":1,\"trigger\":{\"refId\":\"" + UUID.randomUUID() + "\",\"severity\":5.7}}");
    ConstraintFact constraintFact =
        new ConstraintFact("constraint Id", "constraint Name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);
    return Collections.singletonList(constraintFact);
  }

  @Test
  public void testInsert_CommentTooLong() {
    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization);
    String ownerId = organization.getId();
    String comment = StringUtils.repeat("X", 1001);
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest(hash, policy.getId(), ownerId, comment);

    assertThatThrownBy(() -> tempEntity.newPolicyWaiverRequest(policyWaiverRequest))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Comment length must not exceed 1000 characters.");
  }

  @Test
  public void testInsert_StatusNull() {
    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization);
    String ownerId = organization.getId();
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setPolicyId(policy.getId())
        .setOwnerId(ownerId)
        .setHash(hash)
        .setPolicyViolationId("policyViolationId")
        .setStatus(null);

    tempEntity.newPolicyWaiverRequest(policyWaiverRequest);

    assertThat(policyWaiverRequest.getStatus()).isEqualTo(REQUESTED);
  }

  @Test
  public void testInsert_ComponentMatchStrategyNotProvided_HashNotNull() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest().setPolicyId(policy.getId())
            .setOwnerId(organization.getId())
            .setHash("testHash")
            .setPolicyViolationId("policyViolationId")
            .setStatus(null);
    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isNull();

    tempEntity.newPolicyWaiverRequest(policyWaiverRequest);

    policyWaiverRequest = dao.getById(policyWaiverRequest.getId());
    assertThat(policyWaiverRequest.getComponentMatchStrategy())
        .isEqualTo(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT);
  }

  @Test
  public void testInsert_ComponentMatchStrategyNotProvided_HashNull() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setPolicyId(policy.getId())
        .setOwnerId(organization.getId())
        .setPolicyViolationId("policyViolationId")
        .setStatus(null);
    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isNull();

    tempEntity.newPolicyWaiverRequest(policyWaiverRequest);

    policyWaiverRequest = dao.getById(policyWaiverRequest.getId());
    assertThat(policyWaiverRequest.getComponentMatchStrategy())
        .isEqualTo(ComponentMatcherStrategyForWaiver.ALL_COMPONENTS);
  }

  @Test
  public void testInsert_Duplicate_ComponentLevel() {
    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    PolicyWaiverRequest policyWaiverRequest1 =
        new PolicyWaiverRequest().setOwnerId(ownerId)
            .setPolicyId(policyId)
            .setHash(hash)
            .setConstraintFacts(constraintFacts)
            .setPolicyViolationId("policyViolationId");
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest1);

    PolicyWaiverRequest policyWaiverRequest2 =
        new PolicyWaiverRequest().setOwnerId(ownerId)
            .setPolicyId(policyId)
            .setHash(hash)
            .setConstraintFacts(constraintFacts)
            .setPolicyViolationId("policyViolationId");
    assertThatThrownBy(() -> tempEntity.newPolicyWaiverRequest(policyWaiverRequest2))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("This policy waiver request already exists.");
  }

  @Test
  public void testInsert_Duplicate_PolicyLevel() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    List<ConstraintFact> constraintFacts = createRandomConstraintFacts();
    PolicyWaiverRequest policyWaiverRequest1 = new PolicyWaiverRequest().setOwnerId(ownerId)
        .setPolicyId(policyId)
        .setConstraintFacts(constraintFacts)
        .setPolicyViolationId("policyViolationId");
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest1);

    PolicyWaiverRequest policyWaiverRequest2 = new PolicyWaiverRequest().setOwnerId(ownerId)
        .setPolicyId(policyId)
        .setConstraintFacts(constraintFacts)
        .setPolicyViolationId("policyViolationId");
    assertThatThrownBy(() -> tempEntity.newPolicyWaiverRequest(policyWaiverRequest2))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("This policy waiver request already exists.");
  }

  @Test
  public void testUpdate_ComponentMatchStrategyNotProvided_HashNotNull() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("testHash")
        .setPolicyId(policy.getId())
        .setOwnerId(organization.getId())
        .setComment("test comment")
        .setPolicyViolationId("policyViolationId"));
    assertThat(policyWaiverRequest.getComponentMatchStrategy())
        .isEqualTo(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT);

    policyWaiverRequest.setComponentMatchStrategy(null);
    dao.update(policyWaiverRequest);
    policyWaiverRequest = dao.getById(policyWaiverRequest.getId());
    assertThat(policyWaiverRequest.getComponentMatchStrategy())
        .isEqualTo(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT);
  }

  @Test
  public void testUpdate_ComponentMatchStrategyNotProvided_HashNull() {
    Policy policy = tempEntity.newPolicy(organization);

    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setPolicyId(policy.getId())
            .setOwnerId(organization.getId())
            .setComment("test comment")
            .setPolicyViolationId("policyViolationId"));
    assertThat(policyWaiverRequest.getComponentMatchStrategy())
        .isEqualTo(ComponentMatcherStrategyForWaiver.ALL_COMPONENTS);

    policyWaiverRequest.setComponentMatchStrategy(null);
    dao.update(policyWaiverRequest);
    policyWaiverRequest = dao.getById(policyWaiverRequest.getId());
    assertThat(policyWaiverRequest.getComponentMatchStrategy())
        .isEqualTo(ComponentMatcherStrategyForWaiver.ALL_COMPONENTS);
  }

  @Test
  public void testUpdate_CommentTooLong() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setPolicyId(policy.getId())
        .setOwnerId(application.getId())
        .setPolicyViolationId("policyViolationId"));
    String comment = StringUtils.repeat("X", 1001);
    policyWaiverRequest.setComment(comment);

    assertThatThrownBy(() -> dao.update(policyWaiverRequest)).isInstanceOf(BadRequestException.class)
        .hasMessage("Comment length must not exceed 1000 characters.");

    comment = comment.substring(0, 1000);
    policyWaiverRequest.setComment(comment);
    dao.update(policyWaiverRequest);
    assertThat(dao.getById(policyWaiverRequest.getId()).getComment()).isEqualTo(comment);
  }

  @Test
  public void testUpdate_StatusNull() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiverRequest policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setPolicyId(policy.getId())
        .setOwnerId(application.getId())
        .setPolicyViolationId("policyViolationId"));
    policyWaiverRequest.setStatus(null);

    assertThatThrownBy(() -> dao.update(policyWaiverRequest)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot create a policy waiver request with null status.");
  }

  @Test
  public void testUpdate_StatusAlreadyApproved() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiverRequest policyWaiverRequest =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setPolicyId(policy.getId())
            .setOwnerId(application.getId())
            .setStatus(PolicyWaiverRequestStatus.APPROVED)
            .setPolicyViolationId("policyViolationId"));

    assertThatThrownBy(() -> dao.update(policyWaiverRequest)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot update an approved policy waiver request.");
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
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash(hash1)
        .setPolicyId(policyId)
        .setOwnerId(ownerId)
        .setConstraintFacts(constraintFacts)
        .setComment(comment)
        .setPolicyViolationId("policyViolationId1"));
    PolicyWaiverRequest policyWaiverRequest2 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash(hash2)
            .setPolicyId(policyId)
            .setOwnerId(ownerId)
            .setConstraintFacts(constraintFacts)
            .setComment(comment)
            .setPolicyViolationId("policyViolationId2"));

    policyWaiverRequest2.setHash(hash1);
    assertThatThrownBy(() -> dao.update(policyWaiverRequest2)).isInstanceOf(BadRequestException.class)
        .hasMessage("A policy waiver request for the same policy violation already exists.");
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
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setPolicyId(policyId1)
        .setOwnerId(ownerId)
        .setConstraintFacts(constraintFacts)
        .setComment(comment)
        .setPolicyViolationId("policyViolationId1"));
    PolicyWaiverRequest policyWaiverRequest2 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setPolicyId(policyId2)
            .setOwnerId(ownerId)
            .setConstraintFacts(constraintFacts)
            .setComment(comment)
            .setPolicyViolationId("policyViolationId2"));

    policyWaiverRequest2.setPolicyId(policyId1);
    assertThatThrownBy(() -> dao.update(policyWaiverRequest2)).isInstanceOf(BadRequestException.class)
        .hasMessage("A policy waiver request for the same policy violation already exists.");
  }

  @Test
  public void testGetByOwnerId() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    PolicyWaiverRequest policyWaiverRequestRequested =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("requested")
            .setPolicyId(policyId)
            .setOwnerId(ownerId)
            .setPolicyViolationId("policyViolationId")
            .setStatus(REQUESTED));
    PolicyWaiverRequest policyWaiverRequestRejected = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("rejected")
        .setPolicyId(policyId)
        .setOwnerId(ownerId)
        .setPolicyViolationId("policyViolationId")
        .setStatus(REJECTED));

    assertThat(dao.getByOwnerId(ownerId)).extracting(PolicyWaiverRequest::getId)
        .containsExactly(policyWaiverRequestRequested.getId(), policyWaiverRequestRejected.getId());
  }

  @Test
  public void testGetByPolicyId() {
    Policy policy1 = tempEntity.newPolicy(application);
    Policy policy2 = tempEntity.newPolicy(organization);

    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("requested")
        .setPolicyId(policy1.getId())
        .setOwnerId(application.getId())
        .setStatus(REQUESTED)
        .setPolicyViolationId("policyViolationId"));
    PolicyWaiverRequest policy2RequestedWaiverRequest =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("requested")
            .setPolicyId(policy2.getId())
            .setOwnerId(application.getId())
            .setPolicyViolationId("policyViolationId")
            .setStatus(REQUESTED));
    PolicyWaiverRequest policy2RejectedWaiverRequest =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("rejected")
            .setPolicyId(policy2.getId())
            .setOwnerId(application.getId())
            .setPolicyViolationId("policyViolationId")
            .setStatus(REJECTED));

    assertThat(dao.getByPolicyId(policy2.getId())).extracting(PolicyWaiverRequest::getId)
        .contains(policy2RequestedWaiverRequest.getId(), policy2RejectedWaiverRequest.getId());
  }

  @Test
  public void testGetActiveByPolicyId() {
    Policy policy1 = tempEntity.newPolicy(application);
    Policy policy2 = tempEntity.newPolicy(application);

    // Active waiver request for policy1
    PolicyWaiverRequest policyWaiverRequest1Active =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash1")
            .setPolicyId(policy1.getId())
            .setOwnerId(application.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 1)));
    // Inactive waiver request for policy1
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash2")
        .setPolicyId(policy1.getId())
        .setOwnerId(application.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), -1)));
    // Active waiver request for policy2
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash3")
        .setPolicyId(policy2.getId())
        .setOwnerId(application.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 1)));

    JPA.assertContainsEntitiesExactlyInAnyOrder(dao.getActiveByPolicyId(policy1.getId()), policyWaiverRequest1Active);
  }

  @Test
  public void testGetByIdAndOwnerIdNotNull() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();

    PolicyWaiverRequest policyWaiverRequestApp =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("testHash1")
            .setPolicyId(policyId)
            .setOwnerId(application.getId())
            .setPolicyViolationId("policyViolationId"));
    PolicyWaiverRequest policyWaiverRequestOrg =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("testHash2")
            .setPolicyId(policyId)
            .setOwnerId(organization.getId())
            .setPolicyViolationId("policyViolationId"));

    JPA.assertEntityEquals(dao.getByIdAndOwnerIdNotNull(policyWaiverRequestApp.getId(), application.getId()),
        policyWaiverRequestApp);
    JPA.assertEntityEquals(dao.getByIdAndOwnerIdNotNull(policyWaiverRequestOrg.getId(), organization.getId()),
        policyWaiverRequestOrg);
  }

  @Test
  public void testGetByIdAndOwnerIdNotNull_IncorrectParent() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();

    PolicyWaiverRequest policyWaiverRequest =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("testHash1")
            .setPolicyId(policyId)
            .setOwnerId(application.getId())
            .setPolicyViolationId("policyViolationId"));

    Application otherApp = tempEntity.newApplication(organization.getId());

    assertThatThrownBy(() -> {
      dao.getByIdAndOwnerIdNotNull(policyWaiverRequest.getId(), otherApp.getId());
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a policy waiver request with ID "
            + policyWaiverRequest.getId() + " for owner " + otherApp.getId() + ".");
  }

  @Test
  public void testGetByOwnerHierarchyAndPolicyId() {
    Date now = new Date();
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();
    String ownerId = organization.getId();
    PolicyWaiverRequest noExpiryWaiverRequest = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setHash("hash1").setPolicyId(policyId).setOwnerId(ownerId));
    PolicyWaiverRequest expiringWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash2")
        .setPolicyId(policyId)
        .setOwnerId(ownerId)
        .setExpiryTime(DateUtils.addHours(now, 1)));
    PolicyWaiverRequest expiredWaiverRequest =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash3")
            .setPolicyId(policyId)
            .setOwnerId(organization.getParentOrganizationId())
            .setExpiryTime(DateUtils.addHours(now, -1)));

    // PolicyWaiverRequest for unrelated owner
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash4")
        .setPolicyId(policyId)
        .setOwnerId(application.getId()));
    // PolicyWaiverRequest for another policy
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash4")
        .setPolicyId(tempEntity.newPolicy(organization).getId())
        .setOwnerId(organization.getId()));

    assertThat(dao.getByOwnerHierarchyAndPolicyId(organization, policyId)).extracting(PolicyWaiverRequest::getId)
        .containsExactlyInAnyOrder(noExpiryWaiverRequest.getId(), expiringWaiverRequest.getId(),
            expiredWaiverRequest.getId());
  }
}
