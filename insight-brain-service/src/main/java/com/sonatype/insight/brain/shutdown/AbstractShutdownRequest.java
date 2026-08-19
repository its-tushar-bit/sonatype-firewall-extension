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

  private final String origin;

  protected AbstractShutdownRequest(final T item, final int order, final String origin) {
    this.item = item;
    this.order = order;
    this.origin = origin;
  }

  @Override
  public T getItem() {
    return item;
  }

  @Override
  public int getOrder() {
    return order;
  }

  @Override
  public String getOrigin() {
    return origin;
  }
}
