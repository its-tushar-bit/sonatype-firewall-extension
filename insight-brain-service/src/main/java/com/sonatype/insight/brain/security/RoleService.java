/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiRoleAdapter;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.PermissionCategory;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import static java.util.stream.Collectors.toList;

/**
 * @since 1.15.0
 */
@Named
public class RoleService
{
  private final RoleDAO roleDAO;

  private final RolePermissionService rolePermissionService;

  @Inject
  public RoleService(final RoleDAO roleDAO, final RolePermissionService rolePermissionService) {
    this.roleDAO = roleDAO;
    this.rolePermissionService = rolePermissionService;
  }

  /**
   * Note: as an optimization we will not populate the permissions as the UI does not need them for get all.
   */
  @Authorize(permission = Permission.VIEW_ROLES)
  public List<RoleDTO> getAllRoles() {
    return convertRolesToDTO(roleDAO.getAll());
  }

  @Authorize(permission = Permission.VIEW_ROLES)
  public ApiRoleListDTO getRolesAsApiRoleListDTO() {
    return ApiRoleAdapter.convertToDTO(roleDAO.getAll());
  }

  @Authorize(permission = Permission.VIEW_ROLES)
  public RoleDTO getRoleById(final String roleId) {
    Role role = roleDAO.getByIdNotNull(roleId);
    RoleDTO roleDTO = new RoleDTO(role);
    roleDTO.permissionCategories = getPermissionsForRole(roleDTO);
    return roleDTO;
  }

  @Authorize(permission = Permission.EDIT_ROLES)
  public RoleDTO getTemplateForNewRole() {
    RoleDTO roleDTO = new RoleDTO();
    roleDTO.permissionCategories = getPermissionsForNewCustomRole();
    return roleDTO;
  }

  @Authorize(permission = Permission.EDIT_ROLES)
  public RoleDTO addRole(RoleDTO roleDTO) {
    Role role = convertRoleFromDTO(roleDTO);
    Set<Permission> rolePermissions = convertRolePermissionFromDTO(roleDTO);
    role.setId(null);
    try (TransactionContext tx = roleDAO.createTransactionContext()) {
      tx.begin();
      roleDAO.insert(tx, role);
      rolePermissionService.setPermissionsForRole(tx, role.getId(), rolePermissions);
      tx.commit();
    }
    auditRole(role, rolePermissions);
    roleDTO.id = role.getId();
    return roleDTO;
  }

  @Authorize(permission = Permission.EDIT_ROLES)
  public RoleDTO updateRole(RoleDTO roleDTO) {
    Role role = convertRoleFromDTO(roleDTO);
    Set<Permission> rolePermissions = convertRolePermissionFromDTO(roleDTO);
    try (TransactionContext tx = roleDAO.createTransactionContext()) {
      tx.begin();
      roleDAO.update(tx, role);
      rolePermissionService.setPermissionsForRole(tx, roleDTO.id, rolePermissions);
      tx.commit();
    }
    auditRole(role, rolePermissions);
    return roleDTO;
  }

  @Authorize(permission = Permission.EDIT_ROLES)
  public void deleteRole(String roleId) {
    Role role = roleDAO.getByIdNotNull(roleId);
    auditRole(role, rolePermissionService.getPermissionsForRole(roleId));
    roleDAO.delete(role);
  }

  private void auditRole(Role role, Set<Permission> permissions) {
    AuditData.get() //
        .setData("roleId", role.getId()) //
        .setData("roleName", role.getName()) //
        .setData("roleDescription", role.getDescription())
        .setData("grantedPermissions", permissions.stream().map(this::toAuditLogString).sorted().collect(toList()));
  }

  private String toAuditLogString(Permission permission) {
    return permission.getDisplayName() + ' ' + permission.getDescription();
  }

  private List<RoleDTO> convertRolesToDTO(final List<Role> roles) {
    List<RoleDTO> roleDTOs = new ArrayList<>(roles.size());
    for (Role role : roles) {
      roleDTOs.add(new RoleDTO(role));
    }
    return roleDTOs;
  }

  private Role convertRoleFromDTO(final RoleDTO roleDTO) {
    Role role = new Role();
    role.setId(roleDTO.id);
    role.setName(roleDTO.name);
    role.setDescription(roleDTO.description);
    role.setBuiltIn(roleDTO.builtIn);
    return role;
  }

  private Set<Permission> convertRolePermissionFromDTO(final RoleDTO roleDTO) {
    Set<Permission> rolePermissions = EnumSet.noneOf(Permission.class);
    if (roleDTO.permissionCategories != null) {
      for (PermissionCategoryDTO permissionCategory : roleDTO.permissionCategories) {
        if (permissionCategory.permissions != null) {
          for (PermissionDTO permission : permissionCategory.permissions) {
            if (permission.allowed) {
              rolePermissions.add(permission.id);
            }
          }
        }
      }
    }
    return rolePermissions;
  }

  private List<PermissionCategoryDTO> getPermissionsForRole(final RoleDTO roleDTO) {
    boolean customRole = !roleDTO.builtIn;
    Set<Permission> permissionsForRole = rolePermissionService.getPermissionsForRole(roleDTO.id);
    return convertPermissionsToDTO(permissionsForRole, customRole);
  }

  private List<PermissionCategoryDTO> getPermissionsForNewCustomRole() {
    return convertPermissionsToDTO(EnumSet.noneOf(Permission.class), true);
  }

  private List<PermissionCategoryDTO> convertPermissionsToDTO(final Set<Permission> permissions,
                                                              final boolean customRole)
  {
    ListMultimap<PermissionCategory, PermissionDTO> permissionsByCategoryMap = ArrayListMultimap.create();
    for (Permission perm : EnumSet.allOf(Permission.class)) {
      if (customRole && !perm.isAllowedInCustomRoles()) {
        continue;
      }
      if (!perm.isVisible()) {
        continue;
      }
      permissionsByCategoryMap.put(perm.getCategory(), new PermissionDTO(perm, permissions.contains(perm)));
    }

    List<PermissionCategoryDTO> permissionCategories = new ArrayList<>();
    for (PermissionCategory category : permissionsByCategoryMap.keySet()) {
      List<PermissionDTO> categoryPermissions = permissionsByCategoryMap.get(category);
      PermissionCategoryDTO permissionCategoryDTO = new PermissionCategoryDTO(category.getDisplayName());
      permissionCategoryDTO.permissions = categoryPermissions;
      permissionCategories.add(permissionCategoryDTO);
    }
    permissionCategories.sort(PERMISSION_CATEGORY_COMPARATOR);
    return permissionCategories;
  }

  // just so happens that alpha sort works for now
  private static final Comparator<PermissionCategoryDTO> PERMISSION_CATEGORY_COMPARATOR =
      (dto1, dto2) -> dto1.displayName.compareToIgnoreCase(dto2.displayName);
}
