/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

/**
 * The permissions supporting authorization.
 *
 * @since 1.7
 */
public enum Permission
{
  // The order of permissions here determines the order in the UI
  CONFIGURE_SYSTEM("Edit", PermissionCategory.ADMINISTRATOR, "System Configuration and Users", true /* global */,
      false /* allowedInCustomRoles */),

  EDIT_ROLES("Edit", PermissionCategory.ADMINISTRATOR, "Custom Roles", true /* global */,
      false /* allowedInCustomRoles */),

  VIEW_ROLES("View", PermissionCategory.ADMINISTRATOR, "All Roles", true /* global */, true /* allowedInCustomRoles */),

  WAIVE_POLICY_VIOLATIONS("Waive", PermissionCategory.REMEDIATION, "Policy Violations", false /* global */,
      true /* allowedInCustomRoles */),

  CHANGE_LICENSES("Change", PermissionCategory.REMEDIATION, "Licenses", false /* global */,
      true /* allowedInCustomRoles */),

  CHANGE_SECURITY_VULNERABILITIES("Change", PermissionCategory.REMEDIATION, "Security Vulnerabilities",
      false /* global */, true /* allowedInCustomRoles */),

  MANAGE_PROPRIETARY("Edit", PermissionCategory.IQ, "Proprietary Components", false /* global */,
      true /* allowedInCustomRoles */),

  CLAIM_COMPONENT("Claim", PermissionCategory.IQ, "Components", true /* global */, true /* allowedInCustomRoles */),

  WRITE("Edit", PermissionCategory.IQ, "IQ Elements", false /* global */, true /* allowedInCustomRoles */),

  READ("View", PermissionCategory.IQ, "IQ Elements", false /* global */, true /* allowedInCustomRoles */),

  EDIT_ACCESS_CONTROL("Edit", PermissionCategory.IQ, "Access Control", false /* global */,
      true /* allowedInCustomRoles */),

  EVALUATE_APPLICATION("Evaluate", PermissionCategory.IQ, "Applications", false /* global */,
      true /* allowedInCustomRoles */),

  EVALUATE_COMPONENT("Evaluate", PermissionCategory.IQ, "Individual Components", false /* global */,
      true /* allowedInCustomRoles */),

  ADD_APPLICATION("Add", PermissionCategory.IQ, "Applications", false /* global */, true /* allowedInCustomRoles */),
  
  MANAGE_AUTOMATIC_APPLICATION_CREATION("Manage", PermissionCategory.IQ, "Automatic Application Creation",
      true /* global */, true /* allowedInCustomRoles */),

  MANAGE_AUTOMATIC_SCM_CONFIGURATION("Manage", PermissionCategory.IQ, "Automatic Source Control Configuration",
      true /* global */, true /* allowedInCustomRoles */);

  private final String displayName;

  private final PermissionCategory category;

  private final String description;

  private final boolean global;

  private final boolean allowedInCustomRoles;

  private Permission(final String displayName,
                     final PermissionCategory category,
                     final String description,
                     final boolean global,
                     final boolean allowedInCustomRoles)
  {
    this.displayName = displayName;
    this.category = category;
    this.description = description;
    this.global = global;
    this.allowedInCustomRoles = allowedInCustomRoles;
  }

  public String getDisplayName() {
    return displayName;
  }

  public PermissionCategory getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public boolean isAllowedInCustomRoles() {
    return allowedInCustomRoles;
  }

  public boolean isGlobal() {
    return global;
  }
}
