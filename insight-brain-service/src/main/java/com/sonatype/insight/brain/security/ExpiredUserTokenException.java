/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.authc.AuthenticationException;

public class ExpiredUserTokenException
    extends AuthenticationException
{
  public ExpiredUserTokenException() {
    super("User token has expired. Please generate a new token.");
  }

  public ExpiredUserTokenException(String message) {
    super(message);
  }
}
