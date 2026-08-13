/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiIntegrationVersionCacheResource;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiIntegrationVersionCacheResourceTest
{
  private IqTestContext ctx;

  private User testUser;

  private User unauthorizedUser;

  @BeforeEach
  void setUp() {
    // Create a test user with CONFIGURE_SYSTEM permission for "edit-system-config-and-users"
    testUser = ctx.tempEntity().newUser("testConfigUser");
    Role role = ctx.tempEntity().newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    ctx.tempEntity().newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), testUser.getUsername());

    // Create a user without the required permission
    unauthorizedUser = ctx.tempEntity().newUser("unauthorizedUser");
  }

  private HttpRequest restRequest() {
    return ctx.restRequest()
        .path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2 + "/integrationVersions/cache")
        .auth(testUser.getUsername(), testUser.getPassword());
  }

  @Test
  void testInvalidateCache_ReturnsZeroWhenEmpty() throws Exception {
    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(200, response);
    ApiIntegrationVersionCacheResource.CacheInvalidationResponse result =
        response.getBody(ApiIntegrationVersionCacheResource.CacheInvalidationResponse.class);
    assertThat(result.entriesInvalidated()).isZero();
  }

  @Test
  void testInvalidateCache_Success() throws Exception {
    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(200, response);
    ApiIntegrationVersionCacheResource.CacheInvalidationResponse result =
        response.getBody(ApiIntegrationVersionCacheResource.CacheInvalidationResponse.class);
    assertThat(result).isNotNull();
    assertThat(result.entriesInvalidated()).isNotNegative();
  }

  @Test
  void testInvalidateCache_WithoutPermission_Returns403() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2 + "/integrationVersions/cache")
        .auth(unauthorizedUser.getUsername(), unauthorizedUser.getPassword())
        .delete();

    ctx.assertResponseStatus(403, response);
  }
}
