/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import com.sonatype.insight.error.HttpStatusCode;

@HttpStatusCode(500)
public class SbomExportException
    extends RuntimeException
{
  private static final long serialVersionUID = -5586186834991998435L;

  public SbomExportException(final String message) {
    super(message);
  }

  public SbomExportException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
