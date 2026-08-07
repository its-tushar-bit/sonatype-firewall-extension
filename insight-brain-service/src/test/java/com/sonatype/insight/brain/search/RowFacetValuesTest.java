/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the facet bucket-value discovery shared by the index-query and catalog legs.
 */
public class RowFacetValuesTest
{
  private static final Function<Map<String, Object>, Map<String, Object>> IDENTITY = row -> row;

  private static Map<String, Object> row(final String field, final Object value) {
    final Map<String, Object> fields = new LinkedHashMap<>();
    fields.put(field, value);
    return fields;
  }

  @Test
  public void distinctRowValues_dedupesAndPreservesRowOrder() {
    List<Map<String, Object>> rows = List.of(row("org", "Widget Co"), row("org", "Acme"), row("org", "Widget Co"));

    Set<String> values = RowFacetValues.distinctRowValues(rows, "org", IDENTITY, 20);

    // Insertion (row) order is preserved so bucket order is deterministic across requests.
    assertThat(values).containsExactly("Widget Co", "Acme");
  }

  @Test
  public void distinctRowValues_flattensIterableFieldIntoOneValuePerElement() {
    // A denormalized multi-valued field (e.g. policyType/state sets) contributes each element as its
    // own bucket value rather than the collection's toString.
    List<Map<String, Object>> rows =
        List.of(row("policyTypes", List.of("security", "license")), row("policyTypes", List.of("security", "quality")));

    Set<String> values = RowFacetValues.distinctRowValues(rows, "policyTypes", IDENTITY, 20);

    assertThat(values).containsExactly("security", "license", "quality");
  }

  @Test
  public void distinctRowValues_capsAtMaxValues() {
    List<Map<String, Object>> rows = new java.util.ArrayList<>();
    for (int i = 0; i < 50; i++) {
      rows.add(row("org", "Org " + i));
    }

    Set<String> values = RowFacetValues.distinctRowValues(rows, "org", IDENTITY, 20);

    // The per-field cap is a hard ceiling on count() fan-out, and it keeps the earliest values.
    assertThat(values).hasSize(20)
        .containsExactly(
            "Org 0", "Org 1", "Org 2", "Org 3", "Org 4", "Org 5", "Org 6", "Org 7", "Org 8", "Org 9",
            "Org 10", "Org 11", "Org 12", "Org 13", "Org 14", "Org 15", "Org 16", "Org 17", "Org 18", "Org 19");
  }

  @Test
  public void distinctRowValues_capsIterableElementsToo() {
    // The cap applies to flattened elements, not just to whole-field values.
    List<String> many = new java.util.ArrayList<>();
    for (int i = 0; i < 50; i++) {
      many.add("state-" + i);
    }

    Set<String> values = RowFacetValues.distinctRowValues(List.of(row("states", many)), "states", IDENTITY, 20);

    assertThat(values).hasSize(20).startsWith("state-0").endsWith("state-19");
  }

  @Test
  public void distinctRowValues_skipsNullFieldsAndNullElements() {
    List<Map<String, Object>> rows = List.of(
        row("org", null),
        row("org", "Acme"),
        row("org", java.util.Arrays.asList("Widget Co", null)));

    Set<String> values = RowFacetValues.distinctRowValues(rows, "org", IDENTITY, 20);

    // A row missing the field, and a null element inside a multi-valued field, seed no bucket rather
    // than a "null" string bucket.
    assertThat(values).containsExactly("Acme", "Widget Co");
  }

  @Test
  public void distinctRowValues_absentFieldYieldsNoValues() {
    Set<String> values = RowFacetValues.distinctRowValues(List.of(row("other", "x")), "org", IDENTITY, 20);

    assertThat(values).isEmpty();
  }

  @Test
  public void distinctRowValues_stringifiesNonStringValues() {
    List<Map<String, Object>> rows = List.of(row("threatLevel", 9), row("threatLevel", 9), row("threatLevel", 4));

    Set<String> values = RowFacetValues.distinctRowValues(rows, "threatLevel", IDENTITY, 20);

    assertThat(values).containsExactly("9", "4");
  }

  @Test
  public void distinctRowValues_readsThroughTheFieldsAccessor() {
    // Callers pass a row-type-specific accessor (e.g. IndexQueryRow::getFields); values are read from
    // the map it returns.
    record Row(Map<String, Object> fields)
    {
    }
    List<Row> rows = List.of(new Row(row("org", "Acme")), new Row(row("org", "Widget Co")));

    Set<String> values = RowFacetValues.distinctRowValues(rows, "org", Row::fields, 20);

    assertThat(values).containsExactly("Acme", "Widget Co");
  }
}
