/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

public class PageIterator<T>
    implements Iterator<T>
{
  private final int pageSize;

  private final BiFunction<Integer, Integer, List<T>> pageAndPageSizeToEntities;

  private int page;

  private int index;

  private List<T> entities;

  public PageIterator(
      final int page,
      final int pageSize,
      final BiFunction<Integer, Integer, List<T>> pageAndPageSizeToEntities)
  {
    if (page < 1) {
      throw new IllegalArgumentException("Page must be at least 1.");
    }
    if (pageSize < 1) {
      throw new IllegalArgumentException("Page size must be at least 1.");
    }
    this.page = page;
    this.pageSize = pageSize;
    this.pageAndPageSizeToEntities = pageAndPageSizeToEntities;
  }

  @Override
  public boolean hasNext() {
    if (entities == null || index == entities.size()) {
      index = 0;
      entities = pageAndPageSizeToEntities.apply(page++, pageSize);
    }
    return !entities.isEmpty();
  }

  @Override
  public T next() {
    return entities.get(index++);
  }
}
