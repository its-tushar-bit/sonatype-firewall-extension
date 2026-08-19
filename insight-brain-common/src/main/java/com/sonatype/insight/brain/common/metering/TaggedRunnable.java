/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.metering;

import io.micrometer.core.instrument.Tags;

public class TaggedRunnable
    implements Runnable, HasTags
{
  private final Runnable runnable;

  private final Tags tags;

  public TaggedRunnable(final Runnable runnable, final Tags tags) {
    this.runnable = runnable;
    this.tags = tags;
  }

  @Override
  public void run() {
    runnable.run();
  }

  @Override
  public Tags getTags() {
    return tags;
  }
}
