/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ZSCALER;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class ApiZScalerConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiZScalerConfigurationService apiZScalerConfigurationService;

  @Before
  @Override
  public void beforeTest() {
    super.beforeTest();
    tempEntity.newSystemConfigurationProperty(ZSCALER, "true");
  }

  @After
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

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfiguration_Unauthenticated() {
    apiZScalerConfigurationService.getConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfiguration_Unauthorized() {
    login();
    apiZScalerConfigurationService.getConfiguration();
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

  @Test(expected = UnauthenticatedException.class)
  public void testSetConfiguration_Unauthenticated() {
    apiZScalerConfigurationService.setConfiguration(new ApiZScalerConfigurationDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetConfiguration_Unauthorized() {
    login();
    apiZScalerConfigurationService.setConfiguration(new ApiZScalerConfigurationDTO());
  }

  @Test
  public void testDeleteConfiguration() {
    grantConfigureSystemPermission();
    tempEntity.newZScalerConfiguration("user", "password", "https://api.zscaler.net", "validapikey1",
        true, false, false, false);
    apiZScalerConfigurationService.getConfiguration();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteConfiguration_Unauthenticated() {
    apiZScalerConfigurationService.getConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteConfiguration_Unauthorized() {
    login();
    apiZScalerConfigurationService.getConfiguration();
  }
}
