/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;

/**
 * Lucene collector that sums integral {@code sumField} docValues grouped by {@code groupField}
 * SortedDocValues, restricted to the supplied group keys.
 * <p>
 * Group keys are compared and returned lowercased so this backend matches the OpenSearch backend,
 * whose keyword fields carry a lowercase normalizer (see IndexMapping). Reads only doc-values —
 * no stored-field I/O — matching {@link MaxMetricByGroupCollector}.
 */
final class SumGroupedDocValuesCollector
    extends SimpleCollector
{
  private final String groupField;

  private final String sumField;

  private final Map<String, Long> sumsByGroup;

  private SortedDocValues groupValues;

  private SortedNumericDocValues sumDocValues;

  private FixedBitSet allowedOrds;

  SumGroupedDocValuesCollector(
      final String groupField,
      final String sumField,
      final Collection<String> groupValues)
  {
    this.groupField = groupField;
    this.sumField = sumField;
    this.sumsByGroup = new LinkedHashMap<>();
    for (String groupValue : groupValues) {
      if (StringUtils.isNotBlank(groupValue)) {
        sumsByGroup.put(groupValue.toLowerCase(Locale.ROOT), 0L);
      }
    }
  }

  @Override
  protected void doSetNextReader(final LeafReaderContext context) throws IOException {
    groupValues = context.reader().getSortedDocValues(groupField);
    sumDocValues = DocValues.getSortedNumeric(context.reader(), sumField);
    if (groupValues == null || sumsByGroup.isEmpty()) {
      allowedOrds = null;
      return;
    }
    allowedOrds = new FixedBitSet(Math.max(groupValues.getValueCount(), 1));
    for (String key : sumsByGroup.keySet()) {
      int ord = groupValues.lookupTerm(new BytesRef(key));
      if (ord >= 0) {
        allowedOrds.set(ord);
      }
    }
  }

  @Override
  public void collect(final int doc) throws IOException {
    if (groupValues == null || allowedOrds == null || !groupValues.advanceExact(doc)) {
      return;
    }
    int ord = groupValues.ordValue();
    if (!allowedOrds.get(ord)) {
      return;
    }
    String groupKey = groupValues.lookupOrd(ord).utf8ToString();
    if (!sumsByGroup.containsKey(groupKey)) {
      return;
    }
    if (!sumDocValues.advanceExact(doc)) {
      return;
    }
    long docSum = 0L;
    int valueCount = sumDocValues.docValueCount();
    for (int i = 0; i < valueCount; i++) {
      docSum += sumDocValues.nextValue();
    }
    // Always accumulate, including zero docSums. Zero groups are filtered in groupSums() so
    // callers treat absence as zero — matching OpenSearchSessionAggregations.parseTermsSum.
    sumsByGroup.merge(groupKey, docSum, Long::sum);
  }

  @Override
  public ScoreMode scoreMode() {
    return ScoreMode.COMPLETE_NO_SCORES;
  }

  Map<String, Long> groupSums() {
    Map<String, Long> sums = new LinkedHashMap<>();
    sumsByGroup.forEach((groupValue, sum) -> {
      if (sum != 0L) {
        sums.put(groupValue, sum);
      }
    });
    return sums;
  }
}
