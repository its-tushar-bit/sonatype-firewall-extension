/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiLicenseLegalServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiLicenseLegalService apiLicenseLegalService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicenseLegalApplicationReport_Unauthenticated() {
    apiLicenseLegalService.getLicenseLegalApplicationReport(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicenseLegalApplicationReport_Unauthorized() {
    login();
    apiLicenseLegalService.getLicenseLegalApplicationReport(app.getPublicId());
  }

  @Test(expected = NotFoundException.class)
  public void testGetLicenseLegalApplicationReport_Authorized() {
    grantReadPermission(app.getId());
    apiLicenseLegalService.getLicenseLegalApplicationReport(app.getPublicId());
  }
}
