/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

public class UserQuery implements Comparable<Object>
{
  private String username;
  private String displayName;
  private String email;
  private String realm;

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public String getRealm() {
    return realm;
  }

  public void setRealm(final String realm) {
    this.realm = realm;
  }

  public UserQuery() {
  }

  public UserQuery(final String username, final String displayName, final String email, final String realm) {
    this.username = username;
    this.displayName = displayName;
    this.email = email;
    this.realm = realm;
  }

  @Override
  public int compareTo(final Object obj) {
    UserQuery other = (UserQuery) obj;
    return this.username.compareToIgnoreCase(other.username);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return compareTo(obj) == 0;
  }

  @Override
  public int hashCode() {
    return this.username.hashCode();
  }
}
