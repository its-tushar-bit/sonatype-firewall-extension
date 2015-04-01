/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import com.sonatype.insight.error.HttpStatusCode;

@SuppressWarnings("serial")
@HttpStatusCode(400)
public class InvalidUserException
    extends RuntimeException
{
  public InvalidUserException(String message) {
    super(message);
  }
}
