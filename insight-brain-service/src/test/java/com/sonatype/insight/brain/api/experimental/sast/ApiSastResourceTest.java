/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Category(SlowTest.class)
public class ApiSastResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testValidate_Success() throws Exception {
    setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
        .path("validate")
        .get();
    assertResponseStatus(200, response);
    SastValidateResponseDTO sastValidateResponseDTO = response.getBody(SastValidateResponseDTO.class);

    assertThat(sastValidateResponseDTO.isValid()).isTrue();
  }

  @Test
  public void testValidate_withoutDeveloperFeature() throws Exception {
    setLicenseProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
        .path("validate")
        .get();
    assertResponseStatus(200, response);
    SastValidateResponseDTO sastValidateResponseDTO = response.getBody(SastValidateResponseDTO.class);

    assertThat(sastValidateResponseDTO.isValid()).isFalse();
  }

  @Test
  public void testValidate_withoutAnyFeature() throws Exception {
    setLicenseProducts();
    setFeatures();
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
        .path("validate")
        .get();
    assertResponseStatus(200, response);
    SastValidateResponseDTO sastValidateResponseDTO = response.getBody(SastValidateResponseDTO.class);

    assertThat(sastValidateResponseDTO.isValid()).isFalse();
  }

  @Test
  public void testValidate_withoutLicense() throws Exception {
    uninstallLicense();
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
        .path("validate")
        .get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testValidate_withInvalidAuth() throws Exception {
    setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
        .path("validate")
        .auth(new User())
        .get();
    assertResponseStatus(401, response);
  }
}
