/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.license.LicenseThreatGroupResource;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.LicenseThreatGroupsByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IQ Server on PostgreSQL — converted from the legacy
 * {@code com.sonatype.insight.brain.license.OrganizationLicenseThreatGroupResourceTest}
 * (which extended {@code AbstractLicenseThreatGroupResourceTest}). No base class; the base test's
 * helper methods are inlined here since {@link IqTestContext} does not expose them.
 */
@IqPostgresTest
class IqPostgresOrganizationLicenseThreatGroupResourceTest
{
  private IqTestContext ctx;

  private OrganizationDAO orgDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @BeforeEach
  void setUp() {
    orgDAO = ctx.lookup(OrganizationDAO.class);
    licenseThreatGroupDAO = ctx.lookup(LicenseThreatGroupDAO.class);
  }

  @Test
  void testCRUD() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization("testCRUD-Organization");
    testCRUD(organization.getId(), organization.getId());
  }

  @Test
  void testDelete_OwnerIdMismatch() throws Exception {
    Organization organization1 = ctx.tempEntity().newOrganization("testDeleteOwnerIdMismatch1");
    Organization organization2 = ctx.tempEntity().newOrganization("testDeleteOwnerIdMismatch2");
    testDelete_OwnerIdMismatch(organization1, organization2);
  }

  @Test
  void testDelete_InUseByPolicy() throws Exception {
    Organization org = ctx.tempEntity().newOrganization("test");
    testDelete_InUseByPolicy(org);
  }

  @Test
  void testDelete_InUseByPolicyInChildApp() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent("appPublicId", "appName");
    testDelete_InUseByPolicy(app.getOrganizationId(), app.getOrganizationId(), app.getId(), "in application 'appName'");
  }

  @Test
  void testDelete_InUseByPolicyInGrandChildApp() throws Exception {
    Organization org = ctx.tempEntity().newOrganization("orgName");
    Application app = ctx.tempEntity().newApplication("appName", "appPublicId", org.getId());
    testDelete_InUseByPolicy(org.getParentOrganizationId(), org.getParentOrganizationId(), app.getId(),
        "in application 'appName'");
  }

  @Test
  void testDelete_InUseByPolicyInChildOrg() throws Exception {
    Organization org = ctx.tempEntity().newOrganization("orgName");
    testDelete_InUseByPolicy(org.getParentOrganizationId(), org.getParentOrganizationId(), org.getId(),
        "in organization 'orgName'");
  }

  @Test
  void testGetApplicable() throws Exception {
    Organization org = ctx.tempEntity().newOrganization("orgName");
    ctx.tempEntity().newLicenseThreatGroup(org.getId(), "LTG-0", 5, "Apache-2.0");
    ctx.tempEntity().newLicenseThreatGroup(org.getId(), "LTG-1", 5, "EPL-1.0");

    Organization parentOrg = orgDAO.getById(org.getParentOrganizationId());
    ctx.tempEntity().newLicenseThreatGroup(parentOrg.getId(), "LTG-3", 5, "GPL-2.0", "GPL-3.0");

    ApplicableLicenseThreatGroups altgs = getApplicableLicenseThreatGroups(org.getId());
    assertThat(altgs).isNotNull();
    assertThat(altgs.licenseThreatGroupsByOwner).hasSize(2);
    assertLicenseThreatGroupsByOwner(org.getId(), org.getName(), OwnerType.ORGANIZATION, 2,
        altgs.licenseThreatGroupsByOwner.get(0));
    assertThat(altgs.licenseThreatGroupsByOwner.get(0).licenseThreatGroups)
        .allSatisfy(ltgwl -> assertThat(ltgwl.licenses).hasSize(1));
    assertLicenseThreatGroupsByOwner(parentOrg.getId(), parentOrg.getName(), OwnerType.ORGANIZATION,
        LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT + 1, altgs.licenseThreatGroupsByOwner.get(1));
    assertThat(altgs.licenseThreatGroupsByOwner.get(1).licenseThreatGroups)
        .filteredOn(ltgwl -> ltgwl.name.startsWith("LTG-"))
        .allSatisfy(ltgwl -> assertThat(ltgwl.licenses).hasSize(2));
  }

  @Test
  void testUpdateLicenseThreatGroup_DifferentOrg() throws Exception {
    Organization ownerOrg = ctx.tempEntity().newOrganization();
    LicenseThreatGroup ltg = ctx.tempEntity().newLicenseThreatGroup(ownerOrg.getId());

    Organization otherOrg = ctx.tempEntity().newOrganization();
    ltg.setOwnerId(otherOrg.getId());

    HttpResponse response = restRequest(otherOrg.getId()).body(ltg).put();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a license threat group with id " + ltg.getId() + " for owner id " + otherOrg.getId());
  }

  private void testCRUD(String ownerPublicId, String ownerId) throws Exception {
    HttpRequest request = restRequest(ownerPublicId);

    // Get all groups
    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);
    LicenseThreatGroup[] groups = response.getBody(LicenseThreatGroup[].class);
    assertThat(groups).isNotNull();
    int initialLicenseThreatGroupCount = groups.length;

    // Add a group
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("AAA My group");
    group.setThreatLevel(10);
    response = request.body(group).post();
    ctx.assertResponseStatus(200, response);
    group = response.getBody(LicenseThreatGroup.class);
    assertLicenseThreatGroup(ownerId, "AAA My group", 10, group);

    // Get all groups
    response = request.get();
    ctx.assertResponseStatus(200, response);
    groups = response.getBody(LicenseThreatGroup[].class);
    assertThat(groups).hasSize(initialLicenseThreatGroupCount + 1);
    assertLicenseThreatGroup(ownerId, "AAA My group", 10, groups[0]);

    // Update a group
    group.setName("AAA My updated group");
    response = request.body(group).put();
    ctx.assertResponseStatus(200, response);
    group = response.getBody(LicenseThreatGroup.class);
    assertLicenseThreatGroup(ownerId, "AAA My updated group", 10, group);

    // Get all groups
    response = request.get();
    ctx.assertResponseStatus(200, response);
    groups = response.getBody(LicenseThreatGroup[].class);
    assertThat(groups).hasSize(initialLicenseThreatGroupCount + 1);
    assertLicenseThreatGroup(ownerId, "AAA My updated group", 10, groups[0]);

    // Delete a group
    response = request.subpath(group.getId()).delete();
    ctx.assertResponseStatus(204, response);

    // Get all groups
    response = request.get();
    ctx.assertResponseStatus(200, response);
    groups = response.getBody(LicenseThreatGroup[].class);
    assertThat(groups).hasSize(initialLicenseThreatGroupCount);
  }

  private String getOwnerType() {
    return "organization";
  }

  private HttpRequest restRequest(String ownerId) {
    return ctx.restRequest().path(LicenseThreatGroupResource.RESOURCE_PATH).parameter(getOwnerType(), ownerId);
  }

  private void testDelete_OwnerIdMismatch(Owner owner1, Owner owner2) throws Exception {
    LicenseThreatGroup group = ctx.tempEntity().newLicenseThreatGroup(owner1.getId());

    HttpResponse response = restRequest(owner2.getPublicId()).path(group.getId()).delete();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a license threat group with ID " + group.getId() + " for "
        + getOwnerType() + " ID " + owner2.getPublicId());

    // Verify that the group was not deleted
    assertThat(licenseThreatGroupDAO.getById(group.getId())).isNotNull();
  }

  private void testDelete_InUseByPolicy(Owner owner) throws Exception {
    testDelete_InUseByPolicy(owner.getPublicId(), owner.getId(), owner.getId(), null);
  }

  private void testDelete_InUseByPolicy(
      String ownerPublicId,
      String ownerId,
      String policyOwnerId,
      String policyLocation) throws Exception
  {
    LicenseThreatGroup ltg = ctx.tempEntity().newLicenseThreatGroup(ownerId);

    Policy policy = new Policy(null, "policyName");
    policy.setOwnerId(policyOwnerId);
    Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.AND);
    constraint.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is", ltg.getId()));
    policy.addConstraint(constraint);
    ctx.tempEntity().newPolicy(policy);

    HttpResponse response = restRequest(ownerPublicId).path(ltg.getId()).delete();
    ctx.assertResponseStatus(400, response);

    String error =
        "Cannot delete the license threat group because it is used in a condition for the 'policyName' policy";
    if (null != policyLocation) {
      error = error + " " + policyLocation;
    }

    assertThat(response.getBodyText()).isEqualTo(error);
    assertThat(licenseThreatGroupDAO.getById(ltg.getId())).isNotNull();
  }

  private void assertLicenseThreatGroup(String ownerId, String name, int threatLevel, LicenseThreatGroup actual) {
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getName()).isEqualTo(name);
    assertThat(actual.getThreatLevel()).isEqualTo(threatLevel);
  }

  private ApplicableLicenseThreatGroups getApplicableLicenseThreatGroups(String ownerId) throws Exception {
    HttpResponse response = restRequest(ownerId).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    return response.getBody(ApplicableLicenseThreatGroups.class);
  }

  private void assertLicenseThreatGroupsByOwner(
      String ownerId,
      String ownerName,
      OwnerType ownerType,
      int licenseThreatGroupCount,
      LicenseThreatGroupsByOwner actual)
  {
    assertThat(actual.ownerId).isEqualTo(ownerId);
    assertThat(actual.ownerName).isEqualTo(ownerName);
    assertThat(actual.ownerType).isEqualTo(ownerType);
    assertThat(actual.licenseThreatGroups).hasSize(licenseThreatGroupCount)
        .allSatisfy(ltgwl -> assertThat(ltgwl.licenses).isNotNull());
  }
}
