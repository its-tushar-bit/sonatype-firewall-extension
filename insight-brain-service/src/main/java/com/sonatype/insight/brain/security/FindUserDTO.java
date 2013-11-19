/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.model.security.MemberType;

public class FindUserDTO
{
  private MemberType type;
  private String username;
  private String displayName;
  private String email;
  private String realm;

  public MemberType getType() {
    return type;
  }

  public void setType(final MemberType type) {
    this.type = type;
  }

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

  public FindUserDTO() {
  }

  public FindUserDTO(final MemberType type, final String username, final String displayName, final String email, final String realm) {
    this.type = type;
    this.username = username;
    this.displayName = displayName != null ? displayName.trim() : null;
    this.email = email != null ? email.trim() : null;
    this.realm = realm;
  }
}
