/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionTest
{
  @Test
  public void testPermissionsNotAllowedInCustomRoles() {
    for (Permission permission : Permission.values()) {
      switch (permission) {
        case CONFIGURE_SYSTEM:
        case EDIT_ROLES:
          assertThat(permission.isAllowedInCustomRoles()).isFalse();
          break;
        default:
          assertThat(permission.isAllowedInCustomRoles()).isTrue();
          break;
      }
    }
  }

  @Test
  public void testPermissions() {
    assertPermission(Permission.CONFIGURE_SYSTEM, "Edit", PermissionCategory.ADMINISTRATOR,
        "System Configuration and Users", true, false);
    assertPermission(Permission.EDIT_ROLES, "Edit", PermissionCategory.ADMINISTRATOR, "Custom Roles", true, false);
    assertPermission(Permission.VIEW_ROLES, "View", PermissionCategory.ADMINISTRATOR, "All Roles", true, true);
    assertPermission(Permission.ACCESS_AUDIT_LOG, "Access", PermissionCategory.ADMINISTRATOR, "Audit Log", true, true);
    assertPermission(Permission.WAIVE_POLICY_VIOLATIONS, "Waive", PermissionCategory.REMEDIATION, "Policy Violations",
        false, true);
    assertPermission(Permission.CHANGE_LICENSES, "Change", PermissionCategory.REMEDIATION, "Licenses", false, true);
    assertPermission(Permission.CHANGE_SECURITY_VULNERABILITIES, "Change", PermissionCategory.REMEDIATION,
        "Security Vulnerabilities", false, true);
    assertPermission(Permission.MANAGE_PROPRIETARY, "Edit", PermissionCategory.IQ, "Proprietary Components", false,
        true);
    assertPermission(Permission.CLAIM_COMPONENT, "Claim", PermissionCategory.IQ, "Components", true, true);
    assertPermission(Permission.WRITE, "Edit", PermissionCategory.IQ, "IQ Elements", false, true);
    assertPermission(Permission.READ, "View", PermissionCategory.IQ, "IQ Elements", false, true);
    assertPermission(Permission.EDIT_ACCESS_CONTROL, "Edit", PermissionCategory.IQ, "Access Control", false, true);
    assertPermission(Permission.EVALUATE_APPLICATION, "Evaluate", PermissionCategory.IQ, "Applications", false, true);
    assertPermission(Permission.EVALUATE_COMPONENT, "Evaluate", PermissionCategory.IQ, "Individual Components", false,
        true);
    assertPermission(Permission.ADD_APPLICATION, "Add", PermissionCategory.IQ, "Applications", false, true);
    assertPermission(Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION, "Manage", PermissionCategory.IQ,
        "Automatic Application Creation", true, true);
    assertPermission(Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION, "Manage", PermissionCategory.IQ,
        "Automatic Source Control Configuration", true, true);
    assertPermission(Permission.LEGAL_REVIEWER, "Review", PermissionCategory.REMEDIATION,
        "Legal obligations for components licenses", false, true);
  }

  private void assertPermission(
      final Permission permission,
      final String expectedDisplayName,
      final PermissionCategory expectedPermissionCategory,
      final String expectedDescription,
      final boolean expectedGlobal,
      final boolean expectedAllowedInCustomRoles)
  {
    assertThat(permission.getDisplayName()).isEqualTo(expectedDisplayName);
    assertThat(permission.getCategory()).isEqualTo(expectedPermissionCategory);
    assertThat(permission.getDescription()).isEqualTo(expectedDescription);
    assertThat(permission.isGlobal()).isEqualTo(expectedGlobal);
    assertThat(permission.isAllowedInCustomRoles()).isEqualTo(expectedAllowedInCustomRoles);
  }
}
