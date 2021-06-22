/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

public class SourceControlException
    extends RuntimeException
{
  private final boolean partialFailure;

  public SourceControlException(final String message) {
    this(message, false, null);
  }

  public SourceControlException(final String message, final Throwable cause) {
    this(message, false, cause);
  }

  public SourceControlException(final String message, final boolean partialFailure) {
    this(message, partialFailure, null);
  }

  public SourceControlException(final String message, final boolean partialFailure, final Throwable cause) {
    super(message, cause);
    this.partialFailure = partialFailure;
  }

  public boolean isPartialFailure() {
    return partialFailure;
  }
}
