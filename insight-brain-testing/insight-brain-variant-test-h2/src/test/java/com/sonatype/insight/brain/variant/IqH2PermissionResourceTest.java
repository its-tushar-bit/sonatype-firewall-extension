/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.EnumSet;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PermissionResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2PermissionResourceTest
{
  private IqTestContext ctx;

  private User user;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PermissionResource.RESOURCE_PATH).auth(user);
  }

  private HttpRequest validateRequest_PublicApplicationId(String publicApplicationId) {
    return restRequest().path(PermissionResource.PUBLIC_APPLICATION_ID_PATH).parameter(publicApplicationId);
  }

  private HttpRequest validateRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PermissionResource.OWNER_CONTEXT_PATH).parameter(ownerType, ownerId);
  }

  private HttpRequest validateRequest(OwnerType ownerType) {
    return restRequest().path(PermissionResource.SINGLETON_OWNER_CONTEXT_PATH).parameter(ownerType);
  }

  @BeforeEach
  void initUser() {
    user = ctx.tempEntity().newUser();
    Role role = ctx.tempEntity().newRole(true, Permission.READ);
    ctx.tempEntity().newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
  }

  @Test
  void testValidatePermission_GlobalContext() throws Exception {
    HttpResponse response = validateRequest(OwnerType.GLOBAL, "global").body(
        EnumSet.of(Permission.READ, Permission.WRITE)).put();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }

  @Test
  void testValidatePermission_PublicApplicationContext() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();

    HttpResponse response =
        validateRequest_PublicApplicationId(app.getPublicId()).body(EnumSet.of(Permission.READ, Permission.WRITE))
            .put();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }

  @Test
  void testValidatePermission_ApplicationContext() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();

    HttpResponse response =
        validateRequest(OwnerType.APPLICATION, app.getId()).body(EnumSet.of(Permission.READ, Permission.WRITE)).put();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }

  @Test
  void testValidatePermission_OrganizationContext() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();

    HttpResponse response =
        validateRequest(OwnerType.ORGANIZATION, org.getId()).body(EnumSet.of(Permission.READ, Permission.WRITE)).put();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }

  @Test
  void testValidatePermission_RepositoryContext() throws Exception {
    Repository repo = ctx.tempEntity().newRepository();

    HttpResponse response =
        validateRequest(OwnerType.REPOSITORY, repo.getId()).body(EnumSet.of(Permission.READ, Permission.WRITE)).put();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }

  @Test
  void testValidatePermission_RepositoryContainerContext() throws Exception {
    HttpResponse response = validateRequest(OwnerType.REPOSITORY_CONTAINER).body(
        EnumSet.of(Permission.READ, Permission.WRITE)).put();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }

  @Test
  void testValidatePermission_RepositoryManagerContext() throws Exception {
    RepositoryManager repoManager = ctx.tempEntity().newRepositoryManager();

    HttpResponse response =
        validateRequest(
            OwnerType.REPOSITORY_MANAGER,
            repoManager.getId()).body(EnumSet.of(Permission.READ, Permission.WRITE)).put();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }

  @Test
  void testValidatePermission_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    HttpResponse response = validateRequest(OwnerType.REPOSITORY_CONTAINER).body(
        EnumSet.of(Permission.READ, Permission.WRITE)).put();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(Permission[].class)).containsExactlyInAnyOrder(Permission.READ);
  }
}
