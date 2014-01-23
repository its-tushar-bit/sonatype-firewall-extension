/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.7
 */
public class RoleDAO
    extends AbstractOperationalSqlDAO<Role>
{
  @Override
  protected Role getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM Role entity WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public Role getByIdNotNull(String id) {
    Role role = getById(id);
    if (role == null) {
      throw new NotFoundException("Cannot find a role with id " + id);
    }
    return role;
  }

  @Override
  public void delete(EntityManager em, Role entity) {
    // Cascade to permissions
    RolePermissionDAO rolePermissionDAO = new RolePermissionDAO();
    for (RolePermission rolePermission : rolePermissionDAO.getByRoleId(em, entity.getId())) {
      rolePermissionDAO.delete(em, rolePermission);
    }

    super.delete(em, entity);
  }

  /**
   * Gets the role with the given name.
   */
  public Role getByName(String name) {
    String sQuery = "SELECT entity FROM Role entity WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(sQuery, NameHelper.normalize(name));
  }

  /**
   * Gets all roles applicable to the entire system.
   */
  public List<Role> getGlobalRoles() {
    String sQuery = "SELECT entity FROM Role entity WHERE entity.global=TRUE ORDER BY entity.name";
    return getList(sQuery);
  }

  /**
   * Gets all roles applicable to an organization or application.
   */
  public List<Role> getApplicationRoles() {
    String sQuery = "SELECT entity FROM Role entity WHERE entity.global=FALSE ORDER BY entity.name";
    return getList(sQuery);
  }
}
