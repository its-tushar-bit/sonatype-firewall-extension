/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.license.LicenseThreatGroupLicenseResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for {@code LicenseThreatGroupLicenseResource}.
 */
@IqPostgresTest
class IqPostgresLicenseThreatGroupLicenseResourceAuthzTest
{
  private IqTestContext ctx;

  private Organization org;

  private Application app;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void createEntities() {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();
  }

  private void grantReadPermission(String contextId) {
    grantPermission(contextId, Permission.READ);
  }

  private void grantWritePermission(String contextId) {
    grantPermission(contextId, Permission.WRITE);
  }

  private void grantPermission(String contextId, Permission permission) {
    Role role = ctx.tempEntity().newRole(false /* global */, permission);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(LicenseThreatGroupLicenseResource.RESOURCE_PATH);
  }

  private HttpResponse testAuthzGet(HttpRequest request) throws Exception {
    HttpResponse response = request.auth(unauthorized).get();
    assertStatus(response, 403);

    response = request.auth(authorized).get();
    assertStatus(response, null);
    return response;
  }

  private HttpResponse testAuthzPut(HttpRequest request) throws Exception {
    HttpResponse response = request.auth(unauthorized).put();
    assertStatus(response, 403);

    response = request.auth(authorized).put();
    assertStatus(response, null);
    return response;
  }

  private void assertStatus(HttpResponse response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    }
    else {
      assertThat(response.getStatusCode()).isEqualTo(status);
    }
  }

  @Test
  void testGetLicenseThreatGroupLicenses() throws Exception {
    grantReadPermission(app.getId());

    LicenseThreatGroup ltg = ctx.tempEntity().newLicenseThreatGroup(app.getId());

    testAuthzGet(restRequest().parameter(OwnerType.APPLICATION, app.getPublicId(), ltg.getId()));

    grantReadPermission(org.getId());

    testAuthzGet(restRequest().parameter(OwnerType.ORGANIZATION, org.getId(), ltg.getId()));
  }

  @Test
  void testSetLicenseThreatGroupLicenses() throws Exception {
    grantWritePermission(app.getId());

    LicenseThreatGroup ltg = ctx.tempEntity().newLicenseThreatGroup(app.getId());

    HttpRequest request = restRequest().body(Collections.singletonList("MIT"));
    testAuthzPut(request.parameter(OwnerType.APPLICATION, app.getPublicId(), ltg.getId()));

    grantWritePermission(org.getId());

    testAuthzPut(request.parameter(OwnerType.ORGANIZATION, org.getId(), ltg.getId()));
  }
}
