/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import com.sonatype.insight.brain.model.security.Role;

/**
 * @since 1.15.0
 */
public class RoleDTO
{
  public String id;

  public String name;

  public String description;

  public boolean builtIn;

  public List<PermissionCategoryDTO> permissionCategories;

  public RoleDTO() {
  }

  public RoleDTO(final Role role) {
    this.id = role.getId();
    this.name = role.getName();
    this.description = role.getDescription();
    this.builtIn = role.isBuiltIn();
  }
}
