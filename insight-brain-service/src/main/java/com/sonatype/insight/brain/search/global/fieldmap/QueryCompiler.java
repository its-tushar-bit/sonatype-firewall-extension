/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.fieldmap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sonatype.insight.brain.search.global.parser.AstNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.AndNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.EmptyNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.FieldNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.NotNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.OrNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.PhraseNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.TermNode;
import com.sonatype.insight.brain.search.global.parser.FieldValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.EmptyValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.ExactValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.PhraseValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.PrefixValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.RangeValue;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_PUBLIC_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_ARTIFACT_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_GROUP_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_PACKAGE_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_VERSION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_FORMAT;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ITEM_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ORGANIZATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_EVALUATION_STAGE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_CONSTRAINT_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_ID;
import static com.sonatype.insight.brain.search.index.ItemType.APPLICATION;
import static com.sonatype.insight.brain.search.index.ItemType.APPLICATION_CATEGORY;
import static com.sonatype.insight.brain.search.index.ItemType.COMPONENT_LABEL;
import static com.sonatype.insight.brain.search.index.ItemType.LEGAL_VIOLATION;
import static com.sonatype.insight.brain.search.index.ItemType.NON_VULNERABLE_COMPONENT;
import static com.sonatype.insight.brain.search.index.ItemType.ORGANIZATION;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY_VIOLATION;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY_WAIVER;
import static com.sonatype.insight.brain.search.index.ItemType.SECURITY_VULNERABILITY;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

/**
 * Translates a parsed AST into a Lucene {@link Query} scoped to an {@link ItemType}. Unknown fields
 * and value-shape mismatches fail open with a surfaced warning rather than an exception.
 */
public final class QueryCompiler
{
  private QueryCompiler() {
  }

  /** Fields a bare token searches per entity type: a naked {@code q=terraform} hits any of these. */
  private static final Map<ItemType, List<String>> DEFAULT_SEARCH_FIELDS = defaultSearchFields();

  private static Map<ItemType, List<String>> defaultSearchFields() {
    Map<ItemType, List<String>> m = new EnumMap<>(ItemType.class);

    // Bare-term defaults are KEYWORD-kind fields only: a bare token compiles to a single-token
    // TermQuery/PrefixQuery, which effectively never matches an analyzer-tokenized TEXT field, so
    // *Description (TEXT) siblings are intentionally excluded and only the *Name KEYWORD ones kept.
    m.put(APPLICATION, List.of(
        APPLICATION_NAME.label,
        APPLICATION_PUBLIC_ID.label,
        ORGANIZATION_NAME.label,
        APPLICATION_CATEGORY_NAME.label));

    m.put(NON_VULNERABLE_COMPONENT, List.of(
        COMPONENT_NAME.label,
        COMPONENT_FORMAT.label,
        COMPONENT_COORDINATE_NAME.label,
        COMPONENT_COORDINATE_GROUP_ID.label,
        COMPONENT_COORDINATE_ARTIFACT_ID.label,
        COMPONENT_COORDINATE_PACKAGE_ID.label,
        COMPONENT_COORDINATE_VERSION.label,
        COMPONENT_EFFECTIVE_LICENSE_ID.label,
        COMPONENT_EFFECTIVE_LICENSE_NAME.label,
        COMPONENT_LABEL_NAME.label));

    m.put(SECURITY_VULNERABILITY, List.of(
        VULNERABILITY_ID.label,
        COMPONENT_NAME.label));

    List<String> violationFields = List.of(
        APPLICATION_NAME.label,
        APPLICATION_PUBLIC_ID.label,
        POLICY_NAME.label,
        COMPONENT_NAME.label,
        POLICY_EVALUATION_STAGE.label,
        POLICY_VIOLATION_CONSTRAINT_NAME.label);
    m.put(POLICY_VIOLATION, violationFields);
    m.put(LEGAL_VIOLATION, violationFields);

    m.put(POLICY, List.of(POLICY_NAME.label));

    m.put(POLICY_WAIVER, List.of(POLICY_WAIVER_POLICY_NAME.label, APPLICATION_NAME.label, APPLICATION_PUBLIC_ID.label));

    m.put(ORGANIZATION, List.of(ORGANIZATION_NAME.label));

    m.put(APPLICATION_CATEGORY, List.of(APPLICATION_CATEGORY_NAME.label));

    m.put(COMPONENT_LABEL, List.of(COMPONENT_LABEL_NAME.label));

    // SBOM_METADATA intentionally has no bare-term default fields yet: bare q=foo does not search
    // SBOM metadata in this engine. Fielded filters still work; add fields here when supported.

    return Map.copyOf(m);
  }

