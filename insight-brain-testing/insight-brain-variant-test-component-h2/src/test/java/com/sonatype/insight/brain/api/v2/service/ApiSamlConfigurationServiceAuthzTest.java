/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.SAML_ENABLED;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ApiSamlConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiSamlConfigurationService apiSamlConfigurationService;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Test
  public void testGetSamlConfiguration_Authorized() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration());
    grantConfigureSystemPermission();
    apiSamlConfigurationService.getSamlConfiguration();
  }

  @Test
  public void testGetSamlConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiSamlConfigurationService.getSamlConfiguration());
  }

  @Test
  public void testGetSamlConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiSamlConfigurationService.getSamlConfiguration());
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_Authorized() {
    setBaseUrl("http://iq-server:8070/");
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class,
        () -> apiSamlConfigurationService.insertOrUpdateSamlConfiguration("", new ApiSamlConfigurationDTO()));
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiSamlConfigurationService.insertOrUpdateSamlConfiguration("", new ApiSamlConfigurationDTO()));
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiSamlConfigurationService.insertOrUpdateSamlConfiguration("", new ApiSamlConfigurationDTO()));
  }

  @Test
  public void testGetSamlConfiguration_AuthorizedAndSamlDisabled() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration());
    grantConfigureSystemPermission();

    SAML_ENABLED.setEnabled(false);

    assertThrows(NotFoundException.class, () -> apiSamlConfigurationService.getSamlConfiguration());
  }

  @Test
  public void testDeleteSamlConfiguration_Authorized() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration());
    grantConfigureSystemPermission();
    apiSamlConfigurationService.deleteSamlConfiguration();
  }

  @Test
  public void testDeleteSamlConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiSamlConfigurationService.deleteSamlConfiguration());
  }

  @Test
  public void testDeleteSamlConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiSamlConfigurationService.deleteSamlConfiguration());
  }

  @Test
  public void testDeleteSamlConfiguration_AuthorizedAndSamlDisabled() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration());
    grantConfigureSystemPermission();

    SAML_ENABLED.setEnabled(false);

    assertThrows(NotFoundException.class, () -> apiSamlConfigurationService.deleteSamlConfiguration());
  }

  @Test
  public void testGetMetadata_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> apiSamlConfigurationService.getMetadata());
  }

  @Test
  public void testGetMetadata_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiSamlConfigurationService.getMetadata());
  }

  @Test
  public void testGetMetadata_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiSamlConfigurationService.getMetadata());
  }
}
