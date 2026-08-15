/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.LinkedHashSet;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.license.LicenseThreatGroupLicenseResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2LicenseThreatGroupLicenseResourceTest
{
  private IqTestContext ctx;

  private LicenseThreatGroupDAO groupDAO;

  @BeforeEach
  void setUp() {
    groupDAO = ctx.lookup(LicenseThreatGroupDAO.class);
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId, String licenseThreatGroupId) {
    return ctx.restRequest()
        .path(LicenseThreatGroupLicenseResource.RESOURCE_PATH)
        .parameter(ownerType, ownerId, licenseThreatGroupId);
  }

  private void testSetGet(OwnerType ownerType, String ownerPublicId, String ownerId) throws Exception {
    // Create an application and a group
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("My group");
    group.setThreatLevel(4);
    groupDAO.insert(group);

    // Get
    HttpResponse response = restRequest(ownerType, ownerPublicId, group.getId()).get();
    ctx.assertResponseStatus(200, response);
    LicenseThreatGroupLicense[] licenseThreatGroupLicenses = response.getBody(LicenseThreatGroupLicense[].class);
    assertThat(licenseThreatGroupLicenses).isEmpty();

    // Set
    Set<String> licenseIds = new LinkedHashSet<>();
    licenseIds.add("GPL-2.0");
    licenseIds.add("Apache-2.0");
    response = restRequest(ownerType, ownerPublicId, group.getId()).body(licenseIds).put();
    ctx.assertResponseStatus(200, response);

    // Get
    licenseThreatGroupLicenses = response.getBody(LicenseThreatGroupLicense[].class);
    assertThat(licenseThreatGroupLicenses).hasSize(2);
    assertLicenseThreatGroupLicense(ownerId, group.getId(), "Apache-2.0", licenseThreatGroupLicenses[0]);
    assertLicenseThreatGroupLicense(ownerId, group.getId(), "GPL-2.0", licenseThreatGroupLicenses[1]);
  }

  @Test
  void testSetGet_Application() throws Exception {
    String appPublicId = "LicenseThreatGroupLicenseResourceTest_AppId";
    Application application = ctx.tempEntity().newApplicationWithParent(appPublicId);
    testSetGet(OwnerType.APPLICATION, appPublicId, application.getId());
  }

  @Test
  void testSetGet_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization("testSetGet-Organization");
    testSetGet(OwnerType.ORGANIZATION, organization.getId(), organization.getId());
  }

  private void assertLicenseThreatGroupLicense(
      String ownerId,
      String licenseThreatGroupId,
      String licenseId,
      LicenseThreatGroupLicense actual)
  {
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getLicenseThreatGroupId()).isEqualTo(licenseThreatGroupId);
    assertThat(actual.getLicenseId()).isEqualTo(licenseId);
  }
}
