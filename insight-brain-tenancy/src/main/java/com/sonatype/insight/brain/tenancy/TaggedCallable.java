/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import com.sonatype.insight.brain.common.metering.HasTags;

import java.util.concurrent.Callable;

import io.micrometer.core.instrument.Tags;

public class TaggedCallable<V>
    implements Callable<V>, HasTags
{
  private final Callable<V> callable;

  private final Tags tags;

  public TaggedCallable(final Callable<V> callable, final Tags tags) {
    this.callable = callable;
    this.tags = tags;
  }

  @Override
  public V call() throws Exception {
    return callable.call();
  }

  @Override
  public Tags getTags() {
    return tags;
  }
}
