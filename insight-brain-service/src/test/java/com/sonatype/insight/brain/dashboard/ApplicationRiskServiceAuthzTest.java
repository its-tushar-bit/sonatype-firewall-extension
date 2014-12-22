/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApplicationRiskServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationRiskService applicationRiskService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationRisks_ExplicitApplicationFilter_Unauthenticated() {
    applicationRiskService.getApplicationRisks(Collections.singleton(app.getId()), null, null, null, null, 1);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicationRisks_ExplicitApplicationFilter_Unauthorized() {
    login();
    applicationRiskService.getApplicationRisks(Collections.singleton(app.getId()), null, null, null, null, 1);
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    applicationRiskService.getApplicationRisks(Collections.singleton(app.getId()), null, null, null, null, 1);
  }
}
