/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.dto;

import java.util.List;

/**
 * @since 1.11.0
 */
public class ApiRoleMemberMappingDTO
{
  private String roleId;

  private String roleName;

  private String roleDescription;

  private List<ApiMemberDTO> members;

  public ApiRoleMemberMappingDTO() {
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(final String roleId) {
    this.roleId = roleId;
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(final String roleName) {
    this.roleName = roleName;
  }

  public String getRoleDescription() {
    return roleDescription;
  }

  public void setRoleDescription(final String roleDescription) {
    this.roleDescription = roleDescription;
  }

  public List<ApiMemberDTO> getMembers() {
    return members;
  }

  public void setMembers(final List<ApiMemberDTO> members) {
    this.members = members;
  }
}
