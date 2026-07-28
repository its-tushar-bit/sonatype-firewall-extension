/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sonatype.insight.brain.search.global.FilterValidationException;
import com.sonatype.insight.brain.search.indexquery.IndexQueryFilterSchema.FilterDef;

/**
 * Translates a per-{@link IndexQueryType} filter bag into the {@code q=} chip string the shared
 * global-search parse/compile pipeline already understands, rather than building Lucene queries here.
 */
public final class IndexQueryFilterCompiler
{
  private IndexQueryFilterCompiler() {
  }

  /**
   * Result of compiling a filter bag: {@code q} is the full chip string (free text + structured chips)
   * for the main search; {@code fieldClauses} are only the structured (non-free-text) chips in Lucene
   * {@code field:"value"} form, used to build the whole-corpus facet-count base so counts reflect the
   * same active structured filters as the page (the free-text {@code query} refinement is not reapplied there).
   * <p>
   * {@code autoWaiverRestrictionClause} is the manual-only {@code policyWaiverAuto:"false"} clause when
   * the compiled query restricts to manual waivers -- whether from the absent/null default OR an explicit
   * {@code includeAutoWaivers:false} -- and null otherwise. The auto/manual facet base drops it so its
   * true/false buckets count whole-corpus regardless of which path added the restriction.
   * <p>
   * {@code waiverStatusClauses} is the subset of {@code fieldClauses} produced by the STATE/WAIVER_TYPE
   * filters (both compile against the waiver-status field). The fixed states/waiverType facets subtract
   * these so each fixed count reflects the whole (unrestricted-by-its-own-dimension) corpus rather than
   * self-restricting to the user's current state/waiver selection.
   */
  public record CompiledQuery(
      String q,
      List<String> fieldClauses,
      String autoWaiverRestrictionClause,
      List<String> waiverStatusClauses)
  {
  }

  public static CompiledQuery compileWithClauses(final IndexQueryType queryType, final Map<String, Object> filters) {
    return compileWithClauses(queryType, filters, Clock.systemUTC());
  }

