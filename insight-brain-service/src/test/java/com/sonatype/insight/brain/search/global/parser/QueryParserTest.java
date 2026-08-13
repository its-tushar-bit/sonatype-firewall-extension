/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.parser;

import java.util.List;

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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryParserTest
{
  // ─────────────────────────── happy-path per operator ───────────────────────────

  @Test
  public void bareTerm() {
    ParsedQuery r = QueryParser.parse("log4j");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new TermNode("log4j"));
  }

  @Test
  public void quotedPhraseAtTopLevel() {
    ParsedQuery r = QueryParser.parse("\"log4j core\"");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new PhraseNode("log4j core"));
  }

  @Test
  public void fieldExactValue() {
    ParsedQuery r = QueryParser.parse("applicationName:acme");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new FieldNode("applicationName", new ExactValue("acme")));
  }

  @Test
  public void fieldPhraseValue() {
    ParsedQuery r = QueryParser.parse("applicationName:\"acme corp\"");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new FieldNode("applicationName", new PhraseValue("acme corp")));
  }

  @Test
  public void fieldPrefixValue() {
    ParsedQuery r = QueryParser.parse("componentName:log4j*");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new FieldNode("componentName", new PrefixValue("log4j")));
  }

  @Test
  public void fieldInclusiveRange() {
    ParsedQuery r = QueryParser.parse("policyThreatLevel:[7 TO 10]");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(
        new FieldNode("policyThreatLevel", new RangeValue("7", "10", true, true)));
  }

  @Test
  public void fieldExclusiveRange() {
    ParsedQuery r = QueryParser.parse("vulnerabilitySeverity:{7 TO 10}");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(
        new FieldNode("vulnerabilitySeverity", new RangeValue("7", "10", false, false)));
  }

  @Test
  public void implicitAndBetweenTerms() {
    ParsedQuery r = QueryParser.parse("react itemType:APPLICATION");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new TermNode("react"),
        new FieldNode("itemType", new ExactValue("APPLICATION")))));
  }

  @Test
  public void explicitOr() {
    ParsedQuery r = QueryParser.parse("applicationName:foo OR applicationName:bar");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new OrNode(List.of(
        new FieldNode("applicationName", new ExactValue("foo")),
        new FieldNode("applicationName", new ExactValue("bar")))));
  }

  @Test
  public void notWord() {
    ParsedQuery r = QueryParser.parse("NOT foo");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new NotNode(new TermNode("foo")));
  }

  @Test
  public void minusPrefix() {
    ParsedQuery r = QueryParser.parse("react -itemType:COMPONENT");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new TermNode("react"),
        new NotNode(new FieldNode("itemType", new ExactValue("COMPONENT"))))));
  }

  @Test
  public void grouping() {
    ParsedQuery r = QueryParser.parse("(itemType:APPLICATION OR itemType:COMPONENT) AND react");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new OrNode(List.of(
            new FieldNode("itemType", new ExactValue("APPLICATION")),
            new FieldNode("itemType", new ExactValue("COMPONENT")))),
        new TermNode("react"))));
  }

  // ─────────────────────────── malformed variants ───────────────────────────

  @Test
  public void unclosedQuoteAtTopLevel() {
    ParsedQuery r = QueryParser.parse("\"unclosed");
    assertThat(r.ast()).isEqualTo(new PhraseNode("unclosed"));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Unclosed quote"));
  }

  @Test
  public void unclosedQuoteInFieldValue() {
    ParsedQuery r = QueryParser.parse("applicationName:\"unclosed");
    assertThat(r.ast()).isEqualTo(new FieldNode("applicationName", new PhraseValue("unclosed")));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Unclosed quote"));
  }

  @Test
  public void unclosedInclusiveRange() {
    ParsedQuery r = QueryParser.parse("policyThreatLevel:[7 TO 10");
    // Bound values are present so we keep it as a range but warn about the missing bracket.
    assertThat(r.ast()).isEqualTo(
        new FieldNode("policyThreatLevel", new RangeValue("7", "10", true, true)));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Unclosed range bracket"));
  }

  @Test
  public void unclosedParenthesis() {
    ParsedQuery r = QueryParser.parse("(a OR b");
    assertThat(r.ast()).isEqualTo(new OrNode(List.of(new TermNode("a"), new TermNode("b"))));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Unclosed parenthesis"));
  }

  @Test
  public void trailingFieldColonEmitsEmptyValue() {
    ParsedQuery r = QueryParser.parse("applicationName:");
    assertThat(r.ast()).isEqualTo(new FieldNode("applicationName", new EmptyValue()));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("has no value"));
  }

  @Test
  public void bareColonIsLiteralWithWarning() {
    ParsedQuery r = QueryParser.parse(": foo");
    // Bare `:` becomes a literal term. `foo` follows as a bare term.
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(new TermNode(":"), new TermNode("foo"))));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Bare ':'"));
  }

  @Test
  public void digitLeadingBareTermStillLexes() {
    // A bare token starting with a digit (version / year search) is a plain term, not dropped.
    ParsedQuery r = QueryParser.parse("2021");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new TermNode("2021"));
  }

  @Test
  public void digitLeadingFieldNameIsTreatedAsBareTermWithWarning() {
    // Field names must not start with a digit; `123:foo` is not a filter. The digit-leading token
    // becomes a bare term (`foo` follows as its own bare term) and a warning is surfaced.
    ParsedQuery r = QueryParser.parse("123:foo");
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(new TermNode("123"), new TermNode("foo"))));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("must not start with a digit"));
  }

  @Test
  public void malformedRangeMissingUpperBound() {
    ParsedQuery r = QueryParser.parse("policyThreatLevel:[7 TO]");
    // Range degrades to bare terms.
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new TermNode("policyThreatLevel"),
        new TermNode("7"))));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Malformed range"));
  }

  @Test
  public void malformedRangeMissingLowerBound() {
    ParsedQuery r = QueryParser.parse("policyThreatLevel:[TO 10]");
    // Bare "TO" appears at the low bound — since we're not yet past the
    // opening bracket, the lexer emits it as an IDENT "TO" (the range
    // parser then can't find a delimiter and degrades).
    assertThat(r.ast()).isInstanceOf(AndNode.class);
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Malformed range"));
  }

  @Test
  public void danglingNotOperatorWarnsAndBecomesEmpty() {
    ParsedQuery r = QueryParser.parse("NOT");
    assertThat(r.ast()).isEqualTo(new EmptyNode());
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Dangling NOT"));
  }

  @Test
  public void nestedNotOperatorsCollapse() {
    ParsedQuery r = QueryParser.parse("NOT NOT NOT foo");
    // Odd count → single NOT.
    assertThat(r.ast()).isEqualTo(new NotNode(new TermNode("foo")));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Nested NOT operators collapsed"));
  }

  @Test
  public void doubleNotCancels() {
    ParsedQuery r = QueryParser.parse("NOT NOT foo");
    assertThat(r.ast()).isEqualTo(new TermNode("foo"));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Nested NOT operators collapsed"));
  }

  // ─────────────────────────── precedence ───────────────────────────

  @Test
  public void andBindsTighterThanOr() {
    // a AND b OR c → (a AND b) OR c
    ParsedQuery r = QueryParser.parse("a AND b OR c");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new OrNode(List.of(
        new AndNode(List.of(new TermNode("a"), new TermNode("b"))),
        new TermNode("c"))));
  }

  @Test
  public void orIsNotAssociativeAcrossAnd() {
    // a OR b AND c → a OR (b AND c)
    ParsedQuery r = QueryParser.parse("a OR b AND c");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new OrNode(List.of(
        new TermNode("a"),
        new AndNode(List.of(new TermNode("b"), new TermNode("c"))))));
  }

  @Test
  public void notBindsTighterThanOr() {
    // NOT a OR b → (NOT a) OR b
    ParsedQuery r = QueryParser.parse("NOT a OR b");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new OrNode(List.of(
        new NotNode(new TermNode("a")),
        new TermNode("b"))));
  }

  @Test
  public void notBindsTighterThanAnd() {
    // a AND NOT b → a AND (NOT b)
    ParsedQuery r = QueryParser.parse("a AND NOT b");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new TermNode("a"),
        new NotNode(new TermNode("b")))));
  }

  @Test
  public void chainedOrProducesFlatList() {
    ParsedQuery r = QueryParser.parse("a OR b OR c");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new OrNode(List.of(
        new TermNode("a"), new TermNode("b"), new TermNode("c"))));
  }

  // ─────────────────────────── grouping ───────────────────────────

  @Test
  public void groupingOverridesPrecedence() {
    // (a OR b) AND c
    ParsedQuery r = QueryParser.parse("(a OR b) AND c");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new OrNode(List.of(new TermNode("a"), new TermNode("b"))),
        new TermNode("c"))));
  }

  @Test
  public void nestedGroups() {
    ParsedQuery r = QueryParser.parse("((a OR b) AND (c OR d))");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new OrNode(List.of(new TermNode("a"), new TermNode("b"))),
        new OrNode(List.of(new TermNode("c"), new TermNode("d"))))));
  }

  @Test
  public void emptyGroupProducesEmptyNode() {
    ParsedQuery r = QueryParser.parse("()");
    assertThat(r.ast()).isEqualTo(new EmptyNode());
  }

  @Test
  public void groupAroundSingleTermSimplifies() {
    ParsedQuery r = QueryParser.parse("(foo)");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new TermNode("foo"));
  }

  @Test
  public void groupWithNotInside() {
    ParsedQuery r = QueryParser.parse("(NOT foo OR bar)");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new OrNode(List.of(
        new NotNode(new TermNode("foo")),
        new TermNode("bar"))));
  }

  // ─────────────────────────── tolerance for messy inputs ───────────────────────────

  @Test
  public void unclosedFieldPhrase() {
    ParsedQuery r = QueryParser.parse("applicationName:\"unclosed");
    assertThat(r.ast()).isEqualTo(new FieldNode("applicationName", new PhraseValue("unclosed")));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Unclosed quote"));
  }

  @Test
  public void unclosedRangeMissingCloser() {
    ParsedQuery r = QueryParser.parse("policyThreatLevel:[7 TO");
    // No closing bracket AND no high bound — malformed, degrades to bare
    // terms and warns.
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Malformed range"));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Unclosed range bracket"));
  }

  @Test
  public void trailingColon() {
    ParsedQuery r = QueryParser.parse("applicationName:");
    assertThat(r.ast()).isEqualTo(new FieldNode("applicationName", new EmptyValue()));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("has no value"));
  }

  @Test
  public void reservedWordsAsBareTerms() {
    // Uppercase AND/OR/NOT are reserved. "AND OR NOT" in isolation:
    // - starting AND: no left operand — first parseUnary sees AND, doesn't consume, so `parseAnd`
    // ends up with no children on the left; the outer parseOr grabs it, and each subsequent
    // `OR` produces a right operand.
    // Result depends on exact behavior; assert we don't throw and get a well-formed AST.
    ParsedQuery r = QueryParser.parse("AND OR NOT");
    assertThat(r.ast()).isNotNull();
  }

  @Test
  public void lowercaseAndOrNotAreBareTerms() {
    // Case-sensitive reserved words: `and`, `or`, `not` should be terms.
    ParsedQuery r = QueryParser.parse("and or not");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new TermNode("and"),
        new TermNode("or"),
        new TermNode("not"))));
  }

  @Test
  public void allWhitespaceIsEmpty() {
    ParsedQuery r = QueryParser.parse("   \t\n  ");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new EmptyNode());
  }

  @Test
  public void unknownReservedWordLikeTokensAreTerms() {
    ParsedQuery r = QueryParser.parse("XOR NAND");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new TermNode("XOR"), new TermNode("NAND"))));
  }

  @Test
  public void tabAndNewlineWhitespaceAreTreatedAsSpace() {
    ParsedQuery r = QueryParser.parse("foo\tbar\nbaz");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new TermNode("foo"), new TermNode("bar"), new TermNode("baz"))));
  }

  // ─────────────────────────── empty / null input ───────────────────────────

  @Test
  public void emptyStringIsEmpty() {
    ParsedQuery r = QueryParser.parse("");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new EmptyNode());
  }

  @Test
  public void nullInputIsEmpty() {
    ParsedQuery r = QueryParser.parse(null);
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new EmptyNode());
  }

  // ─────────────────────────── stress / unicode ───────────────────────────

  @Test
  public void longInputDoesNotStackOverflow() {
    StringBuilder q = new StringBuilder();
    for (int k = 0; k < 200; k++) {
      if (k > 0)
        q.append(" OR ");
      q.append("term").append(k);
    }
    ParsedQuery r = QueryParser.parse(q.toString());
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isInstanceOf(OrNode.class);
    assertThat(((OrNode) r.ast()).children()).hasSize(200);
  }

  @Test
  public void moderatelyNestedGroupsParseNormally() {
    // 10 layers of `(…)` around a single term stays well under the nesting cap.
    StringBuilder q = new StringBuilder();
    for (int k = 0; k < 10; k++)
      q.append('(');
    q.append("foo");
    for (int k = 0; k < 10; k++)
      q.append(')');
    ParsedQuery r = QueryParser.parse(q.toString());
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new TermNode("foo"));
  }

  @Test
  public void deeplyNestedGroupsDoNotStackOverflow() {
    // Pathological nesting (10,000 layers) must not throw; the parser caps descent and warns
    // instead of recursing, so the StackOverflowError (an Error, not Exception) cannot escape.
    StringBuilder q = new StringBuilder();
    for (int k = 0; k < 10_000; k++)
      q.append('(');
    q.append("foo");
    for (int k = 0; k < 10_000; k++)
      q.append(')');
    ParsedQuery r = QueryParser.parse(q.toString());
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("nesting exceeds"));
  }

  @Test
  public void overLengthQueryTruncatesWithWarningWithoutThrowing() {
    // An extremely long raw query is truncated (not thrown) before parsing so it cannot produce a
    // huge token list or deep AST. The truncated head still parses normally.
    int over = 5000;
    StringBuilder q = new StringBuilder("log4j ");
    while (q.length() < over) {
      q.append('(');
    }
    ParsedQuery r = QueryParser.parse(q.toString());
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("maximum length"));
    assertThat(r.ast()).isEqualTo(new TermNode("log4j"));
  }

  @Test
  public void unicodeInsideQuotedPhrase() {
    ParsedQuery r = QueryParser.parse("applicationName:\"café \u00E9toile\"");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(
        new FieldNode("applicationName", new PhraseValue("café \u00E9toile")));
  }

  @Test
  public void unicodeAsBareTerm() {
    ParsedQuery r = QueryParser.parse("café");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new TermNode("café"));
  }

  // ─────────────────────────── warnings — precise text ───────────────────────────

  @Test
  public void warnsWithPositionForUnclosedQuote() {
    ParsedQuery r = QueryParser.parse("foo \"unclosed");
    assertThat(r.warnings()).anySatisfy(w -> {
      assertThat(w).contains("Unclosed quote");
      assertThat(w).contains("position 4");
    });
  }

  @Test
  public void warnsWithFieldNameForEmptyValue() {
    ParsedQuery r = QueryParser.parse("applicationName:");
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("\"applicationName\""));
  }

  @Test
  public void warnsForMalformedRangeWithFieldName() {
    ParsedQuery r = QueryParser.parse("policyThreatLevel:[7 TO]");
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("\"policyThreatLevel\""));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("bare terms"));
  }

  @Test
  public void warnsForBareColon() {
    ParsedQuery r = QueryParser.parse(":");
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("Bare ':'"));
    assertThat(r.warnings()).anySatisfy(w -> assertThat(w).contains("position 0"));
  }

  @Test
  public void warnsForDanglingNotWithPosition() {
    ParsedQuery r = QueryParser.parse("foo NOT");
    assertThat(r.warnings()).anySatisfy(w -> {
      assertThat(w).contains("Dangling NOT");
      assertThat(w).contains("position 4");
    });
  }

  @Test
  public void warnsForUnclosedParenWithPosition() {
    ParsedQuery r = QueryParser.parse("(foo");
    assertThat(r.warnings()).anySatisfy(w -> {
      assertThat(w).contains("Unclosed parenthesis");
      assertThat(w).contains("position 0");
    });
  }

  // ─────────────────────────── mixed real-world queries ───────────────────────────

  @Test
  public void terraformItemTypeApplication() {
    ParsedQuery r = QueryParser.parse("terraform itemType:APPLICATION");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new TermNode("terraform"),
        new FieldNode("itemType", new ExactValue("APPLICATION")))));
  }

  @Test
  public void openLoRangeWithStar() {
    ParsedQuery r = QueryParser.parse("policyThreatLevel:[* TO 10]");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(
        new FieldNode("policyThreatLevel", new RangeValue(null, "10", true, true)));
  }

  @Test
  public void openHiRangeWithStar() {
    ParsedQuery r = QueryParser.parse("policyThreatLevel:[7 TO *]");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(
        new FieldNode("policyThreatLevel", new RangeValue("7", null, true, true)));
  }

  @Test
  public void fieldNamesArePreservedCaseSensitive() {
    ParsedQuery r = QueryParser.parse("ApplicationName:acme");
    assertThat(r.ast()).isEqualTo(new FieldNode("ApplicationName", new ExactValue("acme")));
  }

  @Test
  public void adjacentTermsInsideAndOutsideGroup() {
    ParsedQuery r = QueryParser.parse("a (b c) d");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new TermNode("a"),
        new AndNode(List.of(new TermNode("b"), new TermNode("c"))),
        new TermNode("d"))));
  }

  @Test
  public void prefixWithoutValueIsLoneStar() {
    ParsedQuery r = QueryParser.parse("componentName:*");
    assertThat(r.ast()).isEqualTo(new FieldNode("componentName", new PrefixValue("")));
  }

  @Test
  public void chainedFieldPredicates() {
    ParsedQuery r = QueryParser.parse("itemType:APPLICATION applicationName:\"acme corp\" -organizationName:legacy");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.ast()).isEqualTo(new AndNode(List.of(
        new FieldNode("itemType", new ExactValue("APPLICATION")),
        new FieldNode("applicationName", new PhraseValue("acme corp")),
        new NotNode(new FieldNode("organizationName", new ExactValue("legacy"))))));
  }

  @Test
  public void mixedInclusiveExclusiveRangeSyntaxTolerated() {
    // `[7 TO 10}` mixes brackets — accept and encode as loInclusive=true, hiInclusive=false.
    ParsedQuery r = QueryParser.parse("policyThreatLevel:[7 TO 10}");
    assertThat(r.ast()).isEqualTo(
        new FieldNode("policyThreatLevel", new RangeValue("7", "10", true, false)));
  }
}
