/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationLicenseThreatGroupResourceTest
    extends AbstractLicenseThreatGroupResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    Organization organization = tempEntity.newOrganization("testCRUD-Organization");
    testCRUD(organization.getId(), organization.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch() throws Exception {
    Organization organization1 = tempEntity.newOrganization("testDeleteOwnerIdMismatch1");
    Organization organization2 = tempEntity.newOrganization("testDeleteOwnerIdMismatch2");
    testDelete_OwnerIdMismatch(organization1, organization2);
  }

  @Test
  public void testDelete_InUseByPolicy() throws Exception {
    Organization org = tempEntity.newOrganization("test");
    testDelete_InUseByPolicy(org);
  }

  @Test
  public void testDelete_InUseByPolicyInChildApp() throws Exception {
    Application app = tempEntity.newApplicationWithParent("appPublicId", "appName");
    testDelete_InUseByPolicy(app.getOrganizationId(), app.getOrganizationId(), app.getId(), "in application 'appName'");
  }

  @Test
  public void testDelete_InUseByPolicyInGrandChildApp() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    Application app = tempEntity.newApplication("appName", "appPublicId", org.getId());
    testDelete_InUseByPolicy(org.getParentOrganizationId(), org.getParentOrganizationId(), app.getId(),
        "in application 'appName'");
  }

  @Test
  public void testDelete_InUseByPolicyInChildOrg() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    testDelete_InUseByPolicy(org.getParentOrganizationId(), org.getParentOrganizationId(), org.getId(),
        "in organization 'orgName'");
  }

  @Test
  public void testGetApplicable() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    tempEntity.newLicenseThreatGroup(org.getId(), "LTG-0", 5, "Apache-2.0");
    tempEntity.newLicenseThreatGroup(org.getId(), "LTG-1", 5, "EPL-1.0");

    Organization parentOrg = orgDAO.getById(org.getParentOrganizationId());
    tempEntity.newLicenseThreatGroup(parentOrg.getId(), "LTG-3", 5, "GPL-2.0", "GPL-3.0");

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
  public void testUpdateLicenseThreatGroup_DifferentOrg() throws Exception {
    Organization ownerOrg = tempEntity.newOrganization();
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(ownerOrg.getId());

    Organization otherOrg = tempEntity.newOrganization();
    ltg.setOwnerId(otherOrg.getId());

    HttpResponse response = restRequest(otherOrg.getId()).body(ltg).put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a license threat group with id " + ltg.getId() + " for owner id " + otherOrg.getId());
  }

  protected void testCRUD(String ownerPublicId, String ownerId) throws Exception {
    HttpRequest request = restRequest(ownerPublicId);

    // Get all groups
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    LicenseThreatGroup[] groups = response.getBody(LicenseThreatGroup[].class);
    assertThat(groups).isNotNull();
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
    assertThat(groups).hasSize(initialLicenseThreatGroupCount + 1);
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
    assertThat(groups).hasSize(initialLicenseThreatGroupCount + 1);
    assertLicenseThreatGroup(ownerId, "AAA My updated group", 10, groups[0]);

    // Delete a group
    response = request.subpath(group.getId()).delete();
    assertResponseStatus(204, response);

    // Get all groups
    response = request.get();
    assertResponseStatus(200, response);
    groups = response.getBody(LicenseThreatGroup[].class);
    assertThat(groups).hasSize(initialLicenseThreatGroupCount);
  }

  @Override
  protected String getOwnerType() {
    return "organization";
  }
}
