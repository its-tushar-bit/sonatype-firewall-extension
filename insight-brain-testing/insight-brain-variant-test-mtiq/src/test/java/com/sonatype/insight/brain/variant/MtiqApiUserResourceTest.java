/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.security.Permission;

import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * Creating an internal user through the public API is gated by the {@code USER_MANAGEMENT_PAGES} feature, which is
 * disabled by default for MTIQ tenants, so {@code POST /api/v2/users} returns 404 while the read, update, and delete
 * endpoints stay available. This mirrors the gating already applied to the internal {@code /rest/user}
 * {@code UserResource}.
 */
@MtiqTest
class MtiqApiUserResourceTest
{
  private MtiqTestContext ctx;

  @Test
  void testCreateUser_blockedInMtiqByDefault() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path("/api/v2/users")
        .auth(ctx.createUserWithRole(Permission.values()))
        .body("{\"username\":\"svc-account\",\"firstName\":\"Svc\",\"lastName\":\"Account\","
            + "\"email\":\"svc@example.com\",\"password\":\"changeme123\"}")
        .post();

    ctx.assertResponseStatus(SC_NOT_FOUND, response);
  }

  @Test
  void testListUsers_availableInMtiq() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path("/api/v2/users")
        .auth(ctx.createUserWithRole(Permission.values()))
        .get();

    ctx.assertResponseStatus(SC_OK, response);
  }

  @Test
  void testUpdateUser_availableInMtiq() throws Exception {
    String username = createInternalUser();

    HttpResponse response = ctx.restRequest()
        .path("/api/v2/users/" + username)
        .auth(ctx.createUserWithRole(Permission.values()))
        .body("{\"firstName\":\"Updated\"}")
        .put();

    ctx.assertResponseStatus(SC_OK, response);
  }

  @Test
  void testDeleteUser_availableInMtiq() throws Exception {
    String username = createInternalUser();

    HttpResponse response = ctx.restRequest()
        .path("/api/v2/users/" + username)
        .auth(ctx.createUserWithRole(Permission.values()))
        .delete();

    ctx.assertResponseStatus(SC_NO_CONTENT, response);
  }

  private String createInternalUser() {
    AtomicReference<String> username = new AtomicReference<>();
    ctx.testAsTestTenant(t -> username.set(ctx.tempEntity().newUser().getUsername()));
    return username.get();
  }
}
