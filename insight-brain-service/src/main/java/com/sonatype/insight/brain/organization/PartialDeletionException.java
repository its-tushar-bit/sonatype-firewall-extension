/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

public class PartialDeletionException
    extends RuntimeException
{
  private static final long serialVersionUID = -3603846830993828453L;

  private final Throwable rootCause;

  public PartialDeletionException(Throwable cause) {
    super();
    this.rootCause = findRootCause(cause);
  }

  @Override
  public String getMessage() {
    return "The delete operation was partially successful." +
        " Some sub-Orgs and applications of this Org were deleted," +
        " while some failed with error(s) below." + "\n" + rootCause.getMessage();
  }

  private Throwable findRootCause(Throwable exception) {
    Throwable cause = exception;
    while (cause.getCause() != null && cause.getCause() != cause) {
      cause = cause.getCause();
    }
    return cause;
  }
}
