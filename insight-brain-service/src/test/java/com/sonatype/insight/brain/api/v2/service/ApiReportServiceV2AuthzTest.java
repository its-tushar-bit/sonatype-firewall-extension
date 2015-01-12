/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
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

public class ApiReportServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiReportServiceV2 apiReportServiceV2;

  @Test(expected = UnauthenticatedException.class)
  public void testGetData_Anon() throws Exception {
    apiReportServiceV2.getByApplicationId(app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetData_Unauthorized() throws Exception {
    login();
    apiReportServiceV2.getByApplicationId(app.getId());
  }

  @Test(expected = NotFoundException.class)
  public void testGetData_Authorized() throws Exception {
    grantReadPermission(app.getId());
    apiReportServiceV2.getByApplicationId("fakeappid");
  }
}
