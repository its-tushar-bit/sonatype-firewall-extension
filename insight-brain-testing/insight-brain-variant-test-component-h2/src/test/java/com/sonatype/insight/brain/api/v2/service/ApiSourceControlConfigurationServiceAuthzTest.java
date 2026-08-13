/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlConfigurationDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiSourceControlConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiSourceControlConfigurationService service;

  @Test
  public void testGetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.getConfiguration());
  }

  @Test
  public void testGetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.getConfiguration());
  }

  @Test
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    tempEntity.newSourceControlConfiguration();
    ApiSourceControlConfigurationDTO result = service.getConfiguration();
    assertThat(result).isNotNull();
  }

  @Test
  public void testSetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.setConfiguration(null));
  }

  @Test
  public void testSetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.setConfiguration(null));
  }

  @Test
  public void testSetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class, () -> service.setConfiguration(null));
  }

  @Test
  public void testDeleteConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.deleteConfiguration());
  }

  @Test
  public void testDeleteConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.deleteConfiguration());
  }

  @Test
  public void testDeleteConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> service.deleteConfiguration());
  }
}
