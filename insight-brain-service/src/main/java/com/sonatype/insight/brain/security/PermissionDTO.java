/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.model.security.Permission;

/**
 * @since 1.15.0
 */
public class PermissionDTO
{
  public Permission id;

  public String displayName;

  public String description;

  public boolean allowed;

  public PermissionDTO() {
  }

  public PermissionDTO(final Permission permission, final boolean allowed) {
    this.id = permission;
    this.allowed = allowed;
    displayName = permission.getDisplayName();
    description = permission.getDescription();
  }
}
