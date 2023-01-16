/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import java.util.concurrent.ConcurrentHashMap;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.7
 */
public class RolePermissionDAO
    extends AbstractOperationalSqlDAO<RolePermission>
{
  private static final Logger log = LoggerFactory.getLogger(RolePermissionDAO.class);

  private static final TenantReference<Map<Permission, Set<String>>> roleIdsByPermission
      = new TenantReference<>(ConcurrentHashMap::new);

  private static Runnable clearRolePermissionCacheForAllOtherNodes =
      () -> log.warn("Clear role permission cache for all other nodes not set.");

  public static Runnable getClearRolePermissionCacheForAllOtherNodes() {
    return clearRolePermissionCacheForAllOtherNodes;
  }

  public static void setClearRolePermissionCacheForAllOtherNodes(Runnable clearRolePermissionCacheForAllOtherNodes) {
    RolePermissionDAO.clearRolePermissionCacheForAllOtherNodes = clearRolePermissionCacheForAllOtherNodes;
  }

  public static void clearRolePermissionCache() {
    roleIdsByPermission.remove();
  }

  List<RolePermission> getByRoleId(String roleId) {
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
    clearRolePermissionCache();
    clearRolePermissionCacheForAllOtherNodes.run();
  }

  @Override
  public void update(TransactionContext tx, RolePermission entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(TransactionContext tx, RolePermission entity) {
    super.delete(tx, entity);
    clearRolePermissionCache();
    clearRolePermissionCacheForAllOtherNodes.run();
  }

  public void setPermissionsForRole(String roleId, Set<Permission> permissions) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      setPermissionsForRole(tx, roleId, permissions);
      tx.commit();
    }
  }

  public void setPermissionsForRole(TransactionContext tx, String roleId, Set<Permission> permissions) {
    Role role = new RoleDAO().getByIdNotNull(tx, roleId);
    if (role.isBuiltIn()) {
      throw new BadRequestException("Cannot change permissions for built-in role '" + role.getName() + "'");
    }

    Set<Permission> alreadySet = EnumSet.noneOf(Permission.class);
    for (RolePermission assoc : getByRoleId(tx, roleId)) {
      if (permissions.contains(assoc.getPermission())) {
        alreadySet.add(assoc.getPermission());
      }
      else {
        delete(tx, assoc);
      }
    }
    for (Permission permission : permissions) {
      if (!permission.isAllowedInCustomRoles()) {
        throw new BadRequestException("Cannot assign permission '" + permission + "' to custom role '" + role.getName()
            + "'");
      }
      if (!alreadySet.contains(permission)) {
        insert(tx, new RolePermission(roleId, permission));
      }
    }
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
    Map<Permission, Set<String>> map = roleIdsByPermission.get();
    if (map == null || map.isEmpty()) {
      map = new EnumMap<>(Permission.class);
      for (Permission perm : Permission.values()) {
        map.put(perm, new HashSet<String>());
      }
      for (RolePermission rolePerm : getList("SELECT entity FROM RolePermission entity")) {
        map.get(rolePerm.getPermission()).add(rolePerm.getRoleId());
      }
      for (Map.Entry<Permission, Set<String>> entry : map.entrySet()) {
        entry.setValue(Collections.unmodifiableSet(entry.getValue()));
      }
      roleIdsByPermission.set(map);
    }
    return map.get(permission);
  }

  /**
   * @since 1.35
   */
  public List<RolePermission> getAll() {
    String sQuery = "SELECT entity FROM RolePermission entity";
    return getList(sQuery);
  }
}
