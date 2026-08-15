/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the {@code AbstractResourceAuthzTest} fixture (org + authorized/unauthorized users) that the legacy
 * {@code ApiLifecycleResourceAuthzTest} inherited from its base class.
 * <p>
 * Authorization tests for ApiLifecycleResource.
 * <p>
 * Access to the Hosted Repository Scanning configuration is restricted to:
 * <ul>
 * <li>System Administrator — via {@code CONFIGURE_SYSTEM} permission at the global context.</li>
 * <li>Policy Administrator, Owner, Developer (and any custom role granted {@code READ}) —
 * when the user holds {@code READ} on at least one owner (root org, org, or application).</li>
 * </ul>
 */
@IqH2Test
class IqH2ApiLifecycleResourceAuthzTest
{
  private IqTestContext ctx;

  private Organization org;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void createEntities() {
    org = ctx.tempEntity().newOrganization();
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();

    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
  }

  @AfterEach
  void tearDown() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon();
  }

  private void grantConfigureSystemPermission() {
    Role role = ctx.tempEntity().newRole(true /* global */, Permission.CONFIGURE_SYSTEM);
    ctx.tempEntity()
        .newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());
  }

  private void grantReadPermission(String contextId) {
    grantPermission(contextId, Permission.READ);
  }

  private void grantPermission(String contextId, Permission permission) {
    Role role = ctx.tempEntity().newRole(false /* global */, permission);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  @Test
  void testGetRepositoryManagers_UnauthenticatedReturns401() throws Exception {
    HttpResponse response = restRequest().path("/api/v2/lifecycle/repositoryManagers").anon().get();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testGetRepositoryManagers_WithoutPermissionReturns403() throws Exception {
    HttpResponse response = restRequest().path("/api/v2/lifecycle/repositoryManagers").auth(unauthorized).get();
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testGetRepositoryManagers_WithConfigureSystemPermissionSucceeds() throws Exception {
    grantConfigureSystemPermission();

    HttpResponse response = restRequest().path("/api/v2/lifecycle/repositoryManagers").auth(authorized).get();
    assertThat(response.getStatusCode()).isLessThan(400);
  }

  @Test
  void testGetRepositoryManagers_WithReadPermissionOnAnyOwnerSucceeds() throws Exception {
    // Covers Policy Administrator / Owner / Developer who hold READ instead of CONFIGURE_SYSTEM.
    grantReadPermission(org.getId());

    HttpResponse response = restRequest().path("/api/v2/lifecycle/repositoryManagers").auth(authorized).get();
    assertThat(response.getStatusCode()).isLessThan(400);
  }
}
