/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
      assertEquals("Cannot find a policy waiver with id fake id", expected.getMessage());
    }
  }

  @Test
  public void testCRUD() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash = "123456789012345678901";
    assertTrue(hash.length() > 20);
    String truncatedHash = hash.substring(0, 20);
    String policyId = "MyPolicyId";
    String ownerId = "MyOwnerId";
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

    // Update is not allowed
    try {
      dao.update(policyWaiver);
      fail("Expected UnsupportedOperationException, updates to PolicyWaiver are not allowed");
    }
    catch (UnsupportedOperationException expected) {
    }

    // Delete
    dao.delete(policyWaiver);

    policyWaiver = dao.getById(policyWaiver.getId());
    assertNull(policyWaiver);
  }

  private void assertPolicyWaiver(String hash, String policyId, String ownerId, String comment, Date createTime,
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
  public void testAddDuplicate() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash = "12345678901234567890";
    String policyId = "MyPolicyId";
    String ownerId = "MyOwnerId";
    String comment = "My comment";
    PolicyWaiver policyWaiver1 = new PolicyWaiver(hash, policyId, ownerId, comment);
    dao.insert(policyWaiver1);

    PolicyWaiver policyWaiver2 = new PolicyWaiver(hash, policyId, ownerId, comment);
    try {
      dao.insert(policyWaiver2);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("This policy waiver already exists", expected.getMessage());
    }

    dao.delete(policyWaiver1);
  }

  @Test
  public void testGetByOwnerId_Inherited() {
    createDefaultApplication();
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    PolicyWaiver policyWaiverOrg = new PolicyWaiver("1", "MyPolicyId1", organization.getId(), "My comment1");
    dao.insert(policyWaiverOrg);

    PolicyWaiver policyWaiverApp = new PolicyWaiver("2", "MyPolicyId2", application.getId(), "My comment2");
    dao.insert(policyWaiverApp);

    // Assert for application
    List<PolicyWaiver> policyWaivers = dao.getByOwnerId(application.getId(), false /* inherit */);
    assertEquals(1, policyWaivers.size());
    assertPolicyWaiver(policyWaiverApp, policyWaivers.get(0));

    policyWaivers = dao.getByOwnerId(application.getId(), true /* inherit */);
    assertEquals(2, policyWaivers.size());
    assertPolicyWaiver(policyWaiverOrg, policyWaivers.get(0));
    assertPolicyWaiver(policyWaiverApp, policyWaivers.get(1));

    // Assert for organizationn
    policyWaivers = dao.getByOwnerId(organization.getId(), false /* inherit */);
    assertEquals(1, policyWaivers.size());
    assertPolicyWaiver(policyWaiverOrg, policyWaivers.get(0));

    policyWaivers = dao.getByOwnerId(organization.getId(), true /* inherit */);
    assertEquals(1, policyWaivers.size());
    assertPolicyWaiver(policyWaiverOrg, policyWaivers.get(0));
  }

  @Test
  public void testCommentTooLong() throws Exception {
    PolicyWaiverDAO dao = new PolicyWaiverDAO();

    String hash = "12345678901234567890";
    String policyId = "MyPolicyId";
    String ownerId = "MyOwnerId";
    String comment = StringUtils.repeat("X", 1001);
    PolicyWaiver policyWaiver1 = new PolicyWaiver(hash, policyId, ownerId, comment);

    try {
      dao.insert(policyWaiver1);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("Comment length must not exceed 1000 characters", expected.getMessage());
    }

    dao.delete(policyWaiver1);
  }
}
