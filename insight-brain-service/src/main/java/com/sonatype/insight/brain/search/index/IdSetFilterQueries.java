/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.util.BytesRef;

/**
 * Builds boolean-clause-budget-exempt restrictions to known field-value sets.
 * <p>
 * Lucene {@link TermInSetQuery} and OpenSearch {@code terms} queries do not consume one bool clause
 * per id, so callers can AND large known id sets onto an already-large base query (CLM-44783).
 */
public final class IdSetFilterQueries
{
  private IdSetFilterQueries() {
  }

  /**
   * Combines {@code restrictions} into a single Lucene FILTER query, or {@code null} when there are
   * no restrictions. Restrictions are ANDed. Each {@link IndexTermSetRestriction} is a
   * {@link TermInSetQuery} (or {@link MatchNoDocsQuery} when its id set is empty/blank-only). Each
   * {@link IndexOrTermSetGroup} is a SHOULD of its alternatives (Classic org∪app union).
   */
  public static Query combineLuceneFilters(final List<? extends IndexFilterRestriction> restrictions) {
    if (restrictions == null || restrictions.isEmpty()) {
      return null;
    }
    if (restrictions.size() == 1) {
      return toLuceneFilter(restrictions.get(0));
    }
    BooleanQuery.Builder combined = new BooleanQuery.Builder();
    for (IndexFilterRestriction restriction : restrictions) {
      combined.add(toLuceneFilter(restriction), Occur.FILTER);
    }
    return combined.build();
  }

  private static Query toLuceneFilter(final IndexFilterRestriction restriction) {
    if (restriction instanceof IndexTermSetRestriction termSet) {
      return build(termSet.field(), termSet.ids());
    }
    if (restriction instanceof IndexOrTermSetGroup orGroup) {
      List<IndexTermSetRestriction> alternatives = orGroup.alternatives();
      if (alternatives.size() == 1) {
        return build(alternatives.get(0).field(), alternatives.get(0).ids());
      }
      BooleanQuery.Builder should = new BooleanQuery.Builder();
      for (IndexTermSetRestriction alternative : alternatives) {
        should.add(build(alternative.field(), alternative.ids()), Occur.SHOULD);
      }
      // Explicit msm=1: a SHOULD-only BooleanQuery with msm=0 matches every document. Lucene may
      // coerce pure-SHOULD queries, but keep the requirement explicit so a future MUST/FILTER on
      // this builder cannot silently turn Classic org∪app into a no-op scope bypass.
      return should.setMinimumNumberShouldMatch(1).build();
    }
    throw new IllegalArgumentException("Unknown IndexFilterRestriction: " + restriction);
  }

  /**
   * Lucene filter over {@code idField} for the given ids. Blank ids are dropped; remaining values are
   * lower-cased. Empty / blank-only / null {@code ids} yields {@link MatchNoDocsQuery} (fail closed —
   * never unrestricted).
   */
  public static Query build(final String idField, final Collection<String> ids) {
    requireIdField(idField);
    List<BytesRef> terms = toBytesRefs(ids);
    if (terms.isEmpty()) {
      return new MatchNoDocsQuery("empty id set");
    }
    return new TermInSetQuery(idField, terms);
  }

  /**
   * Normalized id values for OpenSearch {@code terms} filters: blanks dropped, trimmed, lower-cased.
   * Empty when {@code ids} is null, empty, or blank-only.
   */
  public static List<String> normalizedIds(final Collection<String> ids) {
    List<String> normalized = new ArrayList<>();
    if (ids == null) {
      return normalized;
    }
    for (String id : ids) {
      if (id == null || id.isBlank()) {
        continue;
      }
      normalized.add(id.trim().toLowerCase(Locale.ROOT));
    }
    return normalized;
  }

  private static List<BytesRef> toBytesRefs(final Collection<String> ids) {
    List<String> normalized = normalizedIds(ids);
    List<BytesRef> terms = new ArrayList<>(normalized.size());
    for (String id : normalized) {
      terms.add(new BytesRef(id));
    }
    return terms;
  }

  private static void requireIdField(final String idField) {
    if (idField == null || idField.isBlank()) {
      throw new IllegalArgumentException("idField must be non-blank");
    }
  }
}
