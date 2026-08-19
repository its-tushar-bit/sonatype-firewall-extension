/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.ldap;

/**
 * @since 1.7
 */
public enum LdapProtocol
{
  /**
   * @since 1.7
   */
  LDAP("ldap"),

  /**
   * @since 1.7
   */
  LDAPS("ldaps");

  private String protocol;

  private LdapProtocol(String name) {
    this.protocol = name;
  }

  public String getProtocol() {
    return protocol;
  }
}
