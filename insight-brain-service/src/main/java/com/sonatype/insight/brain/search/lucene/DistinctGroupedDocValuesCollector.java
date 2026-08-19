/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.BytesRef;

/**
 * Lucene collector that counts distinct {@code distinctField} values grouped by {@code groupField},
 * restricted to the supplied group keys, reading from docValues.
 * <p>
 * Supports both single-valued ({@link SortedDocValues}) and multi-valued ({@link SortedSetDocValues})
 * group fields. Uses {@link DocValues#getSortedSet} for uniform handling: single-valued fields are
 * wrapped as a singleton set.
 * <p>
 * Group keys are compared and returned lowercased so both backends key the result map identically:
 * OpenSearch bucket keys come back lowercased for vocabulary fields that carry the lowercase
 * normalizer, and its include terms are lowercased for the rest, so callers look up this map with a
 * lowercased key on either backend. Opaque id columns are mapped case-sensitively (see IndexMapping),
 * but their values are lowercase hex, so folding them is a no-op rather than a mismatch.
 * <p>
 * The distinct field is read via {@link SortedDocValues} (single-valued).
 */
public final class DistinctGroupedDocValuesCollector
    extends SimpleCollector
{
  private final String groupField;

  private final String distinctField;

  private final Map<String, Set<String>> distinctValuesByGroup;

  private SortedSetDocValues groupDocValues;

  private SortedDocValues distinctDocValues;

  /**
   * Per-segment cache of group ordinal to the target distinct-value set, or {@code null} for an ordinal
   * whose term is not one of the requested group keys. An ancestor closure repeats the same handful of
   * ordinals across every matching document, so resolving each ordinal once per segment avoids decoding
   * the same term thousands of times. Ordinals are segment-local, so this is reset per segment.
   */
  private Map<Long, Set<String>> groupSetByOrd;

  /** Per-segment cache of distinct ordinal to its decoded term, reset per segment with the group cache. */
  private Map<Integer, String> distinctValueByOrd;

  private long matchedDocuments;

  public DistinctGroupedDocValuesCollector(
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues)
  {
    this.groupField = groupField;
    this.distinctField = distinctField;
    this.distinctValuesByGroup = new LinkedHashMap<>();
    for (String groupValue : groupValues) {
      if (StringUtils.isNotBlank(groupValue)) {
        distinctValuesByGroup.put(groupValue.toLowerCase(Locale.ROOT), new LinkedHashSet<>());
      }
    }
  }

  @Override
  protected void doSetNextReader(final LeafReaderContext context) throws IOException {
    // DocValues.getSortedSet handles both SortedSetDocValues (multi-valued) and SortedDocValues
    // (single-valued, wrapped as singleton). A field present in a segment without doc values (e.g. an
    // index written before facet doc-values existed) is treated as empty rather than throwing.
    groupDocValues = sortedSetOrEmpty(context.reader(), groupField);
    distinctDocValues = sortedOrEmpty(context.reader(), distinctField);
    groupSetByOrd = new HashMap<>();
    distinctValueByOrd = new HashMap<>();
  }

  /**
   * The distinct-value set for a group ordinal, or {@code null} when that ordinal's term is not a
   * requested group key. Resolved from the term dictionary on first use and cached for the segment.
   */
  private Set<String> groupSetForOrd(final long ord) throws IOException {
    Set<String> cached = groupSetByOrd.get(ord);
    if (cached != null || groupSetByOrd.containsKey(ord)) {
      return cached;
    }
    Set<String> target = null;
    BytesRef groupBytesRef = groupDocValues.lookupOrd(ord);
    if (groupBytesRef != null) {
      String groupValue = groupBytesRef.utf8ToString();
      if (StringUtils.isNotBlank(groupValue)) {
        target = distinctValuesByGroup.get(groupValue.toLowerCase(Locale.ROOT));
      }
    }
    groupSetByOrd.put(ord, target);
    return target;
  }

  /**
   * The decoded term for a distinct-field ordinal, or {@code null} when it is absent or blank. Cached for
   * the segment so a value shared by many documents is decoded once and stored as one string instance.
   */
  private String distinctValueForOrd(final int ord) throws IOException {
    String cached = distinctValueByOrd.get(ord);
    if (cached != null || distinctValueByOrd.containsKey(ord)) {
      return cached;
    }
    String value = null;
    BytesRef distinctBytesRef = distinctDocValues.lookupOrd(ord);
    if (distinctBytesRef != null) {
      String decoded = distinctBytesRef.utf8ToString();
      if (StringUtils.isNotBlank(decoded)) {
        value = decoded;
      }
    }
    distinctValueByOrd.put(ord, value);
    return value;
  }

  private static SortedSetDocValues sortedSetOrEmpty(final LeafReader reader, final String field) throws IOException {
    FieldInfo info = reader.getFieldInfos().fieldInfo(field);
    DocValuesType type = info == null ? DocValuesType.NONE : info.getDocValuesType();
    if (type == DocValuesType.SORTED || type == DocValuesType.SORTED_SET) {
      return DocValues.getSortedSet(reader, field);
    }
    return DocValues.emptySortedSet();
  }

  private static SortedDocValues sortedOrEmpty(final LeafReader reader, final String field) throws IOException {
    FieldInfo info = reader.getFieldInfos().fieldInfo(field);
    DocValuesType type = info == null ? DocValuesType.NONE : info.getDocValuesType();
    if (type == DocValuesType.SORTED) {
      return DocValues.getSorted(reader, field);
    }
    if (type == DocValuesType.SORTED_SET) {
      // The distinct column is read single-valued. A multi-valued column would count as zero on every
      // document, which is indistinguishable from "no matches", so say so rather than under-report.
      throw new IllegalArgumentException(
          "distinct field '" + field + "' is multi-valued (SORTED_SET); it must be single-valued (SORTED)");
    }
    return DocValues.emptySorted();
  }

  @Override
  public void collect(final int doc) throws IOException {
    // Advance to the document for both fields
    boolean hasGroupValues = groupDocValues.advanceExact(doc);
    boolean hasDistinctValue = distinctDocValues.advanceExact(doc);

    if (!hasGroupValues || !hasDistinctValue) {
      return;
    }

    // Get the distinct value (single-valued field)
    String distinctValue = distinctValueForOrd(distinctDocValues.ordValue());
    if (distinctValue == null) {
      return;
    }

    // Iterate over ALL group values (multi-valued field)
    // For single-valued fields, this iterates once over the single value
    long ord;
    boolean contributed = false;
    while ((ord = groupDocValues.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
      Set<String> distinctValues = groupSetForOrd(ord);
      if (distinctValues != null) {
        distinctValues.add(distinctValue);
        contributed = true;
      }
    }

    if (contributed) {
      matchedDocuments++;
    }
  }

  @Override
  public ScoreMode scoreMode() {
    return ScoreMode.COMPLETE_NO_SCORES;
  }

  public Map<String, Long> groupCounts() {
    Map<String, Long> counts = new LinkedHashMap<>();
    distinctValuesByGroup.forEach((groupValue, distinctValues) -> {
      if (!distinctValues.isEmpty()) {
        counts.put(groupValue, (long) distinctValues.size());
      }
    });
    return counts;
  }

  /**
   * Documents that contributed to at least one group count (non-blank fields and at least one
   * allowed group value). Does not equal total Lucene hits collected for the query.
   */
  public long matchedDocuments() {
    return matchedDocuments;
  }
}
