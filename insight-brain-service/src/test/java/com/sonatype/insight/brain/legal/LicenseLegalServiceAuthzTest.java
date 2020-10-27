/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseLegalServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private LicenseLegalService licenseLegalService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetLatestRawReportForApplication_Unauthenticated() {
    licenseLegalService.getLatestRawReportForApplication(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLatestRawReportForApplication_Unauthorized() {
    login();
    licenseLegalService.getLatestRawReportForApplication(app.getPublicId());
  }

  @Test
  public void testGetLatestRawReportForApplication_Authorized() {
    grantReadPermission(app.getId());
    licenseLegalService.getLatestRawReportForApplication(app.getPublicId());
  }

  @Test
  public void testGetApplications_Unauthenticated() {
    assertThat(licenseLegalService.getApplications()).isEmpty();
  }

  @Test
  public void testGetApplications_Unauthorized() {
    login();
    assertThat(licenseLegalService.getApplications()).isEmpty();
  }

  @Test
  public void testGetApplications_Authorized() {
    grantReadPermission(app.getId());
    tempEntity.newApplicationWithParent();
    assertThat(licenseLegalService.getApplications()).extracting(Application::getId).containsExactly(app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReportsForOrg_Unauthenticated() {
    licenseLegalService.getReportsForOrg(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReportsForOrg_Unauthorized() {
    login();
    licenseLegalService.getReportsForOrg(org.getId());
  }

  @Test
  public void testGetReportsForOrg_Authorized() {
    grantReadPermission(org.getId());
    licenseLegalService.getReportsForOrg(org.getId());
  }
}
