/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Date;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiDevelopmentPrioritiesResourceTest
{
  private IqTestContext ctx;

  static final String GIVEN_APP_ID = "some-app-public-id";

  static final String GIVEN_SCAN_ID = "57e6e8169eca4b5a8e5d48d624c9e1ee";

  static final String GIVEN_ORG_ID = "some-org-id";

  private Application application;

  @BeforeEach
  void setup() throws Exception {
    final Organization organization = ctx.tempEntity().newOrganizationWithSpecificId(GIVEN_ORG_ID);
    application = ctx.tempEntity()
        .newApplicationWithParent(GIVEN_APP_ID, "some-app-name",
            organization.getId());

    ctx.tempFolder().newFile();
    ctx.createReportFile(application.getId(), GIVEN_SCAN_ID,
        "/DevelopmentPrioritiesApiResourceTest/sample-report");
    ctx.createScanFile(application.getId(), GIVEN_SCAN_ID);
  }

  private static final String GET_PRIORITIES_PATH =
      "api/v2/developer/priorities/" + GIVEN_APP_ID + "/" + GIVEN_SCAN_ID;

  @Test
  void testGetPriorities_returnsCorrectErrorWithoutAuthentication() throws Exception {
    final HttpResponse response = ctx.restRequest()
        .anon()
        .path(GET_PRIORITIES_PATH)
        .get();

    ctx.assertResponseStatus(401, response);
    assertThat(response.getBodyText()).isEqualTo("Missing credentials.");
  }

  @Test
  void testGetPriorities_ReturnsCorrectErrorWhenUserDoesNotHaveReadAccessToApp() throws Exception {
    final User userWithoutPermissionsToViewApp = ctx.tempEntity().newUser();

    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);

    final HttpResponse response = ctx.restRequest()
        .auth(userWithoutPermissionsToViewApp)
        .path(GET_PRIORITIES_PATH)
        .get();

    ctx.assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo("Insufficient permissions");
  }

  @Test
  void testGetPriorities_returnsCorrectErrorWhenLicenseDoesNotIncludeDevelopment() throws Exception {
    ctx.setLicenseProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    // Explicitly clear the DEVELOPER_DASHBOARD feature — reset() sets EnumSet.allOf(LicensedFeature.class)
    // on DefaultProductLicense, and setProducts() alone doesn't update features in the mock manager,
    // so the feature check falls through to super.getFeatures() which still has all features.
    ctx.setFeatures();

    final HttpResponse response = ctx.restRequest()
        .auth()
        .path(GET_PRIORITIES_PATH)
        .get();

    ctx.assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo("This server is not licensed for Sonatype Developer.");
  }

  @Test
  void testGetPriorities_returnsSuccessGivenReportExistsAndUserIsAuthorized() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);

    final HttpResponse response = ctx.restRequest()
        .auth()
        .path(GET_PRIORITIES_PATH)
        .get();

    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testGetPrioritiesExport_returnsCorrectErrorWithoutAuthentication() throws Exception {
    final HttpResponse response = ctx.restRequest()
        .anon()
        .path(GET_PRIORITIES_PATH + "/export")
        .get();

    ctx.assertResponseStatus(401, response);
    assertThat(response.getBodyText()).isEqualTo("Missing credentials.");
  }

  @Test
  void testGetPrioritiesExport_ReturnsCorrectErrorWhenUserDoesNotHaveReadAccessToApp() throws Exception {
    final User userWithoutPermissionsToViewApp = ctx.tempEntity().newUser();

    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);

    final HttpResponse response = ctx.restRequest()
        .auth(userWithoutPermissionsToViewApp)
        .path(GET_PRIORITIES_PATH + "/export")
        .get();

    ctx.assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo("Insufficient permissions");
  }

  @Test
  void testGetPrioritiesExport_returnsSuccessWhenCorrectPermissions() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);

    ctx.tempEntity()
        .newPolicyEvaluation(application.getId(), BuildStageType.ID,
            GIVEN_SCAN_ID, new Date(System.currentTimeMillis()));

    final HttpResponse response = ctx.restRequest()
        .auth()
        .path(GET_PRIORITIES_PATH + "/export")
        .get();

    ctx.assertResponseStatus(200, response);

    String[] lines = response.getBodyText().split("\r\n");
    assertThat(lines).hasSize(6);
    assertThat(lines[0]).isEqualTo("Display Name,Component Identifier,Component Hash,Dependency Type," +
        "Has Fail Action On Component,Action,Highest Threat,Highest Threat Policy Name," +
        "Highest Threat Policy Constraint Name,Security Reachable,Priority,Remediation Type,Remediation Version," +
        "Highest Reachable Threat,Has Same Violations On Main,Has Expired Waiver,Has Soon To Expire Waiver," +
        "Is All Violations Waived,Waiver Expiration Details,Waived Violations Count,Has Auto Waiver");
    assertThat(lines[1]).isEqualTo("displayName,maven: {artifactId=jclouds-bouncycastle, " +
        "groupId=org.jclouds.driver, version=1.3.1},1fbeda8a0725179493e9,Unknown,false,none,10,Security-Critical," +
        "Medium risk CVSS score,,1,,,0,false,false,false,false,,0,false");
    assertThat(lines[2]).isEqualTo("displayName,maven: {artifactId=tomcat-util, groupId=tomcat, " +
        "version=5.5.23},1249e25aebb15358be45,Unknown,false,none,9,Security-High,High risk CVSS score,,2,,,0" +
        ",false,false,false,false,,0,false");
    assertThat(lines[3]).isEqualTo("displayName,maven: {artifactId=geronimo-tomcat, groupId=geronimo, " +
        "version=1.0},30a69958223a2c6215e2,Unknown,false,none,7,Security-Medium,Medium risk CVSS score,,3,,,0" +
        ",false,false,false,false,,0,false");
    assertThat(lines[4]).isEqualTo("displayName,maven: {artifactId=com.alkacon.opencms.v8.twitter, " +
        "groupId=org.opencms.modules, version=8.0.2},2c2a4719e64a7e39545c,Unknown,false,none,2,Security-Low," +
        "Medium risk CVSS score,,4,,,0,false,false,false,false,,0,false");
    assertThat(lines[5]).isEqualTo("displayName,maven: {artifactId=logback-access, groupId=ch.qos.logback, " +
        "version=0.6},47b6857af4a1cc50875a,Unknown,false,none,2,Security-Low,Medium risk CVSS score,,5,,,0," +
        "false,false,false,false,,0,false");
  }
}
