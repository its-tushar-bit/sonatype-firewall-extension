/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license.entitlement;

import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EntitlementRequiredExceptionMapperTest
{
  private final EntitlementRequiredExceptionMapper mapper = new EntitlementRequiredExceptionMapper();

  @Test
  public void testReturns402Status() {
    EntitlementRequiredException exception = new EntitlementRequiredException(LicensedFeature.CUSTOM_POLICIES);
    Response response = mapper.toResponse(exception);
    assertThat(response.getStatus()).isEqualTo(402);
  }

  @Test
  public void testReturnsJsonContentType() {
    EntitlementRequiredException exception = new EntitlementRequiredException(LicensedFeature.CUSTOM_POLICIES);
    Response response = mapper.toResponse(exception);
    assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testResponseBodyStructure() {
    EntitlementRequiredException exception = new EntitlementRequiredException(LicensedFeature.CUSTOM_POLICIES);
    Response response = mapper.toResponse(exception);
    Map<String, Object> body = (Map<String, Object>) response.getEntity();

    assertThat(body.get("error")).isEqualTo("feature_not_available");
    assertThat(body.get("code")).isEqualTo("ENTITLEMENT_REQUIRED");
    assertThat(body.get("feature")).isEqualTo("CUSTOM_POLICIES");
    assertThat(body.get("message")).isNotNull();
    assertThat(body.get("upgrade_hint")).isNotNull();
    assertThat(body.get("cta_url")).isNotNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testResponseIncludesDocsUrl() {
    EntitlementRequiredException exception = new EntitlementRequiredException(LicensedFeature.BULK_WAIVERS);
    Response response = mapper.toResponse(exception);
    Map<String, Object> body = (Map<String, Object>) response.getEntity();

    assertThat(body.get("docs_url")).isNotNull();
    assertThat((String) body.get("docs_url")).contains("help.sonatype.com");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testResponseOmitsDocsUrlWhenNull() {
    EntitlementRequiredException exception = new EntitlementRequiredException(LicensedFeature.DASHBOARD);
    Response response = mapper.toResponse(exception);
    Map<String, Object> body = (Map<String, Object>) response.getEntity();

    assertThat(body).doesNotContainKey("docs_url");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testResponseFeatureMatchesException() {
    EntitlementRequiredException exception = new EntitlementRequiredException(LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Response response = mapper.toResponse(exception);
    Map<String, Object> body = (Map<String, Object>) response.getEntity();

    assertThat(body.get("feature")).isEqualTo("AUTO_WAIVER_MANAGEMENT");
  }
}
