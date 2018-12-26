/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.EnumSet;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionResourceTest
    extends AbstractResourceTest
{
  private User user;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PermissionResource.RESOURCE_PATH).auth(user.getUsername(), user.getPassword());
  }

  private HttpRequest validateRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PermissionResource.OWNER_CONTEXT_PATH).parameter(ownerType, ownerId);
  }

  private HttpRequest validateRequest(OwnerType ownerType) {
    return restRequest().path(PermissionResource.SINGLETON_OWNER_CONTEXT_PATH).parameter(ownerType);
  }

  @Before
  public void initUser() {
    user = tempEntity.newUser();
    Role role = tempEntity.newRole(true, Permission.READ);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
  }

  @Test
  public void testValidatePermission_NonSingletonContext() throws Exception {
    HttpResponse response = validateRequest(OwnerType.GLOBAL, "global").body(
        EnumSet.of(Permission.READ, Permission.WRITE)).put();
    assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }

  @Test
  public void testValidatePermission_SingletonContext() throws Exception {
    HttpResponse response = validateRequest(OwnerType.REPOSITORY_CONTAINER).body(
        EnumSet.of(Permission.READ, Permission.WRITE)).put();
    assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }

  @Test
  public void testValidatePermission_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = validateRequest(OwnerType.REPOSITORY_CONTAINER).body(
        EnumSet.of(Permission.READ, Permission.WRITE)).put();
    assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }
}
