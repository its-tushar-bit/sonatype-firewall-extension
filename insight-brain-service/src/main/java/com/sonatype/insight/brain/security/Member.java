/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.model.security.MemberType;

/**
 * DTO representation of a CLM or LDAP Member
 *
 * @since 1.7
 */
public class Member
{
  private MemberType type;

  private String internalName;

  private String displayName;

  private String email;

  private String realm;

  public MemberType getType() {
    return type;
  }

  public void setType(final MemberType type) {
    this.type = type;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(final String internalName) {
    this.internalName = internalName;
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

  public Member() {
  }

  public Member(MemberType type, String internalName, String displayName) {
    this.type = type;
    this.internalName = internalName;
    this.displayName = displayName;
  }

  public Member(MemberType type, String internalName, String displayName, String email, String realm) {
    this.type = type;
    this.internalName = internalName;
    this.displayName = displayName;
    this.email = email;
    this.realm = realm;
  }
}
