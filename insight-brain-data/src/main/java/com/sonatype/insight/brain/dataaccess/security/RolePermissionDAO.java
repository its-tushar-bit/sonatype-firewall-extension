/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.7
 */
public class RolePermissionDAO
    extends AbstractOperationalSqlDAO<RolePermission>
{
  private static volatile Map<Permission, Set<String>> roleIdsByPermission;

  private List<RolePermission> getByRoleId(String roleId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRoleId(tx, roleId);
    }
  }

  List<RolePermission> getByRoleId(TransactionContext tx, String roleId) {
    String sQuery = "SELECT entity FROM RolePermission entity WHERE entity.roleId=?1";
    return getList(tx, sQuery, roleId);
  }

  @Override
  public void insert(TransactionContext tx, RolePermission entity) {
    super.insert(tx, entity);
    roleIdsByPermission = null;
  }

  @Override
  public void update(TransactionContext tx, RolePermission entity) {
    super.update(tx, entity);
    roleIdsByPermission = null;
  }

  @Override
  public void delete(TransactionContext tx, RolePermission entity) {
    super.delete(tx, entity);
    roleIdsByPermission = null;
  }

  /**
   * Gets the permissions assigned to the given role.
   */
  public Set<Permission> getPermissionsForRole(String roleId) {
    Set<Permission> perms = EnumSet.noneOf(Permission.class);
    for (RolePermission assoc : getByRoleId(roleId)) {
      perms.add(assoc.getPermission());
    }
    return perms;
  }

  /**
   * Gets the ids of all roles that grant the given permission.
   */
  public Set<String> getRoleIdsByPermission(Permission permission) {
    Map<Permission, Set<String>> map = roleIdsByPermission;
    if (map == null) {
      map = new EnumMap<Permission, Set<String>>(Permission.class);
      for (Permission perm : Permission.values()) {
        map.put(perm, new HashSet<String>());
      }
      for (RolePermission rolePerm : getList("SELECT entity FROM RolePermission entity")) {
        map.get(rolePerm.getPermission()).add(rolePerm.getRoleId());
      }
      for (Map.Entry<Permission, Set<String>> entry : map.entrySet()) {
        entry.setValue(Collections.unmodifiableSet(entry.getValue()));
      }
      roleIdsByPermission = map;
    }
    return map.get(permission);
  }
}
