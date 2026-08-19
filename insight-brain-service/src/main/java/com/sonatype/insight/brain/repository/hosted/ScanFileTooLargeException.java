/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.IOException;

/**
 * Thrown when a scan file exceeds the maximum allowed upload size.
 */
public class ScanFileTooLargeException
    extends IOException
{
  public ScanFileTooLargeException(final String message) {
    super(message);
  }
}