  public static CompiledQuery compile(AstNode ast, ItemType entityType, FieldMap fieldMap) {
    List<String> warnings = new ArrayList<>();
    Query q = compileNode(ast, entityType, fieldMap, warnings);
    return new CompiledQuery(q, warnings);
  }

  private static Query compileNode(AstNode ast, ItemType entityType, FieldMap fieldMap, List<String> warnings) {
    if (ast instanceof EmptyNode) {
      return new MatchAllDocsQuery();
    }
    if (ast instanceof TermNode term) {
      return compileBareTerm(term.value(), entityType, false);
    }
    if (ast instanceof PhraseNode phrase) {
      return compileBareTerm(phrase.value(), entityType, true);
    }
    if (ast instanceof FieldNode field) {
      return compileField(field, entityType, fieldMap, warnings);
    }
    if (ast instanceof AndNode and) {
      return compileBoolean(and.children(), MUST, entityType, fieldMap, warnings);
    }
    if (ast instanceof OrNode or) {
      BooleanQuery.Builder b = new BooleanQuery.Builder();
      for (AstNode child : or.children()) {
        b.add(compileNode(child, entityType, fieldMap, warnings), SHOULD);
      }
      b.setMinimumNumberShouldMatch(1);
      return b.build();
    }
    if (ast instanceof NotNode not) {
      Query child = compileNode(not.child(), entityType, fieldMap, warnings);
      // A fail-open child (unknown field / empty value) compiled to MatchAllDocsQuery meaning
      // "ignore this clause". Negating "ignore" is still "ignore", so short-circuit to match-all
      // rather than MatchAllDocs MUST + MatchAllDocs MUST_NOT (which would match nothing and
      // invert the fail-open contract). The outer type FILTER still constrains the result set.
      if (child instanceof MatchAllDocsQuery) {
        return new MatchAllDocsQuery();
      }
      return new BooleanQuery.Builder()
          .add(new MatchAllDocsQuery(), MUST)
          .add(child, MUST_NOT)
          .build();
    }
    throw new IllegalStateException("Unhandled AST node: " + ast.getClass().getSimpleName());
  }

  private static Query compileBoolean(
      List<AstNode> children,
      Occur occur,
      ItemType entityType,
      FieldMap fieldMap,
      List<String> warnings)
  {
    BooleanQuery.Builder b = new BooleanQuery.Builder();
    for (AstNode child : children) {
      b.add(compileNode(child, entityType, fieldMap, warnings), occur);
    }
    return b.build();
  }

  /** Below this the typeahead prefix expands too broadly to be worth adding. */
  private static final int MIN_PREFIX_LENGTH = 2;

  /** Truncate the typeahead prefix here to bound worst-case Lucene expansion. */
  private static final int MAX_PREFIX_LENGTH = 20;

  private static Query compileBareTerm(String rawValue, ItemType entityType, boolean phrase) {
    List<String> fields = DEFAULT_SEARCH_FIELDS.getOrDefault(entityType, List.of());
    if (fields.isEmpty() || StringUtils.isEmpty(rawValue)) {
      return new MatchNoDocsQuery();
    }
    String value = rawValue.toLowerCase(Locale.ROOT);
    BooleanQuery.Builder b = new BooleanQuery.Builder();
    for (String field : fields) {
      b.add(bareTermQuery(field, value, phrase), SHOULD);
    }
    b.setMinimumNumberShouldMatch(1);
    return b.build();
  }

