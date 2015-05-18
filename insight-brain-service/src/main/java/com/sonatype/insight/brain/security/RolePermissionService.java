/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.security.Permission;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * @since 1.15.0
 */
@Named
public class RolePermissionService
{
  private final RolePermissionDAO rolePermissionDAO;

  @Inject
  public RolePermissionService(final RolePermissionDAO rolePermissionDAO) {
    this.rolePermissionDAO = rolePermissionDAO;
  }

  @Authorize(permission = Permission.VIEW_ROLES)
  public RolePermissionDTO getPermissionsForNewCustomRole() {
    RolePermissionDTO rolePermissionDTO = new RolePermissionDTO();
    buildDTO(rolePermissionDTO, EnumSet.noneOf(Permission.class));
    return rolePermissionDTO;
  }

  @Authorize(permission = Permission.VIEW_ROLES)
  public RolePermissionDTO getPermissionsForRole(final String roleId) {
    RolePermissionDTO rolePermissionDTO = new RolePermissionDTO(roleId);
    Set<Permission> permissionsForRole = rolePermissionDAO.getPermissionsForRole(roleId);
    buildDTO(rolePermissionDTO, permissionsForRole);
    return rolePermissionDTO;

  }

  private void buildDTO(RolePermissionDTO rolePermissionDTO, Set<Permission> permissions) {
    ListMultimap<String, PermissionDTO> permissionsByCategoryMap = ArrayListMultimap.create();

    for (Permission perm : EnumSet.allOf(Permission.class)) {
      permissionsByCategoryMap.put(perm.getCategory(), new PermissionDTO(perm, permissions.contains(perm)));
    }

    for (String category : permissionsByCategoryMap.keySet()) {
      List<PermissionDTO> categoryPermissions = permissionsByCategoryMap.get(category);
      PermissionCategoryDTO permissionCategoryDTO = new PermissionCategoryDTO(category);
      permissionCategoryDTO.permissions = categoryPermissions;
      rolePermissionDTO.permissionCategories.add(permissionCategoryDTO);
    }
    Collections.sort(rolePermissionDTO.permissionCategories, PERMISSION_CATEGORY_COMPARATOR);
  }

  // just so happens that alpha sort works for now
  static final Comparator<PermissionCategoryDTO> PERMISSION_CATEGORY_COMPARATOR = new Comparator<PermissionCategoryDTO>()
  {
    @Override
    public int compare(final PermissionCategoryDTO o1, final PermissionCategoryDTO o2) {
      return o1.displayName.compareToIgnoreCase(o2.displayName);
    }
  };
}
