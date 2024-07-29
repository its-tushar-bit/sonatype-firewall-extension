/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiRoleDTO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.PermissionCategory;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RoleService roleService;

  @Inject
  private RoleDAO roleDAO;

  @Inject
  private TestProductLicense productLicense;

  private static final List<String> ALL_ROLE_IDS = Arrays.asList(
      Role.SYSTEM_ADMIN_ROLE_ID,
      Role.POLICY_ADMIN_ROLE_ID,
      Role.APPLICATION_EVALUATOR_ROLE_ID,
      Role.COMPONENT_EVALUATOR_ROLE_ID,
      Role.DEVELOPER_ROLE_ID,
      Role.LEGAL_REVIEWER_ROLE_ID,
      Role.SBOM_EXPORTER_ROLE_ID,
      Role.SBOM_IMPORTER_ROLE_ID,
      Role.OWNER_ROLE_ID
  );

  private static final List<String> ROLE_IDS_WITHOUT_SBOM_EXPORT_IMPORT = Arrays.asList(
      Role.SYSTEM_ADMIN_ROLE_ID,
      Role.POLICY_ADMIN_ROLE_ID,
      Role.APPLICATION_EVALUATOR_ROLE_ID,
      Role.COMPONENT_EVALUATOR_ROLE_ID,
      Role.DEVELOPER_ROLE_ID,
      Role.LEGAL_REVIEWER_ROLE_ID,
      Role.OWNER_ROLE_ID
  );

  @Test
  public void testGetRoles_WithSbomManager_WithSecureSharing() {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
    testGetRoles(ALL_ROLE_IDS);
  }

  @Test
  public void testGetRoles_WithSbomManager_WithoutSecureSharing() {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(false);
    testGetRoles(ROLE_IDS_WITHOUT_SBOM_EXPORT_IMPORT);
  }

  @Test
  public void testGetRoles_WithoutSbomManager_WithSecureSharing() {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
    testGetRoles(ROLE_IDS_WITHOUT_SBOM_EXPORT_IMPORT);
  }

  @Test
  public void testGetRoles_WithoutSbomManager_WithoutSecureSharing() {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(false);
    testGetRoles(ROLE_IDS_WITHOUT_SBOM_EXPORT_IMPORT);
  }

  private void testGetRoles(final List<String> expectedRoleIds) {
    List<RoleDTO> roles = roleService.getRoles();

    assertThat(roles).isNotEmpty();
    assertThat(roles).extracting(role -> role.id).containsExactlyInAnyOrderElementsOf(expectedRoleIds);
  }

  @Test
  public void updateRole_Permissions() {
    Role role = tempEntity.newRole(false, Permission.READ);

    RoleDTO roleDTO = roleService.getRoleById(role.getId());
    setAllowedPermissions(roleDTO, Arrays.asList(Permission.WRITE, Permission.EVALUATE_APPLICATION));

    roleService.updateRole(roleDTO);
    RoleDTO updatedRoleDTO = roleService.getRoleById(role.getId());
    assertAllowedPermissions(updatedRoleDTO, Permission.READ, Permission.WRITE, Permission.EVALUATE_APPLICATION);
  }

  @Test
  public void testGetRoleById_Builtin_WithSbomManager_WithSecureSharing() {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
    testGetRoleById_Builtin(true);
  }

  @Test
  public void testGetRoleById_Builtin_WithSbomManager_WithoutSecureSharing() {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(false);
    testGetRoleById_Builtin(false);
  }

  @Test
  public void testGetRoleById_Builtin_WithoutSbomManager_WithSecureSharing() {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
    testGetRoleById_Builtin(false);
  }

  @Test
  public void testGetRoleById_Builtin_WithoutSbomManager_WithoutSecureSharing() {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(false);
    testGetRoleById_Builtin(false);
  }

  private void testGetRoleById_Builtin(final boolean includeSbomRoles) {
    RoleDTO roleDTO = roleService.getRoleById(Role.SYSTEM_ADMIN_ROLE_ID);

    assertThat(roleDTO.id).isEqualTo(Role.SYSTEM_ADMIN_ROLE_ID);

    if (includeSbomRoles) {
      assertThat(roleDTO.permissionCategories).hasSize(4);
    }
    else {
      assertThat(roleDTO.permissionCategories).hasSize(3);
    }

    assertAllowedPermissions(roleDTO, Permission.CONFIGURE_SYSTEM, Permission.VIEW_ROLES);

    PermissionCategoryDTO category = roleDTO.permissionCategories.get(0);
    assertThat(category.displayName).isEqualTo(PermissionCategory.ADMINISTRATOR.getDisplayName());
    assertListedPermissions(category,
        Permission.CONFIGURE_SYSTEM, Permission.EDIT_ROLES, Permission.VIEW_ROLES, Permission.ACCESS_AUDIT_LOG);

    category = roleDTO.permissionCategories.get(1);
    assertThat(category.displayName).isEqualTo(PermissionCategory.IQ.getDisplayName());
    assertListedPermissions(category, //
        Permission.MANAGE_PROPRIETARY, //
        Permission.CLAIM_COMPONENT, //
        Permission.WRITE, //
        Permission.READ, //
        Permission.EDIT_ACCESS_CONTROL, //
        Permission.EVALUATE_APPLICATION, //
        Permission.EVALUATE_COMPONENT, //
        Permission.ADD_APPLICATION, //
        Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION, //
        Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);

    category = roleDTO.permissionCategories.get(2);
    assertThat(category.displayName).isEqualTo(PermissionCategory.REMEDIATION.getDisplayName());
    assertListedPermissions(category, Permission.WAIVE_POLICY_VIOLATIONS, Permission.CHANGE_LICENSES,
        Permission.CHANGE_SECURITY_VULNERABILITIES, Permission.LEGAL_REVIEWER);

    if (includeSbomRoles) {
      category = roleDTO.permissionCategories.get(3);
      assertThat(category.displayName).isEqualTo(PermissionCategory.SBOM.getDisplayName());
      assertListedPermissions(category, Permission.EXPORT_SBOM, Permission.IMPORT_SBOM);
    }
  }

  @Test
  public void testGetRoleById_Custom_WithSbomManager_WithSecureSharing() {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
    testGetRoleById_Custom(true);
  }

  @Test
  public void testGetRoleById_Custom_WithSbomManager_WithoutSecureSharing() {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(false);
    testGetRoleById_Custom(false);
  }

  @Test
  public void testGetRoleById_Custom_WithoutSbomManager_WithSecureSharing() {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
    testGetRoleById_Custom(false);
  }

  @Test
  public void testGetRoleById_Custom_WithoutSbomManager_WithoutSecureSharing() {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(false);
    testGetRoleById_Custom(false);
  }

  private void testGetRoleById_Custom(final boolean includeSbomRoles) {
    Role expectedRole = tempEntity.newRole(false, Permission.WRITE);

    RoleDTO roleDTO = roleService.getRoleById(expectedRole.getId());
    assertThat(roleDTO.id).isEqualTo(expectedRole.getId());
    assertThat(roleDTO.name).isEqualTo(expectedRole.getName());
    assertThat(roleDTO.description).isEqualTo(expectedRole.getDescription());

    if (includeSbomRoles) {
      assertThat(roleDTO.permissionCategories).hasSize(4);
    }
    else {
      assertThat(roleDTO.permissionCategories).hasSize(3);
    }

    assertAllowedPermissions(roleDTO, Permission.WRITE);

    PermissionCategoryDTO category = roleDTO.permissionCategories.get(0);
    assertThat(category.displayName).isEqualTo(PermissionCategory.ADMINISTRATOR.getDisplayName());
    assertListedPermissions(category, Permission.VIEW_ROLES, Permission.ACCESS_AUDIT_LOG);

    category = roleDTO.permissionCategories.get(1);
    assertThat(category.displayName).isEqualTo(PermissionCategory.IQ.getDisplayName());
    assertListedPermissions(category, //
        Permission.MANAGE_PROPRIETARY, //
        Permission.CLAIM_COMPONENT, //
        Permission.WRITE, //
        Permission.READ, //
        Permission.EDIT_ACCESS_CONTROL, //
        Permission.EVALUATE_APPLICATION, //
        Permission.EVALUATE_COMPONENT, //
        Permission.ADD_APPLICATION, //
        Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION, //
        Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);

    category = roleDTO.permissionCategories.get(2);
    assertThat(category.displayName).isEqualTo(PermissionCategory.REMEDIATION.getDisplayName());
    assertListedPermissions(category, Permission.WAIVE_POLICY_VIOLATIONS, Permission.CHANGE_LICENSES,
        Permission.CHANGE_SECURITY_VULNERABILITIES, Permission.LEGAL_REVIEWER);

    if (includeSbomRoles) {
      category = roleDTO.permissionCategories.get(3);
      assertThat(category.displayName).isEqualTo(PermissionCategory.SBOM.getDisplayName());
      assertListedPermissions(category, Permission.EXPORT_SBOM, Permission.IMPORT_SBOM);
    }
  }

  private void testGetTemplateForNewRole(final boolean includeSbomRoles) {
    RoleDTO roleDTO = roleService.getTemplateForNewRole();
    assertThat(roleDTO.id).isNull();
    assertThat(roleDTO.name).isNull();
    assertThat(roleDTO.description).isNull();

    if (includeSbomRoles) {
      assertThat(roleDTO.permissionCategories).hasSize(4);
    }
    else {
      assertThat(roleDTO.permissionCategories).hasSize(3);
    }

    assertAllowedPermissions(roleDTO);

    PermissionCategoryDTO category = roleDTO.permissionCategories.get(0);
    assertThat(category.displayName).isEqualTo(PermissionCategory.ADMINISTRATOR.getDisplayName());
    assertListedPermissions(category, Permission.VIEW_ROLES, Permission.ACCESS_AUDIT_LOG);

    category = roleDTO.permissionCategories.get(1);
    assertThat(category.displayName).isEqualTo(PermissionCategory.IQ.getDisplayName());
    assertListedPermissions(category, //
        Permission.MANAGE_PROPRIETARY, //
        Permission.CLAIM_COMPONENT, //
        Permission.WRITE, //
        Permission.READ, //
        Permission.EDIT_ACCESS_CONTROL, //
        Permission.EVALUATE_APPLICATION, //
        Permission.EVALUATE_COMPONENT, //
        Permission.ADD_APPLICATION, //
        Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION, //
        Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);

    category = roleDTO.permissionCategories.get(2);
    assertThat(category.displayName).isEqualTo(PermissionCategory.REMEDIATION.getDisplayName());
    assertListedPermissions(category, Permission.WAIVE_POLICY_VIOLATIONS, Permission.CHANGE_LICENSES,
        Permission.CHANGE_SECURITY_VULNERABILITIES, Permission.LEGAL_REVIEWER);

    if (includeSbomRoles) {
      category = roleDTO.permissionCategories.get(3);
      assertThat(category.displayName).isEqualTo(PermissionCategory.SBOM.getDisplayName());
      assertListedPermissions(category, Permission.EXPORT_SBOM, Permission.IMPORT_SBOM);
    }
  }

  @Test
  public void testGetTemplateForNewRole_WithSbomManager_WithSecureSharing() {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
    testGetTemplateForNewRole(true);
  }

  @Test
  public void testGetTemplateForNewRole_WithSbomManager_WithoutSecureSharing() {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(false);
    testGetTemplateForNewRole(false);
  }

  @Test
  public void testGetTemplateForNewRole_WithoutSbomManager_WithSecureSharing() {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
    testGetTemplateForNewRole(false);
  }

  @Test
  public void testGetTemplateForNewRole_WithoutSbomManager_WithoutSecureSharing() {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(false);
    testGetTemplateForNewRole(false);
  }

  @Test
  public void testGetRolesAsApiRoleListDTO() {
    List<Role> expectedRoles = ROLE_IDS_WITHOUT_SBOM_EXPORT_IMPORT.stream().map(roleDAO::getById).toList();

    List<ApiRoleDTO> roles = roleService.getRolesAsApiRoleListDTO().roles;

    assertThat(roles).hasSize(expectedRoles.size());
    for (Role role : expectedRoles) {
      ApiRoleDTO roleDTO = roles.stream().filter(r -> role.getId().equals(r.id)).findFirst().orElse(null);
      assertThat(roleDTO).isNotNull();
      assertThat(roleDTO.name).isEqualTo(role.getName());
      assertThat(roleDTO.description).isEqualTo(role.getDescription());
    }
  }

  private void setAllowedPermissions(final RoleDTO roleDTO, final List<Permission> permissions) {
    for (PermissionCategoryDTO categoryDTO : roleDTO.permissionCategories) {
      for (PermissionDTO permissionDTO : categoryDTO.permissions) {
        if (permissions.contains(permissionDTO.id)) {
          permissionDTO.allowed = true;
        }
      }
    }
  }

  private void assertListedPermissions(final PermissionCategoryDTO actual, final Permission... permissions) {
    assertThat(actual.permissions).hasSameSizeAs(permissions);
    for (int i = 0; i < permissions.length; i++) {
      assertThat(actual.permissions.get(i).id).isEqualTo(permissions[i]);
      assertThat(actual.permissions.get(i).displayName).isEqualTo(permissions[i].getDisplayName());
      assertThat(actual.permissions.get(i).description).isEqualTo(permissions[i].getDescription());
    }
  }

  private void assertAllowedPermissions(final RoleDTO actual, final Permission... permissions) {
    assertThat(actual.permissionCategories).flatExtracting(category -> category.permissions)
        .filteredOn(permission -> permission.allowed).extracting(permission -> permission.id)
        .containsExactlyInAnyOrder(permissions);
  }
}
