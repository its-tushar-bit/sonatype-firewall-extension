/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;

/**
 * @since 1.15.0
 */
@Named
public class RoleService
{
  private final RoleDAO roleDAO;

  @Inject
  public RoleService(final RoleDAO roleDAO) {
    this.roleDAO = roleDAO;
  }

  @Authorize(permission = Permission.ADMIN)
  public List<Role> getAllRoles() {
    return roleDAO.getAll();
  }
}
