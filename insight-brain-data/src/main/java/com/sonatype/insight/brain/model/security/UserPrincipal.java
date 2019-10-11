/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.LinkedHashSet;
import java.util.Set;

public class UserPrincipal
{
  private final String username;

  private final String displayName;

  private final boolean isInternalUser;

  private final Set<String> membership;

  public UserPrincipal(String username, String displayName, boolean isInternalUser) {
    this(username, displayName, isInternalUser, null);
  }

  public UserPrincipal(String username, String displayName, boolean isInternalUser, Set<String> membership) {
    this.username = username;
    this.displayName = displayName;
    this.isInternalUser = isInternalUser;
    this.membership = new LinkedHashSet<>();
    if (membership != null) {
      this.membership.addAll(membership);
    }
    this.membership.add(Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  public String getUsername() {
    return this.username;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public boolean isInternalUser() {
    return isInternalUser;
  }

  public Set<String> getMembership() {
    return this.membership;
  }

  @Override
  public String toString() {
    return this.username;
  }

  /**
   * Membership not considered in hashcode to allow UserPrincipal to be created without having to look up Membership
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isInternalUser ? 1231 : 1237);
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
    return result;
  }

  /**
   * Membership not considered in comparison to allow UserPrincipal to be created without having to look up Membership
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    UserPrincipal other = (UserPrincipal) obj;
    if (isInternalUser != other.isInternalUser) {
      return false;
    }
    if (username == null) {
      if (other.username != null) {
        return false;
      }
    }
    else if (!username.equals(other.username)) {
      return false;
    }
    if (displayName == null) {
      if (other.displayName != null) {
        return false;
      }
    }
    else if (!displayName.equals(other.displayName)) {
      return false;
    }
    return true;
  }
}
