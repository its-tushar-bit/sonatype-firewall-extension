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
  CONFIGURE_SYSTEM("Edit", Permission.CATEGORY_ADMINISTRATOR_PERMISSIONS, "System Configuration and Users", false /* allowedInCustomRoles */),

  EDIT_ROLES("Edit", Permission.CATEGORY_ADMINISTRATOR_PERMISSIONS, "Custom Roles", false /* allowedInCustomRoles */),

  VIEW_ROLES("View", Permission.CATEGORY_ADMINISTRATOR_PERMISSIONS, "All Roles"),
  
  MANAGE_PROPRIETARY("Edit", Permission.CATEGORY_ADMINISTRATOR_PERMISSIONS, "Proprietary Components"),

  CLAIM_COMPONENT("Claim", Permission.CATEGORY_CLM_PERMISSIONS, "Components"),

  WRITE("Edit", Permission.CATEGORY_CLM_PERMISSIONS, "CLM elements"),

  READ("View", Permission.CATEGORY_CLM_PERMISSIONS, "CLM elements"),

  EVALUATE_APPLICATION("Evaluate", Permission.CATEGORY_CLM_PERMISSIONS, "Applications"),

  EVALUATE_COMPONENT("Evaluate", Permission.CATEGORY_CLM_PERMISSIONS, "Individual components");

  private static final String CATEGORY_ADMINISTRATOR_PERMISSIONS = "Administrator";

  private static final String CATEGORY_CLM_PERMISSIONS = "CLM";

  private final String displayName;

  private final String category;

  private final String description;

  private final boolean allowedInCustomRoles;

  private Permission(final String displayName, final String category, final String description) {
    this(displayName, category, description, true /* allowedInCustomRoles */);
  }

  private Permission(final String displayName, final String category, final String description,
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

  public String getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public boolean isAllowedInCustomRoles() {
    return allowedInCustomRoles;
  }
}
