/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.RolePermission;

/**
 * @since 1.7
 */
public class RolePermissionDAO
    extends AbstractOperationalSqlDAO<RolePermission>
{
  private List<RolePermission> getByRoleId(String roleId) {
    String sQuery = "SELECT entity FROM RolePermission entity WHERE entity.roleId=?1";
    return getList(sQuery, roleId);
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
}
