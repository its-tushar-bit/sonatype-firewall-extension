/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import com.sonatype.insight.error.HttpStatusCode;

/**
 * @since 1.9
 */
@SuppressWarnings("serial")
@HttpStatusCode(400)
public class InvalidTagException
    extends RuntimeException
{
  public InvalidTagException(String message) {
    super(message);
  }
}
