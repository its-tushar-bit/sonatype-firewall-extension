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
  CONFIGURE_SYSTEM("Edit", PermissionCategory.ADMINISTRATOR, "System Configuration and Users", false /* allowedInCustomRoles */),

  EDIT_ROLES("Edit", PermissionCategory.ADMINISTRATOR, "Custom Roles", false /* allowedInCustomRoles */),

  VIEW_ROLES("View", PermissionCategory.ADMINISTRATOR, "All Roles"),
  
  MANAGE_PROPRIETARY("Edit", PermissionCategory.ADMINISTRATOR, "Proprietary Components"),

  CLAIM_COMPONENT("Claim", PermissionCategory.CLM, "Components"),

  WRITE("Edit", PermissionCategory.CLM, "CLM elements"),

  READ("View", PermissionCategory.CLM, "CLM elements"),

  EVALUATE_APPLICATION("Evaluate", PermissionCategory.CLM, "Applications"),

  EVALUATE_COMPONENT("Evaluate", PermissionCategory.CLM, "Individual components");

  private final String displayName;

  private final PermissionCategory category;

  private final String description;

  private final boolean allowedInCustomRoles;

  private Permission(final String displayName, final PermissionCategory category, final String description) {
    this(displayName, category, description, true /* allowedInCustomRoles */);
  }

  private Permission(final String displayName, final PermissionCategory category, final String description,
      final boolean allowedInCustomRoles)
  {
    this.displayName = displayName;
    this.category = category;
    this.description = description;
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
}
