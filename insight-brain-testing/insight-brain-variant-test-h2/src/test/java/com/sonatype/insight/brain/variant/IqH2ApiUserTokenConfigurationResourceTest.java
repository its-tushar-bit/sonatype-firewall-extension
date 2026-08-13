/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiUserTokenConfigurationResourceTest
{
  private IqTestContext ctx;

  private ApiConfigurationService configService;

  @BeforeEach
  void setUp() {
    configService = ctx.lookup(ApiConfigurationService.class);
  }

  private com.sonatype.insight.brain.HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.USER_TOKEN_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testGetConfiguration_NotConfigured() throws Exception {
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    ApiUserTokenConfigurationDTO config = response.getBody(ApiUserTokenConfigurationDTO.class);
    assertThat(config).isNotNull();
    assertThat(config.userTokenDefaultExpirationDays()).isNull();
  }

  @Test
  void testGetConfiguration_Configured() throws Exception {
    configService.setConfigurationNoAuthz(Map.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 90));

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    ApiUserTokenConfigurationDTO config = response.getBody(ApiUserTokenConfigurationDTO.class);
    assertThat(config).isNotNull();
    assertThat(config.userTokenDefaultExpirationDays()).isEqualTo(90);
  }

  @Test
  void testUpdateConfiguration_ValidValue() throws Exception {
    ApiUserTokenConfigurationDTO config = new ApiUserTokenConfigurationDTO(45);

    HttpResponse response = restRequest().body(config).put();
    ctx.assertResponseStatus(200, response);

    ApiUserTokenConfigurationDTO result = response.getBody(ApiUserTokenConfigurationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.userTokenDefaultExpirationDays()).isEqualTo(45);
  }

  @Test
  void testUpdateConfiguration_BelowMinimum() throws Exception {
    ApiUserTokenConfigurationDTO config = new ApiUserTokenConfigurationDTO(0);

    HttpResponse response = restRequest().body(config).put();
    ctx.assertResponseStatus(400, response);

    assertThat(response.getBodyText()).contains("Expiration days must be between 1 and 365");
  }

  @Test
  void testUpdateConfiguration_AboveMaximum() throws Exception {
    ApiUserTokenConfigurationDTO config = new ApiUserTokenConfigurationDTO(366);

    HttpResponse response = restRequest().body(config).put();
    ctx.assertResponseStatus(400, response);

    assertThat(response.getBodyText()).contains("Expiration days must be between 1 and 365");
  }

  @Test
  void testUpdateConfiguration_NullValue() throws Exception {
    configService.setConfigurationNoAuthz(Map.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 30));

    // Update with null should be no-op
    ApiUserTokenConfigurationDTO config = new ApiUserTokenConfigurationDTO(null);

    HttpResponse response = restRequest().body(config).put();
    ctx.assertResponseStatus(200, response);

    // Value should remain unchanged
    ApiUserTokenConfigurationDTO result = response.getBody(ApiUserTokenConfigurationDTO.class);
    assertThat(result.userTokenDefaultExpirationDays()).isEqualTo(30);
  }

  @Test
  void testResetConfiguration_ValidProperty() throws Exception {
    configService.setConfigurationNoAuthz(Map.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 60));
    HttpResponse response = restRequest()
        .query("property", USER_TOKEN_DEFAULT_EXPIRATION_DAYS)
        .delete();
    ctx.assertResponseStatus(200, response);

    ApiUserTokenConfigurationDTO result = response.getBody(ApiUserTokenConfigurationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.userTokenDefaultExpirationDays()).isNull();
  }

  @Test
  void testResetConfiguration_NoProperties() throws Exception {
    HttpResponse response = restRequest().delete();
    ctx.assertResponseStatus(400, response);

    assertThat(response.getBodyText()).contains("No properties specified for reset");
  }

  @Test
  void testResetConfiguration_InvalidProperty() throws Exception {
    HttpResponse response = restRequest()
        .query("property", "invalidProperty")
        .delete();
    ctx.assertResponseStatus(400, response);

    assertThat(response.getBodyText()).contains("Invalid user token property");
  }
}
