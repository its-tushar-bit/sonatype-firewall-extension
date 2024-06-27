/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ScmUserMatchingServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ScmUserMatchingService scmUserMatchingService;

  @Test(expected = UnauthenticatedException.class)
  public void testAutomaticRoleAssignment_Unauthenticated() {
    scmUserMatchingService.automaticRoleAssignment(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAutomaticRoleAssignment_Unauthorized() {
    login();
    scmUserMatchingService.automaticRoleAssignment(app.getPublicId());
  }

  @Test
  public void testAutomaticRoleAssignment_Authorized() {
    grantEditAccessControlPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> scmUserMatchingService.automaticRoleAssignment(app.getPublicId()));
  }
}
