/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlProvider;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

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
  public void testGetSourceControlByApplicationId_Authorized() {
    grantReadPermission(app.getId());
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    SourceControl sourceControlByApplicationId = sourceControlService.getSourceControlByApplicationId(app.getId());
    assertThat(sourceControlByApplicationId.getId()).isEqualTo(sourceControl.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSourceControlByApplicationId_Unauthenticated() {
    sourceControlService.getSourceControlByApplicationId(app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSourceControlByApplicationId_Unauthorized() {
    login();
    sourceControlService.getSourceControlByApplicationId(app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddSourceControl_Unauthenticated() {
    sourceControlService.addSourceControl(app.getId(), new SourceControl());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddSourceControl_Unauthorized() {
    login();
    sourceControlService.addSourceControl(app.getId(), new SourceControl());
  }

  @Test
  public void testAddSourceControl_Authorized() {
    grantWritePermission(app.getId());
    sourceControlService
        .addSourceControl(app.getId(),
            new SourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateSourceControl_Unauthenticated() {
    sourceControlService.updateSourceControl(app.getId(), new SourceControl());
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateSourceControl_Unauthorized() {
    login();
    sourceControlService.updateSourceControl(app.getId(), new SourceControl());
  }

  @Test
  public void testUpdateSourceControl_Authorized() {
    grantWritePermission(app.getId());
    SourceControl sourceControl = sourceControlService
        .addSourceControl(app.getId(),
            new SourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB));
    sourceControl.setToken("newToken");
    sourceControlService.updateSourceControl(app.getId(), sourceControl);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteSourceControl_Unauthenticated() {
    sourceControlService.deleteSourceControl(app.getId(), "any");
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteSourceControl_Unauthorized() {
    login();
    sourceControlService.deleteSourceControl(app.getId(), "any");
  }

  @Test
  public void testDeleteSourceControl_Authorized() {
    grantWritePermission(app.getId());
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    sourceControlService.deleteSourceControl(app.getId(), sourceControl.getId());
  }
}
