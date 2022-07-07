/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static java.util.stream.Collectors.toList;

public class RoleResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RoleResource.RESOURCE_PATH);
  }

  private RoleDTO role(String id, String name, String description, Permission... permissions) {
    RoleDTO roleDTO = new RoleDTO();
    roleDTO.id = id;
    roleDTO.name = name;
    roleDTO.description = description;
    PermissionCategoryDTO permissionCategoryDTO = new PermissionCategoryDTO();
    permissionCategoryDTO.permissions = new ArrayList<>();
    for (Permission permission : permissions) {
      permissionCategoryDTO.permissions.add(new PermissionDTO(permission, true));
    }
    roleDTO.permissionCategories = Collections.singletonList(permissionCategoryDTO);
    return roleDTO;
  }

  private void assertRoleData(AuditDTO auditDTO,
                              String id,
                              String name,
                              String description,
                              Permission... permissions)
  {
    List<String> permissionNames = Arrays.stream(permissions)
        .map(permission -> permission.getDisplayName() + ' ' + permission.getDescription()).sorted().collect(toList());
    assertCustomData(auditDTO, "roleId", id);
    assertCustomData(auditDTO, "roleName", name);
    assertCustomData(auditDTO, "roleDescription", description);
    assertCustomData(auditDTO, "grantedPermissions", permissionNames);
  }

  @Test
  public void testAddRole() throws Exception {
    RoleDTO roleDTO = role(null, "Test Role", "Just testing", Permission.READ, Permission.VIEW_ROLES);
    roleDTO.id = restRequest().body(roleDTO).post().getBody(RoleDTO.class).id;
    tempEntity.register(new RoleDAO().getByIdNotNull(roleDTO.id));

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ROLE, null);
    assertRoleData(auditDTO, roleDTO.id, roleDTO.name, roleDTO.description, Permission.READ, Permission.VIEW_ROLES);
  }

  @Test
  public void testAddRole_Unauthorized() throws Exception {
    RoleDTO roleDTO = role(null, "Test Role", "Just testing");
    restRequest().with(unauthorizedUser()).body(roleDTO).post();

    assertAuditLog(AuditEvent.CREATE_ROLE, "unauthorized");
  }

  @Test
  public void testUpdateRole() throws Exception {
    RoleDTO roleDTO = role(tempEntity.newRole(false).getId(), "Test Role", "Just testing", Permission.WRITE);
    restRequest().body(roleDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_ROLE, null);
    assertRoleData(auditDTO, roleDTO.id, roleDTO.name, roleDTO.description, Permission.WRITE);
  }

  @Test
  public void testUpdateRole_Unauthorized() throws Exception {
    RoleDTO roleDTO = role(tempEntity.newRole(false).getId(), "Test Role", "Just testing");
    restRequest().with(unauthorizedUser()).body(roleDTO).put();

    assertAuditLog(AuditEvent.UPDATE_ROLE, "unauthorized");
  }

  @Test
  public void testDeleteRole() throws Exception {
    Role role = tempEntity.newRole("Test Role", "Just testing", false, Permission.MANAGE_PROPRIETARY);
    restRequest().path(RoleResource.ROLE_ID_PATH).parameter(role.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ROLE, null);
    assertRoleData(auditDTO, role.getId(), role.getName(), role.getDescription(), Permission.MANAGE_PROPRIETARY);
  }

  @Test
  public void testDeleteRole_Unauthorized() throws Exception {
    Role role = tempEntity.newRole(false);
    restRequest().with(unauthorizedUser()).path(RoleResource.ROLE_ID_PATH).parameter(role.getId()).delete();

    assertAuditLog(AuditEvent.DELETE_ROLE, "unauthorized");
  }
}
