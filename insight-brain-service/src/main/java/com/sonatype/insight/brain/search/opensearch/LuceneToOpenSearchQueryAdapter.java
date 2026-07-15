/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.global.fieldmap.FieldEntry;
import com.sonatype.insight.brain.search.global.fieldmap.FieldMap;

import com.google.common.annotations.VisibleForTesting;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PointRangeQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchNoneQuery;
import org.opensearch.client.opensearch._types.query_dsl.PrefixQuery.Builder;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField;

/**
 * Translates the subset of Lucene queries produced by the Global Search compiler and
 * permission-filter helpers into OpenSearch DSL. Only builder-constructed queries from the
 * Global Search path are supported; any other query type throws {@link IllegalArgumentException},
 * so user-supplied query-string-parsed queries must never be fed here.
 */
public final class LuceneToOpenSearchQueryAdapter
{
  @VisibleForTesting
  static final int MAX_DEPTH = 16;

  /**
   * Numeric point queries lose their Java type: {@link IntPoint} and {@link FloatPoint} both emit a
   * {@link PointRangeQuery} with a 4-byte-per-dimension layout, so the decoder must be chosen by the
   * field's declared type. This maps the indexed label back to {@code Integer.class}/{@code Float.class}
   * using the same {@link FieldMap} the Global Search compiler encoded against.
   */
  private static final Map<String, Class<? extends Number>> NUMERIC_LABEL_TYPES = numericLabelTypes();

  private static Map<String, Class<? extends Number>> numericLabelTypes() {
    Map<String, Class<? extends Number>> byLabel = new HashMap<>();
    FieldMap fieldMap = FieldMap.defaultMap();
    for (String fieldName : fieldMap.knownFieldNames()) {
      FieldEntry entry = fieldMap.lookup(fieldName).orElseThrow();
      if (entry.numericType() != null) {
        byLabel.put(entry.label(), entry.numericType());
      }
    }
    return Map.copyOf(byLabel);
  }

  private LuceneToOpenSearchQueryAdapter() {
  }

  public static org.opensearch.client.opensearch._types.query_dsl.Query toOpenSearch(final Query luceneQuery) {
    return toOs(luceneQuery, 0);
  }

