/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlConfigurationDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSourceControlConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiSourceControlConfigurationService service;

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfiguration_Unauthenticated() {
    service.getConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfiguration_Unauthorized() {
    login();
    service.getConfiguration();
  }

  @Test
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    tempEntity.newSourceControlConfiguration();
    ApiSourceControlConfigurationDTO result = service.getConfiguration();
    assertThat(result).isNotNull();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetConfiguration_Unauthenticated() {
    service.setConfiguration(null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetConfiguration_Unauthorized() {
    login();
    service.setConfiguration(null);
  }

  @Test(expected = BadRequestException.class)
  public void testSetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.setConfiguration(null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteConfiguration_Unauthenticated() {
    service.deleteConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteConfiguration_Unauthorized() {
    login();
    service.deleteConfiguration();
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.deleteConfiguration();
  }
}
