/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

/**
 * Interim Lucene collector: one stored-field pass that counts distinct {@code distinctField}
 * values grouped by {@code groupField}, restricted to the supplied group keys.
 * <p>
 * Callers should monitor {@link #matchedDocuments()} (Lucene session warns above a soft
 * threshold). That counter includes only docs with non-blank fields whose group value is in the
 * allowed set — not every Lucene hit scanned. Pending Track B docValues cardinality.
 */
public final class DistinctGroupedStoredFieldCollector
    extends SimpleCollector
{
  private final StoredFields storedFields;

  private final String groupField;

  private final String distinctField;

  private final Map<String, Set<String>> distinctValuesByGroup;

  private int docBase;

  private long matchedDocuments;

  public DistinctGroupedStoredFieldCollector(
      final StoredFields storedFields,
      final String groupField,
      final String distinctField,
      final Collection<String> groupValues)
  {
    this.storedFields = storedFields;
    this.groupField = groupField;
    this.distinctField = distinctField;
    this.distinctValuesByGroup = new LinkedHashMap<>();
    for (String groupValue : groupValues) {
      if (StringUtils.isNotBlank(groupValue)) {
        distinctValuesByGroup.put(groupValue, new LinkedHashSet<>());
      }
    }
  }

  @Override
  protected void doSetNextReader(final LeafReaderContext context) {
    docBase = context.docBase;
  }

  @Override
  public void collect(final int doc) throws IOException {
    Document document = storedFields.document(docBase + doc, Set.of(groupField, distinctField));
    String groupValue = document.get(groupField);
    String distinctValue = document.get(distinctField);
    if (StringUtils.isBlank(groupValue) || StringUtils.isBlank(distinctValue)) {
      return;
    }
    Set<String> distinctValues = distinctValuesByGroup.get(groupValue);
    if (distinctValues == null) {
      return;
    }
    matchedDocuments++;
    distinctValues.add(distinctValue);
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
   * Documents that contributed to a group count (non-blank fields and an allowed group value).
   * Does not equal total Lucene hits collected for the query.
   */
  public long matchedDocuments() {
    return matchedDocuments;
  }
}
