/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;
import org.opensearch.client.opensearch._types.query_dsl.Query.Kind;

public class LuceneToOpenSearchQueryAdapterTest
{
  @Test
  public void matchAllDocsQuery_translatesToMatchAll() {
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(new MatchAllDocsQuery());
    assertThat(result._kind()).isEqualTo(Kind.MatchAll);
  }

  @Test
  public void matchNoDocsQuery_translatesToMatchNone() {
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(new MatchNoDocsQuery("none"));
    assertThat(result._kind()).isEqualTo(Kind.MatchNone);
  }

  @Test
  public void termQuery_translatesToTerm() {
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(new TermQuery(new Term("applicationName", "log4j")));
    assertThat(result._kind()).isEqualTo(Kind.Term);
    assertThat(result.term().field()).isEqualTo("applicationName");
    assertThat(result.term().value()._kind().name()).isEqualToIgnoringCase("String");
    assertThat(result.term().value().stringValue()).isEqualTo("log4j");
  }

  @Test
  public void phraseQuery_translatesToMatchPhrase() {
    PhraseQuery phrase = new PhraseQuery.Builder()
        .add(new Term("applicationName", "widget"))
        .add(new Term("applicationName", "co"))
        .build();
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(phrase);
    assertThat(result._kind()).isEqualTo(Kind.MatchPhrase);
    assertThat(result.matchPhrase().field()).isEqualTo("applicationName");
    assertThat(result.matchPhrase().query()).isEqualTo("widget co");
  }

  @Test
  public void phraseQuery_withSlop_preservesSlop() {
    PhraseQuery phrase = new PhraseQuery.Builder()
        .add(new Term("applicationName", "widget"))
        .add(new Term("applicationName", "co"))
        .setSlop(2)
        .build();
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(phrase);
    assertThat(result._kind()).isEqualTo(Kind.MatchPhrase);
    assertThat(result.matchPhrase().slop()).isEqualTo(2);
  }

  @Test
  public void prefixQuery_translatesToPrefix() {
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(new PrefixQuery(new Term("applicationName", "log")));
    assertThat(result._kind()).isEqualTo(Kind.Prefix);
    assertThat(result.prefix().field()).isEqualTo("applicationName");
    assertThat(result.prefix().value()).isEqualTo("log");
  }

  @Test
  public void termInSetQuery_translatesToTerms() {
    TermInSetQuery tis = new TermInSetQuery("allowedContextIds",
        List.of(new BytesRef("org-1"), new BytesRef("org-2")));
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(tis);
    assertThat(result._kind()).isEqualTo(Kind.Terms);
    assertThat(result.terms().field()).isEqualTo("allowedContextIds");
    List<String> values = result.terms()
        .terms()
        .value()
        .stream()
        .map(v -> v.stringValue())
        .toList();
    assertThat(values).containsExactlyInAnyOrder("org-1", "org-2");
  }

  @Test
  public void booleanQuery_translatesToBoolWithClauses() {
    BooleanQuery bool = new BooleanQuery.Builder()
        .add(new TermQuery(new Term("a", "x")), Occur.MUST)
        .add(new TermQuery(new Term("b", "y")), Occur.SHOULD)
        .add(new TermQuery(new Term("c", "z")), Occur.MUST_NOT)
        .add(new TermQuery(new Term("d", "w")), Occur.FILTER)
        .build();
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(bool);
    assertThat(result._kind()).isEqualTo(Kind.Bool);
    assertThat(result.bool().must()).hasSize(1);
    assertThat(result.bool().should()).hasSize(1);
    assertThat(result.bool().mustNot()).hasSize(1);
    assertThat(result.bool().filter()).hasSize(1);
  }

  @Test
  public void emptyBooleanQuery_translatesToMatchNoneLikeMatchNoDocs() {
    org.opensearch.client.opensearch._types.query_dsl.Query emptyBool =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(new BooleanQuery.Builder().build());
    org.opensearch.client.opensearch._types.query_dsl.Query matchNoDocs =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(new MatchNoDocsQuery("none"));
    assertThat(emptyBool._kind()).isEqualTo(Kind.MatchNone);
    assertThat(emptyBool._kind()).isEqualTo(matchNoDocs._kind());
  }

  @Test
  public void shouldOnlyBooleanQuery_setsMinimumShouldMatchToOne() {
    BooleanQuery bool = new BooleanQuery.Builder()
        .add(new TermQuery(new Term("a", "x")), Occur.SHOULD)
        .add(new TermQuery(new Term("b", "y")), Occur.SHOULD)
        .build();
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(bool);
    assertThat(result._kind()).isEqualTo(Kind.Bool);
    assertThat(result.bool().should()).hasSize(2);
    assertThat(result.bool().minimumShouldMatch()).isEqualTo("1");
  }

  @Test
  public void mixedMustAndShouldBooleanQuery_doesNotForceMinimumShouldMatch() {
    BooleanQuery bool = new BooleanQuery.Builder()
        .add(new TermQuery(new Term("a", "x")), Occur.MUST)
        .add(new TermQuery(new Term("b", "y")), Occur.SHOULD)
        .build();
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(bool);
    assertThat(result._kind()).isEqualTo(Kind.Bool);
    assertThat(result.bool().minimumShouldMatch()).isNull();
  }

