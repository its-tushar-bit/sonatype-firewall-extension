/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Locale;

import com.sonatype.insight.brain.model.security.MemberType;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

  private String dn;

  @JsonIgnore
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  private String userId;

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

  @JsonIgnore
  public String getInternalNameLowerCase() {
    return (internalName != null) ? internalName.toLowerCase(Locale.ENGLISH) : null;
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

  @JsonIgnore
  public String getDn() {
    return dn;
  }

  public void setDn(String dn) {
    this.dn = dn;
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

  public Member(MemberType type, String internalName, String displayName, String email, String realm, String userId) {
    this.type = type;
    this.internalName = internalName;
    this.displayName = displayName;
    this.email = email;
    this.realm = realm;
    this.userId = userId;
  }

  @Override
  public String toString() {
    return "Member [type=" + type + ", internalName=" + internalName + ", displayName=" + displayName + ", email="
        + email + ", realm=" + realm + ", userId=" + userId + "]";
  }
}
