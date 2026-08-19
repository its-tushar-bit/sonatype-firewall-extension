/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

/**
 * Distinct composite keys grouped by name, reading only the requested stored fields. Each
 * {@link CollectorManager#newCollector()} instance is independent so Lucene can search index
 * slices concurrently.
 * <p>
 * Materializes one {@link HashSet} entry per distinct key in memory (CLM-40928). Because
 * {@link CollectorManager#newCollector()} allocates a set per slice per named key, peak memory is
 * higher than a sequential collect over the same keys.
 */
final class NamedDistinctStoredFieldsCollector
    extends SimpleCollector
{
  private final Map<String, List<String>> namedCompositeKeyFields;

  private final Set<String> allFields;

  final Map<String, Set<String>> distinctByName;

  private StoredFields storedFields;

  NamedDistinctStoredFieldsCollector(
      final Map<String, List<String>> namedCompositeKeyFields,
      final Set<String> allFields)
  {
    this.namedCompositeKeyFields = namedCompositeKeyFields;
    this.allFields = allFields;
    this.distinctByName = new LinkedHashMap<>();
    namedCompositeKeyFields.keySet().forEach(name -> distinctByName.put(name, new HashSet<>()));
  }

  @Override
  protected void doSetNextReader(final LeafReaderContext context) throws IOException {
    storedFields = context.reader().storedFields();
  }

  @Override
  public void collect(final int doc) throws IOException {
    Document document = storedFields.document(doc, allFields);
    namedCompositeKeyFields.forEach((name, fields) -> {
      StringBuilder key = new StringBuilder();
      for (int i = 0; i < fields.size(); i++) {
        if (i > 0) {
          key.append('\u0000');
        }
        String value = document.get(fields.get(i));
        key.append(value == null ? "" : value);
      }
      distinctByName.get(name).add(key.toString());
    });
  }

  @Override
  public ScoreMode scoreMode() {
    return ScoreMode.COMPLETE_NO_SCORES;
  }

  static CollectorManager<NamedDistinctStoredFieldsCollector, Map<String, Long>> manager(
      final Map<String, List<String>> namedCompositeKeyFields,
      final Set<String> allFields)
  {
    return new CollectorManager<>()
    {
      @Override
      public NamedDistinctStoredFieldsCollector newCollector() {
        return new NamedDistinctStoredFieldsCollector(namedCompositeKeyFields, allFields);
      }

      @Override
      public Map<String, Long> reduce(final Collection<NamedDistinctStoredFieldsCollector> collectors) {
        Map<String, Set<String>> merged = new LinkedHashMap<>();
        namedCompositeKeyFields.keySet().forEach(name -> merged.put(name, new HashSet<>()));
        for (NamedDistinctStoredFieldsCollector collector : collectors) {
          collector.distinctByName.forEach((name, values) -> merged.get(name).addAll(values));
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        merged.forEach((name, values) -> counts.put(name, (long) values.size()));
        return counts;
      }
    };
  }
}
