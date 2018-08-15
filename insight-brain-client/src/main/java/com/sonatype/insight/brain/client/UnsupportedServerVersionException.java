/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

public class UnsupportedServerVersionException
    extends RuntimeException
{
  private static final long serialVersionUID = 7504827296232263803L;

  public UnsupportedServerVersionException(String serverVersion, String minimalServerVersionRequired) {
    super(String.format("The IQ Server version %s is not compatible. Supported IQ server versions are %s or newer.",
        serverVersion, minimalServerVersionRequired));
  }
}
