/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * An association between a role and permission. Each such association denotes a permission that is assigned to a role.
 * 
 * @since 1.7
 */
@Entity
@Table(name = "role_permission")
public class RolePermission
    implements HasStringId
{
  @Id
  @Column(name = "role_permission_id")
  private String id;

  @Column(name = "role_id")
  private String roleId;

  @Column(name = "permission")
  @Enumerated(EnumType.STRING)
  private Permission permission;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public Permission getPermission() {
    return permission;
  }

  public void setPermission(Permission permission) {
    this.permission = permission;
  }
}
