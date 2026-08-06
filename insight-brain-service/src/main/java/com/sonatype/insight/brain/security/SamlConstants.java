/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

/**
 * Shared SAML constants.
 */
public final class SamlConstants
{
  /**
   * Request path that initiates SAML login (GET) and receives the IdP assertion (POST).
   */
  public static final String SAML_REQUEST_PATH = "/saml";

  private SamlConstants() {
    // constants only
  }
}
