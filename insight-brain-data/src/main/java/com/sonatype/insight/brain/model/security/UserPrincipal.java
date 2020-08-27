/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserPrincipal
{
  private final String username;

  private final String displayName;

  private final Set<String> membership;

  /**
   * The id of the realm that authenticated the user.
   * If the user was authenticated by an LDAP server, then this is the id of that LDAP server.
   */
  private final String realmId;

  public UserPrincipal(String username, String displayName, String realmId) {
    this(username, displayName, realmId, null);
  }

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public UserPrincipal(
      @JsonProperty("username") String username,
      @JsonProperty("displayName") String displayName,
      @JsonProperty("realmId") String realmId,
      @JsonProperty("membership") Set<String> membership)
  {
    this.username = username;
    this.displayName = displayName;
    this.membership = new LinkedHashSet<>();
    if (membership != null) {
      this.membership.addAll(membership);
    }
    this.membership.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    this.realmId = realmId;
  }

  public String getUsername() {
    return this.username;
  }

  public String getDisplayName() {
    return this.displayName;
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
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
    result = prime * result + realmId.hashCode();
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
    return realmId.equals(other.realmId);
  }

  public String getRealmId() {
    return realmId;
  }
}
