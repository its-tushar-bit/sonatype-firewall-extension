/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  public RolePermission() {
  }

  public RolePermission(String roleId, Permission permission) {
    this.roleId = roleId;
    this.permission = permission;
  }

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
