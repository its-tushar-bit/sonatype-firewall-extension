/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.search.opensearch.OpenSearchSessionAggregations.TermsInclude;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the group-value normalization contract the aggregation include terms depend on: group values are
 * lowercased with a fixed locale so they match the indexed keyword bytes regardless of the JVM default
 * locale, blanks and duplicates are dropped, and a wholly unrequestable input yields no include terms so
 * the caller can short-circuit. Also pins the absent-aggregation parse path.
 */
public class OpenSearchSessionAggregationsTest
{
  @Test
  public void prepareIncludeTerms_lowercasesWithRootLocaleAndDropsBlanks() {
    TermsInclude terms = OpenSearchSessionAggregations.prepareIncludeTerms(
        Arrays.asList("APP-One", " ", null, "app-two", "APP-ONE"));

    // Lowercased to match the indexed keyword bytes, de-duplicated, blanks and nulls dropped.
    assertThat(terms).isNotNull();
    assertThat(terms.includeTerms()).containsExactly("app-one", "app-two");
    assertThat(terms.requestedKeys()).containsExactly("app-one", "app-two");
  }

  /**
   * Turkish "I" lowercases to a dotless i under a Turkish default locale, which would not match the
   * indexed bytes; the normalization must be locale-independent.
   */
  @Test
  public void prepareIncludeTerms_isLocaleIndependent() {
    TermsInclude terms = OpenSearchSessionAggregations.prepareIncludeTerms(List.of("ID-42"));

    assertThat(terms).isNotNull();
    assertThat(terms.includeTerms()).containsExactly("id-42");
  }

  @Test
  public void prepareIncludeTerms_returnsNullWhenNothingRequestable() {
    assertThat(OpenSearchSessionAggregations.prepareIncludeTerms(null)).isNull();
    assertThat(OpenSearchSessionAggregations.prepareIncludeTerms(List.of())).isNull();
    assertThat(OpenSearchSessionAggregations.prepareIncludeTerms(Arrays.asList(" ", null))).isNull();
  }

  @Test
  public void parseTermsCardinality_returnsEmptyMapWhenAggregationIsAbsent() {
    assertThat(OpenSearchSessionAggregations.parseTermsCardinality(null, "groups", Set.of("app-one")))
        .isEmpty();
    assertThat(OpenSearchSessionAggregations.parseTermsCardinality(Map.of(), "groups", Set.of("app-one")))
        .isEmpty();
  }
}
