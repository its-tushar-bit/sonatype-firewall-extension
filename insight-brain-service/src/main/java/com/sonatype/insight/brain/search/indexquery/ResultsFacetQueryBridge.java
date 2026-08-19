/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sonatype.insight.brain.search.global.fieldmap.FieldEntry;
import com.sonatype.insight.brain.search.global.fieldmap.FieldKind;
import com.sonatype.insight.brain.search.global.fieldmap.FieldMap;
import com.sonatype.insight.brain.search.global.parser.AstNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.AndNode;
import com.sonatype.insight.brain.search.global.parser.AstNode.FieldNode;
import com.sonatype.insight.brain.search.global.parser.FieldValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.ExactValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.PhraseValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.PrefixValue;
import com.sonatype.insight.brain.search.global.parser.FieldValue.RangeValue;
import com.sonatype.insight.brain.search.global.parser.QueryParser;
import com.sonatype.insight.brain.search.index.ItemType;

/**
 * Translates the Global-Search {@code /results} {@code q=} string into the
 * {@link IndexQueryFilterCompiler.CompiledQuery} shape the index-query facet engine
 * ({@link IndexQueryService#computeFacets}) already understands, so {@code /results} can reuse the
 * identical whole-corpus / RBAC-scoped / capped facet machinery instead of a second implementation.
 *
 * <p>
 * The {@code /results} endpoint parses {@code q} into a Lucene {@code Query} object (via the shared
 * {@code QueryParser + QueryCompiler + FieldMap} pipeline) and never exposes the structured clause
 * strings the facet-count base needs. This bridge re-parses {@code q} with the SAME
 * {@link QueryParser}, walks the resulting AST, and rebuilds the structured field chips as Lucene
 * clause strings using the SAME {@link FieldMap} field resolution the index-query path uses — so a
 * bucket count issued for {@code /results} is byte-for-byte the query the index-query endpoint would
 * issue for the equivalent structured filter.
 *
 * <p>
 * Only the top-level AND-conjoined {@link FieldNode}s become structured clauses (mirroring the
 * index-query filter bag, which is an implicit AND of chips). Bare terms/phrases are the free-text
 * {@code query} refinement, which the facet-count base intentionally omits — matching
 * {@link IndexQueryFilterCompiler}. A {@code q} carrying only free text (no {@code field:value} chips)
 * therefore yields an empty clause list, and the facet counts fall back to the whole-corpus base
 * (just the item-type clause) rather than erroring.
 */
final class ResultsFacetQueryBridge
{
  private ResultsFacetQueryBridge() {
  }

  /**
   * Compile the {@code /results} {@code q=} string into the facet-count {@link CompiledQuery} for the
   * given entity type. Unknown fields, fields not allowed on the entity type, and empty/unsupported
   * value shapes are dropped (they are not structured filters for this entity), matching the
   * fail-open contract of the {@code /results} query pipeline.
   */
  static IndexQueryFilterCompiler.CompiledQuery compile(final IndexQueryType queryType, final String q) {
    final FieldMap fieldMap = FieldMap.defaultMap();
    final Set<ItemType> types = queryType.itemTypes();
    final AstNode ast = QueryParser.parse(q == null ? "" : q).ast();

    final List<String> fieldClauses = new ArrayList<>();
    final List<String> waiverStatusClauses = new ArrayList<>();
    final Map<String, List<String>> clausesByField = new LinkedHashMap<>();
    String autoWaiverRestrictionClause = null;

    for (final FieldNode node : topLevelFieldNodes(ast)) {
      final Optional<FieldClause> clause = clauseFor(fieldMap, types, node);
      if (clause.isEmpty()) {
        continue;
      }
      final String c = clause.get().clause();
      fieldClauses.add(c);
      // Keyed by the resolved index field so a facet on that field can subtract its own dimension and
      // keep offering the values the user has not picked yet, as the structured filter path does.
      clausesByField.computeIfAbsent(clause.get().indexField(), f -> new ArrayList<>()).add(c);
      // Mirror IndexQueryFilterCompiler's dimension tracking so the fixed states/waiverType and
      // WAIVER status facets count whole-corpus rather than self-restricting to the user's own
      // waiver-status / auto selection.
      if (isWaiverStatusClause(node)) {
        waiverStatusClauses.add(c);
      }
      if (isManualOnlyAutoRestriction(node)) {
        autoWaiverRestrictionClause = c;
      }
    }

    // The count base omits the free-text refinement (matching IndexQueryFilterCompiler), so the q on
    // the CompiledQuery is only needed for symmetry; the facet engine reads it from fieldClauses.
    // freeText is empty for the same reason: the bare terms in q are not lifted here.
    final String rebuiltQ = String.join(" ", fieldClauses);
    return new IndexQueryFilterCompiler.CompiledQuery(
        rebuiltQ, fieldClauses, autoWaiverRestrictionClause, waiverStatusClauses, List.of(), clausesByField);
  }

