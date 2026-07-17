/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.util.ArrayList;
import java.util.List;
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
   */
  public record CompiledQuery(String q, List<String> fieldClauses)
  {
  }

  public static CompiledQuery compileWithClauses(final IndexQueryType queryType, final Map<String, Object> filters) {
    final Map<String, FilterDef> schema = IndexQueryFilterSchema.forQueryType(queryType);
    final List<String> chips = new ArrayList<>();
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
          }
        }
        case RANGE -> chips.add(compileRange(key, def.field(), value));
        default -> throw new IllegalStateException("unhandled filter kind: " + def.kind());
      }
    }

    final StringBuilder q = new StringBuilder();
    if (!freeText.isBlank()) {
      q.append(freeText.strip());
    }
    for (String chip : chips) {
      if (q.length() > 0) {
        q.append(' ');
      }
      q.append(chip);
    }
    return new CompiledQuery(q.toString(), chips);
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
