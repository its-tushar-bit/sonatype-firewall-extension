/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ZSCALER;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiZScalerConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiZScalerConfigurationService apiZScalerConfigurationService;

  @BeforeEach
  @Override
  public void beforeTest() {
    super.beforeTest();
    tempEntity.newSystemConfigurationProperty(ZSCALER, "true");
  }

  @AfterEach
  @Override
  public void afterTest() {
    tempEntity.deleteSystemConfigurationProperty(ZSCALER);
  }

  @Test
  public void testGetConfiguration() {
    grantConfigureSystemPermission();
    tempEntity.newZScalerConfiguration("user", "password", "https://api.zscaler.net", "validapikey1",
        true, false, false, false);
    apiZScalerConfigurationService.getConfiguration();
  }

  @Test
  public void testGetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiZScalerConfigurationService.getConfiguration());
  }

  @Test
  public void testGetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiZScalerConfigurationService.getConfiguration());
  }

  @Test
  public void testSetConfiguration() {
    grantConfigureSystemPermission();
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setApiKey("validapikey1");
    dto.setMavenFormatEnabled(true);
    dto.setEulaAgreed(true);
    apiZScalerConfigurationService.setConfiguration(dto);
  }

  @Test
  public void testSetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiZScalerConfigurationService.setConfiguration(new ApiZScalerConfigurationDTO()));
  }

  @Test
  public void testSetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiZScalerConfigurationService.setConfiguration(new ApiZScalerConfigurationDTO()));
  }

  @Test
  public void testDeleteConfiguration() {
    grantConfigureSystemPermission();
    tempEntity.newZScalerConfiguration("user", "password", "https://api.zscaler.net", "validapikey1",
        true, false, false, false);
    apiZScalerConfigurationService.getConfiguration();
  }

  @Test
  public void testDeleteConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiZScalerConfigurationService.getConfiguration());
  }

  @Test
  public void testDeleteConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiZScalerConfigurationService.getConfiguration());
  }
}
