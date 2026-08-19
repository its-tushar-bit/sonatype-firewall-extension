/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.ApiPermissionCategoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPermissionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.PermissionCategoryDTO;
import com.sonatype.insight.brain.security.PermissionDTO;
import com.sonatype.insight.brain.security.RoleDTO;

/**
 * @since 1.11.0
 */
public class ApiRoleAdapter
{
  public static ApiRoleListDTO convertToDTO(Collection<Role> roles) {
    ApiRoleListDTO roleListDTO = new ApiRoleListDTO();
    roleListDTO.roles = new ArrayList<>();
    for (Role role : roles) {
      roleListDTO.roles.add(convertToDTO(role));
    }
    return roleListDTO;
  }

  private static ApiRoleDTO convertToDTO(Role role) {
    ApiRoleDTO roleDTO = new ApiRoleDTO();
    roleDTO.id = role.getId();
    roleDTO.name = role.getName();
    roleDTO.description = role.getDescription();
    roleDTO.builtIn = role.isBuiltIn();
    return roleDTO;
  }

  public static ApiRoleDTO convertToDTO(final RoleDTO roleDTO) {
    ApiRoleDTO apiRoleDTO = new ApiRoleDTO();
    apiRoleDTO.id = roleDTO.id;
    apiRoleDTO.name = roleDTO.name;
    apiRoleDTO.description = roleDTO.description;
    apiRoleDTO.builtIn = roleDTO.builtIn;
    apiRoleDTO.permissionCategories = convertPermissionCategoriesToDTO(roleDTO.permissionCategories);
    return apiRoleDTO;
  }

  public static RoleDTO convertFromDTO(final ApiRoleDTO apiRoleDTO) {
    RoleDTO roleDTO = new RoleDTO();
    roleDTO.id = apiRoleDTO.id;
    roleDTO.name = apiRoleDTO.name;
    roleDTO.description = apiRoleDTO.description;
    roleDTO.builtIn = apiRoleDTO.builtIn;
    roleDTO.permissionCategories = convertPermissionCategoriesFromDTO(apiRoleDTO.permissionCategories);
    return roleDTO;
  }

  private static List<ApiPermissionCategoryDTO> convertPermissionCategoriesToDTO(
      final List<PermissionCategoryDTO> categories)
  {
    if (categories == null) {
      return null;
    }
    return categories.stream()
        .map(category -> {
          ApiPermissionCategoryDTO apiCategory = new ApiPermissionCategoryDTO();
          apiCategory.displayName = category.displayName;
          apiCategory.permissions = convertPermissionsToDTO(category.permissions);
          return apiCategory;
        })
        .toList();
  }

  private static List<ApiPermissionDTO> convertPermissionsToDTO(final List<PermissionDTO> permissions) {
    if (permissions == null) {
      return null;
    }
    return permissions.stream()
        .map(permission -> {
          ApiPermissionDTO apiPermission = new ApiPermissionDTO();
          apiPermission.id = permission.id;
          apiPermission.displayName = permission.displayName;
          apiPermission.description = permission.description;
          apiPermission.allowed = permission.allowed;
          return apiPermission;
        })
        .toList();
  }

  private static List<PermissionCategoryDTO> convertPermissionCategoriesFromDTO(
      final List<ApiPermissionCategoryDTO> apiCategories)
  {
    if (apiCategories == null) {
      return null;
    }
    return apiCategories.stream()
        .map(apiCategory -> {
          PermissionCategoryDTO category = new PermissionCategoryDTO();
          category.displayName = apiCategory.displayName;
          category.permissions = convertPermissionsFromDTO(apiCategory.permissions);
          return category;
        })
        .toList();
  }

  private static List<PermissionDTO> convertPermissionsFromDTO(final List<ApiPermissionDTO> apiPermissions) {
    if (apiPermissions == null) {
      return null;
    }
    return apiPermissions.stream()
        .map(apiPermission -> {
          PermissionDTO permission = new PermissionDTO();
          permission.id = apiPermission.id;
          permission.displayName = apiPermission.displayName;
          permission.description = apiPermission.description;
          permission.allowed = apiPermission.allowed;
          return permission;
        })
        .toList();
  }

  public static Set<Permission> convertRolePermissionFromDTO(final ApiRoleDTO roleDTO) {
    if (roleDTO.permissionCategories == null) {
      return EnumSet.noneOf(Permission.class);
    }

    return roleDTO.permissionCategories.stream()
        .map(category -> category.permissions)
        .filter(permissions -> permissions != null)
        .flatMap(List::stream)
        .filter(permission -> permission.allowed)
        .map(permission -> permission.id)
        .collect(() -> EnumSet.noneOf(Permission.class), Set::add, Set::addAll);
  }
}
