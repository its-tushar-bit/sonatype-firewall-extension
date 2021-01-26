/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.experimental.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.junit.Test.None;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiScmOnboardingServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String PROVIDER = "github";

  public static final String GITHUB_COM = "http://github.com";

  @Inject
  public ApiScmOnboardingService apiScmOnboardingService;

  @Test
  public void testLoadRepositories_Authorized() throws Exception {
    grantManageAutomaticSourceControlPermission();

    assertThatThrownBy(() -> {
      apiScmOnboardingService.loadRepositories(org.getId(), GITHUB_COM);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("No provider configured");
  }

  @Test
  public void testLoadRepositories_Authorized_nullOrg() throws Exception {
    grantManageAutomaticSourceControlPermission();

    assertThatThrownBy(() -> {
      apiScmOnboardingService.loadRepositories(null, GITHUB_COM);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("No organization specified");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testLoadRepositories_Unauthenticated() throws Exception {
    apiScmOnboardingService.loadRepositories(org.getId(), GITHUB_COM);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testLoadRepositories_Unauthenticated_nullOrg() throws Exception {
    apiScmOnboardingService.loadRepositories(null, GITHUB_COM);
  }

  @Test(expected = UnauthorizedException.class)
  public void testLoadRepositories_Unauthorized() throws Exception {
    login();
    apiScmOnboardingService.loadRepositories(org.getId(), GITHUB_COM);
  }

  @Test(expected = UnauthorizedException.class)
  public void testLoadRepositories_Unauthorized_nullOrg() throws Exception {
    login();
    apiScmOnboardingService.loadRepositories(null, GITHUB_COM);
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

  @Test(expected = None.class /* no exception expected */)
  public void test_importRepositories_Authorized() throws Exception {
    grantManageAutomaticSourceControlPermission();
    apiScmOnboardingService
        .importRepositories(org.getId(), new ImportRepositoriesRequest(Collections.emptyList(), 0, 0));
  }

  @Test(expected = UnauthorizedException.class)
  public void test_importRepositories_Unauthorized() throws Exception {
    login();
    apiScmOnboardingService.importRepositories(org.getId(), new ImportRepositoriesRequest());
  }

  @Test(expected = UnauthenticatedException.class)
  public void test_importRepositories_Unauthenticated() throws Exception {
    apiScmOnboardingService.importRepositories(org.getId(), new ImportRepositoriesRequest());
  }

  @Test
  public void testCheckScmUrl_Authorized() {
    grantGlobalPermission(Permission.READ);
    apiScmOnboardingService.validateScmHostUrl("GITHUB", "https://localhost/org/proj");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testCheckScmUrl_Unauthenticated() {
    apiScmOnboardingService.validateScmHostUrl("GITHUB", "https://localhost/org/proj");
  }

  @Test(expected = UnauthorizedException.class)
  public void testCheckScmUrl_Unauthorized() {
    login();
    apiScmOnboardingService.validateScmHostUrl("GITHUB", "https://localhost/org/proj");
  }
}
