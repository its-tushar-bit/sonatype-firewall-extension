/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.PolicyWaiverResource.AppliedWaivers;
import com.sonatype.insight.brain.policy.PolicyWaiverResource.PolicyWaiverDTO;
import com.sonatype.insight.brain.policy.PolicyWaiverResource.WaiversByOwner;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyWaiverResourceTest
    extends AbstractResourceTest
{
  private PolicyTagDAO policyTagDAO;

  private PolicyDAO policyDAO;

  private OrganizationDAO organizationDAO;

  private OwnerDAO ownerDAO;

  @Before
  public void setUp() {
    policyTagDAO = lookup(PolicyTagDAO.class);
    policyDAO = lookup(PolicyDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    ownerDAO = lookup(OwnerDAO.class);
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PolicyWaiverResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  private void assertWaiversByOwner(
      Owner owner,
      String policyId,
      String waiverComment,
      String constraintFactsJson,
      List<ConstraintFact> constraints,
      String creatorId,
      String creatorName,
      String reasonText,
      WaiversByOwner actual)
  {
    String expectedOwnerId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();
    assertThat(actual.ownerId).isEqualTo(expectedOwnerId);
    assertThat(actual.ownerName).isEqualTo(owner.getName());
    assertThat(actual.ownerType).isEqualTo(owner.getType());
    assertThat(actual.waivers).hasSize(1);
    assertPolicyWaiverDTO(policyId, expectedOwnerId, waiverComment, constraintFactsJson, constraints,
        creatorId, creatorName, reasonText, actual.waivers.get(0));
  }

  private void assertWaiversByOwner(
      Owner owner,
      String policyId,
      String waiverComment,
      String constraintFactsJson,
      List<ConstraintFact> constraints,
      String creatorId,
      String creatorName,
      String reasonText,
      Date createTime,
      String hash,
      String associatedPackageUrl,
      PolicyWaiver.ComponentMatcherStrategyForWaiver componentMatchStrategy,
      boolean expireWhenRemediationAvailable,
      Date expiryTime,
      WaiversByOwner actual)
  {
    String expectedOwnerId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();
    assertThat(actual.ownerId).isEqualTo(expectedOwnerId);
    assertThat(actual.ownerName).isEqualTo(owner.getName());
    assertThat(actual.ownerType).isEqualTo(owner.getType());
    assertThat(actual.waivers).hasSize(1);
    assertPolicyWaiverDTO(
        policyId,
        expectedOwnerId,
        waiverComment,
        constraintFactsJson,
        constraints,
        creatorId,
        creatorName,
        reasonText,
        createTime,
        hash,
        associatedPackageUrl,
        componentMatchStrategy,
        expireWhenRemediationAvailable,
        expiryTime,
        actual.waivers.get(0));
  }

  @Test
  public void testGetPolicyWaiversByHash_Application() throws Exception {
    testGetPolicyWaiversByHash(tempEntity.newApplicationWithParent("PolicyWaiverResourceTest_AppId1"));
  }

  @Test
  public void testGetPolicyWaiversByHash_Application_TimeBasedWaivers() throws Exception {
    testGetPolicyWaiversByHash_TimeBasedWaivers(tempEntity.newApplicationWithParent("PolicyWaiverResourceTest_AppId1"));
  }

  @Test
  public void testGetPolicyWaiversByHash_Repository() throws Exception {
    testGetPolicyWaiversByHash(tempEntity.newRepository());
  }

  @Test
  public void testGetPolicyWaiversByHash_Repository_TimeBasedWaivers() throws Exception {
    testGetPolicyWaiversByHash_TimeBasedWaivers(tempEntity.newRepository());
  }

  private void testGetPolicyWaiversByHash(Owner owner) throws Exception {
    OwnerDAO ownerDAO = this.ownerDAO;
    Owner parent = ownerDAO.getById(owner.getParentOwnerId());
    Owner grandparent = ownerDAO.getById(parent.getParentOwnerId());

    Policy policy = tempEntity.newPolicy(grandparent);
    String hash = "12345678901234567890";

    String restId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();

    // Verify owner level
    PolicyWaiver ownerWaiver =
        tempEntity.newWaiver(hash, policy.getId(), owner.getId(), null, EXACT_COMPONENT, "My comment");
    HttpResponse response = restRequest(owner.getType(), restId).path("component", hash).get();
    assertResponseStatus(200, response);
    AppliedWaivers waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(
        owner,
        policy.getId(),
        "My comment",
        ownerWaiver.getConstraintFactsJson(),
        ownerWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null, waivers.waiversByOwner.get(0));
    response = restRequest(parent.getType(), parent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(0);
    response = restRequest(grandparent.getType(), grandparent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(0);

    // Verify parent owner level
    PolicyWaiver parentOwnerWaiver =
        tempEntity.newWaiver(hash, policy.getId(), parent.getId(), null, EXACT_COMPONENT, "My comment");
    response = restRequest(owner.getType(), restId).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(2);
    assertWaiversByOwner(owner, policy.getId(), "My comment", ownerWaiver.getConstraintFactsJson(),
        ownerWaiver.getConstraintFacts(), "testuser", "Test User", null, waivers.waiversByOwner.get(0));
    assertWaiversByOwner(parent, policy.getId(), "My comment", parentOwnerWaiver.getConstraintFactsJson(),
        parentOwnerWaiver.getConstraintFacts(), "testuser", "Test User", null, waivers.waiversByOwner.get(1));
    response = restRequest(parent.getType(), parent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(parent, policy.getId(), "My comment", parentOwnerWaiver.getConstraintFactsJson(),
        parentOwnerWaiver.getConstraintFacts(), "testuser", "Test User", null, waivers.waiversByOwner.get(0));
    response = restRequest(grandparent.getType(), grandparent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(0);

    // Verify grandparent organization level
    PolicyWaiver grandparentOwnerWaiver =
        tempEntity.newWaiver(hash, policy.getId(), grandparent.getId(), null, EXACT_COMPONENT, "My comment");
    response = restRequest(owner.getType(), restId).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(3);
    assertWaiversByOwner(owner, policy.getId(), "My comment", ownerWaiver.getConstraintFactsJson(),
        ownerWaiver.getConstraintFacts(), "testuser", "Test User", null, waivers.waiversByOwner.get(0));
    assertWaiversByOwner(parent, policy.getId(), "My comment", parentOwnerWaiver.getConstraintFactsJson(),
        parentOwnerWaiver.getConstraintFacts(), "testuser", "Test User", null, waivers.waiversByOwner.get(1));
    assertWaiversByOwner(grandparent, policy.getId(), "My comment", grandparentOwnerWaiver.getConstraintFactsJson(),
        grandparentOwnerWaiver.getConstraintFacts(), "testuser", "Test User", null, waivers.waiversByOwner.get(2));
    response = restRequest(parent.getType(), parent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(2);
    assertWaiversByOwner(parent, policy.getId(), "My comment", parentOwnerWaiver.getConstraintFactsJson(),
        parentOwnerWaiver.getConstraintFacts(), "testuser", "Test User", null, waivers.waiversByOwner.get(0));
    assertWaiversByOwner(grandparent, policy.getId(), "My comment", grandparentOwnerWaiver.getConstraintFactsJson(),
        grandparentOwnerWaiver.getConstraintFacts(), "testuser", "Test User", null, waivers.waiversByOwner.get(1));
    response = restRequest(grandparent.getType(), grandparent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(grandparent, policy.getId(), "My comment", grandparentOwnerWaiver.getConstraintFactsJson(),
        grandparentOwnerWaiver.getConstraintFacts(), "testuser", "Test User", null, waivers.waiversByOwner.get(0));
  }

  private void testGetPolicyWaiversByHash_TimeBasedWaivers(Owner owner) throws Exception {
    DateTime now = DateTime.now();
    OwnerDAO ownerDAO = this.ownerDAO;
    Owner parent = ownerDAO.getById(owner.getParentOwnerId());
    Owner grandparent = ownerDAO.getById(parent.getParentOwnerId());

    Policy policy1 = tempEntity.newPolicy(grandparent);
    Policy policy2 = tempEntity.newPolicy(grandparent);
    String hash = "12345678901234567890";

    String restId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();

    // Verify owner level
    PolicyWaiver activeWaiver =
        tempEntity.newWaiver(hash, policy1.getId(), owner.getId(), null, EXACT_COMPONENT,
            "App Scope", now.toDate(), now.plusHours(1).toDate());
    tempEntity.newWaiver(hash, policy2.getId(), owner.getId(), null, EXACT_COMPONENT, "Expired", null, now.toDate());
    HttpResponse response = restRequest(owner.getType(), restId).path("component", hash).get();
    assertResponseStatus(200, response);
    AppliedWaivers waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(owner,
        policy1.getId(),
        "App Scope",
        activeWaiver.getConstraintFactsJson(),
        activeWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null,
        activeWaiver.getCreateTime(),
        hash,
        activeWaiver.getAssociatedPackageUrl(),
        activeWaiver.getComponentMatchStrategy(),
        activeWaiver.isExpireWhenRemediationAvailable(),
        activeWaiver.getExpiryTime(),
        waivers.waiversByOwner.get(0));
    response = restRequest(parent.getType(), parent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(0);
    response = restRequest(grandparent.getType(), grandparent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(0);

    // Verify parent owner level
    PolicyWaiver activeParentWaiver =
        tempEntity.newWaiver(hash, policy2.getId(), parent.getId(), null, EXACT_COMPONENT,
            "Parent Scope", now.toDate(), now.plusHours(1).toDate());
    tempEntity.newWaiver(hash, policy1.getId(), parent.getId(), null, EXACT_COMPONENT, "Expired", null, now.toDate());
    response = restRequest(owner.getType(), restId).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(2);
    assertWaiversByOwner(owner,
        policy1.getId(),
        "App Scope",
        activeWaiver.getConstraintFactsJson(),
        activeWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null,
        activeWaiver.getCreateTime(),
        hash,
        activeWaiver.getAssociatedPackageUrl(),
        activeWaiver.getComponentMatchStrategy(),
        activeWaiver.isExpireWhenRemediationAvailable(),
        activeWaiver.getExpiryTime(),
        waivers.waiversByOwner.get(0));
    assertWaiversByOwner(parent,
        policy2.getId(),
        "Parent Scope",
        activeParentWaiver.getConstraintFactsJson(),
        activeParentWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null,
        activeParentWaiver.getCreateTime(),
        hash,
        activeParentWaiver.getAssociatedPackageUrl(),
        activeParentWaiver.getComponentMatchStrategy(),
        activeParentWaiver.isExpireWhenRemediationAvailable(),
        activeParentWaiver.getExpiryTime(),
        waivers.waiversByOwner.get(1));
    response = restRequest(parent.getType(), parent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(parent,
        policy2.getId(),
        "Parent Scope",
        activeParentWaiver.getConstraintFactsJson(),
        activeParentWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null,
        activeWaiver.getCreateTime(),
        hash,
        activeWaiver.getAssociatedPackageUrl(),
        activeWaiver.getComponentMatchStrategy(),
        activeWaiver.isExpireWhenRemediationAvailable(),
        activeWaiver.getExpiryTime(),
        waivers.waiversByOwner.get(0));
    response = restRequest(grandparent.getType(), grandparent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(0);

    // Verify grandparent organization level
    PolicyWaiver activeGrandparentWaiver = tempEntity
        .newWaiver(hash, policy1.getId(), grandparent.getId(), null, EXACT_COMPONENT,
            "Grandparent Scope", now.toDate(), now.plusHours(1).toDate());
    tempEntity.newWaiver(hash, policy2.getId(), grandparent.getId(), null, EXACT_COMPONENT,
        "Expired", null, now.toDate());
    response = restRequest(owner.getType(), restId).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(3);
    assertWaiversByOwner(owner,
        policy1.getId(),
        "App Scope",
        activeWaiver.getConstraintFactsJson(),
        activeWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null,
        activeWaiver.getCreateTime(),
        hash,
        activeWaiver.getAssociatedPackageUrl(),
        activeWaiver.getComponentMatchStrategy(),
        activeWaiver.isExpireWhenRemediationAvailable(),
        activeWaiver.getExpiryTime(),
        waivers.waiversByOwner.get(0));

    assertWaiversByOwner(parent,
        policy2.getId(),
        "Parent Scope",
        activeParentWaiver.getConstraintFactsJson(),
        activeParentWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null,
        activeParentWaiver.getCreateTime(),
        hash,
        activeParentWaiver.getAssociatedPackageUrl(),
        activeParentWaiver.getComponentMatchStrategy(),
        activeParentWaiver.isExpireWhenRemediationAvailable(),
        activeParentWaiver.getExpiryTime(),
        waivers.waiversByOwner.get(1));

    assertWaiversByOwner(grandparent,
        policy1.getId(),
        "Grandparent Scope",
        activeGrandparentWaiver.getConstraintFactsJson(),
        activeGrandparentWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null,
        activeGrandparentWaiver.getCreateTime(),
        hash,
        activeGrandparentWaiver.getAssociatedPackageUrl(),
        activeGrandparentWaiver.getComponentMatchStrategy(),
        activeGrandparentWaiver.isExpireWhenRemediationAvailable(),
        activeGrandparentWaiver.getExpiryTime(),
        waivers.waiversByOwner.get(2));
    response = restRequest(parent.getType(), parent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(2);
    assertWaiversByOwner(parent,
        policy2.getId(),
        "Parent Scope",
        activeParentWaiver.getConstraintFactsJson(),
        activeParentWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null,
        activeParentWaiver.getCreateTime(),
        hash,
        activeParentWaiver.getAssociatedPackageUrl(),
        activeParentWaiver.getComponentMatchStrategy(),
        activeParentWaiver.isExpireWhenRemediationAvailable(),
        activeParentWaiver.getExpiryTime(),
        waivers.waiversByOwner.get(0));

    assertWaiversByOwner(grandparent,
        policy1.getId(),
        "Grandparent Scope",
        activeGrandparentWaiver.getConstraintFactsJson(),
        activeGrandparentWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null,
        activeGrandparentWaiver.getCreateTime(),
        hash,
        activeGrandparentWaiver.getAssociatedPackageUrl(),
        activeGrandparentWaiver.getComponentMatchStrategy(),
        activeGrandparentWaiver.isExpireWhenRemediationAvailable(),
        activeGrandparentWaiver.getExpiryTime(),
        waivers.waiversByOwner.get(1));
    response = restRequest(grandparent.getType(), grandparent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(grandparent,
        policy1.getId(),
        "Grandparent Scope",
        activeGrandparentWaiver.getConstraintFactsJson(),
        activeGrandparentWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null,
        activeGrandparentWaiver.getCreateTime(),
        hash,
        activeGrandparentWaiver.getAssociatedPackageUrl(),
        activeGrandparentWaiver.getComponentMatchStrategy(),
        activeGrandparentWaiver.isExpireWhenRemediationAvailable(),
        activeGrandparentWaiver.getExpiryTime(),
        waivers.waiversByOwner.get(0));
  }

  @Test
  public void testGetPolicyWaiversByHash_WaiverOnNoLongerApplicablePolicy() throws Exception {
    Organization org = tempEntity.newOrganization();
    Tag tag1 = tempEntity.newTag(org.getId());
    Tag tag2 = tempEntity.newTag(org.getId());
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newApplicationTag(app.getId(), tag1.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyTag policyTag = tempEntity.newPolicyTag(policy.getId(), tag1.getId());

    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("hash", policy.getId(), app.getId(), null, EXACT_COMPONENT, "Test Comment");

    // update policy tags so it no longer applies to the application
    policyTagDAO.delete(policyTag);
    tempEntity.newPolicyTag(policy.getId(), tag2.getId());

    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId()).path("component", "hash").get();
    assertResponseStatus(200, response);
    AppliedWaivers waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(
        app, policy.getId(),
        "Test Comment",
        policyWaiver.getConstraintFactsJson(),
        policyWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        null, waivers.waiversByOwner.get(0));
  }

  @Test
  public void testGetPolicyWaiversByHash_WithWaiverReason() throws Exception {
    Organization org = tempEntity.newOrganization();
    Tag tag1 = tempEntity.newTag(org.getId());
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newApplicationTag(app.getId(), tag1.getId());
    Policy policy = tempEntity.newPolicy(org);

    PolicyWaiver policyWaiver =
        tempEntity.newWaiverWithReason("hash", policy.getId(), app.getId(), null, "Test Comment", "Test Comment",
            "Reason");

    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId()).path("component", "hash").get();
    assertResponseStatus(200, response);
    AppliedWaivers waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(
        app, policy.getId(),
        "Test Comment",
        policyWaiver.getConstraintFactsJson(),
        policyWaiver.getConstraintFacts(),
        "testuser",
        "Test User",
        "Reason", waivers.waiversByOwner.get(0));
  }

  private void assertPolicyWaiverDTO(
      String policyId,
      String ownerId,
      String comment,
      String constraintFactsJson,
      List<ConstraintFact> constraints,
      String creatorId,
      String creatorName,
      String reasonText,
      Date createTime,
      String hash,
      String associatedPackageUrl,
      PolicyWaiver.ComponentMatcherStrategyForWaiver componentMatchStrategy,
      boolean expireWhenRemediationAvailable,
      Date expiryTime,
      PolicyWaiverDTO actual)
  {
    assertPolicyWaiver(policyId, ownerId, comment, constraintFactsJson, constraints, creatorId, creatorName, null,
        createTime, hash, associatedPackageUrl, componentMatchStrategy, expireWhenRemediationAvailable, expiryTime,
        actual);
    assertThat(actual.reasonText).isEqualTo(reasonText);
    if (actual.reasonText != null) {
      assertThat(actual.policyWaiverReasonId).isNotNull();
    }
    else {
      assertThat(actual.policyWaiverReasonId).isNull();
    }
    assertThat(actual.policyName).isEqualTo(policyDAO.getById(policyId).getName());
  }

  private void assertPolicyWaiverDTO(
      String policyId,
      String ownerId,
      String comment,
      String constraintFactsJson,
      List<ConstraintFact> constraints,
      String creatorId,
      String creatorName,
      String reasonText,
      PolicyWaiverDTO actual)
  {
    assertPolicyWaiver(policyId, ownerId, comment, constraintFactsJson, constraints, creatorId, creatorName, null,
        actual);
    assertThat(actual.reasonText).isEqualTo(reasonText);
    if (actual.reasonText != null) {
      assertThat(actual.policyWaiverReasonId).isNotNull();
    }
    else {
      assertThat(actual.policyWaiverReasonId).isNull();
    }
    assertThat(actual.policyName).isEqualTo(policyDAO.getById(policyId).getName());
  }

  private void assertPolicyWaiver(
      String policyId,
      String ownerId,
      String comment,
      String constraintFactsJson,
      List<ConstraintFact> constraints,
      String creatorId,
      String creatorName,
      String reasonText,
      PolicyWaiver actual)
  {
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getComment()).isEqualTo(comment);
    assertThat(actual.getConstraintFactsJson()).isEqualTo(constraintFactsJson);
    assertThat(actual.getCreatorId()).isEqualTo(creatorId);
    assertThat(actual.getCreatorName()).isEqualTo(creatorName);
    if (reasonText == null) {
      assertThat(actual.getWaiverReasonId()).isNull();
    }
    else {
      assertThat(actual.getWaiverReasonId()).isNotNull();
    }
    if (actual.getConstraintFacts() != null) {
      assertThat(actual.getConstraintFacts().get(0).getConstraintId()).isEqualTo(constraints.get(0).getConstraintId());
    }
  }

  private void assertPolicyWaiver(
      String policyId,
      String ownerId,
      String comment,
      String constraintFactsJson,
      List<ConstraintFact> constraints,
      String creatorId,
      String creatorName,
      String reasonText,
      Date createTime,
      String hash,
      String associatedPackageUrl,
      PolicyWaiver.ComponentMatcherStrategyForWaiver componentMatchStrategy,
      boolean expireWhenRemediationAvailable,
      Date expiryTime,
      PolicyWaiver actual)
  {
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getComment()).isEqualTo(comment);
    assertThat(actual.getConstraintFactsJson()).isEqualTo(constraintFactsJson);
    assertThat(actual.getCreatorId()).isEqualTo(creatorId);
    assertThat(actual.getCreatorName()).isEqualTo(creatorName);
    assertThat(actual.getCreateTime()).isEqualTo(createTime);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getAssociatedPackageUrl()).isEqualTo(associatedPackageUrl);
    assertThat(actual.getComponentMatchStrategy()).isEqualTo(componentMatchStrategy);
    assertThat(actual.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(actual.isExpireWhenRemediationAvailable()).isEqualTo(expireWhenRemediationAvailable);

    if (reasonText == null) {
      assertThat(actual.getWaiverReasonId()).isNull();
    }
    else {
      assertThat(actual.getWaiverReasonId()).isNotNull();
    }
    if (actual.getConstraintFacts() != null) {
      assertThat(actual.getConstraintFacts().get(0).getConstraintId()).isEqualTo(constraints.get(0).getConstraintId());
    }
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    String appPublicId = "testGetApplicableContexts";
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = organizationDAO.getById(org.getParentOrganizationId());
    Application app = tempEntity.newApplication(appPublicId, org.getId());

    // Verify application level
    Policy policy = tempEntity.newPolicy(app);
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId())
        .get();
    assertResponseStatus(200, response);
    ApplicableContext result = response.getBody(ApplicableContext.class);
    assertApplicableContext(app, result);
    assertThat(result.getChildren()).isNull();

    // Verify organization level
    policy = tempEntity.newPolicy(org);
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId()).get();
    assertResponseStatus(200, response);
    result = response.getBody(ApplicableContext.class);
    assertApplicableContext(org, result);
    assertThat(result.getChildren()).hasSize(1);
    ApplicableContext childContext = result.getChildren().get(0);
    assertApplicableContext(app, childContext);
    assertThat(childContext.getChildren()).isNull();

    // Verify parent organization level
    policy = tempEntity.newPolicy(parentOrg);
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId()).get();
    assertResponseStatus(200, response);
    result = response.getBody(ApplicableContext.class);
    assertApplicableContext(parentOrg, result);
    assertThat(result.getChildren()).hasSize(1);
    childContext = result.getChildren().get(0);
    assertApplicableContext(org, childContext);
    assertThat(childContext.getChildren()).hasSize(1);
    childContext = childContext.getChildren().get(0);
    assertApplicableContext(app, childContext);
    assertThat(childContext.getChildren()).isNull();
  }

  @Test
  public void testGetApplicableContexts_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();

    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response = restRequest(OwnerType.REPOSITORY, repository.getId()).path("applicable/context",
        policy.getId()).get();
    assertResponseStatus(200, response);
    ApplicableContext result = response.getBody(ApplicableContext.class);

    LinkedList<Owner> ownerHierarchy = new LinkedList<>();
    for (Owner owner : ownerDAO.walkHierarchy(repository)) {
      ownerHierarchy.push(owner);
    }

    ApplicableContext childContext = result;
    while (!ownerHierarchy.isEmpty()) {
      Owner context = ownerHierarchy.pop();
      assertApplicableContext(context, childContext);
      if (ownerHierarchy.isEmpty()) {
        assertThat(childContext.getChildren()).isNull();
      }
      else {
        assertThat(childContext.getChildren()).hasSize(1);
        childContext = childContext.getChildren().get(0);
      }
    }
  }

  @Test
  public void testGetApplicableContexts_PolicyNotApplicable() throws Exception {
    String appPublicId = "testGetApplicableContextsPolicyNotApplicable";
    tempEntity.newApplicationWithParent(appPublicId);
    Application otherApp = tempEntity.newApplicationWithParent("otherApp");

    Policy policy = tempEntity.newPolicy(otherApp);
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId())
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a policy with ID " + policy.getId()
        + " for application public ID " + appPublicId);
  }

  @Test
  public void testGetApplicableContexts_PolicyNotApplicable_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    Application otherApp = tempEntity.newApplicationWithParent("anApp");

    Policy policy = tempEntity.newPolicy(otherApp);
    HttpResponse response = restRequest(OwnerType.REPOSITORY, repository.getId()).path("applicable/context",
        policy.getId()).get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a policy with ID " + policy.getId()
        + " for repository public ID " + repository.getPublicId());
  }

  private void assertApplicableContext(Owner owner, ApplicableContext actual) {
    assertThat(actual).isNotNull();
    assertThat(actual.getId())
        .isEqualTo(OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId());
    assertThat(actual.getName()).isEqualTo(owner.getName());
    assertThat(actual.getType()).isEqualTo(owner.getType());
  }
}
