/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiReportDataServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiReportDataServiceV2 reportDataService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetData_Anon() throws Exception {
    reportDataService.getData(app.getPublicId(), "irrelevant");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetData_Unauthorized() throws Exception {
    login();
    reportDataService.getData(app.getPublicId(), "irrelevant");
  }

  @Test(expected = NotFoundException.class)
  public void testGetData_Authorized() throws Exception {
    grantReadPermission(app.getId());
    reportDataService.getData(app.getPublicId(), "irrelevant");
  }
}
