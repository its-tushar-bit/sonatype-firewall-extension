/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.telemetry.TelemetryReceiptResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the {@code AbstractResourceAuthzTest} fixture (authorized/unauthorized users, global
 * CONFIGURE_SYSTEM permission) that the legacy {@code TelemetryReceiptResourceAuthzTest} inherited from its base
 * class.
 */
@IqH2Test
class IqH2TelemetryReceiptResourceAuthzTest
{
  private IqTestContext ctx;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void createEntities() {
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();

    grantConfigureSystemPermission();
  }

  private void grantConfigureSystemPermission() {
    Role role = ctx.tempEntity().newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    ctx.tempEntity()
        .newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon();
  }

  private void assertStatus(HttpResponse response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    }
    else {
      assertThat(response.getStatusCode()).isEqualTo(status);
    }
  }

  private HttpResponse testAuthzGet(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    HttpResponse response = request.auth(unauthorized).get();
    assertStatus(response, 403);

    response = request.auth(authorized).get();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  @Test
  void testGetReceipts_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH)
        .anon()
        .get();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testGetReceipts_authz() throws Exception {
    HttpRequest request = restRequest().path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH);
    testAuthzGet(request, 200);
  }

  @Test
  void testEnableReceipts_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH, "enable")
        .anon()
        .get();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testEnableReceipts_authz() throws Exception {
    HttpRequest request = restRequest().path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH, "enable");
    testAuthzGet(request, 200);
  }

  @Test
  void testDisableReceipts_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH, "disable")
        .anon()
        .get();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testDisableReceipts_authz() throws Exception {
    HttpRequest request = restRequest().path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH, "disable");
    testAuthzGet(request, 200);
  }
}
