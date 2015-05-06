/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.15.0
 */
public class RolePermissionDTO
{
  public List<PermissionCategoryDTO> permissionCategories = new ArrayList<>();

  public String roleId;

  public RolePermissionDTO() {
  }

  public RolePermissionDTO(final String roleId) {
    this.roleId = roleId;
  }
}
