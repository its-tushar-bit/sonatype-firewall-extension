/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiThirdPartyEvaluationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiThirdPartyEvaluationService apiThirdPartyEvaluationService;

  @Test
  public void testEvaluateComponents_Authorized() {
    grantReadPermission(app.getId());
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "Build", "");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponents_Unauthenticated() {
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "Build", "");
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponents_UnauthorizedButAuthenticated() {
    login();
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "Build", "");
  }
}
