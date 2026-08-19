/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

public final class SpdxMediaType
{
  private SpdxMediaType() {
    throw new UnsupportedOperationException(getClass().getSimpleName() + " should not be instantiated.");
  }

  public static final String APPLICATION_SPDX_JSON = "application/spdx+json";

  public static final String APPLICATION_SPDX_XML = "application/spdx+xml";
}
