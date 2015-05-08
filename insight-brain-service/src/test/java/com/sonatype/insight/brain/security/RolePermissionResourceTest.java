/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;

public class RolePermissionResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetPermissionsForRole() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(RolePermissionResource.SERVICE_PATH + "/{roleId}",
        Role.SYSTEM_ADMIN_ROLE_ID));
    assertResponseStatus(200, response);
    System.out.println(response.getResponseBody());
    RolePermissionDTO rolePermissions = fromJson(response, RolePermissionDTO.class);
    assertThat(rolePermissions.roleId, is(Role.SYSTEM_ADMIN_ROLE_ID));
    assertThat(rolePermissions.permissionCategories, not(empty()));
    assertThat(rolePermissions.permissionCategories.get(0).permissions, not(empty()));
  }
}