  /**
   * Bare-term match against one field. Non-phrase input ORs a {@link TermQuery} (exact whole-value
   * on keyword-analyzed fields) with a {@link PrefixQuery} (typeahead, e.g. {@code widget} matching
   * the single keyword token {@code widget co}).
   */
  private static Query bareTermQuery(String field, String lowercasedValue, boolean phrase) {
    if (phrase) {
      // Single token -> TermQuery (matches exact-whole-value on keyword fields); multi-word ->
      // PhraseQuery (needed for StandardAnalyzer-tokenized fields). Both index shapes accept both.
      String[] tokens = lowercasedValue.trim().split("\\s+");
      if (tokens.length == 1) {
        return new TermQuery(new Term(field, tokens[0]));
      }
      PhraseQuery.Builder pb = new PhraseQuery.Builder();
      for (String t : tokens) {
        if (!t.isEmpty()) {
          pb.add(new Term(field, t));
        }
      }
      return pb.build();
    }
    BooleanQuery.Builder or = new BooleanQuery.Builder();
    or.add(new TermQuery(new Term(field, lowercasedValue)), SHOULD);
    if (lowercasedValue.length() >= MIN_PREFIX_LENGTH) {
      String prefix = lowercasedValue.length() > MAX_PREFIX_LENGTH
          ? lowercasedValue.substring(0, MAX_PREFIX_LENGTH)
          : lowercasedValue;
      or.add(new PrefixQuery(new Term(field, prefix)), SHOULD);
    }
    or.setMinimumNumberShouldMatch(1);
    return or.build();
  }

  private static Query compileField(FieldNode node, ItemType entityType, FieldMap fieldMap, List<String> warnings) {
    String fieldName = node.field();
    FieldValue value = node.value();

    if (!fieldMap.isKnown(fieldName)) {
      warnings.add("Unknown filter \"" + fieldName + "\" — ignored.");
      return new MatchAllDocsQuery();
    }
    FieldEntry entry = fieldMap.lookup(fieldName).orElseThrow();

    if (!entry.allowedTypes().contains(entityType)) {
      return new MatchNoDocsQuery();
    }

    if (value instanceof EmptyValue) {
      warnings.add("Filter \"" + fieldName + "\" has no value — ignored.");
      return new MatchAllDocsQuery();
    }

    // The itemType discriminator is validated by the entity-scope gate (allowedTypes) and the
    // caller's tab/allowedTypes set, not by the enum-value allowlist, so any real ItemType token
    // (e.g. NON_VULNERABLE_COMPONENT) is accepted without a spurious "not one of" warning and the
    // user-facing alias (COMPONENT) is mapped to the index term in compileKeyword.
    boolean isItemType = ITEM_TYPE.label.equals(entry.label());
    if (!isItemType && entry.enumValues() != null) {
      String literal = literalForValue(value);
      if (literal != null && entry.enumValues().stream().noneMatch(v -> v.equalsIgnoreCase(literal))) {
        warnings.add("Value \"" + literal + "\" for filter \"" + fieldName + "\" is not one of "
            + entry.enumValues() + ".");
      }
    }

    switch (entry.kind()) {
      case KEYWORD:
        return compileKeyword(entry, value, fieldName, warnings);
      case TEXT:
        return compileText(entry, value, fieldName, warnings);
      case NUMERIC:
        return compileNumeric(entry, value, fieldName, warnings);
      default:
        throw new IllegalStateException("Unknown FieldKind: " + entry.kind());
    }
  }

  /**
   * Resolve a user-facing itemType token to the term stored in the index. Documents index the
   * lowercased {@link ItemType} name (via {@link ItemType#searchFieldName()}); the user-facing
   * {@code COMPONENT} alias maps to {@code NON_VULNERABLE_COMPONENT}. Unrecognised tokens fall
   * through as their lowercased form (matching nothing) with a surfaced warning, consistent with
   * the fail-open contract and the enum-value warnings on other fields.
   */
  private static String resolveItemTypeTerm(String literal, String fieldName, List<String> warnings) {
    String upper = literal.toUpperCase(Locale.ROOT);
    if ("COMPONENT".equals(upper)) {
      return NON_VULNERABLE_COMPONENT.searchFieldName();
    }
    for (ItemType t : ItemType.values()) {
      if (t.name().equals(upper)) {
        return t.searchFieldName();
      }
    }
    warnings.add("Value \"" + literal + "\" for filter \"" + fieldName + "\" is not one of "
        + itemTypeValues() + ".");
    return literal.toLowerCase(Locale.ROOT);
  }

