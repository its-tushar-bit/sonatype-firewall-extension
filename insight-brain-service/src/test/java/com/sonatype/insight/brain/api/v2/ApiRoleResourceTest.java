/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPermissionCategoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPermissionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiRoleResourceTest
    extends AbstractBrainServiceIntegrationTest
{
  private RoleDAO roleDAO;

  private RolePermissionDAO rolePermissionDAO;

  @Before
  public void setUp() {
    roleDAO = lookup(RoleDAO.class);
    rolePermissionDAO = lookup(RolePermissionDAO.class);
  }

  @Test
  public void testGetRoles() throws Exception {
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(true);
    try {
      HttpResponse response = restRequest().get();
      ApiRoleListDTO apiRoleListDTO = response.getBody(ApiRoleListDTO.class);

      assertResponseStatus(200, response);
      assertThat(apiRoleListDTO.roles)
          .extracting(role -> role.id)
          .containsExactlyInAnyOrder(
              Role.SYSTEM_ADMIN_ROLE_ID,
              Role.POLICY_ADMIN_ROLE_ID,
              Role.APPLICATION_EVALUATOR_ROLE_ID,
              Role.COMPONENT_EVALUATOR_ROLE_ID,
              Role.DEVELOPER_ROLE_ID,
              Role.LEGAL_REVIEWER_ROLE_ID,
              Role.OWNER_ROLE_ID,
              Role.USAGE_VIEWER_ROLE_ID);
    }
    finally {
      SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(false);
    }
  }

  @Test
  public void testGetRoleById() throws Exception {
    Role role = tempEntity.newRole(false, Permission.CLAIM_COMPONENT);
    HttpResponse response = restRequest().path(role.getId()).get();
    assertResponseStatus(200, response);
    ApiRoleDTO apiRoleDTO = response.getBody(ApiRoleDTO.class);
    assertThat(apiRoleDTO).isNotNull();
    assertThat(apiRoleDTO.id).isEqualTo(role.getId());
    assertThat(apiRoleDTO.name).isEqualTo(role.getName());
    assertThat(apiRoleDTO.description).isEqualTo(role.getDescription());
    assertThat(apiRoleDTO.builtIn).isFalse();
    assertThat(apiRoleDTO.permissionCategories).isNotNull();
  }

  @Test
  public void testGetTemplateForNewRole() throws Exception {
    HttpResponse response = restRequest().path("new").get();
    assertResponseStatus(200, response);
    ApiRoleDTO role = response.getBody(ApiRoleDTO.class);
    assertThat(role).isNotNull();
    assertThat(role.permissionCategories).hasSize(3);
  }

  @Test
  public void testAddRole() throws Exception {
    ApiRoleDTO apiRoleDTO = new ApiRoleDTO();
    apiRoleDTO.name = "New Name";
    apiRoleDTO.description = "New Description";
    apiRoleDTO.permissionCategories = new ArrayList<>();
    String categoryDisplayName = "TestDisplayName";
    apiRoleDTO.permissionCategories.add(createPermissionCategoryDTO(categoryDisplayName));

    HttpResponse response = restRequest().body(apiRoleDTO).post();
    assertResponseStatus(200, response);
    ApiRoleDTO newRoleDTO = response.getBody(ApiRoleDTO.class);
    assertThat(newRoleDTO.id).isNotNull();

    assertApiRoleDTO(newRoleDTO, apiRoleDTO, categoryDisplayName);
    assertRole(newRoleDTO, Permission.READ, Permission.WRITE);
  }

  @Test
  public void testUpdateRole() throws Exception {
    Role role = tempEntity.newRole(false);
    ApiRoleDTO apiRoleDTO = new ApiRoleDTO();
    apiRoleDTO.id = role.getId();
    apiRoleDTO.name = "Updated Name";
    apiRoleDTO.description = "Updated Description";
    apiRoleDTO.permissionCategories = new ArrayList<>();
    String categoryDisplayName = "TestDisplayName";
    apiRoleDTO.permissionCategories.add(createPermissionCategoryDTO(categoryDisplayName));

    HttpResponse response = restRequest().path(role.getId()).body(apiRoleDTO).put();
    assertResponseStatus(200, response);
    ApiRoleDTO updatedRoleDTO = response.getBody(ApiRoleDTO.class);
    assertThat(updatedRoleDTO.id).isEqualTo(apiRoleDTO.id);

    assertApiRoleDTO(updatedRoleDTO, apiRoleDTO, categoryDisplayName);
    assertRole(apiRoleDTO, Permission.READ, Permission.WRITE);
  }

  @Test
  public void testDeleteRole() throws Exception {
    Role role = tempEntity.newRole(false);
    HttpResponse response = restRequest().path(role.getId()).delete();
    assertResponseStatus(204, response);
    assertThat(roleDAO.getById(role.getId())).isNull();
  }

  private ApiPermissionCategoryDTO createPermissionCategoryDTO(final String categoryDisplayName) {
    ApiPermissionCategoryDTO permissionCategoryDTO = new ApiPermissionCategoryDTO();
    permissionCategoryDTO.displayName = categoryDisplayName;
    permissionCategoryDTO.permissions = new ArrayList<>();
    permissionCategoryDTO.permissions.add(createPermissionDTO(Permission.READ, true));
    permissionCategoryDTO.permissions.add(createPermissionDTO(Permission.WRITE, true));
    return permissionCategoryDTO;
  }

  private ApiPermissionDTO createPermissionDTO(final Permission permission, final boolean allowed) {
    ApiPermissionDTO permissionDTO = new ApiPermissionDTO();
    permissionDTO.id = permission;
    permissionDTO.allowed = allowed;
    permissionDTO.displayName = permission.getDisplayName();
    permissionDTO.description = permission.getDescription();
    return permissionDTO;
  }

  private void assertApiRoleDTO(
      final ApiRoleDTO actualRole,
      final ApiRoleDTO expectedRole,
      final String expectedPermissionCategoryName)
  {
    assertThat(actualRole.name).isEqualTo(expectedRole.name);
    assertThat(actualRole.description).isEqualTo(expectedRole.description);
    assertThat(actualRole.permissionCategories).hasSize(1);

    ApiPermissionCategoryDTO actualPermissionCategoryDTO = actualRole.permissionCategories.get(0);
    assertThat(actualPermissionCategoryDTO.displayName).isEqualTo(expectedPermissionCategoryName);
    assertThat(actualPermissionCategoryDTO.permissions).extracting(dto -> dto.id)
        .containsExactlyInAnyOrder(Permission.WRITE, Permission.READ);
  }

  private void assertRole(final ApiRoleDTO expected, final Permission... expectedPermissions) {
    Role updatedRole = roleDAO.getByIdNotNull(expected.id);
    assertThat(updatedRole.getId()).isEqualTo(expected.id);
    assertThat(updatedRole.getName()).isEqualTo(expected.name);
    assertThat(updatedRole.getDescription()).isEqualTo(expected.description);
    Set<Permission> updatedPermissions = rolePermissionDAO.getPermissionsForRole(expected.id);
    assertThat(updatedPermissions).containsExactlyInAnyOrder(expectedPermissions);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ROLE_RESOURCE_PATH_V2);
  }
}
