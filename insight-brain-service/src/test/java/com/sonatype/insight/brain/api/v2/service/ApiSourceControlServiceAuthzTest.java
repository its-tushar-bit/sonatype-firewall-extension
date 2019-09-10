/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.66
 */
public class ApiSourceControlServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String VALID_URL = "https://example.com/organization/project";
  
  @Inject
  public ApiSourceControlService sourceControlService;

  private ApiSourceControlAdapter apiSourceControlAdapter = new ApiSourceControlAdapter();

  @Test(expected = UnauthenticatedException.class)
  public void testGetAll_Unauthenticated() {
    sourceControlService.getAll();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAll_Unauthorized() {
    login();
    sourceControlService.getAll();
  }

  @Test
  public void testGetAll_Authorized() {
    grantGlobalPermission(Permission.READ);
    assertThat(sourceControlService.getAll()).isEmpty();
  }

  @Test
  public void testGetSourceControlByOwner_Authorized() {
    grantReadPermission(app.getId());
    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    ApiSourceControlDTO sourceControlByApplicationId =
        sourceControlService.getSourceControlByOwner(
            OwnerType.APPLICATION, app.getId());
    assertThat(sourceControlByApplicationId.id).isEqualTo(sourceControl.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSourceControlByOwner_Unauthenticated() {
    sourceControlService.getSourceControlByOwner(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSourceControlByOwner_Unauthorized() {
    login();
    sourceControlService.getSourceControlByOwner(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddSourceControlByOwner_Unauthenticated() {
    sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddSourceControlByOwner_Unauthorized() {
    login();
    sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
  }

  @Test
  public void testAddSourceControlByOwner_Authorized() {
    grantWritePermission(app.getId());
    sourceControlService.addSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(),
        apiSourceControlAdapter.convertToDTO(new SourceControl(
            app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB)));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateSourceControlByOwner_Unauthenticated() {
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateSourceControlByOwner_Unauthorized() {
    login();
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), new ApiSourceControlDTO());
  }

  @Test
  public void testUpdateSourceControlByOwner_Authorized() {
    grantWritePermission(app.getId());
    ApiSourceControlDTO sourceControl = sourceControlService.addSourceControlByOwner(OwnerType.APPLICATION,
        app.getId(), apiSourceControlAdapter.convertToDTO(
            new SourceControl(app.getId(), VALID_URL, "token",
                SourceControlProvider.GITHUB)));
    sourceControl.token = "newToken";
    sourceControlService.updateSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), sourceControl);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteSourceControlByOwner_Unauthenticated() {
    sourceControlService.deleteSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), "any");
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteSourceControlByOwner_Unauthorized() {
    login();
    sourceControlService.deleteSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), "any");
  }

  @Test
  public void testDeleteSourceControlByOwner_Authorized() {
    grantWritePermission(app.getId());
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    sourceControlService.deleteSourceControlByOwner(
        OwnerType.APPLICATION, app.getId(), sourceControl.getId());
  }

  @Test
  public void testAddOrUpdateSourceControl_Authorized() {
    // ensure org record exists
    tempEntity.newSourceControl(app.getOrganizationId(), null, "token", SourceControlProvider.GITHUB);
    grantWritePermission(app.getId());
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddOrUpdateSourceControl_Unauthorized() {
    login();
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddOrUpdateSourceControl_Unauthenticated() {
    sourceControlService.addOrUpdateSourceControl(app.getPublicId(), VALID_URL);
  }
}
