/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.authorization;

import java.util.EnumSet;
import java.util.Set;

public enum AuthJWTClaims
{
  USER_EMAIL_CLAIM("https://www.sonatype.com/email"),
  SUBJECT_CLAIM("sub");

  private final String claim;

  AuthJWTClaims(String claim) {
    this.claim = claim;
  }

  public String getClaim() {
    return claim;
  }

  public static Set<AuthJWTClaims> getClaimsSet() {
    return EnumSet.allOf(AuthJWTClaims.class);
  }
}
