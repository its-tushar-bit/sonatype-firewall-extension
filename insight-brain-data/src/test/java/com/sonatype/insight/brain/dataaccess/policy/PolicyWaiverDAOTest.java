/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PolicyWaiverDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testGetByIdNotNull() {
    try {
      new PolicyWaiverDAO().getByIdNotNull("fake id");
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("Cannot find a policy waiver with ID fake id.", expected.getMessage());
    }
  }

  @Test
  public void testCRUD() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash = "123456789012345678901";
    assertTrue(hash.length() > 20);
    String truncatedHash = hash.substring(0, 20);
    Policy policy = tempEntity.newPolicy(organization.getId(), "PolicyWaiverDAOTest");
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "My comment";

    // Create
    PolicyWaiver policyWaiver = new PolicyWaiver(hash, policyId, ownerId, comment);
    assertNull(policyWaiver.getId());
    long beforeInsert = System.currentTimeMillis();
    dao.insert(policyWaiver);
    long afterInsert = System.currentTimeMillis();
    assertNotNull(policyWaiver.getId());
    assertNotNull(policyWaiver.getCreateTime());
    Date createTime = policyWaiver.getCreateTime();
    assertTrue(beforeInsert <= createTime.getTime());
    assertTrue(createTime.getTime() <= afterInsert);

    // Read
    policyWaiver = dao.getById(policyWaiver.getId());
    assertNotNull(policyWaiver);
    assertPolicyWaiver(truncatedHash, policyId, ownerId, comment, createTime, policyWaiver);

    // Update
    String updateComment = "Updated comment";
    policyWaiver.setComment(updateComment);
    dao.update(policyWaiver);

    // Read
    policyWaiver = dao.getById(policyWaiver.getId());
    assertNotNull(policyWaiver);
    assertPolicyWaiver(truncatedHash, policyId, ownerId, updateComment, createTime, policyWaiver);

    // Delete
    dao.delete(policyWaiver);

    policyWaiver = dao.getById(policyWaiver.getId());
    assertNull(policyWaiver);
  }

  private void assertPolicyWaiver(String hash,
                                  String policyId,
                                  String ownerId,
                                  String comment,
                                  Date createTime,
                                  PolicyWaiver actual)
  {
    assertEquals(hash, actual.getHash());
    assertEquals(policyId, actual.getPolicyId());
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(comment, actual.getComment());
    assertEquals(createTime, actual.getCreateTime());
  }

  private void assertPolicyWaiver(PolicyWaiver expected, PolicyWaiver actual) {
    assertEquals(expected.getHash(), actual.getHash());
    assertEquals(expected.getPolicyId(), actual.getPolicyId());
    assertEquals(expected.getOwnerId(), actual.getOwnerId());
    assertEquals(expected.getComment(), actual.getComment());
  }

  @Test
  public void testInsert_Duplicate_ComponentLevel() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization.getId(), "PolicyWaiverDAOTest");
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(hash, policyId, ownerId, comment);
    dao.insert(policyWaiver1);

    PolicyWaiver policyWaiver2 = new PolicyWaiver(hash, policyId, ownerId, comment);
    try {
      dao.insert(policyWaiver2);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("This policy waiver already exists.", expected.getMessage());
    }

    dao.delete(policyWaiver1);
  }

  @Test
  public void testInsert_Duplicate_PolicyLevel() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    Policy policy = tempEntity.newPolicy(organization.getId(), "PolicyWaiverDAOTest");
    String policyId = policy.getId();
    String ownerId = organization.getId();
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(policyId, ownerId, comment);
    dao.insert(policyWaiver1);

    PolicyWaiver policyWaiver2 = new PolicyWaiver(policyId, ownerId, comment);
    try {
      dao.insert(policyWaiver2);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("This policy waiver already exists.", expected.getMessage());
    }

    dao.delete(policyWaiver1);
  }

  @Test
  public void testGetApplicableByOwnerId() {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();
    Policy policyApp = tempEntity.newPolicy(application.getId(), "PolicyWaiverDAOTest1");
    Policy policyOrg = tempEntity.newPolicy(organization.getId(), "PolicyWaiverDAOTest2");
    Policy policyParentOrg = tempEntity.newPolicy(organization.getParentOrganizationId(), "PolicyWaiverDAOTest3");

    PolicyWaiver policyWaiverParentOrg = tempEntity.newWaiver("0", policyParentOrg.getId(),
        organization.getParentOrganizationId());
    PolicyWaiver policyWaiverOrg = tempEntity.newWaiver("1", policyOrg.getId(), organization.getId());
    PolicyWaiver policyWaiverApp = tempEntity.newWaiver("2", policyApp.getId(), application.getId());

    // Assert for application
    List<PolicyWaiver> policyWaivers = dao.getApplicableByOwnerId(application.getId());
    assertEquals(3, policyWaivers.size());
    assertPolicyWaiver(policyWaiverParentOrg, policyWaivers.get(0));
    assertPolicyWaiver(policyWaiverOrg, policyWaivers.get(1));
    assertPolicyWaiver(policyWaiverApp, policyWaivers.get(2));

    // Assert for organization
    policyWaivers = dao.getApplicableByOwnerId(organization.getId());
    assertEquals(2, policyWaivers.size());
    assertPolicyWaiver(policyWaiverParentOrg, policyWaivers.get(0));
    assertPolicyWaiver(policyWaiverOrg, policyWaivers.get(1));

    // Assert for parent organization
    policyWaivers = dao.getApplicableByOwnerId(organization.getParentOrganizationId());
    assertEquals(1, policyWaivers.size());
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

    try {
      dao.insert(policyWaiver1);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("Comment length must not exceed 1000 characters.", expected.getMessage());
    }

    dao.delete(policyWaiver1);
  }

  @Test
  public void testUpdate_CommentTooLong() throws Exception {
    Policy policy = tempEntity.newPolicy(application.getId(), "name");
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
    PolicyWaiverDAO dao = new PolicyWaiverDAO();
    String comment = StringUtils.repeat("X", 1001);
    policyWaiver.setComment(comment);

    try {
      dao.update(policyWaiver);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertEquals("Comment length must not exceed 1000 characters.", expected.getMessage());
    }

    comment = comment.substring(0, 1000);
    policyWaiver.setComment(comment);
    dao.update(policyWaiver);
    policyWaiver = dao.getById(policyWaiver.getId());
    assertThat(policyWaiver.getComment(), is(comment));
  }

  @Test
  public void testUpdate_Duplicate_ComponentLevel() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash1 = "11111111111111111111";
    String hash2 = "11111111111111111112";
    Policy policy = tempEntity.newPolicy(organization.getId(), "PolicyWaiverDAOTest");
    String policyId = policy.getId();
    String ownerId = organization.getId();
    tempEntity.newWaiver(hash1, policyId, ownerId);
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver(hash2, policyId, ownerId);

    policyWaiver2.setHash(hash1);
    try {
      dao.update(policyWaiver2);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("A policy waiver for the same hash, policy and owner already exists.", expected.getMessage());
    }
  }

  @Test
  public void testUpdate_Duplicate_PolicyLevel() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    Policy policy1 = tempEntity.newPolicy(organization.getId(), "PolicyWaiverDAOTest1");
    Policy policy2 = tempEntity.newPolicy(organization.getId(), "PolicyWaiverDAOTest2");
    String policyId1 = policy1.getId();
    String policyId2 = policy2.getId();
    String ownerId = organization.getId();
    tempEntity.newWaiver(policyId1, ownerId);
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver(policyId2, ownerId);

    policyWaiver2.setPolicyId(policyId1);
    try {
      dao.update(policyWaiver2);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("A policy waiver for the same hash, policy and owner already exists.", expected.getMessage());
    }
  }

  @Test
  public void testGetByOwnerIdAndHash() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash = "12345678901234567890";
    Policy policy = tempEntity.newPolicy(organization.getId(), "PolicyWaiverDAOTest");
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

    assertThat(waivers, is(notNullValue()));
    assertThat(waivers, hasSize(2));
    assertThat(waivers.get(0).getId(), is(policyWaiver1.getId()));
    assertThat(waivers.get(1).getId(), is(policyWaiver2.getId()));
  }

  @Test
  public void testDeleteDoesNotCascadeToWaivedPolicyViolation() {
    Policy policy = tempEntity.newPolicy(applicationId, "testDeleteDoesNotCascadeToWaivedPolicyViolation");
    PolicyWaiver policyWaiver = tempEntity.newWaiver("ababababab", policy.getId(), applicationId);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyWaiverDAOTest");
    WaivedPolicyViolation waivedPolicyViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        policyWaiver);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    assertThat(policyViolationDAO.getById(waivedPolicyViolation.getId()), notNullValue());
    WaivedPolicyViolationDAO waivedPolicyViolationDAO = new WaivedPolicyViolationDAO();
    assertThat(waivedPolicyViolationDAO.getById(waivedPolicyViolation.getId()), notNullValue());

    new PolicyWaiverDAO().delete(policyWaiver);
    assertThat(policyViolationDAO.getById(waivedPolicyViolation.getId()), notNullValue());
    assertThat(waivedPolicyViolationDAO.getById(waivedPolicyViolation.getId()), notNullValue());
  }
}
