/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.ldap;

/**
 * @see http://docs.oracle.com/javase/tutorial/jndi/ldap/auth_mechs.html
 * @since 1.7
 */
public enum LdapAuthenticationMethod
{
  /**
   * @since 1.7
   */
  NONE("none"),

  /**
   * @since 1.7
   */
  SIMPLE("simple"),

  /**
   * @since 1.7
   */
  DIGESTMD5("DIGEST-MD5"),

  /**
   * @since 1.7
   */
  CRAMMD5("CRAM-MD5");

  private String method;

  private LdapAuthenticationMethod(String name) {
    this.method = name;
  }

  public String getMethod() {
    return method;
  }
}