  /**
   * Clock-injectable overload so the active-vs-expired boundary the {@code expiry} filter resolves
   * against is deterministic in tests. Production uses {@link Clock#systemUTC()} via the two-arg
   * overload; the page query and its facet-count base share one compile call, so they observe one now.
   */
  static CompiledQuery compileWithClauses(
      final IndexQueryType queryType,
      final Map<String, Object> filters,
      final Clock clock)
  {
    final Map<String, FilterDef> schema = IndexQueryFilterSchema.forQueryType(queryType);
    final List<String> chips = new ArrayList<>();
    // The manual-only restriction clause, if the query restricts to manual waivers (explicit-false or
    // the absent/null default). Captured separately (not folded into fieldClauses) so the auto/manual
    // facet base can omit it and report whole-corpus true/false counts regardless of the toggle.
    String autoWaiverRestrictionClause = null;
    final List<String> waiverStatusChips = new ArrayList<>();
    // Whether the request carries a filter that implies POLICY_WAIVER_REQUEST docs: an explicit
    // waiverStates selection (which fully owns the item-type + auto/manual scoping per state), or a
    // status filter (policyWaiverRequestStatus, request-only). When either is present the manual-only
    // default must NOT be layered on top — the default's policyWaiverAuto:"false" restriction targets
    // POLICY_WAIVER docs, and AND'ing it would drop every request doc (requests have no auto field),
    // zeroing out the requested/rejected/status views.
    boolean requestScopedFilterPresent = false;
    String freeText = "";

    for (Map.Entry<String, Object> entry : filters.entrySet()) {
      final String key = entry.getKey();
      final FilterDef def = schema.get(key);
      if (def == null) {
        throw badRequest("unknown filter key '" + key + "' for entityType " + queryType);
      }
      final Object value = entry.getValue();
      if (value == null) {
        continue;
      }
      switch (def.kind()) {
        case TEXT -> freeText = compileText(key, value);
        case TERMS -> {
          String chip = compileTerms(key, def.field(), value);
          if (chip != null) {
            chips.add(chip);
            // A status filter targets the request-only policyWaiverRequestStatus field, so it implies
            // request docs; suppress the manual-only default that would otherwise exclude them.
            if ("policyWaiverRequestStatus".equals(def.field())) {
              requestScopedFilterPresent = true;
            }
          }
        }
        case RANGE -> chips.add(compileRange(key, def.field(), value));
        case STATE -> {
          String chip = compileState(key, def.field(), value);
          if (chip != null) {
            chips.add(chip);
            waiverStatusChips.add(chip);
          }
        }
        case WAIVER_TYPE -> {
          String chip = compileWaiverType(key, def.field(), value);
          chips.add(chip);
          waiverStatusChips.add(chip);
        }
        case AUTO_WAIVER_TOGGLE -> {
          // true -> include both kinds (no clause). explicit false -> exclude auto (manual committed
          // waivers only). Item-type-scoped, (itemType:policy_waiver AND policyWaiverAuto:"false"),
          // for the same reason as the absent/null default below: a bare policyWaiverAuto:"false"
          // would drop every POLICY_WAIVER_REQUEST doc by field mismatch. Recorded so the auto/manual
          // facet base can drop it and report whole-corpus true/false counts.
          if (!compileAutoWaiverInclude(key, value)) {
            final String clause = "(" + WAIVER_TYPE_CLAUSE + " AND " + def.field() + ":\"false\")";
            chips.add(clause);
            autoWaiverRestrictionClause = clause;
          }
        }
        case EXPIRY_STATUS -> chips.add(compileExpiry(key, def.field(), value, clock));
        case WAIVER_STATES -> {
          String chip = compileWaiverStates(key, value);
          if (chip != null) {
            // Set only when a state was actually selected: an empty waiverStates:[] then behaves like
            // an absent filter (manual-only committed default), not like a request-scoped selection.
            requestScopedFilterPresent = true;
            chips.add(chip);
            // The status facet base subtracts the waiver-state clauses so each fixed status count is
            // whole-corpus rather than self-restricting to the user's state selection, mirroring how
            // the STATE/WAIVER_TYPE chips are tracked for the violation facets.
            waiverStatusChips.add(chip);
          }
        }
        default -> throw new IllegalStateException("unhandled filter kind: " + def.kind());
      }
    }

    // Default when the includeAutoWaivers toggle is absent OR present-but-null: manual committed
    // waivers only. Expressed as an ITEM-TYPE-SCOPED clause, (itemType:policy_waiver AND
    // policyWaiverAuto:"false"), NOT a bare policyWaiverAuto:"false": the bare form silently drops
    // every POLICY_WAIVER_REQUEST doc (which has no policyWaiverAuto field), so it would zero out any
    // query that should include request docs. The item-type-scoped form keeps the same manual-only
    // default over committed waivers while excluding requests by item type rather than by an
    // accidental field mismatch. Skipped entirely when a waiverStates filter is present — waiverStates
    // then fully owns the item-type + auto/manual scoping per state (existing = policy_waiver AND
    // auto:false, excluded = policy_waiver AND auto:true, requested/rejected = policy_waiver_request
    // AND status), so layering the default on top would drop the request states. Only WAIVER carries
    // an AUTO_WAIVER_TOGGLE entry, so skip the schema scan entirely for other entity types.
    if (queryType == IndexQueryType.WAIVER && !requestScopedFilterPresent) {
      for (Map.Entry<String, FilterDef> e : schema.entrySet()) {
        final String key = e.getKey();
        final FilterDef def = e.getValue();
        if (def.kind() == IndexQueryFilterSchema.Kind.AUTO_WAIVER_TOGGLE
            && (!filters.containsKey(key) || filters.get(key) == null))
        {
          final String clause = "(" + WAIVER_TYPE_CLAUSE + " AND " + def.field() + ":\"false\")";
          chips.add(clause);
          autoWaiverRestrictionClause = clause;
        }
      }
    }
    // A request-scoped filter (waiverStates/status) fully owns the item-type + auto/manual scoping per
    // state, so drop any manual-only auto restriction added by an explicit includeAutoWaivers:false in
    // the loop above (the absent/null default is already skipped for that case). Otherwise
    // (itemType:policy_waiver AND auto:false) would AND with the request-doc clause
    // (itemType:policy_waiver_request AND ...) into an impossible condition that returns 0 rows,
    // regardless of Map iteration order.
    if (queryType == IndexQueryType.WAIVER && requestScopedFilterPresent && autoWaiverRestrictionClause != null) {
      chips.remove(autoWaiverRestrictionClause);
      autoWaiverRestrictionClause = null;
    }

    final StringBuilder q = new StringBuilder();
    if (!freeText.isBlank()) {
      q.append(freeText.strip());
    }
    // Chips are joined by whitespace only: the custom QueryParser treats juxtaposition as implicit AND
    // (see QueryParser.parseAnd()). This is NOT Lucene's StandardQueryParser (LuceneComponents
    // .newQueryParser()), which defaults to OR — that parser does not run this path.
    for (String chip : chips) {
      if (q.length() > 0) {
        q.append(' ');
      }
      q.append(chip);
    }
    return new CompiledQuery(q.toString(), chips, autoWaiverRestrictionClause, waiverStatusChips);
  }

