/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.fieldmap;

import java.util.List;

import com.sonatype.insight.brain.search.global.parser.AstNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.AndNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.EmptyNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.FieldNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.NotNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.OrNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.PhraseNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.TermNode;
import com.sonatype.insight.brain.search.global.parser.FieldValue.EmptyValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.ExactValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.PhraseValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.PrefixValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.RangeValue;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryCompilerTest
{
  private final FieldMap map = FieldMap.defaultMap();

  private CompiledQuery compile(AstNode ast, ItemType type) {
    return QueryCompiler.compile(ast, type, map);
  }

  private CompiledQuery compile(AstNode ast) {
    return compile(ast, ItemType.APPLICATION);
  }

  // ─────────────────────────── empty / bare ───────────────────────────

  @Test
  public void emptyNodeMatchesEverything() {
    CompiledQuery r = compile(new EmptyNode());
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isInstanceOf(MatchAllDocsQuery.class);
  }

  @Test
  public void bareTermExpandsAcrossApplicationDefaultFields() {
    CompiledQuery r = compile(new TermNode("Terraform"));
    assertThat(r.warnings()).isEmpty();
    BooleanQuery bq = (BooleanQuery) r.luceneQuery();
    assertThat(bq.getMinimumNumberShouldMatch()).isEqualTo(1);
    // Every top-level clause is a SHOULD wrapping a per-field (TermQuery OR PrefixQuery) bool.
    // The typeahead prefix is what makes "terraform" hit "terraform-prod" under whole-value
    // keyword analysis.
    java.util.Set<String> fieldsSeen = new java.util.LinkedHashSet<>();
    for (BooleanClause topClause : bq.clauses()) {
      assertThat(topClause.getOccur()).isEqualTo(Occur.SHOULD);
      BooleanQuery perField = (BooleanQuery) topClause.getQuery();
      assertThat(perField.getMinimumNumberShouldMatch()).isEqualTo(1);
      // Per-field bool has exactly two clauses: exact TermQuery + typeahead PrefixQuery.
      assertThat(perField.clauses()).hasSize(2);
      TermQuery exact = (TermQuery) perField.clauses().get(0).getQuery();
      PrefixQuery prefix = (PrefixQuery) perField.clauses().get(1).getQuery();
      assertThat(exact.getTerm().text()).isEqualTo("terraform");
      assertThat(prefix.getPrefix().text()).isEqualTo("terraform");
      assertThat(exact.getTerm().field()).isEqualTo(prefix.getPrefix().field());
      fieldsSeen.add(exact.getTerm().field());
    }
    // Application default set includes at least these four fields.
    assertThat(fieldsSeen)
        .contains("applicationName", "applicationPublicId", "organizationName", "applicationCategoryName");
  }

  @Test
  public void barePhraseAcrossComponentDefaultFieldsBuildsPhraseQueries() {
    CompiledQuery r = compile(new PhraseNode("Left Pad"), ItemType.NON_VULNERABLE_COMPONENT);
    assertThat(r.warnings()).isEmpty();
    BooleanQuery bq = (BooleanQuery) r.luceneQuery();
    // Multi-word phrase produces PhraseQuery per default field.
    assertThat(bq.clauses()).allMatch(c -> c.getQuery() instanceof PhraseQuery);
  }

  @Test
  public void barePhraseOnVulnerabilityHitsVulnerabilityDefaults() {
    // Vulnerability default-search fields cover the vulnerability id and its component context
    // (users search for a CVE by the component name it affects). The TEXT description field is
    // intentionally excluded from bare-term defaults (a single-token query never matches it).
    CompiledQuery r = compile(new PhraseNode("log4j"), ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).isEmpty();
    BooleanQuery bq = (BooleanQuery) r.luceneQuery();
    assertThat(bq.getMinimumNumberShouldMatch()).isEqualTo(1);
    assertThat(bq.clauses().stream().map(c -> ((TermQuery) c.getQuery()).getTerm().field()).toList())
        .containsExactlyInAnyOrder("vulnerabilityId", "componentName")
        .doesNotContain("vulnerabilityDescription");
  }

  @Test
  public void bareTermOnVulnerabilityUsesOnlyKeywordFields() {
    // Bare-term defaults must be KEYWORD-kind only: the TEXT vulnerabilityDescription field must not
    // appear, since a single-token TermQuery/PrefixQuery never matches an analyzer-tokenized field.
    CompiledQuery r = compile(new TermNode("log4j"), ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).isEmpty();
    BooleanQuery bq = (BooleanQuery) r.luceneQuery();
    java.util.Set<String> fieldsSeen = new java.util.LinkedHashSet<>();
    for (BooleanClause topClause : bq.clauses()) {
      BooleanQuery perField = (BooleanQuery) topClause.getQuery();
      TermQuery exact = (TermQuery) perField.clauses().get(0).getQuery();
      fieldsSeen.add(exact.getTerm().field());
    }
    assertThat(fieldsSeen)
        .containsExactlyInAnyOrder("vulnerabilityId", "componentName")
        .doesNotContain("vulnerabilityDescription");
  }

  // ─────────────────────────── keyword field ───────────────────────────

  @Test
  public void keywordExactValueLowercasesToTermQuery() {
    CompiledQuery r = compile(new FieldNode("applicationName", new ExactValue("Acme")));
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("applicationName", "acme")));
  }

  @Test
  public void keywordPhraseValueLowercasesToTermQuery() {
    CompiledQuery r = compile(new FieldNode("applicationName", new PhraseValue("Acme Corp")));
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("applicationName", "acme corp")));
  }

  @Test
  public void keywordPrefixValueBuildsPrefixQuery() {
    CompiledQuery r = compile(new FieldNode("applicationName", new PrefixValue("Acm")));
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isEqualTo(new PrefixQuery(new Term("applicationName", "acm")));
  }

  @Test
  public void keywordPrefixTooShortMatchesNothingWithWarning() {
    // A single-character fielded prefix (below the floor of 2) would expand across nearly every
    // term in the field — CPU DoS. It must warn and match nothing instead.
    CompiledQuery r = compile(new FieldNode("applicationName", new PrefixValue("a")));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("too short");
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void keywordEmptyPrefixWildcardMatchesNothingWithWarning() {
    // field:* — empty prefix — must not scan the entire field; fail open to MatchNoDocs + warning.
    CompiledQuery r = compile(new FieldNode("applicationName", new PrefixValue("")));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("too short");
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void keywordPrefixOverMaxLengthIsTruncated() {
    String longPrefix = "a".repeat(40);
    CompiledQuery r = compile(new FieldNode("applicationName", new PrefixValue(longPrefix)));
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isEqualTo(new PrefixQuery(new Term("applicationName", "a".repeat(20))));
  }

  @Test
  public void keywordRangeValueWarnsAndFallsBackToTermMatch() {
    CompiledQuery r = compile(
        new FieldNode("applicationName", new RangeValue("Foo", "Bar", true, true)));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("Range query on non-numeric filter \"applicationName\"");
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("applicationName", "foo")));
  }

  @Test
  public void keywordRangeAllStarsFallsBackToMatchAll() {
    CompiledQuery r = compile(new FieldNode("applicationName", new RangeValue("*", "*", true, true)));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.luceneQuery()).isInstanceOf(MatchAllDocsQuery.class);
  }

  // ─────────────────────────── text (tokenized) field ───────────────────────────

  @Test
  public void textFieldExactValueUsesTermQuery() {
    CompiledQuery r = compile(
        new FieldNode("vulnerabilityDescription", new ExactValue("Heartbleed")),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("vulnerabilityDescription", "heartbleed")));
  }

  @Test
  public void textFieldMultiwordPhraseValueUsesPhraseQuery() {
    CompiledQuery r = compile(
        new FieldNode("vulnerabilityDescription", new PhraseValue("Buffer Overflow")),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).isEmpty();
    PhraseQuery expected = new PhraseQuery.Builder()
        .add(new Term("vulnerabilityDescription", "buffer"))
        .add(new Term("vulnerabilityDescription", "overflow"))
        .build();
    assertThat(r.luceneQuery()).isEqualTo(expected);
  }

  @Test
  public void textFieldSingleWordPhraseCollapsesToTermQuery() {
    CompiledQuery r = compile(
        new FieldNode("vulnerabilityDescription", new PhraseValue("Overflow")),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("vulnerabilityDescription", "overflow")));
  }

  @Test
  public void textFieldPrefixValueBuildsPrefixQuery() {
    CompiledQuery r = compile(
        new FieldNode("vulnerabilityDescription", new PrefixValue("Heart")),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isEqualTo(new PrefixQuery(new Term("vulnerabilityDescription", "heart")));
  }

  @Test
  public void textFieldPrefixTooShortMatchesNothingWithWarning() {
    CompiledQuery r = compile(
        new FieldNode("vulnerabilityDescription", new PrefixValue("h")),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("too short");
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void textFieldRangeValueWarnsAndFallsBack() {
    CompiledQuery r = compile(
        new FieldNode("vulnerabilityDescription", new RangeValue("a", "z", true, true)),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("Range query on non-numeric filter");
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("vulnerabilityDescription", "a")));
  }

  // ─────────────────────────── numeric field ───────────────────────────

  @Test
  public void numericIntExactValueBuildsIntPointExact() {
    CompiledQuery r = compile(new FieldNode("policyThreatLevel", new ExactValue("7")),
        ItemType.POLICY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery().toString()).contains("policyThreatLevel");
    // IntPoint.newExactQuery produces a Point-based Query whose string form
    // encodes the exact value; we assert the range endpoints for stability.
    assertThat(r.luceneQuery().toString()).contains("[7 TO 7]");
  }

  @Test
  public void numericIntInclusiveRangeBuildsIntPointRange() {
    CompiledQuery r = compile(new FieldNode("policyThreatLevel", new RangeValue("7", "10", true, true)),
        ItemType.POLICY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery().toString()).contains("[7 TO 10]");
  }

  @Test
  public void numericIntExclusiveRangeAdjustsBounds() {
    CompiledQuery r = compile(new FieldNode("policyThreatLevel", new RangeValue("7", "10", false, false)),
        ItemType.POLICY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery().toString()).contains("[8 TO 9]");
  }

  @Test
  public void numericIntOpenLowRange() {
    CompiledQuery r = compile(new FieldNode("policyThreatLevel", new RangeValue("*", "5", true, true)),
        ItemType.POLICY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery().toString()).contains(String.valueOf(Integer.MIN_VALUE));
    assertThat(r.luceneQuery().toString()).contains(" TO 5]");
  }

  @Test
  public void numericLongExactValueBuildsLongPointExact() {
    CompiledQuery r = compile(
        new FieldNode("applicationLastEvaluationTimeEpochMs", new ExactValue("1784746240000")),
        ItemType.APPLICATION);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery().toString()).contains("applicationLastEvaluationTimeEpochMs");
    assertThat(r.luceneQuery().toString()).contains("[1784746240000 TO 1784746240000]");
  }

  @Test
  public void numericLongOpenHighRangeBuildsLongPointRange() {
    CompiledQuery r = compile(
        new FieldNode("applicationLastEvaluationTimeEpochMs", new RangeValue("1700000000000", "*", true, true)),
        ItemType.APPLICATION);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery().toString()).contains("[1700000000000 TO ");
    assertThat(r.luceneQuery().toString()).contains(String.valueOf(Long.MAX_VALUE));
  }

  @Test
  public void numericLongExclusiveRangeAdjustsBounds() {
    CompiledQuery r = compile(
        new FieldNode("applicationLastEvaluationTimeEpochMs", new RangeValue("100", "200", false, false)),
        ItemType.APPLICATION);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery().toString()).contains("[101 TO 199]");
  }

  @Test
  public void numericFloatExactValueBuildsFloatPointExact() {
    CompiledQuery r = compile(new FieldNode("vulnerabilitySeverity", new ExactValue("7.5")),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery().toString()).contains("vulnerabilitySeverity");
    assertThat(r.luceneQuery().toString()).contains("[7.5 TO 7.5]");
  }

  @Test
  public void numericFloatRangeInclusive() {
    CompiledQuery r = compile(new FieldNode("vulnerabilitySeverity", new RangeValue("7.0", "10.0", true, true)),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery().toString()).contains("[7.0 TO 10.0]");
  }

  @Test
  public void numericPrefixWarnsAndFallsBackToExact() {
    CompiledQuery r = compile(new FieldNode("policyThreatLevel", new PrefixValue("7")),
        ItemType.POLICY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("Prefix query on numeric filter");
    assertThat(r.luceneQuery().toString()).contains("[7 TO 7]");
  }

  @Test
  public void numericQuotedValueParsesAndWarns() {
    CompiledQuery r = compile(new FieldNode("policyThreatLevel", new PhraseValue("7")),
        ItemType.POLICY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("Quoted value on numeric filter");
    assertThat(r.luceneQuery().toString()).contains("[7 TO 7]");
  }

  @Test
  public void numericGarbageValueWarnsAndMatchesNothing() {
    CompiledQuery r = compile(new FieldNode("policyThreatLevel", new ExactValue("notanumber")),
        ItemType.POLICY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("is not a number");
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void numericRangeWithGarbageBoundsWarnsAndMatchesNothing() {
    CompiledQuery r = compile(new FieldNode("policyThreatLevel", new RangeValue("foo", "bar", true, true)),
        ItemType.POLICY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("Range bounds for filter");
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void numericInvertedRangeWarnsAndMatchesNothing() {
    // [9 TO 1] is inverted; IntPoint.newRangeQuery rejects lo > hi. The compiler must fail open
    // (warning + MatchNoDocs) rather than let the IllegalArgumentException escape as a 500.
    CompiledQuery r = compile(new FieldNode("policyThreatLevel", new RangeValue("9", "1", true, true)),
        ItemType.POLICY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("empty or inverted");
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void numericExclusiveRangeCollapsingToEmptyWarnsAndMatchesNothing() {
    // {5 TO 5} exclusive on both ends adjusts to lo=6, hi=4 (lo > hi) — empty range, must fail open.
    CompiledQuery r = compile(new FieldNode("policyThreatLevel", new RangeValue("5", "5", false, false)),
        ItemType.POLICY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("empty or inverted");
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void floatInvertedRangeWarnsAndMatchesNothing() {
    CompiledQuery r = compile(new FieldNode("vulnerabilitySeverity", new RangeValue("9.0", "1.0", true, true)),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("empty or inverted");
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  // ─────────────────────────── unknown fields ───────────────────────────

  @Test
  public void unknownFieldWarnsAndMatchesAll() {
    CompiledQuery r = compile(new FieldNode("bogusField", new ExactValue("x")));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).isEqualTo("Unknown filter \"bogusField\" — ignored.");
    assertThat(r.luceneQuery()).isInstanceOf(MatchAllDocsQuery.class);
  }

  @Test
  public void unknownFieldStillMatchesAllWithPhraseValue() {
    CompiledQuery r = compile(new FieldNode("madeUp", new PhraseValue("hello world")));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.luceneQuery()).isInstanceOf(MatchAllDocsQuery.class);
  }

  @Test
  public void unknownFieldWithRangeStillMatchesAll() {
    CompiledQuery r = compile(new FieldNode("madeUp", new RangeValue("1", "2", true, true)));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.luceneQuery()).isInstanceOf(MatchAllDocsQuery.class);
  }

  @Test
  public void unknownFieldNamesAreCaseSensitive() {
    // Lowercase "applicationname" is not a known filter — case sensitive.
    CompiledQuery r = compile(new FieldNode("applicationname", new ExactValue("x")));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("Unknown filter \"applicationname\"");
    assertThat(r.luceneQuery()).isInstanceOf(MatchAllDocsQuery.class);
  }

  @Test
  public void unknownFieldInAndClauseStillMatchesAllForThatClause() {
    // AND of an unknown filter and a known one: unknown becomes match-all,
    // known becomes its Term query; the AND is effectively the known query.
    CompiledQuery r = compile(new AndNode(List.of(
        new FieldNode("madeUp", new ExactValue("x")),
        new FieldNode("applicationName", new ExactValue("acme")))));
    assertThat(r.warnings()).hasSize(1);
    BooleanQuery bq = (BooleanQuery) r.luceneQuery();
    assertThat(bq.clauses()).hasSize(2);
    assertThat(bq.clauses()).allMatch(c -> c.getOccur() == Occur.MUST);
  }

  // ─────────────────────────── entity-scope no-op ───────────────────────────

  @Test
  public void vulnerabilityIdOnApplicationMatchesNothing() {
    CompiledQuery r = compile(new FieldNode("vulnerabilityId", new ExactValue("CVE-2024-1234")),
        ItemType.APPLICATION);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void applicationNameOnVulnerabilityMatchesTerm() {
    // applicationName was widened to SECURITY_VULNERABILITY (the field is indexed on vuln docs via
    // setOwner), so a vuln-scoped query now compiles to a real TermQuery instead of MatchNoDocs.
    CompiledQuery r = compile(new FieldNode("applicationName", new ExactValue("acme")),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("applicationName", "acme")));
  }

  @Test
  public void applicationIdOnVulnerabilityMatchesNothing() {
    // applicationId stays APPLICATION-only (not widened), so it still compiles to no docs.
    CompiledQuery r = compile(new FieldNode("applicationId", new ExactValue("acme-app-id")),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void componentEffectiveLicenseIdOnPolicyMatchesNothing() {
    CompiledQuery r = compile(new FieldNode("componentEffectiveLicenseId", new ExactValue("MIT")),
        ItemType.POLICY);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void policyFieldOnComponentMatchesNothing() {
    CompiledQuery r = compile(new FieldNode("policyName", new ExactValue("Copyleft Policy")),
        ItemType.NON_VULNERABLE_COMPONENT);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void sbomSpecificationOnComponentMatchesNothing() {
    CompiledQuery r = compile(new FieldNode("sbomSpecification", new ExactValue("SPDX")),
        ItemType.NON_VULNERABLE_COMPONENT);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isInstanceOf(MatchNoDocsQuery.class);
  }

  // ─────────────────────────── enum-value validation ───────────────────────────

  @Test
  public void bogusThreatCategoryWarnsButStillCompiles() {
    CompiledQuery r = compile(new FieldNode("policyThreatCategory", new ExactValue("bogus")),
        ItemType.POLICY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("Value \"bogus\" for filter \"policyThreatCategory\"");
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("policyThreatCategory", "bogus")));
  }

  @Test
  public void validThreatCategoryEmitsNoWarning() {
    CompiledQuery r = compile(new FieldNode("policyThreatCategory", new ExactValue("security")),
        ItemType.POLICY);
    assertThat(r.warnings()).isEmpty();
  }

  @Test
  public void bogusVulnerabilityStatusWarns() {
    CompiledQuery r = compile(new FieldNode("vulnerabilityStatus", new ExactValue("Pending")),
        ItemType.SECURITY_VULNERABILITY);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("vulnerabilityStatus");
  }

  @Test
  public void bogusSbomSpecificationWarns() {
    CompiledQuery r = compile(new FieldNode("sbomSpecification", new ExactValue("MyFormat")),
        ItemType.APPLICATION);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("sbomSpecification");
  }

  @Test
  public void bogusEvaluationStageWarns() {
    CompiledQuery r = compile(new FieldNode("policyEvaluationStage", new ExactValue("bogus")),
        ItemType.POLICY_VIOLATION);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("policyEvaluationStage");
  }

  @Test
  public void bogusWaiverStatusWarns() {
    CompiledQuery r = compile(new FieldNode("policyViolationWaiverStatus", new ExactValue("Draft")),
        ItemType.POLICY_VIOLATION);
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("policyViolationWaiverStatus");
  }

  // ─────────────────────────── empty value ───────────────────────────

  @Test
  public void emptyValueWarnsAndMatchesAll() {
    CompiledQuery r = compile(new FieldNode("applicationName", new EmptyValue()));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("has no value");
    assertThat(r.luceneQuery()).isInstanceOf(MatchAllDocsQuery.class);
  }

  // ─────────────────────────── boolean composition ───────────────────────────

  @Test
  public void andNodeProducesMustBooleanQuery() {
    CompiledQuery r = compile(new AndNode(List.of(
        new FieldNode("applicationName", new ExactValue("acme")),
        new FieldNode("applicationPublicId", new ExactValue("acme-app")))));
    assertThat(r.warnings()).isEmpty();
    BooleanQuery bq = (BooleanQuery) r.luceneQuery();
    assertThat(bq.clauses()).hasSize(2);
    assertThat(bq.clauses()).allMatch(c -> c.getOccur() == Occur.MUST);
  }

  @Test
  public void orNodeProducesShouldBooleanQueryWithMinimumOne() {
    CompiledQuery r = compile(new OrNode(List.of(
        new FieldNode("applicationName", new ExactValue("acme")),
        new FieldNode("applicationName", new ExactValue("widget")))));
    assertThat(r.warnings()).isEmpty();
    BooleanQuery bq = (BooleanQuery) r.luceneQuery();
    assertThat(bq.getMinimumNumberShouldMatch()).isEqualTo(1);
    assertThat(bq.clauses()).allMatch(c -> c.getOccur() == Occur.SHOULD);
  }

  @Test
  public void notNodeProducesMatchAllMustAndChildMustNot() {
    CompiledQuery r = compile(new NotNode(new FieldNode("applicationName", new ExactValue("acme"))));
    assertThat(r.warnings()).isEmpty();
    BooleanQuery bq = (BooleanQuery) r.luceneQuery();
    assertThat(bq.clauses()).hasSize(2);
    assertThat(bq.clauses().get(0).getOccur()).isEqualTo(Occur.MUST);
    assertThat(bq.clauses().get(0).getQuery()).isInstanceOf(MatchAllDocsQuery.class);
    assertThat(bq.clauses().get(1).getOccur()).isEqualTo(Occur.MUST_NOT);
    assertThat(bq.clauses().get(1).getQuery()).isEqualTo(new TermQuery(new Term("applicationName", "acme")));
  }

  @Test
  public void notOfUnknownFieldMatchesEverything() {
    // -unknownField:foo — the child fails open to MatchAllDocs ("ignore this clause"). Negating
    // "ignore" must remain "ignore", i.e. match everything permitted, not MatchNoDocs.
    CompiledQuery r = compile(new NotNode(new FieldNode("bogusField", new ExactValue("foo"))));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("Unknown filter");
    assertThat(r.luceneQuery()).isInstanceOf(MatchAllDocsQuery.class);
  }

  @Test
  public void notOfEmptyValueMatchesEverything() {
    // NOT emptyField: — the child (empty value) fails open to MatchAllDocs; negating it must still
    // match everything permitted rather than collapse to MatchNoDocs.
    CompiledQuery r = compile(new NotNode(new FieldNode("applicationName", new EmptyValue())));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("has no value");
    assertThat(r.luceneQuery()).isInstanceOf(MatchAllDocsQuery.class);
  }

  @Test
  public void nestedBooleanComposition() {
    // (a OR b) AND NOT c
    AstNode ast = new AndNode(List.of(
        new OrNode(List.of(
            new FieldNode("applicationName", new ExactValue("a")),
            new FieldNode("applicationName", new ExactValue("b")))),
        new NotNode(new FieldNode("applicationName", new ExactValue("c")))));
    CompiledQuery r = compile(ast);
    assertThat(r.warnings()).isEmpty();
    BooleanQuery outer = (BooleanQuery) r.luceneQuery();
    assertThat(outer.clauses()).hasSize(2);
    Query first = outer.clauses().get(0).getQuery();
    Query second = outer.clauses().get(1).getQuery();
    assertThat(first).isInstanceOf(BooleanQuery.class);
    assertThat(((BooleanQuery) first).getMinimumNumberShouldMatch()).isEqualTo(1);
    assertThat(second).isInstanceOf(BooleanQuery.class);
    assertThat(((BooleanQuery) second).clauses()).extracting(BooleanClause::getOccur)
        .containsExactly(Occur.MUST, Occur.MUST_NOT);
  }

  @Test
  public void impliedAndBetweenBareAndFieldTerm() {
    // "react itemType:APPLICATION" — parser wraps as AndNode; compiler should
    // produce the two-clause MUST query without warnings.
    AstNode ast = new AndNode(List.of(
        new TermNode("react"),
        new FieldNode("itemType", new ExactValue("APPLICATION"))));
    CompiledQuery r = compile(ast);
    assertThat(r.warnings()).isEmpty();
    BooleanQuery bq = (BooleanQuery) r.luceneQuery();
    assertThat(bq.clauses()).hasSize(2);
    assertThat(bq.clauses()).allMatch(c -> c.getOccur() == Occur.MUST);
  }

  // ─────────────────────────── item type discriminator ───────────────────────────

  @Test
  public void itemTypeExactValueWorksForApplicationEntity() {
    CompiledQuery r = compile(new FieldNode("itemType", new ExactValue("APPLICATION")));
    assertThat(r.warnings()).isEmpty();
    // itemType maps the user token to the index term (lowercased ItemType name). APPLICATION
    // resolves to "application", which is exactly what an application document indexes.
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("itemType", "application")));
  }

  @Test
  public void itemTypeComponentAliasMapsToIndexTerm() {
    // The user-facing COMPONENT token maps to the index discriminator non_vulnerable_component,
    // which is what component documents actually store; a naive lowercase would match nothing.
    CompiledQuery r = compile(new FieldNode("itemType", new ExactValue("COMPONENT")),
        ItemType.NON_VULNERABLE_COMPONENT);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("itemType", "non_vulnerable_component")));
  }

  @Test
  public void itemTypeRealDiscriminatorDoesNotWarn() {
    // NON_VULNERABLE_COMPONENT is a real ItemType; it must not trip a "not one of" warning
    // (the itemType field is validated by the entity-scope gate, not the enum allowlist).
    CompiledQuery r = compile(new FieldNode("itemType", new ExactValue("NON_VULNERABLE_COMPONENT")),
        ItemType.NON_VULNERABLE_COMPONENT);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("itemType", "non_vulnerable_component")));
  }

  @Test
  public void itemTypeUnknownValueWarnsAndMatchesNothing() {
    // An unrecognised discriminator warns (mirroring the other enum-field "not one of" warnings)
    // and resolves to its lowercased form, which matches no indexed discriminator.
    CompiledQuery r = compile(new FieldNode("itemType", new ExactValue("SPACESHIP")));
    assertThat(r.warnings()).hasSize(1);
    assertThat(r.warnings().get(0)).contains("Value \"SPACESHIP\" for filter \"itemType\" is not one of");
    assertThat(r.luceneQuery()).isEqualTo(new TermQuery(new Term("itemType", "spaceship")));
  }
}
