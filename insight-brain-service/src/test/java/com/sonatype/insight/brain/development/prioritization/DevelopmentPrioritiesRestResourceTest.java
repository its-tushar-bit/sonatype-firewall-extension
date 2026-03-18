/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.IOException;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class DevelopmentPrioritiesRestResourceTest
    extends AbstractResourceTest
{
  static final String GIVEN_APP_ID = "some-app-public-id";

  static final String GIVEN_SCAN_ID = "57e6e8169eca4b5a8e5d48d624c9e1ee";

  @Before
  public void setup() throws IOException {
    final Application application = tempEntity.newApplicationWithParent(GIVEN_APP_ID);

    tempDir.newFile();
    createReportFile(application.getId(), GIVEN_SCAN_ID, "/DevelopmentPrioritiesApiResourceTest/sample-report");
    createScanFile(application.getId(), GIVEN_SCAN_ID);
  }

  private static final String GET_PRIORITIES_PATH =
      "rest/developer/priorities/some-app-public-id/57e6e8169eca4b5a8e5d48d624c9e1ee";

  @Test
  public void testGetPriorities_returnsCorrectErrorWithoutAuthentication() throws Exception {
    final HttpResponse response = restRequest()
        .anon()
        .path(GET_PRIORITIES_PATH)
        .get();

    assertResponseStatus(401, response);
    assertThat(response.getBodyText()).isEqualTo("Missing credentials.");
  }

  @Test
  public void testGetPriorities_ReturnsCorrectErrorWhenUserDoesNotHaveReadAccessToApp() throws Exception {
    final User userWithoutPermissionsToViewApp = tempEntity.newUser();

    getTestProductLicenseManager().setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);

    final HttpResponse response = restRequest()
        .auth(userWithoutPermissionsToViewApp)
        .path(GET_PRIORITIES_PATH)
        .get();

    assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo("Insufficient permissions");
  }

  @Test
  public void testGetPriorities_returnsCorrectErrorWhenLicenseDoesNotIncludeDevelopment() throws Exception {
    getTestProductLicenseManager().setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);

    final HttpResponse response = restRequest()
        .path(GET_PRIORITIES_PATH)
        .get();

    assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo("This server is not licensed for Sonatype Developer.");
  }

  @Test
  public void testGetPriorities_returnsSuccessGivenReportExistsAndUserIsAuthorized() throws Exception {
    getTestProductLicenseManager().setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);

    final HttpResponse response = restRequest()
        .path(GET_PRIORITIES_PATH)
        .get();

    assertResponseStatus(200, response);
  }
}