  private static List<String> itemTypeValues() {
    List<String> values = new ArrayList<>();
    values.add("COMPONENT");
    for (ItemType t : ItemType.values()) {
      values.add(t.name());
    }
    return values;
  }

  private static String literalForValue(FieldValue v) {
    if (v instanceof ExactValue e) {
      return e.value();
    }
    if (v instanceof PhraseValue p) {
      return p.value();
    }
    if (v instanceof PrefixValue p) {
      return p.prefix();
    }
    return null;
  }

  private static Query compileKeyword(FieldEntry entry, FieldValue value, String fieldName, List<String> warnings) {
    String label = entry.label();
    if (ITEM_TYPE.label.equals(label)) {
      String literal = literalForValue(value);
      if (literal != null) {
        return new TermQuery(new Term(label, resolveItemTypeTerm(literal, fieldName, warnings)));
      }
      return new MatchAllDocsQuery();
    }
    if (value instanceof ExactValue e) {
      return new TermQuery(new Term(label, e.value().toLowerCase(Locale.ROOT)));
    }
    if (value instanceof PhraseValue p) {
      return new TermQuery(new Term(label, p.value().toLowerCase(Locale.ROOT)));
    }
    if (value instanceof PrefixValue p) {
      return boundedPrefixQuery(label, p.prefix(), fieldName, warnings);
    }
    if (value instanceof RangeValue r) {
      warnings.add("Range query on non-numeric filter \"" + fieldName + "\" — falling back to bare text match.");
      return bareTextFallback(label, r);
    }
    return new MatchAllDocsQuery();
  }

  /**
   * Fielded prefix query with the same min-length floor and max-length cap as bare typeahead terms,
   * to bound worst-case Lucene term expansion (CPU DoS). A prefix shorter than the floor (including
   * the empty {@code field:*} wildcard) matches nothing rather than scanning every term in the field.
   */
  private static Query boundedPrefixQuery(
      String label,
      String rawPrefix,
      String fieldName,
      List<String> warnings)
  {
    String prefix = rawPrefix.toLowerCase(Locale.ROOT);
    if (prefix.length() < MIN_PREFIX_LENGTH) {
      warnings.add("Prefix for filter \"" + fieldName + "\" is too short (min " + MIN_PREFIX_LENGTH
          + " characters) — ignored.");
      return new MatchNoDocsQuery();
    }
    if (prefix.length() > MAX_PREFIX_LENGTH) {
      prefix = prefix.substring(0, MAX_PREFIX_LENGTH);
    }
    return new PrefixQuery(new Term(label, prefix));
  }

  private static Query compileText(FieldEntry entry, FieldValue value, String fieldName, List<String> warnings) {
    String label = entry.label();
    if (value instanceof ExactValue e) {
      return new TermQuery(new Term(label, e.value().toLowerCase(Locale.ROOT)));
    }
    if (value instanceof PhraseValue p) {
      String[] tokens = p.value().toLowerCase(Locale.ROOT).trim().split("\\s+");
      if (tokens.length == 1) {
        return new TermQuery(new Term(label, tokens[0]));
      }
      PhraseQuery.Builder pb = new PhraseQuery.Builder();
      for (String t : tokens) {
        if (!t.isEmpty()) {
          pb.add(new Term(label, t));
        }
      }
      return pb.build();
    }
    if (value instanceof PrefixValue p) {
      return boundedPrefixQuery(label, p.prefix(), fieldName, warnings);
    }
    if (value instanceof RangeValue r) {
      warnings.add("Range query on non-numeric filter \"" + fieldName + "\" — falling back to bare text match.");
      return bareTextFallback(label, r);
    }
    return new MatchAllDocsQuery();
  }

