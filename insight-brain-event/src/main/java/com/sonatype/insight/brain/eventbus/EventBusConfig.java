/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.eventbus;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AsyncEventBus configuration.
 *
 * @since 1.25.0
 */
public class EventBusConfig
{
  /**
   * The size of the thread pool used by the {@link AsyncEventBus}
   */
  @NotNull
  @JsonProperty
  private int maxPoolSize = AsyncEventBus.DEFAULT_MAX_POOL_SIZE;

  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  public void setMaxPoolSize(final int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
  }
}
