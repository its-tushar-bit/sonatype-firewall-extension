/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiIntegrationVersionCacheResourceTest
    extends AbstractResourceTest
{
  private User testUser;

  private User unauthorizedUser;

  @Before
  public void setUp() {
    // Create a test user with CONFIGURE_SYSTEM permission for "edit-system-config-and-users"
    testUser = tempEntity.newUser("testConfigUser");
    Role role = tempEntity.newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), testUser.getUsername());

    // Create a user without the required permission
    unauthorizedUser = tempEntity.newUser("unauthorizedUser");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2 + "/integrationVersions/cache")
        .auth(testUser.getUsername(), testUser.getPassword());
  }

  @Test
  public void testInvalidateCache_ReturnsZeroWhenEmpty() throws Exception {
    HttpResponse response = restRequest().delete();

    assertResponseStatus(200, response);
    ApiIntegrationVersionCacheResource.CacheInvalidationResponse result =
        response.getBody(ApiIntegrationVersionCacheResource.CacheInvalidationResponse.class);
    assertThat(result.entriesInvalidated()).isZero();
  }

  @Test
  public void testInvalidateCache_Success() throws Exception {
    HttpResponse response = restRequest().delete();

    assertResponseStatus(200, response);
    ApiIntegrationVersionCacheResource.CacheInvalidationResponse result =
        response.getBody(ApiIntegrationVersionCacheResource.CacheInvalidationResponse.class);
    assertThat(result).isNotNull();
    assertThat(result.entriesInvalidated()).isNotNegative();
  }

  @Test
  public void testInvalidateCache_WithoutPermission_Returns403() throws Exception {
    HttpResponse response = super.restRequest()
        .path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2 + "/integrationVersions/cache")
        .auth(unauthorizedUser.getUsername(), unauthorizedUser.getPassword())
        .delete();

    assertResponseStatus(403, response);
  }
}
