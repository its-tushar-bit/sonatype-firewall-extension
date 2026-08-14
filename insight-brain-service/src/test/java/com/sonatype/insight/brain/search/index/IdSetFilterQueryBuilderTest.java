/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.List;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IdSetFilterQueryBuilderTest
{
  @Test
  public void build_lowercasesAndDropsBlanks() {
    Query query = IdSetFilterQueries.build("vulnerabilityId", List.of("CVE-1", "  ", "cve-2"));
    assertThat(query).isInstanceOf(TermInSetQuery.class);
    assertThat(query.toString()).contains("vulnerabilityId");
    assertThat(query.toString()).contains("cve-1");
    assertThat(query.toString()).contains("cve-2");
    assertThat(query.toString()).doesNotContain("CVE-1");
  }

  @Test
  public void build_emptyOrBlankOnly_isMatchNoDocs() {
    assertThat(IdSetFilterQueries.build("vulnerabilityId", List.of())).isInstanceOf(MatchNoDocsQuery.class);
    assertThat(IdSetFilterQueries.build("vulnerabilityId", List.of("  "))).isInstanceOf(MatchNoDocsQuery.class);
    assertThat(IdSetFilterQueries.build("vulnerabilityId", null)).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void build_blankIdField_throws() {
    assertThatThrownBy(() -> IdSetFilterQueries.build("  ", List.of("cve-1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("idField");
  }

  @Test
  public void normalizedIds_lowercasesAndDropsBlanks() {
    assertThat(IdSetFilterQueries.normalizedIds(List.of("CVE-1", "  ", "cve-2")))
        .containsExactly("cve-1", "cve-2");
    assertThat(IdSetFilterQueries.normalizedIds(null)).isEmpty();
    assertThat(IdSetFilterQueries.normalizedIds(List.of())).isEmpty();
  }

  @Test
  public void combineLuceneFilters_multipleRestrictions_andEachAsFilter() {
    Query query = IdSetFilterQueries.combineLuceneFilters(List.of(
        IndexTermSetRestriction.of("vulnerabilityId", List.of("cve-1")),
        IndexTermSetRestriction.of("applicationId", List.of("app-1"))));
    assertThat(query).isInstanceOf(BooleanQuery.class);
    assertThat(query.toString()).contains("vulnerabilityId");
    assertThat(query.toString()).contains("applicationId");
    assertThat(query.toString()).contains("cve-1");
    assertThat(query.toString()).contains("app-1");
  }

  @Test
  public void combineLuceneFilters_nullOrEmpty_returnsNull() {
    assertThat(IdSetFilterQueries.combineLuceneFilters(null)).isNull();
    assertThat(IdSetFilterQueries.combineLuceneFilters(List.of())).isNull();
  }

  @Test
  public void combineLuceneFilters_orGroup_isShouldOfAlternatives() {
    Query query = IdSetFilterQueries.combineLuceneFilters(List.of(
        IndexOrTermSetGroup.of(
            IndexTermSetRestriction.of("organizationId", List.of("org-1")),
            IndexTermSetRestriction.of("applicationId", List.of("app-1")))));
    assertThat(query).isInstanceOf(BooleanQuery.class);
    BooleanQuery booleanQuery = (BooleanQuery) query;
    assertThat(booleanQuery.getMinimumNumberShouldMatch()).isEqualTo(1);
    assertThat(booleanQuery.clauses()).allMatch(clause -> clause.getOccur() == Occur.SHOULD);
    assertThat(booleanQuery.clauses()).hasSize(2);
    assertThat(query.toString()).contains("organizationId");
    assertThat(query.toString()).contains("applicationId");
    assertThat(query.toString()).contains("org-1");
    assertThat(query.toString()).contains("app-1");
  }
}
