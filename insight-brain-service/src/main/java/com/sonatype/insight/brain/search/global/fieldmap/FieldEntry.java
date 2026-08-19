/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.fieldmap;

import java.util.Set;

import com.sonatype.insight.brain.search.index.ItemType;

import static com.sonatype.insight.brain.search.global.fieldmap.FieldKind.KEYWORD;
import static com.sonatype.insight.brain.search.global.fieldmap.FieldKind.NUMERIC;
import static com.sonatype.insight.brain.search.global.fieldmap.FieldKind.TEXT;

/**
 * How one query-language field name maps onto the Lucene index.
 *
 * @param allowedTypes a lookup for an {@link ItemType} outside this set yields {@code MatchNoDocsQuery}.
 * @param enumValues when non-null, values outside the set emit a warning but the query is still built.
 * @param numericType for {@link FieldKind#NUMERIC} only; must be {@code Integer.class},
 *          {@code Long.class}, or {@code Float.class}.
 */
public record FieldEntry(
    String label,
    FieldKind kind,
    Set<ItemType> allowedTypes,
    Set<String> enumValues,
    Class<? extends Number> numericType)
{
  public FieldEntry {
    allowedTypes = Set.copyOf(allowedTypes);
    if (enumValues != null) {
      enumValues = Set.copyOf(enumValues);
    }
    if (kind == NUMERIC) {
      if (numericType != Integer.class && numericType != Long.class && numericType != Float.class) {
        throw new IllegalArgumentException(
            "NUMERIC field " + label + " must declare numericType=Integer.class, Long.class, or Float.class");
      }
    }
  }

  public static FieldEntry keyword(String label, Set<ItemType> types) {
    return new FieldEntry(label, KEYWORD, types, null, null);
  }

  public static FieldEntry keyword(String label, Set<ItemType> types, Set<String> enumValues) {
    return new FieldEntry(label, KEYWORD, types, enumValues, null);
  }

  public static FieldEntry text(String label, Set<ItemType> types) {
    return new FieldEntry(label, TEXT, types, null, null);
  }

  public static FieldEntry numericInt(String label, Set<ItemType> types) {
    return new FieldEntry(label, NUMERIC, types, null, Integer.class);
  }

  public static FieldEntry numericLong(String label, Set<ItemType> types) {
    return new FieldEntry(label, NUMERIC, types, null, Long.class);
  }

  public static FieldEntry numericFloat(String label, Set<ItemType> types) {
    return new FieldEntry(label, NUMERIC, types, null, Float.class);
  }
}
