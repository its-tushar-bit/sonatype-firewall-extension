/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Discovery of facet bucket values from a page of result rows, shared by the index-query and catalog
 * legs so both count the same way.
 */
public final class RowFacetValues
{
  private RowFacetValues() {
    // static utility
  }

  /**
   * Distinct string values of a (possibly multi-valued) page row-field, in row order, capped at
   * {@code maxValues}. A multi-valued field (e.g. a denormalized policyType/state set) contributes each
   * element as its own value rather than the collection's {@code toString}. Null rows values and null
   * elements are skipped.
   *
   * @param rows page rows to read values from
   * @param rowField name of the row field to read
   * @param fieldsAccessor extracts the field map from a row
   * @param maxValues hard ceiling on the number of distinct values returned
   */
  public static <R> Set<String> distinctRowValues(
      final List<R> rows,
      final String rowField,
      final Function<R, Map<String, Object>> fieldsAccessor,
      final int maxValues)
  {
    final Set<String> values = new LinkedHashSet<>();
    for (R row : rows) {
      final Object value = fieldsAccessor.apply(row).get(rowField);
      if (value == null) {
        continue;
      }
      if (value instanceof Iterable<?> many) {
        for (Object element : many) {
          if (element != null && values.size() < maxValues) {
            values.add(String.valueOf(element));
          }
        }
      }
      else if (values.size() < maxValues) {
        values.add(String.valueOf(value));
      }
    }
    return values;
  }
}