  /**
   * A compiled chip together with the index field it filters, so the caller can build the per-field
   * clause map without repeating {@link FieldMap} resolution.
   */
  private record FieldClause(String indexField, String clause)
  {
  }

  /**
   * Top-level field chips: a bare {@link FieldNode}, or the {@link FieldNode} children of a top-level AND.
   * <p>
   * Only conjunctive structure is lifted. A top-level {@code OR}, a negated chip ({@code NOT} / {@code -}),
   * or a grouped sub-expression is dropped, even though the page query honours all of them
   * ({@code QueryCompiler} maps {@code OrNode} to SHOULD and {@code NotNode} to MUST_NOT). The facet base is
   * therefore a superset of the page filter for such a query, so counts can over-count but never report
   * buckets narrower than the results on screen. This matches the {@code /index-query} filter bag, which is
   * AND-only, and the chip rail only emits ANDs.
   */
  private static List<FieldNode> topLevelFieldNodes(final AstNode ast) {
    final List<FieldNode> out = new ArrayList<>();
    if (ast instanceof FieldNode node) {
      out.add(node);
    }
    else if (ast instanceof AndNode and) {
      for (final AstNode child : and.children()) {
        if (child instanceof FieldNode node) {
          out.add(node);
        }
      }
    }
    return out;
  }

  /**
   * Build the Lucene clause string for a field chip using the SAME {@link FieldMap} resolution the
   * index-query path uses: KEYWORD/TEXT values become {@code label:"lowercased"}; NUMERIC exact
   * becomes {@code label:[v TO v]}; NUMERIC range becomes {@code label:[lo TO hi]}. Returns empty when
   * the field is unknown, not allowed on the entity type, or carries an unsupported value shape.
   */
  private static Optional<FieldClause> clauseFor(
      final FieldMap fieldMap,
      final Set<ItemType> entityTypes,
      final FieldNode node)
  {
    final String fieldName = node.field();
    final Optional<FieldEntry> lookup = fieldMap.lookup(fieldName);
    if (lookup.isEmpty()) {
      return Optional.empty();
    }
    final FieldEntry entry = lookup.get();
    // Allowed when the field is indexed on any of the entity's item types.
    if (entityTypes.stream().noneMatch(entry.allowedTypes()::contains)) {
      return Optional.empty();
    }
    final String label = entry.label();
    final FieldValue value = node.value();
    final Optional<String> clause = entry.kind() == FieldKind.NUMERIC
        ? numericClause(label, value)
        : textClause(label, value);
    return clause.map(c -> new FieldClause(label, c));
  }

