/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

public abstract class AbstractShutdownRequest<T>
    implements ShutdownRequest<T>
{
  private final T item;

  private final int order;

  protected AbstractShutdownRequest(final T item, final int order) {
    this.item = item;
    this.order = order;
  }

  @Override
  public T getItem() {
    return item;
  }

  @Override
  public int getOrder() {
    return order;
  }
}
