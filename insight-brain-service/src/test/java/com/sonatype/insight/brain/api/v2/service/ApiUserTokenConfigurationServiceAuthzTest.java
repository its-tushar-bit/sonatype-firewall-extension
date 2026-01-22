/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenConfigurationDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiUserTokenConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiUserTokenConfigurationService service;

  @Test
  public void testGetConfiguration_Unauthenticated() {
    assertThatThrownBy(() -> service.getConfiguration())
        .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  public void testGetConfiguration_Unauthorized() {
    login();
    assertThatThrownBy(() -> service.getConfiguration())
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    ApiUserTokenConfigurationDTO config = service.getConfiguration();
    assertThat(config).isNotNull();
  }

  @Test
  public void testUpdateConfiguration_Unauthenticated() {
    ApiUserTokenConfigurationDTO config = new ApiUserTokenConfigurationDTO(30);
    assertThatThrownBy(() -> service.updateConfiguration(config))
        .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  public void testUpdateConfiguration_Unauthorized() {
    login();
    ApiUserTokenConfigurationDTO config = new ApiUserTokenConfigurationDTO(30);
    assertThatThrownBy(() -> service.updateConfiguration(config))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void testUpdateConfiguration_Authorized() {
    grantConfigureSystemPermission();
    // Pass null to trigger validation error, proving auth passed
    assertThatThrownBy(() -> service.updateConfiguration(null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Configuration cannot be null");
  }

  @Test
  public void testResetConfiguration_Unauthenticated() {
    assertThatThrownBy(() -> service.resetConfiguration(Set.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS)))
        .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  public void testResetConfiguration_Unauthorized() {
    login();
    assertThatThrownBy(() -> service.resetConfiguration(Set.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS)))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void testResetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    // Pass empty set to trigger validation error, proving auth passed
    assertThatThrownBy(() -> service.resetConfiguration(Set.of()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("No properties specified for reset");
  }
}
