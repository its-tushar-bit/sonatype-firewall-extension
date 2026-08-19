/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.IOException;

/**
 * Exception thrown when support zip generation is already in progress.
 *
 * @since 1.197
 */
public class SupportZipInProgressException
    extends IOException
{
  public SupportZipInProgressException(String message) {
    super(message);
  }
}
