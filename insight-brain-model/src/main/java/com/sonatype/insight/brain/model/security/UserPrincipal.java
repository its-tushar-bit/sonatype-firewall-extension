/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

public class UserPrincipal
{
  public final String username;

  public final boolean clmUser;

  public UserPrincipal(String username, boolean clmUser) {
    this.username = username;
    this.clmUser = clmUser;
  }

  @Override
  public String toString() {
    return this.username;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (clmUser ? 1231 : 1237);
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    return result;
  }

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
