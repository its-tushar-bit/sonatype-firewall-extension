/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.UserMapping;
import com.sonatype.insight.brain.git.ScmUserMappingService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.utils.ScmUserMappingsHelper.getRandomMappings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ScmUserMappingServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ScmUserMappingService scmUserMappingService;

  private Organization organization;

  private SCMUserMappingsDTO scmUserMappingsDTO;

  @BeforeEach
  public void before() {
    organization = tempEntity.newOrganization();
    scmUserMappingsDTO = new SCMUserMappingsDTO(null,
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL)));
  }

  @Test
  public void testAddOrUpdateUserMappingByOrg_Authorized() {
    grantPermission(organization.getId(), Permission.WRITE);
    scmUserMappingService.addOrUpdateUserMappingByOrg(organization.getId(), scmUserMappingsDTO);
  }

  @Test
  public void testAddOrUpdateUserMappingByOrg_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> scmUserMappingService.addOrUpdateUserMappingByOrg(organization.getId(), scmUserMappingsDTO));
  }

  @Test
  public void testAddOrUpdateUserMappingByOrg_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> scmUserMappingService.addOrUpdateUserMappingByOrg(organization.getId(), scmUserMappingsDTO));
  }

  @Test
  public void testDeleteUserMappingByOrg_Authorized() {
    grantPermission(organization.getId(), Permission.WRITE);
    scmUserMappingService.deleteUserMappingByOrg(organization.getId());
  }

  @Test
  public void testDeleteUserMappingByOrg_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> scmUserMappingService.deleteUserMappingByOrg(organization.getId()));
  }

  @Test
  public void testDeleteUserMappingByOrg_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> scmUserMappingService.deleteUserMappingByOrg(organization.getId()));
  }

  @Test
  public void testGetUserMappingsByOwner() {
    Application application = tempEntity.newApplication(org.getId());
    tempEntity.createScmUserMappings(Role.OWNER_ROLE_ID, org.getId(), getRandomMappings());
    grantReadPermission(application.getId());
    SCMUserMappingsResponseDTO results =
        scmUserMappingService.getUserMappingsByOwner(OwnerType.APPLICATION, application.getId());

    assertThat(results).isNotNull();
  }

  @Test
  public void testGetUserMappingsByOwner_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> scmUserMappingService.getUserMappingsByOwner(OwnerType.APPLICATION, app.getId()));
  }

  @Test
  public void testGetUserMappingsByOwner_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> scmUserMappingService.getUserMappingsByOwner(OwnerType.APPLICATION, "any"));
  }
}
