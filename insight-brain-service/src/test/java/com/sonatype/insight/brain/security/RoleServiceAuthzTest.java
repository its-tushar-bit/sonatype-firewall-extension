/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RoleService roleService;

  @Test
  public void testGetAllRoles_Authorized() {
    grantGlobalPermission(Permission.VIEW_ROLES);
    List<RoleDTO> roles = roleService.getAllRoles();
    assertThat(roles).isNotEmpty();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAllRoles_Unauthorized() {
    login();
    roleService.getAllRoles();
  }

  @Test
  public void testGetRolesAsApiRoleListDTO_Authorized() {
    grantGlobalPermission(Permission.VIEW_ROLES);
    ApiRoleListDTO rolesAsApiRoleListDTO = roleService.getRolesAsApiRoleListDTO();
    assertThat(rolesAsApiRoleListDTO.roles).isNotEmpty();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRolesAsApiRoleListDTO_Unauthorized() {
    login();
    roleService.getRolesAsApiRoleListDTO();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRolesAsApiRoleListDTO_Unauthenticated() {
    roleService.getRolesAsApiRoleListDTO();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAllRoles_Unauthenticated() {
    roleService.getAllRoles();
  }

  @Test
  public void testGetRoleById_Authorized() {
    grantGlobalPermission(Permission.VIEW_ROLES);
    RoleDTO roleDTO = roleService.getRoleById(Role.POLICY_ADMIN_ROLE_ID);
    assertThat(roleDTO).isNotNull();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRoleById_Unauthorized() {
    login();
    roleService.getRoleById("dummyId");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRoleById_Unauthenticated() {
    roleService.getRoleById("dummyId");
  }

  @Test
  public void testGetTemplateForNewRole_Authorized() {
    grantGlobalPermission(Permission.EDIT_ROLES);
    RoleDTO roleDTO = roleService.getTemplateForNewRole();
    assertThat(roleDTO).isNotNull();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetTemplateForNewRole_Unauthorized() {
    login();
    roleService.getTemplateForNewRole();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetTemplateForNewRole_Unauthenticated() {
    roleService.getTemplateForNewRole();
  }

  @Test
  public void testAddRole_Authorized() {
    grantGlobalPermission(Permission.EDIT_ROLES);
    Role role = new Role("Name", "Description");
    RoleDTO roleDTO = roleService.addRole(new RoleDTO(role));
    role.setId(roleDTO.id);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddRole_Unauthorized() {
    login();
    roleService.addRole(new RoleDTO(new Role("Name", "Description")));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddRole_Unauthenticated() {
    roleService.addRole(new RoleDTO(new Role("Name", "Description")));
  }

  @Test
  public void testUpdateRole_Authorized() {
    grantGlobalPermission(Permission.EDIT_ROLES);
    roleService.updateRole(new RoleDTO(tempEntity.newRole(false)));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateRole_Unauthorized() {
    login();
    roleService.updateRole(new RoleDTO(tempEntity.newRole(false)));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateRole_Unauthenticated() {
    roleService.updateRole(new RoleDTO(tempEntity.newRole(false)));
  }

  @Test
  public void testDeleteRole_Authorized() {
    grantGlobalPermission(Permission.EDIT_ROLES);
    roleService.deleteRole(tempEntity.newRole(false).getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteRole_Unauthorized() {
    login();
    roleService.deleteRole(tempEntity.newRole(false).getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteRole_Unauthenticated() {
    roleService.deleteRole(tempEntity.newRole(false).getId());
  }
}
