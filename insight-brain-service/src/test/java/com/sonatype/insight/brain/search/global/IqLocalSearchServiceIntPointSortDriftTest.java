/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.lucene.DocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.Test;

/**
 * Drift guard between {@code IqLocalSearchService.INT_POINT_SORT_FIELDS} and the point-field widths
 * {@link DocumentBuilder} actually writes.
 *
 * <p>
 * Lucene's numeric comparator reads the same-named points index to build a competitive iterator and
 * rejects a byte-width mismatch outright ("indexed with 4 bytes per dimension, but ... expected 8"). The
 * failure only surfaces once a segment holds a value for the field, so a sortable field whose point twin
 * changed from {@code LongPoint} to {@code IntPoint} (or a newly added {@code IntPoint} sortable field)
 * compiles, passes an empty-index test, and only 500s on real data. This test drives every numeric
 * {@link DocumentBuilder} setter, reads the emitted point field's concrete type straight off the built
 * {@link Document}, and asserts the {@link IntPoint}-backed set matches the sort set exactly, so drift
 * fails the build instead of production.
 *
 * <p>
 * The point CLASS is what matters, not the byte width: {@code FloatPoint} is also 4 bytes but must sort
 * as {@code FLOAT} (handled by the float sort set), so width alone would misclassify it as an int field.
 */
public class IqLocalSearchServiceIntPointSortDriftTest
{
  @Test
  public void everyIntPointBackedSortableField_isDeclaredInIntPointSortFields() {
    Map<String, Boolean> intPointByLabel = intPointFlagsWrittenByDocumentBuilder();

    Set<FieldIdentifier> expectedIntPointSortFields = new LinkedHashSet<>();
    for (FieldIdentifier sortable : IqLocalSearchService.allSortableIndexFields()) {
      if (Boolean.TRUE.equals(intPointByLabel.get(sortable.label))) {
        expectedIntPointSortFields.add(sortable);
      }
    }

    // Both directions matter: a missing entry sorts an IntPoint field as LONG (the 500), and a stale
    // entry sorts a LongPoint field as INT (the same 500 in reverse).
    assertThat(IqLocalSearchService.INT_POINT_SORT_FIELDS)
        .as("INT_POINT_SORT_FIELDS must list exactly the sortable fields DocumentBuilder writes as IntPoint")
        .containsExactlyInAnyOrderElementsOf(expectedIntPointSortFields);
  }

  @Test
  public void documentBuilderWritesAtLeastOneIntPointField() {
    // Guards the guard: if the reflective sweep stopped invoking setters (renamed setters, changed
    // signatures), the assertion above would trivially pass against an empty expected set.
    Map<String, Boolean> flags = intPointFlagsWrittenByDocumentBuilder();
    assertThat(flags).as("reflective DocumentBuilder sweep must observe point fields").isNotEmpty();
    assertThat(flags.values()).as("sweep must observe at least one IntPoint field").contains(true);
    assertThat(flags.values()).as("sweep must observe at least one non-IntPoint point field").contains(false);
  }

  /**
   * Invokes every single-argument numeric {@link DocumentBuilder} setter on one builder, then records for
   * each point field on the built document whether it is an {@link IntPoint}, keyed by field label.
   * Reading the concrete point class off the emitted field (rather than the setter's declared parameter
   * type) is what makes this a real guard: an {@code int}-typed setter may still write a
   * {@code LongPoint}, and a 4-byte {@code FloatPoint} is not an int field.
   */
  private static Map<String, Boolean> intPointFlagsWrittenByDocumentBuilder() {
    // build() accumulates whatever was set regardless of the item type, so any type drives every setter.
    DocumentBuilder builder = new DocumentBuilder(ItemType.APPLICATION);
    for (Method method : DocumentBuilder.class.getDeclaredMethods()) {
      if (!method.getName().startsWith("set") || method.getParameterCount() != 1) {
        continue;
      }
      Object value = numericSampleFor(method.getParameterTypes()[0]);
      if (value == null) {
        continue;
      }
      try {
        method.setAccessible(true);
        method.invoke(builder, value);
      }
      catch (ReflectiveOperationException | RuntimeException e) {
        // A setter that rejects the sample (validation, unrelated preconditions) simply contributes no
        // point field; the companion test above ensures the sweep still observes some.
        continue;
      }
    }

    Map<String, Boolean> isIntPoint = new TreeMap<>();
    Document document = builder.build();
    for (IndexableField field : document.getFields()) {
      if (field.fieldType().pointDimensionCount() > 0) {
        // A label carrying several point twins would be a bug elsewhere; OR the flags so any IntPoint twin
        // marks the label rather than letting iteration order decide.
        isIntPoint.merge(field.name(), field instanceof IntPoint, Boolean::logicalOr);
      }
    }
    return isIntPoint;
  }

  /** Sample value for a numeric setter parameter, or {@code null} for a type this sweep does not drive. */
  private static Object numericSampleFor(final Class<?> parameterType) {
    if (parameterType == int.class || parameterType == Integer.class) {
      return 7;
    }
    if (parameterType == long.class || parameterType == Long.class) {
      return 7L;
    }
    if (parameterType == float.class || parameterType == Float.class) {
      return 7.5f;
    }
    if (parameterType == double.class || parameterType == Double.class) {
      return 7.5d;
    }
    return null;
  }
}
