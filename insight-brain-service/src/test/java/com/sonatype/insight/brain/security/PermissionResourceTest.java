/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;
import org.junit.Test;

public class PermissionResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PermissionResource.RESOURCE_PATH, PermissionResource.OWNER_CONTEXT_PATH);
  }

  protected HttpRequest singleOwnerRequest() {
    return super.restRequest().path(PermissionResource.RESOURCE_PATH, PermissionResource.SINGLETON_OWNER_CONTEXT_PATH);

  }

  @Test
  public void testAdminUserWithAdminPerm() throws Exception {
    HttpResponse response = restRequest().parameter(OwnerType.GLOBAL, "*")
        .body(EnumSet.of(Permission.CONFIGURE_SYSTEM)).put();
    assertResponseStatus(200, response);
    List<Permission> permissions = Arrays.asList(response.getBody(Permission[].class));
    Assert.assertTrue(permissions.contains(Permission.CONFIGURE_SYSTEM));
  }

  @Test
  public void testNonAdminUserWithAdminPerm() throws Exception {
    tempEntity.newUser("testNonAdminUser");
    HttpResponse response = restRequest().parameter(OwnerType.GLOBAL, "*")
        .body(EnumSet.of(Permission.CONFIGURE_SYSTEM)).auth("testNonAdminUser", "secret").put();
    assertResponseStatus(200, response);
    List<Permission> permissions = Arrays.asList(response.getBody(Permission[].class));
    Assert.assertFalse(permissions.contains(Permission.CONFIGURE_SYSTEM));
  }

  @Test
  public void testValidatePermission_AdminUserWithAdminPerm() throws Exception {
    HttpResponse response = singleOwnerRequest().parameter(OwnerType.REPOSITORY_CONTAINER)
        .body(EnumSet.of(Permission.READ)).put();
    assertResponseStatus(200, response);
    List<Permission> permissions = Arrays.asList(response.getBody(Permission[].class));
    Assert.assertTrue(permissions.contains(Permission.READ));
  }

  @Test
  public void testValidatePermission_NonAdminUserWithAdminPerm() throws Exception {
    tempEntity.newUser("testNonAdminUser");
    HttpResponse response = singleOwnerRequest().parameter(OwnerType.REPOSITORY_CONTAINER)
        .body(EnumSet.of(Permission.READ)).auth("testNonAdminUser", "secret").put();
    assertResponseStatus(200, response);
    List<Permission> permissions = Arrays.asList(response.getBody(Permission[].class));
    Assert.assertFalse(permissions.contains(Permission.READ));
  }
}
