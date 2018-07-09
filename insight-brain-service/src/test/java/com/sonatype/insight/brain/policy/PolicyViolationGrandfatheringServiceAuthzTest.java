/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class PolicyViolationGrandfatheringServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService;

  @Test
  public void testRevokeGrandfathering_Authorized() throws Exception {
    grantWritePermission(app.getId());
    policyViolationGrandfatheringService.revokeGrandfathering(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRevokeGrandfathering_Unauthorized() throws Exception {
    login();
    policyViolationGrandfatheringService.revokeGrandfathering(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRevokeGrandfathering_Unauthenticated() throws Exception {
    policyViolationGrandfatheringService.revokeGrandfathering(app.getPublicId());
  }
}
