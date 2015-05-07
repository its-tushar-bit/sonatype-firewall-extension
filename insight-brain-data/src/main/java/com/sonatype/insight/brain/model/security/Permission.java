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
  CONFIGURE_SYSTEM("Configure System", Permission.CATEGORY_SYSTEM_CONFIGURATION,
      "Configure LDAP, product license, users and other global aspects."),

  /**
   * Manages policies, role-to-user membership mappings, etc. for org/app.
   */
  WRITE("Write", Permission.CATEGORY_POLICY,
      "Add, delete and edit policies, organizations, applications, etc."),

  /**
   * View policy definition and consume policy evaluation results.
   */
  READ("View", Permission.CATEGORY_POLICY,
      "View policies, organizations, applications, etc."),

  /**
   * Evaluate policies on applications.
   */
  EVALUATE_APPLICATION("Evaluate Application", Permission.CATEGORY_POLICY, "Evaluate policies on applications."),

  /**
   * Evaluate policies on components.
   */
  EVALUATE_COMPONENT("Evaluate Component", Permission.CATEGORY_POLICY, "Evaluate policies on components.");

  private static final String CATEGORY_SYSTEM_CONFIGURATION = "System Configuration";

  private static final String CATEGORY_POLICY = "Policy";

  private final String displayName;

  private final String category;

  private final String description;

  private Permission(final String displayName, final String category, final String description) {
    this.displayName = displayName;
    this.category = category;
    this.description = description;
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
}
