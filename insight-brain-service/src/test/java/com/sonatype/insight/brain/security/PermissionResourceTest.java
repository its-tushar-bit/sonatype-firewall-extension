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
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Assert;
import org.junit.Test;

public class PermissionResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PermissionResource.SERVICE_PATH);
  }

  @Test
  public void testAdminUserWithAdminPerm() throws Exception {
    HttpResponse response = restRequest().parameter(IdUtils.TYPE_GLOBAL, "*").body(EnumSet.of(Permission.CONFIGURE_SYSTEM))
        .put();
    assertResponseStatus(200, response);
    List<Permission> permissions = Arrays.asList(fromJson(response, Permission[].class));
    Assert.assertTrue(permissions.contains(Permission.CONFIGURE_SYSTEM));
  }

  @Test
  public void testNonAdminUserWithAdminPerm() throws Exception {
    tempEntity.newUser("testNonAdminUser");
    HttpResponse response = restRequest().parameter(IdUtils.TYPE_GLOBAL, "*").body(EnumSet.of(Permission.CONFIGURE_SYSTEM))
        .auth("testNonAdminUser", "secret").put();
    assertResponseStatus(200, response);
    List<Permission> permissions = Arrays.asList(fromJson(response, Permission[].class));
    Assert.assertFalse(permissions.contains(Permission.CONFIGURE_SYSTEM));
  }
}
