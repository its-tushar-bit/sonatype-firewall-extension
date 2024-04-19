/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

public class TestShutdownHandler
    extends ShutdownHandler
{
  @Override
  public void exit(final int status) {
    // no-op
  }
}
