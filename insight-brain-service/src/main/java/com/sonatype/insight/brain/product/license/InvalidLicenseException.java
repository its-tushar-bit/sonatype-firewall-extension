/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.error.HttpStatusCode;

/**
 * Create an HTTP 402 (Payment Required) exception
 */
@HttpStatusCode(402)
public class InvalidLicenseException
    extends RuntimeException
{
  private static final long serialVersionUID = 1308434983601088106L;

  public static final String INVALID_LICENSE_MSG = "Your IQ Server license does not enable this feature.";

  public InvalidLicenseException() {
    this(INVALID_LICENSE_MSG);
  }

  public InvalidLicenseException(String msg) {
    super(msg);
  }
}
