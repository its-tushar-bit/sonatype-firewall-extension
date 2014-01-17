/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.util.Set;

/**
 * User details populated from LDAP.
 * 
 * @since 1.7
 */
public class LdapUser
    implements Comparable<LdapUser>
{
  private String username;

  private String password;

  private String dn;

  private String realName;

  private String email;

  private Set<String> membership;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getDn() {
    return dn;
  }

  public void setDn(String dn) {
    this.dn = dn;
  }

  public String getRealName() {
    return realName;
  }

  public void setRealName(String realName) {
    this.realName = realName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Set<String> getMembership() {
    return membership;
  }

  public void setMembership(Set<String> membership) {
    this.membership = membership;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();

    buf.append("User:");
    buf.append("\n\tUsername: ").append(username);
    buf.append("\n\tDN: ").append(dn);
    buf.append("\n\tReal Name: ").append(realName);
    buf.append("\n\tEmail: ").append(email);

    if (getMembership() != null && !getMembership().isEmpty()) {
      buf.append("\n\tMembership: ").append(getMembership());
    }

    return buf.toString();
  }

  @Override
  public int compareTo(LdapUser lhs) {
    return lhs != null ? getUsername().compareTo(lhs.getUsername()) : 1;
  }
}
