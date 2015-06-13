/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.license.LicenseThreatGroupResource.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.license.LicenseThreatGroupResource.LicenseThreatGroupWithLicenses;
import com.sonatype.insight.brain.license.LicenseThreatGroupResource.LicenseThreatGroupsByOwner;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;

abstract class AbstractLicenseThreatGroupResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(String ownerId) {
    return restRequest().path(LicenseThreatGroupResource.SERVICE_PATH).parameter(getOwnerType(), ownerId);
  }

  protected void testDelete_OwnerIdMismatch(String ownerPublicId1, String ownerId1, String ownerPublicId2,
      String ownerId2) throws Exception
  {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId1);
    group.setName("AAA My group");
    group.setThreatLevel(4);
    HttpResponse response = restRequest(ownerPublicId1).body(group).post();
    assertResponseStatus(200, response);
    group = fromJson(response, LicenseThreatGroup.class);

    response = restRequest(ownerPublicId2).path(group.getId()).delete();
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a license threat group with ID " + group.getId() + " for " + getOwnerType()
        + " ID " + ownerPublicId2, response.getResponseBody());
    // Verify that the group was not deleted
    response = restRequest(ownerPublicId1).get();
    assertResponseStatus(200, response);
    LicenseThreatGroup[] groups = fromJson(response, LicenseThreatGroup[].class);
    Assert.assertNotNull(groups);
    Assert.assertEquals(1, groups.length);
    assertLicenseThreatGroup(ownerId1, "AAA My group", 4, groups[0]);
  }

  protected void testDelete_InUseByPolicy(String ownerPublicId, String ownerId, String policyOwnerId) throws Exception {
    testDelete_InUseByPolicy(ownerPublicId, ownerId, policyOwnerId, null);
  }

  protected void testDelete_InUseByPolicy(String ownerPublicId, String ownerId, String policyOwnerId,
      String policyLocation) throws Exception
  {
    LicenseThreatGroupDAO ltgDAO = new LicenseThreatGroupDAO();
    LicenseThreatGroup ltg = new LicenseThreatGroup(ownerId, "ltgName", 5);
    ltgDAO.insert(ltg);

    Policy policy = new Policy(null, "policyName");
    policy.setOwnerId(policyOwnerId);
    Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.AND);
    constraint.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is", ltg.getId()));
    policy.addConstraint(constraint);
    new PolicyDAO().insert(policy);

    HttpResponse response = restRequest(ownerPublicId).path(ltg.getId()).delete();
    assertResponseStatus(400, response);

    String error = "Cannot delete the license threat group because it is used in a condition for the 'policyName' policy";
    if (null != policyLocation) {
      error = error + " " + policyLocation;
    }

    Assert.assertEquals(error, response.getResponseBody());
    Assert.assertNotNull(ltgDAO.getById(ltg.getId()));
  }

  protected void testCRUD(String ownerPublicId, String ownerId) throws Exception {
    HttpRequest request = restRequest(ownerPublicId);

    // Get all groups
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    LicenseThreatGroup[] groups = fromJson(response, LicenseThreatGroup[].class);
    Assert.assertNotNull(groups);
    int initialLicenseThreatGroupCount = groups.length;

    // Add a group
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("AAA My group");
    group.setThreatLevel(10);
    response = request.body(group).post();
    assertResponseStatus(200, response);
    group = fromJson(response, LicenseThreatGroup.class);
    assertLicenseThreatGroup(ownerId, "AAA My group", 10, group);

    // Get all groups
    response = request.get();
    assertResponseStatus(200, response);
    groups = fromJson(response, LicenseThreatGroup[].class);
    Assert.assertNotNull(groups);
    Assert.assertEquals(initialLicenseThreatGroupCount + 1, groups.length);
    assertLicenseThreatGroup(ownerId, "AAA My group", 10, groups[0]);

    // Update a group
    group.setName("AAA My updated group");
    response = request.body(group).put();
    assertResponseStatus(200, response);
    group = fromJson(response, LicenseThreatGroup.class);
    assertLicenseThreatGroup(ownerId, "AAA My updated group", 10, group);

    // Get all groups
    response = request.get();
    assertResponseStatus(200, response);
    groups = fromJson(response, LicenseThreatGroup[].class);
    Assert.assertNotNull(groups);
    Assert.assertEquals(initialLicenseThreatGroupCount + 1, groups.length);
    assertLicenseThreatGroup(ownerId, "AAA My updated group", 10, groups[0]);

    // Delete a group
    response = request.subpath(group.getId()).delete();
    assertResponseStatus(204, response);

    // Get all groups
    response = request.get();
    assertResponseStatus(200, response);
    groups = fromJson(response, LicenseThreatGroup[].class);
    Assert.assertNotNull(groups);
    Assert.assertEquals(initialLicenseThreatGroupCount, groups.length);
  }

  private void assertLicenseThreatGroup(String ownerId, String name, int threatLevel, LicenseThreatGroup actual) {
    Assert.assertEquals(ownerId, actual.getOwnerId());
    Assert.assertEquals(name, actual.getName());
    Assert.assertEquals(threatLevel, actual.getThreatLevel());
  }

  protected ApplicableLicenseThreatGroups getApplicableLicenseThreatGroups(String ownerId) throws Exception {
    HttpResponse response = restRequest(ownerId).path("applicable").get();
    assertResponseStatus(200, response);
    return fromJson(response, ApplicableLicenseThreatGroups.class);
  }

  protected void assertLicenseThreatGroupsByOwner(String ownerId, String ownerName, String ownerType,
      int licenseThreatGroupCount, LicenseThreatGroupsByOwner actual)
  {
    Assert.assertEquals(ownerId, actual.ownerId);
    Assert.assertEquals(ownerName, actual.ownerName);
    Assert.assertEquals(ownerType, actual.ownerType);
    Assert.assertNotNull(actual.licenseThreatGroups);
    Assert.assertEquals(licenseThreatGroupCount, actual.licenseThreatGroups.size());
    for (LicenseThreatGroupWithLicenses ltgwl : actual.licenseThreatGroups) {
      Assert.assertNotNull(ltgwl.licenses);
    }
  }

  protected abstract String getOwnerType();
}
