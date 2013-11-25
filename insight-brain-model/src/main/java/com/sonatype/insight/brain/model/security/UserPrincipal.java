/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Collections;
import java.util.Set;

public class UserPrincipal
{
  public final String username;

  public final boolean clmUser;

  public final Set<String> membership;

  public UserPrincipal(String username, boolean clmUser) {
    this(username, clmUser, null);
  }

  public UserPrincipal(String username, boolean clmUser, Set<String> membership) {
    this.username = username;
    this.clmUser = clmUser;
    this.membership = membership != null ? membership : Collections.<String>emptySet();
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
    result = prime * result + (clmUser ? 1231 : 1237);
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    return result;
  }

  /**
   * Membership not considered in comparison to allow UserPrincipal to be created without having to look up Membership
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    UserPrincipal other = (UserPrincipal) obj;
    if (clmUser != other.clmUser)
      return false;
    if (username == null) {
      if (other.username != null)
        return false;
    }
    else if (!username.equals(other.username))
      return false;
    return true;
  }
}
