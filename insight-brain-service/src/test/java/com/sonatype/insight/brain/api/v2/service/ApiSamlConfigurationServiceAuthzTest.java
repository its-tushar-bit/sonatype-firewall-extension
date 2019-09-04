/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiSamlConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiSamlConfigurationService apiSamlConfigurationService;

  @Inject
  private InsightConfig config;

  @Test
  public void testGetSamlConfiguration_Authorized() {
    tempEntity.newSamlConfiguration("<xml></xml>", "ent-id", "first-name", "last-name", "e-mail", "user-name", "teams");
    grantConfigureSystemPermission();
    apiSamlConfigurationService.getSamlConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSamlConfiguration_Unauthorized() {
    login();
    apiSamlConfigurationService.getSamlConfiguration();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSamlConfiguration_Unauthenticated() {
    apiSamlConfigurationService.getSamlConfiguration();
  }

  @Test(expected = BadRequestException.class)
  public void testInsertOrUpdateSamlConfiguration_Authorized() {
    config.setBaseUrl("http://iq-server:8070/");
    grantConfigureSystemPermission();
    apiSamlConfigurationService.insertOrUpdateSamlConfiguration("", new ApiSamlConfigurationDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testInsertOrUpdateSamlConfiguration_Unauthorized() {
    login();
    apiSamlConfigurationService.insertOrUpdateSamlConfiguration("", new ApiSamlConfigurationDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testInsertOrUpdateSamlConfiguration_Unauthenticated() {
    apiSamlConfigurationService.insertOrUpdateSamlConfiguration("", new ApiSamlConfigurationDTO());
  }

  @Test
  public void testDeleteSamlConfiguration_Authorized() throws Exception {
    tempEntity.newSamlConfiguration(validIdentityProviderXml(),
        "ent-id", "first-name", "last-name", "e-mail", "user-name", "teams");
    grantConfigureSystemPermission();
    apiSamlConfigurationService.deleteSamlConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteSamlConfiguration_Unauthorized() {
    login();
    apiSamlConfigurationService.deleteSamlConfiguration();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteSamlConfiguration_Unauthenticated() {
    apiSamlConfigurationService.deleteSamlConfiguration();
  }

  @Test(expected = NotFoundException.class)
  public void testGetMetadata_Authorized() {
    grantConfigureSystemPermission();
    apiSamlConfigurationService.getMetadata();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetMetadata_Unauthorized() {
    login();
    apiSamlConfigurationService.getMetadata();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetMetadata_Unauthenticated() {
    apiSamlConfigurationService.getMetadata();
  }

  private String validIdentityProviderXml() throws Exception {
    URL resource = getClass().getResource("/" + getClass().getSimpleName() + "/identity-provider-metadata.xml");
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }
}
