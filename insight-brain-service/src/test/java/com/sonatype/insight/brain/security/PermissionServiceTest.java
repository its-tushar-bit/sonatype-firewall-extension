/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.EnumSet;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class PermissionServiceTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private PermissionService service;

  private static final Permission[] NONE = {};

  private void assertPermissions(OwnerType ownerType, String ownerId, Permission... expected) {
    assertThat(service.hasPermissions(subject, ownerType, ownerId, EnumSet.allOf(Permission.class)),
        containsInAnyOrder(expected));
  }

  @Test
  public void testHasPermissions_GlobalContext_Unauthenticated() {
    assertPermissions(OwnerType.GLOBAL, null, NONE);
  }

  @Test
  public void testHasPermissions_GlobalContext_Authenticated() {
    grantConfigureSystemPermission();
    assertPermissions(OwnerType.GLOBAL, null, Permission.CONFIGURE_SYSTEM);
  }

  @Test
  public void testHasPermissions_RepoContainerContext_Unauthenticated() {
    assertPermissions(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, NONE);
  }

  @Test
  public void testHasPermissions_RepoContainerContext_Authenticated() {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertPermissions(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);
  }

  @Test
  public void testHasPermissions_OrgContext_Unauthenticated() {
    assertPermissions(OwnerType.ORGANIZATION, org.getId(), NONE);
  }

  @Test
  public void testHasPermissions_OrgContext_Authenticated() {
    grantReadPermission(org.getId());
    grantWritePermission(org.getId());
    assertPermissions(OwnerType.ORGANIZATION, org.getId(), Permission.READ, Permission.WRITE);
  }

  @Test
  public void testHasPermissions_AppContext_Unauthenticated() {
    assertPermissions(OwnerType.APPLICATION, app.getId(), NONE);
  }

  @Test
  public void testHasPermissions_AppContext_Authenticated() {
    grantReadPermission(app.getId());
    grantWritePermission(app.getId());
    assertPermissions(OwnerType.APPLICATION, app.getId(), Permission.READ, Permission.WRITE);
  }
}
