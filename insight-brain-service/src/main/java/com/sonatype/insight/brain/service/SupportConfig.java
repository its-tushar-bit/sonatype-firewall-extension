/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 1.27
 */
public class SupportConfig
{
  static final long DEFAULT_READ_LIMIT_30MB = 31457280;

  @JsonProperty
  private long readLimitBytes = DEFAULT_READ_LIMIT_30MB;

  public void setReadLimitBytes(final long readLimitBytes) {
    this.readLimitBytes = readLimitBytes;
  }

  public long getReadLimitBytes() {
    return readLimitBytes;
  }
}
