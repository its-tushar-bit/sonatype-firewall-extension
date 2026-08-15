/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.search.catalog.CatalogRequest;
import com.sonatype.insight.brain.search.catalog.CatalogResource;
import com.sonatype.insight.brain.search.catalog.CatalogResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the {@code AbstractResourceAuthzTest} fixture (org + authorized/unauthorized users) and
 * {@code grantReadPermission} helper that the legacy {@code CatalogResourceAuthzTest} inherited from its base
 * class.
 */
@IqH2Test
class IqH2CatalogResourceAuthzTest
{
  private IqTestContext ctx;

  private Organization org;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void setUp() {
    org = ctx.tempEntity().newOrganization();
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();

    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    grantReadPermission(org.getId());
  }

  @AfterEach
  void tearDown() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  private void grantReadPermission(String contextId) {
    Role role = ctx.tempEntity().newRole(false /* global */, Permission.READ);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(CatalogResource.RESOURCE_PATH);
  }

  private static CatalogRequest catalogComponentRequest() {
    return new CatalogRequest("COMPONENT", "catalog", Map.of(), 1, 25, null, null, false);
  }

  @Test
  void unauthenticated_returns401() throws Exception {
    HttpResponse response = restRequest().anon().body(catalogComponentRequest()).post();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void authenticatedWithoutReadContext_returns403() throws Exception {
    HttpResponse response = restRequest().auth(unauthorized).body(catalogComponentRequest()).post();
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void authorized_returns200() throws Exception {
    HttpResponse response = restRequest().auth(authorized).body(catalogComponentRequest()).post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    CatalogResponse body = response.getBody(CatalogResponse.class);
    assertThat(body.entityType()).isEqualTo("COMPONENT");
  }
}