  /**
   * Compiles the {@code expiry} active-vs-expired status filter into a range chip on the epoch-millis
   * point field, resolved against the server clock at request time:
   * <ul>
   * <li>{@code "expired"} -> {@code field:[* TO now]} (a point present and at or before now);</li>
   * <li>{@code "active"} -> {@code NOT field:[* TO now]} (never-expiring docs carry no point and so
   * fall outside the expired range, leaving them active as intended).</li>
   * </ul>
   * Any other value is a 400.
   */
  private static String compileExpiry(final String key, final String field, final Object value, final Clock clock) {
    if (!(value instanceof String s)) {
      throw badRequest("filter '" + key + "' must be a string (\"active\" or \"expired\")");
    }
    final long now = clock.millis();
    final String expiredClause = field + ":[* TO " + now + "]";
    return switch (s.strip().toLowerCase(Locale.ROOT)) {
      case "expired" -> expiredClause;
      case "active" -> "NOT " + expiredClause;
      default -> throw badRequest("filter '" + key + "' must be \"active\" or \"expired\"");
    };
  }

  /** Item-type search-field token for a POLICY_WAIVER doc (lowercased itemType, see ItemType). */
  private static final String WAIVER_TYPE_CLAUSE = "itemType:policy_waiver";

  /** Item-type search-field token for a POLICY_WAIVER_REQUEST doc. */
  private static final String WAIVER_REQUEST_TYPE_CLAUSE = "itemType:policy_waiver_request";

  /**
   * Compiles the {@code waiverStates} multi-select into an OR of per-state clauses spanning both
   * WAIVER item types (see {@link IndexQueryFilterSchema.Kind#WAIVER_STATES}):
   * <ul>
   * <li>{@code existing} → committed non-excluded (manual) waivers;</li>
   * <li>{@code excluded} → committed excluded (auto) waivers;</li>
   * <li>{@code requested} → pending requests (status REQUESTED);</li>
   * <li>{@code rejected} → rejected requests (status REJECTED).</li>
   * </ul>
   * An empty/blank selection returns null (no restriction). Any unknown value is a 400.
   */
  private static String compileWaiverStates(final String key, final Object value) {
    if (!(value instanceof List<?> list)) {
      throw badRequest("filter '" + key + "' must be an array");
    }
    final List<String> clauses = new ArrayList<>();
    for (Object element : list) {
      if (element == null) {
        continue;
      }
      final String state = String.valueOf(element).strip().toLowerCase(Locale.ROOT);
      final String clause = switch (state) {
        case "existing" -> "(" + WAIVER_TYPE_CLAUSE + " AND policyWaiverAuto:\"false\")";
        case "excluded" -> "(" + WAIVER_TYPE_CLAUSE + " AND policyWaiverAuto:\"true\")";
        case "requested" -> "(" + WAIVER_REQUEST_TYPE_CLAUSE + " AND policyWaiverRequestStatus:\"REQUESTED\")";
        case "rejected" -> "(" + WAIVER_REQUEST_TYPE_CLAUSE + " AND policyWaiverRequestStatus:\"REJECTED\")";
        default -> throw badRequest(
            "filter '" + key + "' values must be existing, excluded, requested, or rejected");
      };
      if (!clauses.contains(clause)) {
        clauses.add(clause);
      }
    }
    if (clauses.isEmpty()) {
      return null;
    }
    if (clauses.size() == 1) {
      return clauses.get(0);
    }
    return "(" + String.join(" OR ", clauses) + ")";
  }

  private static boolean compileAutoWaiverInclude(final String key, final Object value) {
    if (!(value instanceof Boolean b)) {
      throw badRequest("filter '" + key + "' must be a boolean");
    }
    return b;
  }

  private static String compileText(final String key, final Object value) {
    if (!(value instanceof String s)) {
      throw badRequest("filter '" + key + "' must be a string");
    }
    return sanitizeBareText(s);
  }

  private static String compileTerms(final String key, final String field, final Object value) {
    if (!(value instanceof List<?> list)) {
      throw badRequest("filter '" + key + "' must be an array");
    }
    final List<String> clauses = new ArrayList<>();
    for (Object element : list) {
      if (element == null) {
        continue;
      }
      final String sanitized = sanitizeTermValue(String.valueOf(element));
      if (!sanitized.isEmpty()) {
        clauses.add(field + ":\"" + sanitized + "\"");
      }
    }
    if (clauses.isEmpty()) {
      return null;
    }
    if (clauses.size() == 1) {
      return clauses.get(0);
    }
    return "(" + String.join(" OR ", clauses) + ")";
  }

