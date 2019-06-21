/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.purl;

import com.sonatype.insight.error.HttpStatusCode;

/**
 * @since 67
 */
@SuppressWarnings("serial")
@HttpStatusCode(400)
public class InvalidPackageURLException
    extends RuntimeException
{
  public InvalidPackageURLException(final String message, final Throwable e) {
    super(message, e);
  }
}