  private static Query compileNumeric(FieldEntry entry, FieldValue value, String fieldName, List<String> warnings) {
    String label = entry.label();
    boolean isFloat = entry.numericType() == Float.class;

    if (value instanceof ExactValue e) {
      return exactNumeric(label, e.value(), isFloat, fieldName, warnings);
    }
    if (value instanceof PhraseValue p) {
      warnings.add("Quoted value on numeric filter \"" + fieldName + "\" — attempting to parse as a number.");
      return exactNumeric(label, p.value(), isFloat, fieldName, warnings);
    }
    if (value instanceof PrefixValue p) {
      warnings.add("Prefix query on numeric filter \"" + fieldName
          + "\" is not supported — falling back to exact-value match on \"" + p.prefix() + "\".");
      return exactNumeric(label, p.prefix(), isFloat, fieldName, warnings);
    }
    if (value instanceof RangeValue r) {
      return rangeNumeric(label, r, isFloat, fieldName, warnings);
    }
    return new MatchAllDocsQuery();
  }

  private static Query exactNumeric(
      String label,
      String raw,
      boolean isFloat,
      String fieldName,
      List<String> warnings)
  {
    try {
      if (isFloat) {
        return FloatPoint.newExactQuery(label, Float.parseFloat(raw));
      }
      return IntPoint.newExactQuery(label, Integer.parseInt(raw));
    }
    catch (NumberFormatException nfe) {
      warnings.add("Value \"" + raw + "\" for numeric filter \"" + fieldName + "\" is not a number — ignored.");
      return new MatchNoDocsQuery();
    }
  }

  private static Query rangeNumeric(
      String label,
      RangeValue r,
      boolean isFloat,
      String fieldName,
      List<String> warnings)
  {
    try {
      if (isFloat) {
        float lo = parseFloatBound(r.lo(), true);
        float hi = parseFloatBound(r.hi(), false);
        if (!r.loInclusive() && lo != Float.NEGATIVE_INFINITY) {
          lo = FloatPoint.nextUp(lo);
        }
        if (!r.hiInclusive() && hi != Float.POSITIVE_INFINITY) {
          hi = FloatPoint.nextDown(hi);
        }
        if (lo > hi) {
          warnings.add("Range for filter \"" + fieldName + "\" is empty or inverted — ignored.");
          return new MatchNoDocsQuery();
        }
        return FloatPoint.newRangeQuery(label, lo, hi);
      }
      int lo = parseIntBound(r.lo(), true);
      int hi = parseIntBound(r.hi(), false);
      if (!r.loInclusive() && lo != Integer.MIN_VALUE) {
        lo = Math.addExact(lo, 1);
      }
      if (!r.hiInclusive() && hi != Integer.MAX_VALUE) {
        hi = Math.subtractExact(hi, 1);
      }
      if (lo > hi) {
        warnings.add("Range for filter \"" + fieldName + "\" is empty or inverted — ignored.");
        return new MatchNoDocsQuery();
      }
      return IntPoint.newRangeQuery(label, lo, hi);
    }
    catch (NumberFormatException | ArithmeticException e) {
      warnings.add("Range bounds for filter \"" + fieldName + "\" are not valid numbers — ignored.");
      return new MatchNoDocsQuery();
    }
    catch (IllegalArgumentException e) {
      // Defense in depth: some point types / Lucene versions reject an inverted range (lo > hi)
      // from newRangeQuery rather than yielding an empty match. Fail open per the query contract
      // rather than escaping as a 500. (The explicit lo > hi guard above already handles the
      // int/float paths on the current Lucene, which returns an empty range instead of throwing.)
      warnings.add("Range for filter \"" + fieldName + "\" is empty or inverted — ignored.");
      return new MatchNoDocsQuery();
    }
  }

  private static int parseIntBound(String bound, boolean isLow) {
    if (bound == null || bound.equals("*")) {
      return isLow ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    }
    return Integer.parseInt(bound);
  }

  private static float parseFloatBound(String bound, boolean isLow) {
    if (bound == null || bound.equals("*")) {
      return isLow ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
    }
    return Float.parseFloat(bound);
  }

  private static Query bareTextFallback(String label, RangeValue r) {
    // Range on a keyword field: probe with the first non-open bound; "* TO *" collapses to match-all.
    String probe = null;
    if (r.lo() != null && !r.lo().equals("*")) {
      probe = r.lo();
    }
    else if (r.hi() != null && !r.hi().equals("*")) {
      probe = r.hi();
    }
    if (probe == null) {
      return new MatchAllDocsQuery();
    }
    return new TermQuery(new Term(label, probe.toLowerCase(Locale.ROOT)));
  }
}