  private static Optional<String> textClause(final String label, final FieldValue value) {
    // A prefix chip compiles to a genuine PrefixQuery on the page path (QueryCompiler#boundedPrefixQuery),
    // so counting it as an exact phrase here would undercount every bucket. Drop it instead, the same
    // fail-open treatment an unsupported range gets: a facet base that is a superset of the page filter
    // over-counts rather than silently reporting buckets narrower than the results on screen.
    if (value instanceof PrefixValue) {
      return Optional.empty();
    }
    final String literal = literalForValue(value);
    if (literal == null || literal.isBlank()) {
      return Optional.empty();
    }
    // Keyword terms are lowercased at index/read time (QueryCompiler.compileKeyword), so lowercase the
    // count-clause value to match.
    return Optional.of(label + ":\"" + escape(literal.toLowerCase(Locale.ROOT)) + "\"");
  }

  private static Optional<String> numericClause(final String label, final FieldValue value) {
    if (value instanceof RangeValue r) {
      final Optional<String> lo = bound(r.lo());
      final Optional<String> hi = bound(r.hi());
      // A non-numeric bound would build a malformed range that the count query cannot parse, turning a
      // request that succeeds without includeFacets into an error. Drop the clause instead, mirroring the
      // exact-value branch below.
      if (lo.isEmpty() || hi.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(label + ":[" + lo.get() + " TO " + hi.get() + "]");
    }
    final String literal = literalForValue(value);
    if (literal == null || literal.isBlank()) {
      return Optional.empty();
    }
    final String trimmed = literal.trim();
    // A non-finite exact value would build a malformed range; drop it rather than count against it.
    if (!isFiniteNumber(trimmed)) {
      return Optional.empty();
    }
    return Optional.of(label + ":[" + trimmed + " TO " + trimmed + "]");
  }

  /**
   * A range bound for the count clause: an absent/blank bound is the open {@code *}, a numeric bound is
   * kept, and anything else is rejected as empty so {@link #numericClause} can drop the whole clause.
   */
  private static Optional<String> bound(final String raw) {
    if (raw == null || raw.isBlank()) {
      return Optional.of("*");
    }
    final String trimmed = raw.trim();
    if ("*".equals(trimmed)) {
      return Optional.of(trimmed);
    }
    if (!isFiniteNumber(trimmed)) {
      return Optional.empty();
    }
    return Optional.of(trimmed);
  }

  /**
   * Whether the value is a finite number. {@code Double.parseDouble} accepts {@code NaN},
   * {@code Infinity} and {@code -Infinity} without throwing, and any of those would build a range the
   * numeric point field cannot parse, so they are rejected alongside outright non-numeric text. Mirrors
   * the finite check {@code IndexQueryFilterCompiler#rangeBound} applies on the structured filter path.
   */
  private static boolean isFiniteNumber(final String value) {
    try {
      final double parsed = Double.parseDouble(value);
      return !Double.isNaN(parsed) && !Double.isInfinite(parsed);
    }
    catch (NumberFormatException e) {
      return false;
    }
  }

  private static String literalForValue(final FieldValue v) {
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

  /**
   * The user-facing waiver-status filter key whose clauses the fixed state/waiverType facet base
   * subtracts. Read from the shared {@link FieldMap} grammar constant rather than an inline literal, so a
   * rename of the key is a compile error here instead of a silent change to whole-corpus bucket counts.
   */
  private static boolean isWaiverStatusClause(final FieldNode node) {
    return FieldMap.KEY_POLICY_VIOLATION_WAIVER_STATUS.equals(node.field());
  }

  /**
   * Manual-only restriction: an explicit {@code policyWaiverAuto:false} narrows to manual waivers. The
   * auto/manual facet base drops it so both true/false buckets count whole-corpus.
   */
  private static boolean isManualOnlyAutoRestriction(final FieldNode node) {
    if (!FieldMap.KEY_POLICY_WAIVER_AUTO.equals(node.field())) {
      return false;
    }
    final String literal = literalForValue(node.value());
    return "false".equalsIgnoreCase(literal == null ? null : literal.trim());
  }

  private static String escape(final String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
