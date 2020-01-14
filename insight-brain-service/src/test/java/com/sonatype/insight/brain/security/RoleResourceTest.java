/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleResourceTest
    extends AbstractResourceTest
{
  private RoleDAO roleDAO = new RoleDAO();

  private RolePermissionDAO rolePermissionDAO = new RolePermissionDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RoleResource.RESOURCE_PATH);
  }

  @Test
  public void testGetAllRoles() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    RoleDTO[] roles = response.getBody(RoleDTO[].class);
    assertThat(roles).isNotEmpty();
  }

  @Test
  public void testGetRoleById() throws Exception {
    Role role = tempEntity.newRole(false, Permission.CLAIM_COMPONENT);
    HttpResponse response = restRequest().path(RoleResource.ROLE_ID_PATH).parameter(role.getId()).get();
    assertResponseStatus(200, response);
    RoleDTO roleDTO = response.getBody(RoleDTO.class);
    assertThat(roleDTO).isNotNull();
    assertThat(roleDTO.id).isEqualTo(role.getId());
  }

  @Test
  public void testGetTemplateForNewRole() throws Exception {
    HttpResponse response = restRequest().path(RoleResource.NEW_PATH).get();
    assertResponseStatus(200, response);
    RoleDTO role = response.getBody(RoleDTO.class);
    assertThat(role).isNotNull();
    assertThat(role.permissionCategories).hasSize(3);
  }

  @Test
  public void testAddRole() throws Exception {
    RoleDTO roleDTO = new RoleDTO();
    roleDTO.name = "New Name";
    roleDTO.description = "New Description";
    roleDTO.permissionCategories = new ArrayList<>();
    String categoryDisplayName = "TestDisplayName";
    roleDTO.permissionCategories.add(createPermissionCategoryDTO(categoryDisplayName));

    HttpResponse response = restRequest().body(roleDTO).post();
    assertResponseStatus(200, response);
    RoleDTO newRoleDTO = response.getBody(RoleDTO.class);
    assertThat(newRoleDTO.id).isNotNull();
    tempEntity.register(roleDAO.getByIdNotNull(newRoleDTO.id));

    assertRoleDTO(newRoleDTO, roleDTO, categoryDisplayName);
    assertRole(newRoleDTO, Permission.READ, Permission.WRITE);
  }

  @Test
  public void testUpdateRole() throws Exception {
    Role role = tempEntity.newRole(false);
    RoleDTO roleDTO = new RoleDTO(role);
    roleDTO.name = "Updated Name";
    roleDTO.description = "Updated Description";
    roleDTO.permissionCategories = new ArrayList<>();
    String categoryDisplayName = "TestDisplayName";
    roleDTO.permissionCategories.add(createPermissionCategoryDTO(categoryDisplayName));

    HttpResponse response = restRequest().body(roleDTO).put();
    assertResponseStatus(200, response);
    RoleDTO updatedRoleDTO = response.getBody(RoleDTO.class);
    assertThat(updatedRoleDTO.id).isEqualTo(roleDTO.id);

    assertRoleDTO(updatedRoleDTO, roleDTO, categoryDisplayName);
    assertRole(roleDTO, Permission.READ, Permission.WRITE);
  }

  @Test
  public void testDeleteRole() throws Exception {
    Role role = tempEntity.newRole(false);
    HttpResponse response = restRequest().path(RoleResource.ROLE_ID_PATH).parameter(role.getId()).delete();
    assertResponseStatus(204, response);
    assertThat(new RoleDAO().getById(role.getId())).isNull();
  }

  private PermissionCategoryDTO createPermissionCategoryDTO(final String categoryDisplayName) {
    PermissionCategoryDTO permissionCategoryDTO = new PermissionCategoryDTO(categoryDisplayName);
    permissionCategoryDTO.permissions = new ArrayList<>();
    permissionCategoryDTO.permissions.add(new PermissionDTO(Permission.READ, true));
    permissionCategoryDTO.permissions.add(new PermissionDTO(Permission.WRITE, true));
    return permissionCategoryDTO;
  }

  private void assertRoleDTO(final RoleDTO actualRole,
                             final RoleDTO expectedRole,
                             final String expectedPermissionCategoryName)
  {
    assertThat(actualRole.name).isEqualTo(expectedRole.name);
    assertThat(actualRole.description).isEqualTo(expectedRole.description);
    assertThat(actualRole.permissionCategories).hasSize(1);

    PermissionCategoryDTO actualPermissionCategoryDTO = actualRole.permissionCategories.get(0);
    assertThat(actualPermissionCategoryDTO.displayName).isEqualTo(expectedPermissionCategoryName);
    assertThat(actualPermissionCategoryDTO.permissions).extracting(dto -> dto.id)
        .containsExactlyInAnyOrder(Permission.WRITE, Permission.READ);
  }

  private void assertRole(final RoleDTO expected, final Permission... expectedPermissions) {
    Role updatedRole = roleDAO.getByIdNotNull(expected.id);
    assertThat(updatedRole.getId()).isEqualTo(expected.id);
    assertThat(updatedRole.getName()).isEqualTo(expected.name);
    assertThat(updatedRole.getDescription()).isEqualTo(expected.description);
    Set<Permission> updatedPermissions = rolePermissionDAO.getPermissionsForRole(expected.id);
    assertThat(updatedPermissions).containsExactlyInAnyOrder(expectedPermissions);
  }
}
