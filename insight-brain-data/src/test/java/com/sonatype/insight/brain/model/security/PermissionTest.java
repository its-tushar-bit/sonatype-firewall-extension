/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionTest
    extends AbstractDatabaseTest
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
  public void testPermissions_Default() {
    TestDAOFactory testDAOFactory = new TestDAOFactory(databaseRule);
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO =
        testDAOFactory.createSystemConfigurationPropertyDAO();
    SystemConfigurationPropertyFeature.injectDependencies(systemConfigurationPropertyDAO);

    assertPermission(Permission.CONFIGURE_SYSTEM, "Edit", PermissionCategory.ADMINISTRATOR,
        "System Configuration and Users", true, false, true);
    assertPermission(Permission.EDIT_ROLES, "Edit", PermissionCategory.ADMINISTRATOR, "Custom Roles", true, false,
        true);
    assertPermission(Permission.VIEW_ROLES, "View", PermissionCategory.ADMINISTRATOR, "All Roles", true, true, true);
    assertPermission(Permission.ACCESS_AUDIT_LOG, "Access", PermissionCategory.ADMINISTRATOR, "Audit Log", true, true,
        true);
    assertPermission(Permission.WAIVE_POLICY_VIOLATIONS, "Waive", PermissionCategory.REMEDIATION, "Policy Violations",
        false, true, true);
    assertPermission(Permission.CHANGE_LICENSES, "Change", PermissionCategory.REMEDIATION, "Licenses", false, true,
        true);
    assertPermission(Permission.CHANGE_SECURITY_VULNERABILITIES, "Change", PermissionCategory.REMEDIATION,
        "Security Vulnerabilities", false, true, true);
    assertPermission(Permission.MANAGE_PROPRIETARY, "Edit", PermissionCategory.IQ, "Proprietary Components", false,
        true, true);
    assertPermission(Permission.CLAIM_COMPONENT, "Claim", PermissionCategory.IQ, "Components", true, true, true);
    assertPermission(Permission.WRITE, "Edit", PermissionCategory.IQ, "IQ Elements", false, true, true);
    assertPermission(Permission.READ, "View", PermissionCategory.IQ, "IQ Elements", false, true, true);
    assertPermission(Permission.EDIT_ACCESS_CONTROL, "Edit", PermissionCategory.IQ, "Access Control", false, true,
        true);
    assertPermission(Permission.EVALUATE_APPLICATION, "Evaluate", PermissionCategory.IQ, "Applications", false, true,
        true);
    assertPermission(Permission.EVALUATE_COMPONENT, "Evaluate", PermissionCategory.IQ, "Individual Components", false,
        true, true);
    assertPermission(Permission.ADD_APPLICATION, "Add", PermissionCategory.IQ, "Applications", false, true, true);
    assertPermission(Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION, "Manage", PermissionCategory.IQ,
        "Automatic Application Creation", true, true, true);
    assertPermission(Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION, "Manage", PermissionCategory.IQ,
        "Automatic Source Control Configuration", true, true, true);
    assertPermission(Permission.LEGAL_REVIEWER, "Review", PermissionCategory.REMEDIATION,
        "Legal obligations for components licenses", false, true, true);
    assertPermission(Permission.CREATE_PULL_REQUESTS, "Create", PermissionCategory.REMEDIATION,
        "Pull requests", false, true, true);
  }

  private void assertPermission(
      final Permission permission,
      final String expectedDisplayName,
      final PermissionCategory expectedPermissionCategory,
      final String expectedDescription,
      final boolean expectedGlobal,
      final boolean expectedAllowedInCustomRoles,
      final boolean expectedVisible)
  {
    assertThat(permission.getDisplayName()).isEqualTo(expectedDisplayName);
    assertThat(permission.getCategory()).isEqualTo(expectedPermissionCategory);
    assertThat(permission.getDescription()).isEqualTo(expectedDescription);
    assertThat(permission.isGlobal()).isEqualTo(expectedGlobal);
    assertThat(permission.isAllowedInCustomRoles()).isEqualTo(expectedAllowedInCustomRoles);
    assertThat(permission.isVisible()).isEqualTo(expectedVisible);
  }
}
