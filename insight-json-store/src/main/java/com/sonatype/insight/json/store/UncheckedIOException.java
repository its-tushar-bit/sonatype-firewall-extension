/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.IOException;

@SuppressWarnings("serial")
public class UncheckedIOException
    extends RuntimeException
{
  public UncheckedIOException(IOException cause) {
    super(cause);
  }
}
