/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenConfigurationDTO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ApiUserTokenConfigurationServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApiUserTokenConfigurationService userTokenConfigService;

  @Inject
  private ApiConfigurationService configurationService;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Test
  public void testGetConfiguration_NotConfigured() {
    ApiUserTokenConfigurationDTO config = userTokenConfigService.getConfiguration();
    assertThat(config.userTokenDefaultExpirationDays()).isNull();
  }

  @Test
  public void testGetConfiguration_Configured() {
    configurationService.setConfigurationNoAuthz(Map.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 90));

    ApiUserTokenConfigurationDTO config = userTokenConfigService.getConfiguration();
    assertThat(config.userTokenDefaultExpirationDays()).isEqualTo(90);
  }

  @Test
  public void testUpdateConfiguration_ValidValue() {
    ApiUserTokenConfigurationDTO config = new ApiUserTokenConfigurationDTO(45);
    userTokenConfigService.updateConfiguration(config);

    Map<String, Object> result =
        configurationService.getConfigurationNoAuthz(Set.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS));
    assertThat(result).containsEntry(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 45);
  }

  @Test
  public void testUpdateConfiguration_NullValue() {
    configurationService.setConfigurationNoAuthz(Map.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 30));

    // Update with null should be no-op
    ApiUserTokenConfigurationDTO config = new ApiUserTokenConfigurationDTO(null);
    userTokenConfigService.updateConfiguration(config);

    // Value should remain unchanged
    Map<String, Object> result =
        configurationService.getConfigurationNoAuthz(Set.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS));
    assertThat(result).containsEntry(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 30);
  }

  @Test
  public void testUpdateConfiguration_NullConfiguration() {
    assertThatThrownBy(() -> userTokenConfigService.updateConfiguration(null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Configuration cannot be null");
  }

  @Test
  public void testUpdateConfiguration_BelowMinimum() {
    ApiUserTokenConfigurationDTO config = new ApiUserTokenConfigurationDTO(0);

    assertThatThrownBy(() -> userTokenConfigService.updateConfiguration(config))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Expiration days must be between 1 and 365");
  }

  @Test
  public void testUpdateConfiguration_AboveMaximum() {
    ApiUserTokenConfigurationDTO config = new ApiUserTokenConfigurationDTO(366);

    assertThatThrownBy(() -> userTokenConfigService.updateConfiguration(config))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Expiration days must be between 1 and 365");
  }

  @Test
  public void testResetConfiguration_ValidProperty() {
    configurationService.setConfigurationNoAuthz(Map.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 60));
    userTokenConfigService.resetConfiguration(Set.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS));

    Map<String, Object> result = configurationService.getConfigurationNoAuthz(
        Set.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS));
    assertThat(result.get(USER_TOKEN_DEFAULT_EXPIRATION_DAYS)).isNull();
  }

  @Test
  public void testResetConfiguration_EmptySet() {
    assertThatThrownBy(() -> userTokenConfigService.resetConfiguration(Collections.emptySet()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("No properties specified for reset");
  }

  @Test
  public void testResetConfiguration_NullSet() {
    assertThatThrownBy(() -> userTokenConfigService.resetConfiguration(null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("No properties specified for reset");
  }

  @Test
  public void testResetConfiguration_InvalidProperty() {
    assertThatThrownBy(() -> userTokenConfigService.resetConfiguration(Set.of("invalidProperty")))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Invalid user token property: invalidProperty");
  }

  @Test
  public void testParseExpirationDays_StringValue() {
    // Set String value directly in database to test parsing logic
    tempEntity.newSystemConfigurationProperty(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, "75");

    ApiUserTokenConfigurationDTO config = userTokenConfigService.getConfiguration();
    assertThat(config.userTokenDefaultExpirationDays()).isEqualTo(75);
  }

  @Test
  public void testParseExpirationDays_InvalidString() {
    // NumberUtils.toInt() returns 0 for invalid strings
    tempEntity.newSystemConfigurationProperty(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, "not-a-number");

    ApiUserTokenConfigurationDTO config = userTokenConfigService.getConfiguration();
    assertThat(config.userTokenDefaultExpirationDays()).isZero();
  }
}