  /**
   * OPEN/WAIVED violation state. WAIVED is {@code waiverStatus:(Waived OR AutoWaived)}; OPEN is the
   * negation so a violation with an absent/unknown waiver status counts as OPEN, keeping the filter,
   * the row-state derivation, and the state facet in agreement. Selecting both (or neither) is a no-op.
   */
  private static String compileState(final String key, final String field, final Object value) {
    if (!(value instanceof List<?> list)) {
      throw badRequest("filter '" + key + "' must be an array");
    }
    boolean wantsOpen = false;
    boolean wantsWaived = false;
    for (Object element : list) {
      if (element == null) {
        continue;
      }
      final String state = String.valueOf(element).strip().toUpperCase(java.util.Locale.ROOT);
      switch (state) {
        case "OPEN" -> wantsOpen = true;
        case "WAIVED" -> wantsWaived = true;
        default -> throw badRequest("filter '" + key + "' values must be OPEN or WAIVED");
      }
    }
    if (wantsOpen == wantsWaived) {
      return null;
    }
    final String waivedClause = IndexQueryWaiverStatus.waivedClause(field);
    return wantsWaived ? waivedClause : "NOT " + waivedClause;
  }

  /**
   * AUTO/MANUAL waiver type. AUTO is {@code waiverStatus:"AutoWaived"}; MANUAL is
   * {@code waiverStatus:"Waived"}. Scalar-valued; a single-element array is unwrapped as a
   * convenience (consumers may pass {@code ["AUTO"]} by analogy with the array-valued filters), but
   * a multi-element array is rejected with an explicit message rather than an opaque {@code "[..]"}.
   */
  private static String compileWaiverType(final String key, final String field, final Object value) {
    final Object scalar = unwrapScalar(key, value);
    final String raw = String.valueOf(scalar).strip().toUpperCase(java.util.Locale.ROOT);
    return switch (raw) {
      case "AUTO" -> field + ":\"" + IndexQueryWaiverStatus.AUTO_WAIVED + "\"";
      case "MANUAL" -> field + ":\"" + IndexQueryWaiverStatus.WAIVED + "\"";
      default -> throw badRequest("filter '" + key + "' must be AUTO or MANUAL");
    };
  }

  /**
   * Accept a scalar or a single-element array for a scalar-only filter, unwrapping the latter. An
   * array with any other size gets a specific message naming the offending shape, so a consumer that
   * passed {@code ["AUTO", "MANUAL"]} sees the real problem instead of a stringified {@code "[..]"}.
   */
  private static Object unwrapScalar(final String key, final Object value) {
    if (value instanceof List<?> list) {
      if (list.size() != 1) {
        throw badRequest("filter '" + key + "' takes a single value, not an array of " + list.size());
      }
      return list.get(0);
    }
    return value;
  }

  private static String compileRange(final String key, final String field, final Object value) {
    if (!(value instanceof List<?> list) || list.size() != 2) {
      throw badRequest("filter '" + key + "' must be a two-element [min, max] array");
    }
    if (list.get(0) == null && list.get(1) == null) {
      throw badRequest("range filter requires at least one bound");
    }
    final String min = rangeBound(key, list.get(0));
    final String max = rangeBound(key, list.get(1));
    return field + ":[" + min + " TO " + max + "]";
  }

  private static String rangeBound(final String key, final Object bound) {
    if (bound == null) {
      return "*";
    }
    if (bound instanceof Number n) {
      final double d = n.doubleValue();
      if (Double.isNaN(d) || Double.isInfinite(d)) {
        throw badRequest("filter '" + key + "' range bounds must be finite numbers");
      }
      // Render integer-valued bounds without a trailing ".0" so numeric field parsing is clean.
      if (d == Math.rint(d)) {
        // A double cast to long saturates (JLS 5.1.3) rather than throwing, so reject out-of-range first.
        if (d < Long.MIN_VALUE || d > Long.MAX_VALUE) {
          throw badRequest("filter '" + key + "' range bounds are out of range");
        }
        return Long.toString(n.longValue());
      }
      return n.toString();
    }
    throw badRequest("filter '" + key + "' range bounds must be numeric");
  }

  /**
   * Strip only the structural chars (quotes, colons, brackets, braces, parens) that could open a
   * field/phrase clause. Remaining tokens -- including AND, OR, NOT and the {@code + - * ~ ^} prefixes --
   * are interpreted by the shared tolerant {@code QueryParser} as the product's global-search query
   * language, NOT as Lucene query-string syntax, so there is no Lucene injection to strip here. That
   * parser never throws on user input (malformed input yields an AST plus warnings).
   */
  static String sanitizeBareText(final String raw) {
    if (raw == null) {
      return "";
    }
    return raw.replaceAll("[\":\\[\\](){}]", " ").strip();
  }

  /** Strip quotes and backslashes so a term value cannot terminate its own quoted chip. */
  static String sanitizeTermValue(final String raw) {
    if (raw == null) {
      return "";
    }
    return raw.replace("\\", "").replace("\"", "").strip();
  }

  private static FilterValidationException badRequest(final String message) {
    return new FilterValidationException(FilterValidationException.Code.INVALID_FILTER, message);
  }
}
