/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

/**
 * Counts documents per half-open {@code [minInclusive, maxExclusive)} band of a float metric column in a
 * SINGLE pass, optionally counting distinct values of another column per band.
 * <p>
 * Bands are resolved from the metric's doc-values column per document rather than by running one filtered
 * search per band, so a four-band CVSS tile costs one collect instead of four (plus one for the total).
 * <p>
 * The metric column is written with {@link NumericUtils#floatToSortableInt(float)} (see
 * {@code LuceneIndexingContext}), so values are decoded with {@link NumericUtils#sortableIntToFloat(int)}
 * — the same contract {@link MaxMetricByGroupCollector} uses.
 * <p>
 * A document whose metric value falls in no band is counted only in the total, and a band whose bounds are
 * empty ({@code min == max}) can never match. Distinct values are compared as decoded strings, matching
 * {@link DistinctGroupedDocValuesCollector}; the decoded term is cached per segment ordinal so a value
 * shared by many documents is decoded once.
 */
final class BandedDistinctDocValuesCollector
    extends SimpleCollector
{
  private final String metricField;

  private final String distinctField;

  private final List<String> bandLabels;

  private final float[] bandMinInclusive;

  private final float[] bandMaxExclusive;

  /** Raw document counts per band; used when {@code distinctField} is null. */
  private final long[] documentCountByBand;

  /** Distinct decoded values per band; used when {@code distinctField} is non-null. */
  private final List<Set<String>> distinctValuesByBand;

  /**
   * Distinct decoded values across every matching document, including documents whose metric value
   * falls in no band (unscored). Used when {@code distinctField} is non-null.
   * <p>
   * Peak heap is this set plus the per-band sets (about 2× the distinct cardinality). Band sets
   * cannot replace this set: the same value can land in more than one band, and unscored values
   * land in none. Accepted ceiling for dashboard CVE counts (CLM-40928).
   */
  private final Set<String> overallDistinct;

  private SortedNumericDocValues metricValues;

  private SortedDocValues distinctValues;

  private Map<Integer, String> distinctValueByOrd;

  private long matchedDocuments;

  BandedDistinctDocValuesCollector(
      final String metricField,
      final String distinctField,
      final Map<String, float[]> ranges)
  {
    this.metricField = metricField;
    this.distinctField = distinctField;
    this.bandLabels = new ArrayList<>(ranges.size());
    this.bandMinInclusive = new float[ranges.size()];
    this.bandMaxExclusive = new float[ranges.size()];
    this.distinctValuesByBand = new ArrayList<>(ranges.size());
    this.overallDistinct = new LinkedHashSet<>();
    int index = 0;
    for (Map.Entry<String, float[]> entry : ranges.entrySet()) {
      bandLabels.add(entry.getKey());
      bandMinInclusive[index] = entry.getValue()[0];
      bandMaxExclusive[index] = entry.getValue()[1];
      distinctValuesByBand.add(new LinkedHashSet<>());
      index++;
    }
    this.documentCountByBand = new long[ranges.size()];
  }

  /**
   * True when this collector can read both columns it needs from doc values. A miss means a required
   * column is absent or the wrong {@code DocValuesType}.
   * {@link LuceneSearchIndexClient#countDistinctAndFloatBands} treats that as a zero aggregation;
   * {@link LuceneSearchIndexClient#aggregateCountByFloatField} keeps its per-band filtered-search
   * fallback.
   */
  static boolean canCollect(
      final LeafReader reader,
      final String metricField,
      final String distinctField) throws IOException
  {
    if (!hasDocValues(reader, metricField, DocValuesType.SORTED_NUMERIC)) {
      return false;
    }
    return distinctField == null || hasDocValues(reader, distinctField, DocValuesType.SORTED);
  }

  private static boolean hasDocValues(final LeafReader reader, final String field, final DocValuesType expected) {
    FieldInfo info = reader.getFieldInfos().fieldInfo(field);
    if (info == null) {
      // A segment that does not carry the field at all cannot contradict the expected type.
      return true;
    }
    return info.getDocValuesType() == expected;
  }

  @Override
  protected void doSetNextReader(final LeafReaderContext context) throws IOException {
    metricValues = numericOrEmpty(context.reader(), metricField);
    distinctValues = distinctField == null ? null : sortedOrEmpty(context.reader(), distinctField);
    // Ordinals are segment-local, so the decoded-term cache is reset per segment.
    distinctValueByOrd = new HashMap<>();
  }

  private static SortedNumericDocValues numericOrEmpty(final LeafReader reader, final String field) throws IOException {
    FieldInfo info = reader.getFieldInfos().fieldInfo(field);
    DocValuesType type = info == null ? DocValuesType.NONE : info.getDocValuesType();
    if (type == DocValuesType.SORTED_NUMERIC) {
      return DocValues.getSortedNumeric(reader, field);
    }
    return DocValues.emptySortedNumeric();
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
    matchedDocuments++;
    String distinctValue = distinctField == null ? null : distinctValueForDoc(doc);
    if (distinctValue != null) {
      overallDistinct.add(distinctValue);
    }
    if (!metricValues.advanceExact(doc)) {
      return;
    }
    // A multi-valued metric contributes to every band its values land in, mirroring what a per-band
    // range query would have matched.
    for (int i = 0, valueCount = metricValues.docValueCount(); i < valueCount; i++) {
      float value = NumericUtils.sortableIntToFloat((int) metricValues.nextValue());
      int band = bandFor(value);
      if (band < 0) {
        continue;
      }
      if (distinctField == null) {
        documentCountByBand[band]++;
        continue;
      }
      if (distinctValue != null) {
        distinctValuesByBand.get(band).add(distinctValue);
      }
    }
  }

  /** Index of the half-open band containing {@code value}, or -1 when it falls in none. */
  private int bandFor(final float value) {
    for (int i = 0; i < bandLabels.size(); i++) {
      if (bandMinInclusive[i] < bandMaxExclusive[i] && value >= bandMinInclusive[i] && value < bandMaxExclusive[i]) {
        return i;
      }
    }
    return -1;
  }

  private String distinctValueForDoc(final int doc) throws IOException {
    if (distinctValues == null || !distinctValues.advanceExact(doc)) {
      return null;
    }
    int ord = distinctValues.ordValue();
    String cached = distinctValueByOrd.get(ord);
    if (cached != null || distinctValueByOrd.containsKey(ord)) {
      return cached;
    }
    String value = null;
    BytesRef bytes = distinctValues.lookupOrd(ord);
    if (bytes != null) {
      String decoded = bytes.utf8ToString();
      if (StringUtils.isNotBlank(decoded)) {
        value = decoded;
      }
    }
    distinctValueByOrd.put(ord, value);
    return value;
  }

  @Override
  public ScoreMode scoreMode() {
    return ScoreMode.COMPLETE_NO_SCORES;
  }

  /** Per-band counts in the order the ranges were supplied. Every requested band is present. */
  Map<String, Long> bandCounts() {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (int i = 0; i < bandLabels.size(); i++) {
      counts.put(
          bandLabels.get(i),
          distinctField == null ? documentCountByBand[i] : (long) distinctValuesByBand.get(i).size());
    }
    return counts;
  }

  long matchedDocuments() {
    return matchedDocuments;
  }

  /**
   * Distinct values of {@code distinctField} across every matching document, including unscored
   * documents that sit in no band. When {@code distinctField} is null this is the raw matching
   * document count.
   */
  long overallDistinctCount() {
    return distinctField == null ? matchedDocuments : overallDistinct.size();
  }
}
