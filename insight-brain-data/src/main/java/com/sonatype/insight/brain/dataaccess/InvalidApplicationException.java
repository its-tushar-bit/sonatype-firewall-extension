/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.error.HttpStatusCode;

@SuppressWarnings("serial")
@HttpStatusCode(400)
public class InvalidApplicationException
    extends RuntimeException
{
  public InvalidApplicationException(String message) {
    super(message);
  }
}
