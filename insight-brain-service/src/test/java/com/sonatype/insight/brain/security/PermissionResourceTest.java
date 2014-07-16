/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import org.junit.Assert;
import org.junit.Test;

public class PermissionResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testAdminUserWithAdminPerm() throws Exception {
    Response response = AuthedRestAccess
        .put(getRestUrl(PermissionResource.SERVICE_PATH, IdUtils.TYPE_GLOBAL, "*"),
            toJson(Collections.singleton(Permission.ADMIN)));
    assertResponseStatus(200, response);
    List<Permission> permissions = Arrays.asList(fromJson(response, Permission[].class));
    Assert.assertTrue(permissions.contains(Permission.ADMIN));
  }

  @Test
  public void testNonAdminUserWithAdminPerm() throws Exception {
    tempEntity.newUser("testNonAdminUser");
    Response response = AuthedRestAccess
        .put(getRestUrl(PermissionResource.SERVICE_PATH, IdUtils.TYPE_GLOBAL, "*"),
            toJson(Collections.singleton(Permission.ADMIN)),
            "testNonAdminUser", "secret");
    assertResponseStatus(200, response);
    List<Permission> permissions = Arrays.asList(fromJson(response, Permission[].class));
    Assert.assertFalse(permissions.contains(Permission.ADMIN));
  }
}
