/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.LicenseThreatGroupWithLicenses;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.LicenseThreatGroupsByOwner;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

abstract class AbstractLicenseThreatGroupResourceTest
    extends AbstractResourceTest
{
  protected OrganizationDAO orgDAO = new OrganizationDAO();

  private HttpRequest restRequest(String ownerId) {
    return restRequest().path(LicenseThreatGroupResource.RESOURCE_PATH).parameter(getOwnerType(), ownerId);
  }

  protected void testDelete_OwnerIdMismatch(Owner owner1, Owner owner2) throws Exception {
    LicenseThreatGroup group = tempEntity.newLicenseThreatGroup(owner1.getId());

    HttpResponse response = restRequest(owner2.getPublicId()).path(group.getId()).delete();
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a license threat group with ID " + group.getId() + " for " + getOwnerType()
        + " ID " + owner2.getPublicId(), response.getBodyText());

    // Verify that the group was not deleted
    assertThat(new LicenseThreatGroupDAO().getById(group.getId()), is(notNullValue()));
  }

  protected void testDelete_InUseByPolicy(Owner owner) throws Exception {
    testDelete_InUseByPolicy(owner.getPublicId(), owner.getId(), owner.getId(), null);
  }

  protected void testDelete_InUseByPolicy(String ownerPublicId,
                                          String ownerId,
                                          String policyOwnerId,
                                          String policyLocation) throws Exception
  {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(ownerId);

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

    Assert.assertEquals(error, response.getBodyText());
    Assert.assertNotNull(new LicenseThreatGroupDAO().getById(ltg.getId()));
  }

  protected void testCRUD(String ownerPublicId, String ownerId) throws Exception {
    HttpRequest request = restRequest(ownerPublicId);

    // Get all groups
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    LicenseThreatGroup[] groups = response.getBody(LicenseThreatGroup[].class);
    Assert.assertNotNull(groups);
    int initialLicenseThreatGroupCount = groups.length;

    // Add a group
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("AAA My group");
    group.setThreatLevel(10);
    response = request.body(group).post();
    assertResponseStatus(200, response);
    group = response.getBody(LicenseThreatGroup.class);
    assertLicenseThreatGroup(ownerId, "AAA My group", 10, group);

    // Get all groups
    response = request.get();
    assertResponseStatus(200, response);
    groups = response.getBody(LicenseThreatGroup[].class);
    Assert.assertNotNull(groups);
    Assert.assertEquals(initialLicenseThreatGroupCount + 1, groups.length);
    assertLicenseThreatGroup(ownerId, "AAA My group", 10, groups[0]);

    // Update a group
    group.setName("AAA My updated group");
    response = request.body(group).put();
    assertResponseStatus(200, response);
    group = response.getBody(LicenseThreatGroup.class);
    assertLicenseThreatGroup(ownerId, "AAA My updated group", 10, group);

    // Get all groups
    response = request.get();
    assertResponseStatus(200, response);
    groups = response.getBody(LicenseThreatGroup[].class);
    Assert.assertNotNull(groups);
    Assert.assertEquals(initialLicenseThreatGroupCount + 1, groups.length);
    assertLicenseThreatGroup(ownerId, "AAA My updated group", 10, groups[0]);

    // Delete a group
    response = request.subpath(group.getId()).delete();
    assertResponseStatus(204, response);

    // Get all groups
    response = request.get();
    assertResponseStatus(200, response);
    groups = response.getBody(LicenseThreatGroup[].class);
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
    return response.getBody(ApplicableLicenseThreatGroups.class);
  }

  protected void assertLicenseThreatGroupsByOwner(String ownerId,
                                                  String ownerName,
                                                  OwnerType ownerType,
                                                  int licenseThreatGroupCount,
                                                  LicenseThreatGroupsByOwner actual)
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
