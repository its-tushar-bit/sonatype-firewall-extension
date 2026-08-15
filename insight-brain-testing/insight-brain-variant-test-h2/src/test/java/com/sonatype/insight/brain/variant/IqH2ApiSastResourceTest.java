/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.sast.SastValidateResponseDTO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.LicensedFeature;

import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@IqH2Test
class IqH2ApiSastResourceTest
{
  private IqTestContext ctx;

  @Test
  void testValidate_Success() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    final HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
        .path("validate")
        .get();
    ctx.assertResponseStatus(200, response);
    SastValidateResponseDTO sastValidateResponseDTO = response.getBody(SastValidateResponseDTO.class);

    assertThat(sastValidateResponseDTO.isValid()).isTrue();
  }

  @Test
  void testValidate_withoutDeveloperFeature() throws Exception {
    ctx.setLicenseProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    final HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
        .path("validate")
        .get();
    ctx.assertResponseStatus(200, response);
    SastValidateResponseDTO sastValidateResponseDTO = response.getBody(SastValidateResponseDTO.class);

    assertThat(sastValidateResponseDTO.isValid()).isFalse();
  }

  @Test
  void testValidate_withoutAnyFeature() throws Exception {
    ctx.setLicenseProducts();
    ctx.setFeatures();
    final HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
        .path("validate")
        .get();
    ctx.assertResponseStatus(200, response);
    SastValidateResponseDTO sastValidateResponseDTO = response.getBody(SastValidateResponseDTO.class);

    assertThat(sastValidateResponseDTO.isValid()).isFalse();
  }

  @Test
  void testValidate_withoutLicense() throws Exception {
    ctx.uninstallLicense();
    final HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
        .path("validate")
        .get();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testValidate_withInvalidAuth() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    final HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
        .path("validate")
        .auth(new User())
        .get();
    ctx.assertResponseStatus(401, response);
  }
}
