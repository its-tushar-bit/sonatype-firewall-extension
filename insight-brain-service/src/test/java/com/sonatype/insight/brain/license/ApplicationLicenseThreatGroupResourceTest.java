/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationLicenseThreatGroupResourceTest
    extends AbstractLicenseThreatGroupResourceTest
{
  private ApplicationDAO applicationDAO;

  @Before
  @Override
  public void setUp() {
    super.setUp();
    applicationDAO = lookup(ApplicationDAO.class);
  }

  @Test
  public void testCRUD() throws Exception {
    String appPublicId = "LicenseThreatGroupResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);
    testCRUD(appPublicId, application.getId());
  }

  @Test
  public void testDelete_OwnerIdMismatch() throws Exception {
    Application application1 = tempEntity.newApplicationWithParent("LicenseThreatGroupResourceTest_AppId1");
    Application application2 = tempEntity.newApplicationWithParent("LicenseThreatGroupResourceTest_AppId2");
    testDelete_OwnerIdMismatch(application1, application2);
  }

  @Test
  public void testDelete_InUseByPolicy() throws Exception {
    Application app = tempEntity.newApplicationWithParent("appPublicId");
    testDelete_InUseByPolicy(app);
  }

  @Test
  public void testGetApplicable() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    tempEntity.newLicenseThreatGroup(org.getId(), "LTG-2", 5, "GPL-2.0", "GPL-3.0");
    Application app = tempEntity.newApplication("appName", "appPublicId", org.getId());
    app.setOrganizationId(org.getId());
    applicationDAO.update(app);
    tempEntity.newLicenseThreatGroup(app.getId(), "LTG-0", 5, "Apache-2.0");
    tempEntity.newLicenseThreatGroup(app.getId(), "LTG-1", 5, "EPL-1.0");

    Organization parentOrg = orgDAO.getById(org.getParentOrganizationId());
    tempEntity.newLicenseThreatGroup(parentOrg.getId(), "LTG-3", 5, "GPL-2.0", "GPL-3.0");

    ApplicableLicenseThreatGroups altgs = getApplicableLicenseThreatGroups(app.getPublicId());
    assertThat(altgs).isNotNull();
    assertThat(altgs.licenseThreatGroupsByOwner).hasSize(3);
    assertLicenseThreatGroupsByOwner(app.getId(), app.getName(), OwnerType.APPLICATION, 2,
        altgs.licenseThreatGroupsByOwner.get(0));
    assertThat(altgs.licenseThreatGroupsByOwner.get(0).licenseThreatGroups)
        .allSatisfy(ltgwl -> assertThat(ltgwl.licenses).hasSize(1));
    assertLicenseThreatGroupsByOwner(org.getId(), org.getName(), OwnerType.ORGANIZATION, 1,
        altgs.licenseThreatGroupsByOwner.get(1));
    assertThat(altgs.licenseThreatGroupsByOwner.get(1).licenseThreatGroups)
        .allSatisfy(ltgwl -> assertThat(ltgwl.licenses).hasSize(2));
    assertLicenseThreatGroupsByOwner(parentOrg.getId(), parentOrg.getName(), OwnerType.ORGANIZATION,
        LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT + 1, altgs.licenseThreatGroupsByOwner.get(2));
    assertThat(altgs.licenseThreatGroupsByOwner.get(1).licenseThreatGroups)
        .filteredOn(ltgwl -> ltgwl.name.startsWith("LTG-"))
        .allSatisfy(ltgwl -> assertThat(ltgwl.licenses).hasSize(2));
  }

  @Test
  public void testUpdateLicenseThreatGroup_DifferentApp() throws Exception {
    Application ownerApp = tempEntity.newApplicationWithParent("owner");
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(ownerApp.getId());

    Application otherApp = tempEntity.newApplicationWithParent("other");
    ltg.setOwnerId(otherApp.getId());

    HttpResponse response = restRequest(otherApp.getPublicId()).body(ltg).put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Cannot find a license threat group with id " + ltg.getId() + " for owner id " + otherApp.getPublicId());
  }

  protected void testCRUD(String ownerPublicId, String ownerId) throws Exception {
    HttpRequest request = restRequest(ownerPublicId);

    // Get all groups
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    LicenseThreatGroup[] groups = response.getBody(LicenseThreatGroup[].class);
    assertThat(groups).isNotNull();
    int initialLicenseThreatGroupCount = groups.length;

    // Try to add a group
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("AAA My group");
    group.setThreatLevel(10);
    response = request.body(group).post();
    assertResponseStatus(400, response); // apps not allowed to add ltgs
    assertThat(response.getBodyText()).isEqualTo("Applications are not allowed to add license threat groups.");

    group = tempEntity.newLicenseThreatGroup(ownerId, "AAA My group", 10);

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
    return "application";
  }
}
