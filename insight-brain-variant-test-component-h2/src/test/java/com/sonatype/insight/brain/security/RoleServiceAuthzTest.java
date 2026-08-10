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
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class RoleServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private RoleService roleService;

  @Test
  public void testGetAllRoles_Authorized() {
    grantGlobalPermission(Permission.VIEW_ROLES);
    List<RoleDTO> roles = roleService.getAllRoles();
    assertThat(roles).isNotEmpty();
  }

  @Test
  public void testGetAllRoles_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> roleService.getAllRoles());
  }

  @Test
  public void testGetRolesAsApiRoleListDTO_Authorized() {
    grantGlobalPermission(Permission.VIEW_ROLES);
    ApiRoleListDTO rolesAsApiRoleListDTO = roleService.getRolesAsApiRoleListDTO();
    assertThat(rolesAsApiRoleListDTO.roles).isNotEmpty();
  }

  @Test
  public void testGetRolesAsApiRoleListDTO_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> roleService.getRolesAsApiRoleListDTO());
  }

  @Test
  public void testGetRolesAsApiRoleListDTO_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> roleService.getRolesAsApiRoleListDTO());
  }

  @Test
  public void testGetAllRoles_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> roleService.getAllRoles());
  }

  @Test
  public void testGetRoleById_Authorized() {
    grantGlobalPermission(Permission.VIEW_ROLES);
    RoleDTO roleDTO = roleService.getRoleById(Role.POLICY_ADMIN_ROLE_ID);
    assertThat(roleDTO).isNotNull();
  }

  @Test
  public void testGetRoleById_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> roleService.getRoleById("dummyId"));
  }

  @Test
  public void testGetRoleById_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> roleService.getRoleById("dummyId"));
  }

  @Test
  public void testGetTemplateForNewRole_Authorized() {
    grantGlobalPermission(Permission.EDIT_ROLES);
    RoleDTO roleDTO = roleService.getTemplateForNewRole();
    assertThat(roleDTO).isNotNull();
  }

  @Test
  public void testGetTemplateForNewRole_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> roleService.getTemplateForNewRole());
  }

  @Test
  public void testGetTemplateForNewRole_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> roleService.getTemplateForNewRole());
  }

  @Test
  public void testAddRole_Authorized() {
    grantGlobalPermission(Permission.EDIT_ROLES);
    Role role = new Role("Name", "Description");
    RoleDTO roleDTO = roleService.addRole(new RoleDTO(role));
    role.setId(roleDTO.id);
  }

  @Test
  public void testAddRole_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> roleService.addRole(new RoleDTO(new Role("Name", "Description"))));
  }

  @Test
  public void testAddRole_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> roleService.addRole(new RoleDTO(new Role("Name", "Description"))));
  }

  @Test
  public void testUpdateRole_Authorized() {
    grantGlobalPermission(Permission.EDIT_ROLES);
    roleService.updateRole(new RoleDTO(tempEntity.newRole(false)));
  }

  @Test
  public void testUpdateRole_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> roleService.updateRole(new RoleDTO(tempEntity.newRole(false))));
  }

  @Test
  public void testUpdateRole_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> roleService.updateRole(new RoleDTO(tempEntity.newRole(false))));
  }

  @Test
  public void testDeleteRole_Authorized() {
    grantGlobalPermission(Permission.EDIT_ROLES);
    roleService.deleteRole(tempEntity.newRole(false).getId());
  }

  @Test
  public void testDeleteRole_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> roleService.deleteRole(tempEntity.newRole(false).getId()));
  }

  @Test
  public void testDeleteRole_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> roleService.deleteRole(tempEntity.newRole(false).getId()));
  }
}