  private static org.opensearch.client.opensearch._types.query_dsl.Query toOs(
      final Query luceneQuery,
      final int depth)
  {
    if (depth > MAX_DEPTH) {
      throw new IllegalArgumentException(
          "Lucene query nesting exceeds MAX_DEPTH=" + MAX_DEPTH + " for Global Search adapter");
    }
    if (luceneQuery instanceof MatchAllDocsQuery) {
      return org.opensearch.client.opensearch._types.query_dsl.Query
          .of(q -> q.matchAll(new MatchAllQuery.Builder().build()));
    }
    if (luceneQuery instanceof MatchNoDocsQuery) {
      return org.opensearch.client.opensearch._types.query_dsl.Query
          .of(q -> q.matchNone(new MatchNoneQuery.Builder().build()));
    }
    if (luceneQuery instanceof TermQuery termQuery) {
      String field = termQuery.getTerm().field();
      String value = termQuery.getTerm().text();
      return org.opensearch.client.opensearch._types.query_dsl.Query.of(q -> q
          .term(t -> t.field(field).value(FieldValue.of(value))));
    }
    if (luceneQuery instanceof PhraseQuery phraseQuery) {
      org.apache.lucene.index.Term[] terms = phraseQuery.getTerms();
      if (terms.length == 0) {
        throw new IllegalArgumentException("PhraseQuery with no terms is not translatable");
      }
      String field = terms[0].field();
      StringBuilder phrase = new StringBuilder();
      for (int i = 0; i < terms.length; i++) {
        if (i > 0) {
          if (!field.equals(terms[i].field())) {
            throw new IllegalArgumentException(
                "PhraseQuery spans multiple fields (" + field + ", " + terms[i].field()
                    + ") and cannot be translated to a single match_phrase");
          }
          phrase.append(' ');
        }
        phrase.append(terms[i].text());
      }
      String query = phrase.toString();
      int slop = phraseQuery.getSlop();
      return org.opensearch.client.opensearch._types.query_dsl.Query.of(q -> q
          .matchPhrase(mp -> slop > 0 ? mp.field(field).query(query).slop(slop) : mp.field(field).query(query)));
    }
    if (luceneQuery instanceof PrefixQuery prefixQuery) {
      String field = prefixQuery.getPrefix().field();
      String value = prefixQuery.getPrefix().text();
      return org.opensearch.client.opensearch._types.query_dsl.Query.of(q -> q
          .prefix(new Builder().field(field).value(value).build()));
    }
    if (luceneQuery instanceof TermInSetQuery termsQuery) {
      String field = termsQuery.getField();
      List<FieldValue> values = new ArrayList<>();
      try {
        BytesRefIterator iter = termsQuery.getBytesRefIterator();
        BytesRef next;
        while ((next = iter.next()) != null) {
          values.add(FieldValue.of(next.utf8ToString()));
        }
      }
      catch (IOException e) {
        throw new IllegalStateException("Unable to enumerate TermInSetQuery terms", e);
      }
      TermsQueryField tqf = new TermsQueryField.Builder().value(values).build();
      return org.opensearch.client.opensearch._types.query_dsl.Query.of(q -> q
          .terms(new TermsQuery.Builder().field(field).terms(tqf).build()));
    }
    if (luceneQuery instanceof PointRangeQuery pointRangeQuery) {
      return toRange(pointRangeQuery);
    }
    if (luceneQuery instanceof BooleanQuery booleanQuery) {
      // A zero-clause BooleanQuery is equivalent to MatchNoDocsQuery on Lucene 9+; an empty
      // OpenSearch bool would instead match everything, inverting a permission-sensitive filter.
      if (booleanQuery.clauses().isEmpty()) {
        return org.opensearch.client.opensearch._types.query_dsl.Query
            .of(q -> q.matchNone(new MatchNoneQuery.Builder().build()));
      }
      BoolQuery.Builder bool = new BoolQuery.Builder();
      boolean hasShould = false;
      boolean hasMustOrFilter = false;
      for (BooleanClause clause : booleanQuery.clauses()) {
        org.opensearch.client.opensearch._types.query_dsl.Query inner = toOs(clause.getQuery(), depth + 1);
        switch (clause.getOccur()) {
          case MUST -> {
            bool.must(inner);
            hasMustOrFilter = true;
          }
          case SHOULD -> {
            bool.should(inner);
            hasShould = true;
          }
          case MUST_NOT -> bool.mustNot(inner);
          case FILTER -> {
            bool.filter(inner);
            hasMustOrFilter = true;
          }
        }
      }
      int minShould = booleanQuery.getMinimumNumberShouldMatch();
      if (minShould > 0) {
        bool.minimumShouldMatch(String.valueOf(minShould));
      }
      else if (hasShould && !hasMustOrFilter) {
        // OpenSearch defaults minimum_should_match to 0 for SHOULD-only bools; Lucene requires 1.
        bool.minimumShouldMatch("1");
      }
      return org.opensearch.client.opensearch._types.query_dsl.Query.of(q -> q.bool(bool.build()));
    }
    throw new IllegalArgumentException(
        "Unsupported Lucene Query class for Global Search OpenSearch adapter: " + luceneQuery.getClass().getName());
  }

  private static org.opensearch.client.opensearch._types.query_dsl.Query toRange(final PointRangeQuery prq) {
    if (prq.getNumDims() != 1) {
      throw new IllegalArgumentException(
          "Multi-dimensional PointRangeQuery (numDims=" + prq.getNumDims()
              + ") is not produced by the Global Search compiler and cannot be translated");
    }
    String field = prq.getField();
    Class<? extends Number> numericType = NUMERIC_LABEL_TYPES.get(field);
    if (numericType == null) {
      throw new IllegalArgumentException(
          "PointRangeQuery on unknown numeric field \"" + field + "\" for Global Search OpenSearch adapter");
    }
    JsonData gte;
    JsonData lte;
    if (numericType == Float.class) {
      gte = JsonData.of((double) FloatPoint.decodeDimension(prq.getLowerPoint(), 0));
      lte = JsonData.of((double) FloatPoint.decodeDimension(prq.getUpperPoint(), 0));
    }
    else {
      gte = JsonData.of(IntPoint.decodeDimension(prq.getLowerPoint(), 0));
      lte = JsonData.of(IntPoint.decodeDimension(prq.getUpperPoint(), 0));
    }
    return org.opensearch.client.opensearch._types.query_dsl.Query
        .of(q -> q.range(new RangeQuery.Builder().field(field).gte(gte).lte(lte).build()));
  }
}
