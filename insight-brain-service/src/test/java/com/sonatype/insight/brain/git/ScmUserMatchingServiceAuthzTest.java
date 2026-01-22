/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import com.google.common.collect.Lists;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ScmUserMatchingServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ScmUserMatchingService scmUserMatchingService;

  @Test(expected = UnauthenticatedException.class)
  public void testAutomaticRoleAssignmentByMapping_Unauthenticated() {
    scmUserMatchingService.automaticRoleAssignmentByMapping(
        app.getPublicId(),
        new SCMUserMappingsDTO(null, Lists.newArrayList()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testAutomaticRoleAssignmentByMapping_Unauthorized() {
    login();
    scmUserMatchingService.automaticRoleAssignmentByMapping(
        app.getPublicId(),
        new SCMUserMappingsDTO(null, Lists.newArrayList()));
  }

  @Test
  public void testAutomaticRoleAssignmentByMapping_Authorized() {
    grantEditAccessControlPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> scmUserMatchingService.automaticRoleAssignmentByMapping(
            app.getPublicId(),
            new SCMUserMappingsDTO(null, Lists.newArrayList())));
  }
}
