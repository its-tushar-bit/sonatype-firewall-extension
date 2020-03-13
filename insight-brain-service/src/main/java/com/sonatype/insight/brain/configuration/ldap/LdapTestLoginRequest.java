/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

public class LdapTestLoginRequest
{
  private LdapUserMapping ldapUserMapping;

  private String username;

  private String password;

  public LdapUserMapping getUserMapping() {
    return ldapUserMapping;
  }

  public void setUserMapping(LdapUserMapping ldapUserMapping) {
    this.ldapUserMapping = ldapUserMapping;
  }

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
}
