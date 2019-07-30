/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import org.sonatype.licensing.LicensingException;

@SuppressWarnings("serial")
public class ExternalDatabaseNotSupportedException
    extends LicensingException
{
  public ExternalDatabaseNotSupportedException(String message) {
    super(message);
  }
}
