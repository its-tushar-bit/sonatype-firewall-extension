/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

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
  public void getRawData_Anon() throws Exception {
    reportDataService.getRawData(app.getPublicId(), "irrelevant");
  }

  @Test(expected = UnauthorizedException.class)
  public void getRawData_Unauthorized() throws Exception {
    login();
    reportDataService.getRawData(app.getPublicId(), "irrelevant");
  }

  @Test(expected = NotFoundException.class)
  public void getRawData_Authorized() throws Exception {
    grantReadPermission(app.getId());
    reportDataService.getRawData(app.getPublicId(), "irrelevant");
  }
  
  @Test(expected = UnauthenticatedException.class)
  public void getDependencyTree_Anon() throws Exception {
    reportDataService.getDependencyTree(app.getPublicId(), "irrelevant");
  }

  @Test(expected = UnauthorizedException.class)
  public void getDependencyTree_Unauthorized() throws Exception {
    login();
    reportDataService.getDependencyTree(app.getPublicId(), "irrelevant");
  }

  @Test(expected = NotFoundException.class)
  public void getDependencyTree_Authorized() throws Exception {
    grantReadPermission(app.getId());
    reportDataService.getDependencyTree(app.getPublicId(), "irrelevant");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyViolations_Anon() throws Exception {
    reportDataService.getPolicyViolationsData(app.getPublicId(), "irrelevant", false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyViolations_Unauthorized() throws Exception {
    login();
    reportDataService.getPolicyViolationsData(app.getPublicId(), "irrelevant", false);
  }

  @Test(expected = NotFoundException.class)
  public void testGetPolicyViolations_Authorized() throws Exception {
    grantReadPermission(app.getId());
    reportDataService.getPolicyViolationsData(app.getPublicId(), "irrelevant", false);
  }
}
