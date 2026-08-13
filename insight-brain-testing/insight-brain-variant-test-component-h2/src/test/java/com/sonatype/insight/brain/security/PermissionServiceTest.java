/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentH2Test
public class PermissionServiceTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private PermissionService service;

  private static final Permission[] NONE = {};

  private static final HashSet<Permission> NONE_SET = new HashSet<>(Arrays.asList(NONE));

  private void assertPublicAppPermissionsNone(String publicApplicationId) {
    assertThat(service.validatePermissionForPublicApplicationId(subject, publicApplicationId, NONE_SET))
        .containsExactlyInAnyOrder(NONE);
  }

  private void assertPublicAppPermissions(String publicApplicationId, Permission... expected) {
    assertThat(service.validatePermissionForPublicApplicationId(subject, publicApplicationId,
        EnumSet.allOf(Permission.class))).containsExactlyInAnyOrder(expected);
  }

  private void assertPermissions(OwnerType ownerType, String ownerId, Permission... expected) {
    assertThat(service.validatePermission(subject, ownerType, ownerId, EnumSet.allOf(Permission.class)))
        .containsExactlyInAnyOrder(expected);
  }

  @Test
  public void testValidatePermissions_GlobalContext_Unauthenticated() {
    assertPermissions(OwnerType.GLOBAL, null, NONE);
  }

  @Test
  public void testValidatePermissions_GlobalContext_Authenticated() {
    grantConfigureSystemPermission();
    assertPermissions(OwnerType.GLOBAL, null, Permission.CONFIGURE_SYSTEM);
  }

  @Test
  public void testValidatePermissions_RepoContainerContext_Unauthenticated() {
    assertPermissions(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, NONE);
  }

  @Test
  public void testValidatePermissions_RepoContainerContext_Authenticated() {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertPermissions(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);
  }

  @Test
  public void testValidatePermissions_RepoContext_Unauthenticated() {
    assertPermissions(OwnerType.REPOSITORY, repository.getId(), NONE);
  }

  @Test
  public void testValidatePermissions_RepoContext_Authenticated() {
    grantReadPermission(repository.getId());
    grantWritePermission(repository.getId());
    assertPermissions(OwnerType.REPOSITORY, repository.getId(), Permission.READ, Permission.WRITE);
  }

  @Test
  public void testValidatePermissions_OrgContext_Unauthenticated() {
    assertPermissions(OwnerType.ORGANIZATION, org.getId(), NONE);
  }

  @Test
  public void testValidatePermissions_OrgContext_Authenticated() {
    grantReadPermission(org.getId());
    grantWritePermission(org.getId());
    assertPermissions(OwnerType.ORGANIZATION, org.getId(), Permission.READ, Permission.WRITE);
  }

  @Test
  public void testValidatePermissions_PublicAppContext_NoPermission() {
    assertThatThrownBy(() -> assertPublicAppPermissionsNone(app.getPublicId()))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Must specify permissions to check.");
  }

  @Test
  public void testValidatePermissions_PublicAppContext_Unauthenticated() {
    assertPublicAppPermissions(app.getPublicId(), NONE);
  }

  @Test
  public void testValidatePermissions_PublicAppContext_Authenticated() {
    grantReadPermission(app.getId());
    grantWritePermission(app.getId());
    assertPublicAppPermissions(app.getPublicId(), Permission.READ, Permission.WRITE);
  }

  @Test
  public void testValidatePermissions_AppContext_Unauthenticated() {
    assertPermissions(OwnerType.APPLICATION, app.getId(), NONE);
  }

  @Test
  public void testValidatePermissions_AppContext_Authenticated() {
    grantReadPermission(app.getId());
    grantWritePermission(app.getId());
    assertPermissions(OwnerType.APPLICATION, app.getId(), Permission.READ, Permission.WRITE);
  }

  @Test
  public void testGetContextIdsForUserWithPermission_Global() {
    grantGlobalPermission(Permission.READ);
    UserPrincipal principal = new UserPrincipal(user.getUsername(), "", InternalRealm.ID);

    assertThat(service.getContextIdsForUserWithPermission(principal, Permission.READ)).containsOnly("global");
  }

  @Test
  public void testGetContextIdsForUserWithPermission_UserApplication() {
    grantReadPermission(app.getId());
    UserPrincipal principal = new UserPrincipal(user.getUsername(), "", InternalRealm.ID);

    assertThat(service.getContextIdsForUserWithPermission(principal, Permission.READ)).containsOnly(app.getId());
  }

  @Test
  public void testGetContextIdsForUserWithPermission_UserOrganization() {
    grantReadPermission(org.getId());

    UserPrincipal principal = new UserPrincipal(user.getUsername(), "", InternalRealm.ID);

    assertThat(service.getContextIdsForUserWithPermission(principal, Permission.READ)).containsOnly(org.getId());
  }

  @Test
  public void testGetContextIdsForUserWithPermission_UserApplicationAndOrganization() {
    grantReadPermission(app.getId());
    grantReadPermission(org.getId());
    UserPrincipal principal = new UserPrincipal(user.getUsername(), "", InternalRealm.ID);

    Set<String> ids = service.getContextIdsForUserWithPermission(principal, Permission.READ);
    assertThat(ids).containsOnly(app.getId(), org.getId());
  }
}
