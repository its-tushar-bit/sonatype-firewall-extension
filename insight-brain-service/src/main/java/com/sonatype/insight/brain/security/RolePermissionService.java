/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.EnumSet;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

@Named
public class RolePermissionService
{
  private final RoleDAO roleDAO;

  private final RolePermissionDAO rolePermissionDAO;

  @Inject
  public RolePermissionService(
      final RoleDAO roleDAO,
      final RolePermissionDAO rolePermissionDAO)
  {
    this.roleDAO = roleDAO;
    this.rolePermissionDAO = rolePermissionDAO;
  }

  public void setPermissionsForRole(String roleId, Set<Permission> permissions) {
    try (TransactionContext tx = rolePermissionDAO.createTransactionContext()) {
      tx.begin();
      setPermissionsForRole(tx, roleId, permissions);
      tx.commit();
    }
  }

  public void setPermissionsForRole(TransactionContext tx, String roleId, Set<Permission> permissions) {
    Role role = roleDAO.getByIdNotNull(tx, roleId);
    if (role.isBuiltIn()) {
      throw new BadRequestException("Cannot change permissions for built-in role '" + role.getName() + "'");
    }

    Set<Permission> alreadySet = EnumSet.noneOf(Permission.class);
    for (RolePermission assoc : rolePermissionDAO.getByRoleId(tx, roleId)) {
      if (permissions.contains(assoc.getPermission())) {
        alreadySet.add(assoc.getPermission());
      }
      else {
        rolePermissionDAO.delete(tx, assoc);
      }
    }
    for (Permission permission : permissions) {
      if (!permission.isAllowedInCustomRoles()) {
        throw new BadRequestException("Cannot assign permission '" + permission + "' to custom role '" + role.getName()
            + "'");
      }
      if (!alreadySet.contains(permission)) {
        rolePermissionDAO.insert(tx, new RolePermission(roleId, permission));
      }
    }
  }

  public Set<Permission> getPermissionsForRole(final String roleId) {
    return rolePermissionDAO.getPermissionsForRole(roleId);
  }
}