  @Test
  public void explicitMinimumShouldMatch_isPreserved() {
    BooleanQuery bool = new BooleanQuery.Builder()
        .add(new TermQuery(new Term("a", "x")), Occur.SHOULD)
        .add(new TermQuery(new Term("b", "y")), Occur.SHOULD)
        .setMinimumNumberShouldMatch(2)
        .build();
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(bool);
    assertThat(result.bool().minimumShouldMatch()).isEqualTo("2");
  }

  @Test
  public void intPointRangeQuery_translatesToRangeWithBounds() {
    Query range = IntPoint.newRangeQuery("policyThreatLevel", 2, 8);
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(range);
    assertThat(result._kind()).isEqualTo(Kind.Range);
    assertThat(result.range().field()).isEqualTo("policyThreatLevel");
    assertThat(result.range().gte().to(Integer.class)).isEqualTo(2);
    assertThat(result.range().lte().to(Integer.class)).isEqualTo(8);
  }

  @Test
  public void intPointExactQuery_translatesToRangeWithEqualBounds() {
    Query exact = IntPoint.newExactQuery("policyThreatLevel", 5);
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(exact);
    assertThat(result._kind()).isEqualTo(Kind.Range);
    assertThat(result.range().field()).isEqualTo("policyThreatLevel");
    assertThat(result.range().gte().to(Integer.class)).isEqualTo(5);
    assertThat(result.range().lte().to(Integer.class)).isEqualTo(5);
  }

  @Test
  public void floatPointRangeQuery_translatesToRangeWithFloatBounds() {
    Query range = FloatPoint.newRangeQuery("vulnerabilitySeverity", 3.5f, 7.5f);
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(range);
    assertThat(result._kind()).isEqualTo(Kind.Range);
    assertThat(result.range().field()).isEqualTo("vulnerabilitySeverity");
    assertThat(result.range().gte().to(Double.class)).isEqualTo(3.5);
    assertThat(result.range().lte().to(Double.class)).isEqualTo(7.5);
  }

  @Test
  public void longPointRangeQuery_waiverExpiry_translatesWithEightByteBounds() {
    // policyWaiverExpiresAtEpochMs is a Long field; a value beyond the 32-bit int range confirms the
    // decoder uses LongPoint's 8-byte layout rather than misreading it as an IntPoint.
    long lo = 1_700_000_000_000L;
    long hi = 1_800_000_000_000L;
    Query range = LongPoint.newRangeQuery("policyWaiverExpiresAtEpochMs", lo, hi);
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(range);
    assertThat(result._kind()).isEqualTo(Kind.Range);
    assertThat(result.range().field()).isEqualTo("policyWaiverExpiresAtEpochMs");
    assertThat(result.range().gte().to(Long.class)).isEqualTo(lo);
    assertThat(result.range().lte().to(Long.class)).isEqualTo(hi);
  }

  @Test
  public void longPointRangeQuery_appLastEvaluation_translatesToRangeWithLongBounds() {
    // 8-byte LongPoint layout must be decoded with LongPoint.decodeDimension, not IntPoint's 4-byte.
    long lo = 1_700_000_000_000L;
    long hi = 1_800_000_000_000L;
    Query range = LongPoint.newRangeQuery("applicationLastEvaluationTimeEpochMs", lo, hi);
    org.opensearch.client.opensearch._types.query_dsl.Query result =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(range);
    assertThat(result._kind()).isEqualTo(Kind.Range);
    assertThat(result.range().field()).isEqualTo("applicationLastEvaluationTimeEpochMs");
    assertThat(result.range().gte().to(Long.class)).isEqualTo(lo);
    assertThat(result.range().lte().to(Long.class)).isEqualTo(hi);
  }

  @Test
  public void pointRangeQuery_onUnknownNumericField_throwsIllegalArgument() {
    Query range = IntPoint.newRangeQuery("notANumericField", 1, 2);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> LuceneToOpenSearchQueryAdapter.toOpenSearch(range));
  }

  @Test
  public void multiDimensionalPointRangeQuery_throwsIllegalArgument() {
    Query range = IntPoint.newRangeQuery("policyThreatLevel", new int[]{1, 2}, new int[]{3, 4});
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> LuceneToOpenSearchQueryAdapter.toOpenSearch(range));
  }

  @Test
  public void unsupportedQueryType_throwsIllegalArgument() {
    Query wildcard = new WildcardQuery(new Term("a", "ab*"));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> LuceneToOpenSearchQueryAdapter.toOpenSearch(wildcard));
  }

  @Test
  public void recursionDepthCap_throwsWhenExceeded() {
    Query nested = new TermQuery(new Term("a", "x"));
    for (int i = 0; i <= LuceneToOpenSearchQueryAdapter.MAX_DEPTH; i++) {
      nested = new BooleanQuery.Builder().add(nested, Occur.MUST).build();
    }
    final Query deeper = nested;
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> LuceneToOpenSearchQueryAdapter.toOpenSearch(deeper));
  }
}
