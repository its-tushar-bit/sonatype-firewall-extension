/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.List;

public class MembersByRole
{
  public String roleId;

  public String roleName;

  public String roleDescription;

  public List<MembersByOwner> membersByOwner = new ArrayList<>();

  public MembersByRole() {
  }

  public MembersByRole(String roleId, String roleName, String roleDescription) {
    this.roleId = roleId;
    this.roleName = roleName;
    this.roleDescription = roleDescription;
  }

  @Override
  public String toString() {
    return "Role=" + roleName + "(id=" + roleId + "), membersByOwner=" + membersByOwner;
  }
}
