/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.junit.Test.None;

public class ApiScmOnboardingServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String PROVIDER = "github";

  @Inject
  public ApiScmOnboardingService apiScmOnboardingService;

  @Test(expected = None.class)
  public void testLoadRepositories_Authorized() throws Exception {
    grantManageAutomaticSourceControlPermission();

    apiScmOnboardingService.loadRepositories(org.getId());
  }

  @Test(expected = None.class /* no exception expected */)
  public void testLoadRepositories_Authorized_nullOrg() throws Exception {
    grantManageAutomaticSourceControlPermission();

    apiScmOnboardingService.loadRepositories(null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testLoadRepositories_Unauthenticated() throws Exception {
    apiScmOnboardingService.loadRepositories(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testLoadRepositories_Unauthenticated_nullOrg() throws Exception {
    apiScmOnboardingService.loadRepositories(null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testLoadRepositories_Unauthorized() throws Exception {
    login();
    apiScmOnboardingService.loadRepositories(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testLoadRepositories_Unauthorized_nullOrg() throws Exception {
    login();
    apiScmOnboardingService.loadRepositories(null);
  }

  @Test(expected = None.class /* no exception expected */)
  public void test_getDefaultHostUrl_Authorized() throws Exception {
    grantManageAutomaticSourceControlPermission();
    apiScmOnboardingService.getDefaultHostUrl(PROVIDER, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void test_getDefaultHostUrl_Unauthorized() throws Exception {
    login();
    apiScmOnboardingService.getDefaultHostUrl(PROVIDER, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void test_getDefaultHostUrl_Unauthenticated() throws Exception {
    apiScmOnboardingService.getDefaultHostUrl(PROVIDER, org.getId());
  }
}
