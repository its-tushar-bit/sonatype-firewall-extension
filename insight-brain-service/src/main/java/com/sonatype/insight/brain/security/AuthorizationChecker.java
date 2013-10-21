/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Set;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;

/**
 * Evaluates authorization.
 * 
 * @since 1.7
 */
class AuthorizationChecker
{
  private final MembershipMappingDAO membershipDAO;

  private final RolePermissionDAO rolePermissionDAO;

  public AuthorizationChecker() {
    this.rolePermissionDAO = new RolePermissionDAO();
    this.membershipDAO = new MembershipMappingDAO();
  }

  /**
   * Determines whether the given user has the specified permission in any of the supplied contexts.
   */
  public boolean isPermitted(String username, Permission permission, Iterable<String> contextIds) {
    Set<String> roles = rolePermissionDAO.getRoleIdsByPermission(permission);
    for (String contextId : contextIds) {
      for (MembershipMapping membership : membershipDAO.getByContextId(contextId)) {
        if (isUserIncluded(membership, username) && roles.contains(membership.getRoleId())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isUserIncluded(MembershipMapping membership, String username) {
    if (MemberType.USER.equals(membership.getMemberType())) {
      return membership.getMemberName().equalsIgnoreCase(username);
    }
    return false;
  }
}
