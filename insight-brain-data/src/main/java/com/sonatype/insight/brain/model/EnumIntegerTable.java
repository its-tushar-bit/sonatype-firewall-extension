/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * @since 1.52
 */
public class EnumIntegerTable<R extends Enum<R>, C extends Enum<C>>
    extends ForwardingTable<R, C, Integer>
{
  private Table<R, C, Integer> delegate = HashBasedTable.create();

  public EnumIntegerTable(Class<R> row, Class<C> column) {
    for (R r : row.getEnumConstants()) {
      for (C c : column.getEnumConstants()) {
        delegate.put(r, c, 0);
      }
    }
  }

  @Override
  protected Table<R, C, Integer> delegate() {
    return delegate;
  }
}
