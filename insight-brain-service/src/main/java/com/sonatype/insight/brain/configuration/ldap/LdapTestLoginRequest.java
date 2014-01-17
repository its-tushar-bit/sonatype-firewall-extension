/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

public class LdapTestLoginRequest
{
  private LdapUserMapping userMapping;

  private String username;

  private String password;

  public LdapUserMapping getUserMapping() {
    return userMapping;
  }

  public void setUserMapping(LdapUserMapping userMapping) {
    this.userMapping = userMapping;
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
