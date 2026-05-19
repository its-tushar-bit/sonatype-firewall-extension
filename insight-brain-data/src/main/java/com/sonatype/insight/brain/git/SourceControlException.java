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

  private final PullRequestFailureCategory category;

  public SourceControlException(final String message) {
    this(message, false, null, null);
  }

  public SourceControlException(final String message, final Throwable cause) {
    this(message, false, cause, null);
  }

  public SourceControlException(final String message, final boolean partialFailure) {
    this(message, partialFailure, null, null);
  }

  public SourceControlException(final String message, final boolean partialFailure, final Throwable cause) {
    this(message, partialFailure, cause, null);
  }

  public SourceControlException(final String message, final PullRequestFailureCategory category) {
    this(message, false, null, category);
  }

  public SourceControlException(
      final String message,
      final PullRequestFailureCategory category,
      final Throwable cause)
  {
    this(message, false, cause, category);
  }

  public SourceControlException(
      final String message,
      final boolean partialFailure,
      final Throwable cause,
      final PullRequestFailureCategory category)
  {
    super(message, cause);
    this.partialFailure = partialFailure;
    this.category = category;
  }

  public boolean isPartialFailure() {
    return partialFailure;
  }

  public PullRequestFailureCategory getCategory() {
    return category;
  }
}
