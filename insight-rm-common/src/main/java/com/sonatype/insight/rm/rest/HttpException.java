/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import java.io.IOException;

public class HttpException
    extends IOException
{
  private static final long serialVersionUID = 3630192531900782143L;

  private final int status;

  private final String reason;

  public HttpException(int status, String reason, Throwable cause) {
    super("Error code " + status + ": " + reason, cause);
    this.status = status;
    this.reason = reason;
  }

  public int getStatus() {
    return status;
  }

  public String getReason() {
    return reason;
  }
}
