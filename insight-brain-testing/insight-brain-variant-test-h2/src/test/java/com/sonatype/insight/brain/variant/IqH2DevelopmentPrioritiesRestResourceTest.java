/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2DevelopmentPrioritiesRestResourceTest
{
  private IqTestContext ctx;

  static final String GIVEN_APP_ID = "some-app-public-id";

  static final String GIVEN_SCAN_ID = "57e6e8169eca4b5a8e5d48d624c9e1ee";

  @BeforeEach
  void setup() throws Exception {
    final Application application = ctx.tempEntity().newApplicationWithParent(GIVEN_APP_ID);

    ctx.tempFolder().newFile();
    ctx.createReportFile(application.getId(), GIVEN_SCAN_ID, "/DevelopmentPrioritiesApiResourceTest/sample-report");
    ctx.createScanFile(application.getId(), GIVEN_SCAN_ID);
  }

  private static final String GET_PRIORITIES_PATH =
      "rest/developer/priorities/some-app-public-id/57e6e8169eca4b5a8e5d48d624c9e1ee";

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
        .path(GET_PRIORITIES_PATH)
        .get();

    ctx.assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo("This server is not licensed for Sonatype Developer.");
  }

  @Test
  void testGetPriorities_returnsSuccessGivenReportExistsAndUserIsAuthorized() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);

    final HttpResponse response = ctx.restRequest()
        .path(GET_PRIORITIES_PATH)
        .get();

    ctx.assertResponseStatus(200, response);
  }
}
