/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;

import com.sonatype.insight.brain.search.catalog.CatalogFilterSchema.Kind;

/**
 * Filters the IQ index cannot honour are recorded as warnings, not rejected: the caller-supplied
 * catalog filter is valid, it just has no local equivalent.
 */
public final class CatalogLocalRequestBuilder
{
  // 'severities' has no term-compatible local field (local severity is a numeric CVSS score, and
  // vulnerabilityStatus is a triage keyword), so it is left unmapped and falls through to a warning.
  private static final Map<CatalogEntityType, Map<String, String>> LOCAL_FIELD_BY_KEY = Map.of(
      CatalogEntityType.COMPONENT, Map.of(
          "ecosystems", "componentFormat",
          // organizationName is rewritten to parentOrganizationName by the index metric/query layer,
          // so a match on an org includes its descendants (same semantics as v1 classic search).
          "organizations", "organizationName"),
      CatalogEntityType.VULNERABILITY, Map.of());

  private CatalogLocalRequestBuilder() {
  }

  /**
   * {@code fieldClauses} are the structured (non-free-text) filter clauses in Lucene {@code field:"value"}
   * form, mapped onto local index field names. They power whole-corpus facet counts, which count over the
   * structured filters + item type only (the free-text {@code query} refinement is not reapplied there).
   */
  public record LocalQuery(String q, List<String> warnings, List<String> fieldClauses)
  {
  }

  public static LocalQuery build(final CatalogEntityType entityType, final Map<String, Object> filters) {
    // Validate EVERY filter's shape against its schema Kind first (identical to the catalog path), so a
    // wrong-shaped value is a 400 on both sources regardless of whether the field is locally supported.
    // Shape validation precedes the supported/unsupported-warn decision below.
    CatalogRequestBuilder.validateShapes(entityType, filters);
    final Map<String, Kind> schema = CatalogFilterSchema.forEntityType(entityType);
    final Map<String, String> localFields = LOCAL_FIELD_BY_KEY.getOrDefault(entityType, Map.of());
    final List<String> warnings = new ArrayList<>();
    final List<String> chips = new ArrayList<>();
    final List<String> fieldClauses = new ArrayList<>();
    String freeText = "";

    for (Map.Entry<String, Object> entry : filters.entrySet()) {
      final String key = entry.getKey();
      final Kind kind = schema.get(key);
      if (kind == null) {
        throw new BadRequestException("unknown filter key for entityType " + entityType);
      }
      final Object value = entry.getValue();
      if (value == null) {
        continue;
      }
      if (kind == Kind.TEXT) {
        freeText = sanitizeBareText(String.valueOf(value));
        continue;
      }
      // A known TERMS filter whose value is not an array is a client type error, not a
      // locally-unsupported filter: reject it consistently with the catalog path rather than masking
      // it as "unavailable locally". Static, input-free message (no key/value echoed).
      if (kind == Kind.TERMS && !(value instanceof List<?>)) {
        throw new BadRequestException("filter must be an array");
      }
      // Cap TERMS value-list size on the local source too (mirrors CatalogRequestBuilder.MAX_TERMS_PER_FILTER):
      // each value becomes one OR clause, so an unbounded list is a query-fan-out vector. Static message.
      if (kind == Kind.TERMS && value instanceof List<?> terms
          && terms.size() > CatalogRequestBuilder.MAX_TERMS_PER_FILTER)
      {
        throw new BadRequestException("a filter carries too many values");
      }
      final String field = localFields.get(key);
      if (field == null) {
        warnings.add(unavailableLocally(key));
        continue;
      }
      if (kind == Kind.TERMS && value instanceof List<?> list) {
        final String chip = termsChip(field, list);
        if (chip != null) {
          chips.add(chip);
          fieldClauses.add(chip);
        }
      }
      else {
        warnings.add(unavailableLocally(key));
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
    return new LocalQuery(q.toString(), warnings, fieldClauses);
  }

  private static String unavailableLocally(final String key) {
    return "filter '" + key + "' is not available on the local source and was ignored";
  }

  private static String termsChip(final String field, final List<?> list) {
    final List<String> clauses = new ArrayList<>();
    for (Object o : list) {
      if (o == null) {
        continue;
      }
      final String sanitized = sanitizeTermValue(String.valueOf(o));
      if (!sanitized.isEmpty()) {
        clauses.add(field + ":\"" + sanitized + "\"");
      }
    }
    if (clauses.isEmpty()) {
      return null;
    }
    return clauses.size() == 1 ? clauses.get(0) : "(" + String.join(" OR ", clauses) + ")";
  }

  /**
   * Strip only the structural characters that could open a field/phrase clause ({@code " : [ ] ( ) { }}).
   * The remaining tokens (including {@code AND}/{@code OR}/{@code NOT} and {@code + - * ? ~ ^}) are
   * interpreted by the shared tolerant {@link com.sonatype.insight.brain.search.global.parser.QueryParser}
   * as the product's own search language, not Lucene query-string syntax; that parser never throws on
   * user input, so this is a query-shaping step, not injection defense.
   */
  private static String sanitizeBareText(final String raw) {
    if (raw == null) {
      return "";
    }
    return raw.replaceAll("[\":\\[\\](){}]", " ").strip();
  }

  private static String sanitizeTermValue(final String raw) {
    if (raw == null) {
      return "";
    }
    return raw.replace("\\", "").replace("\"", "").strip();
  }
}
