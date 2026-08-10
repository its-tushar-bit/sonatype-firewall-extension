/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import com.google.common.collect.Lists;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ScmUserMatchingServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ScmUserMatchingService scmUserMatchingService;

  @Test
  public void testAutomaticRoleAssignmentByMapping_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> scmUserMatchingService.automaticRoleAssignmentByMapping(
            app.getPublicId(),
            new SCMUserMappingsDTO(null, Lists.newArrayList())));
  }

  @Test
  public void testAutomaticRoleAssignmentByMapping_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> scmUserMatchingService.automaticRoleAssignmentByMapping(
            app.getPublicId(),
            new SCMUserMappingsDTO(null, Lists.newArrayList())));
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
