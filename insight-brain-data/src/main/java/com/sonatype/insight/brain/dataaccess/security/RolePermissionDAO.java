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
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.RolePermission.ROLE_PERMISSION;

/**
 * @since 1.7
 */
@Named
@Singleton
public class RolePermissionDAO
    extends AbstractOperationalSqlDAO<RolePermission>
{
  private static final Logger log = LoggerFactory.getLogger(RolePermissionDAO.class);

  private static final TenantReference<Map<Permission, Set<String>>> roleIdsByPermission =
      new TenantReference<>(ConcurrentHashMap::new);

  private static final Runnable DEFAULT_CLEAR_ROLE_PERMISSION_CACHE_FOR_ALL_OTHER_NODES =
      () -> log.warn("Clear role permission cache for all other nodes not set.");

  private static Runnable clearRolePermissionCacheForAllOtherNodes =
      DEFAULT_CLEAR_ROLE_PERMISSION_CACHE_FOR_ALL_OTHER_NODES;

  @Inject
  public RolePermissionDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public static Runnable getClearRolePermissionCacheForAllOtherNodes() {
    return clearRolePermissionCacheForAllOtherNodes;
  }

  public static void setClearRolePermissionCacheForAllOtherNodes(Runnable clearRolePermissionCacheForAllOtherNodes) {
    RolePermissionDAO.clearRolePermissionCacheForAllOtherNodes = clearRolePermissionCacheForAllOtherNodes;
  }

  @VisibleForTesting
  public static void resetClearRolePermissionCacheForAllOtherNodes() {
    setClearRolePermissionCacheForAllOtherNodes(DEFAULT_CLEAR_ROLE_PERMISSION_CACHE_FOR_ALL_OTHER_NODES);
  }

  public static void clearRolePermissionCache() {
    roleIdsByPermission.remove();
  }

  public List<RolePermission> getByRoleId(String roleId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRoleId(tx, roleId);
    }
  }

  public List<RolePermission> getByRoleId(TransactionContext tx, String roleId) {
    return tx.dsl()
        .selectFrom(ROLE_PERMISSION)
        .where(ROLE_PERMISSION.ROLE_ID.eq(roleId))
        .fetch()
        .map(this::toEntity);
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
        map.put(perm, new HashSet<>());
      }
      try (TransactionContext tx = createTransactionContext()) {
        for (RolePermission rolePerm : tx.dsl().selectFrom(ROLE_PERMISSION).fetch().map(this::toEntity)) {
          map.get(rolePerm.getPermission()).add(rolePerm.getRoleId());
        }
      }
      for (Map.Entry<Permission, Set<String>> entry : map.entrySet()) {
        entry.setValue(Collections.unmodifiableSet(entry.getValue()));
      }
      roleIdsByPermission.set(map);
    }
    return map.get(permission);
  }

  @Override
  public Table<?> getJooqTable() {
    return ROLE_PERMISSION;
  }

  @Override
  public Class<RolePermission> getEntityClass() {
    return RolePermission.class;
  }
}
