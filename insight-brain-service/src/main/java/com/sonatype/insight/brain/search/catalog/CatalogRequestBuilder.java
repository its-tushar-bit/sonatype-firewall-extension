/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchRequest;
import com.sonatype.insight.brain.search.catalog.CatalogFilterSchema.Kind;

public final class CatalogRequestBuilder
{
  /**
   * Upper bound on the number of values a single TERMS filter may carry. Each value becomes one OR
   * clause forwarded to the backend, so an unbounded list is a CPU/memory amplification vector
   * (thousands of ORs against HDS). 100 comfortably covers real ecosystem/severity/license pick-lists
   * while capping the fan-out.
   */
  static final int MAX_TERMS_PER_FILTER = 100;

  /**
   * The yes/no SCALAR fields the catalog path maps through {@link #yesNoBool}, which accepts a JSON
   * Boolean as well as a String. Kept in sync with the {@code yesNoBool(...)} call sites so
   * {@link #validateShapes} accepts the same shapes the catalog builder does (no source-dependent
   * type contract). The remaining SCALAR fields (latestStable/publishedWindow) stay String-only.
   */
  private static final Set<String> YES_NO_SCALAR_KEYS = Set.of("malware", "kev", "patchAvailable");

  private CatalogRequestBuilder() {
  }

  public static GuideComponentSearchRequest component(
      final Map<String, Object> filters,
      final int offset,
      final int limit)
  {
    validateKeys(CatalogEntityType.COMPONENT, filters);
    // 'organizations' is a local (My Scan Data) filter; the public Guide/HDS corpus has no org
    // dimension, so reject rather than silently drop it on the catalog source.
    if (filters.containsKey("organizations")) {
      throw new BadRequestException("filter 'organizations' is not supported for the catalog source");
    }
    final RangePair cvss = range(filters, "cvss");
    final RangePair epss = range(filters, "epss");
    final RangePair versionScore = range(filters, "versionScore");
    return new GuideComponentSearchRequest(
        text(filters, "query"),
        offset,
        limit,
        null,
        null,
        terms(filters, "ecosystems"),
        terms(filters, "categories"),
        terms(filters, "severities"),
        cvss.min(),
        cvss.max(),
        epss.min(),
        epss.max(),
        terms(filters, "licenseFamilies"),
        terms(filters, "licenses"),
        versionScore.minInt(),
        versionScore.maxInt(),
        scalarString(filters, "latestStable"),
        scalarString(filters, "publishedWindow"),
        yesNoBool(filters, "malware"),
        null);
  }

  public static GuideVulnerabilitySearchRequest vulnerability(
      final Map<String, Object> filters,
      final int offset,
      final int limit)
  {
    validateKeys(CatalogEntityType.VULNERABILITY, filters);
    final RangePair cvss = range(filters, "cvss");
    final RangePair epss = range(filters, "epss");
    return new GuideVulnerabilitySearchRequest(
        text(filters, "query"),
        offset,
        limit,
        null,
        null,
        terms(filters, "severities"),
        cvss.min(),
        cvss.max(),
        epss.min(),
        epss.max(),
        yesNoBool(filters, "malware"),
        yesNoBool(filters, "patchAvailable"),
        terms(filters, "cwes"),
        yesNoBool(filters, "kev"),
        scalarString(filters, "publishedWindow"),
        terms(filters, "affectedEcosystems"),
        null);
  }

  private static void validateKeys(final CatalogEntityType entityType, final Map<String, Object> filters) {
    final Map<String, Kind> schema = CatalogFilterSchema.forEntityType(entityType);
    for (String key : filters.keySet()) {
      if (!schema.containsKey(key)) {
        throw new BadRequestException("unknown filter key for entityType " + entityType);
      }
    }
  }

  /**
   * Validate every filter's key and value shape against its {@link Kind}, independent of which source
   * will honour it: TEXT must be a string, TERMS an array, RANGE a two-element numeric array, SCALAR a
   * string (or a boolean for the yes/no fields the catalog path coerces through {@link #yesNoBool}).
   * Shared by the local path so a wrong-shaped value is a 400 on both sources (rather than being
   * String.valueOf-coerced or silently ignored-with-warning locally). Shape only; the catalog builder
   * re-runs the same per-field checks when it maps the values.
   */
  public static void validateShapes(final CatalogEntityType entityType, final Map<String, Object> filters) {
    final Map<String, Kind> schema = CatalogFilterSchema.forEntityType(entityType);
    for (Map.Entry<String, Object> entry : filters.entrySet()) {
      final Kind kind = schema.get(entry.getKey());
      if (kind == null) {
        throw new BadRequestException("unknown filter key for entityType " + entityType);
      }
      final Object v = entry.getValue();
      if (v == null) {
        continue;
      }
      switch (kind) {
        case TEXT -> {
          if (!(v instanceof String)) {
            throw new BadRequestException("filter must be a string");
          }
        }
        case SCALAR -> {
          // The catalog path routes the yes/no SCALAR fields through yesNoBool, which accepts a JSON
          // Boolean or a String. Accept both here for those keys so {"malware": true} is not a
          // source-dependent type contract (400 on local but 200 on catalog). Other SCALAR fields
          // (latestStable/publishedWindow) go through scalarString and stay String-only.
          final boolean yesNoField = YES_NO_SCALAR_KEYS.contains(entry.getKey());
          if (!(v instanceof String) && !(yesNoField && v instanceof Boolean)) {
            throw new BadRequestException(
                yesNoField ? "filter must be a string or boolean" : "filter must be a string");
          }
        }
        case TERMS -> {
          if (!(v instanceof List<?>)) {
            throw new BadRequestException("filter must be an array");
          }
        }
        case RANGE -> {
          if (!(v instanceof List<?> list) || list.size() != 2) {
            throw new BadRequestException("filter must be a two-element [min, max] array");
          }
          for (Object o : list) {
            if (o != null && !(o instanceof Number)) {
              throw new BadRequestException("range filter bounds must be numeric");
            }
          }
        }
      }
    }
  }

  private static String text(final Map<String, Object> filters, final String key) {
    final Object v = filters.get(key);
    if (v == null) {
      return null;
    }
    if (!(v instanceof String s)) {
      throw new BadRequestException("filter '" + key + "' must be a string");
    }
    return s.isBlank() ? null : s.strip();
  }

  private static List<String> terms(final Map<String, Object> filters, final String key) {
    final Object v = filters.get(key);
    if (v == null) {
      return null;
    }
    if (!(v instanceof List<?> list)) {
      throw new BadRequestException("filter '" + key + "' must be an array");
    }
    if (list.size() > MAX_TERMS_PER_FILTER) {
      // Static, input-free message: do not echo the key or the offending list.
      throw new BadRequestException("a filter carries too many values");
    }
    final List<String> out = new ArrayList<>(list.size());
    for (Object o : list) {
      if (o != null) {
        out.add(String.valueOf(o));
      }
    }
    return out.isEmpty() ? null : out;
  }

  private static String scalarString(final Map<String, Object> filters, final String key) {
    final Object v = filters.get(key);
    if (v == null) {
      return null;
    }
    // Validate type rather than String.valueOf-coercing a number/array (which would forward "42" or
    // "[recent]" to HDS). Consistent with text()'s runtime-type check.
    if (!(v instanceof String s)) {
      throw new BadRequestException("filter '" + key + "' must be a string");
    }
    return s;
  }

  private static Boolean yesNoBool(final Map<String, Object> filters, final String key) {
    final Object v = filters.get(key);
    if (v == null) {
      return null;
    }
    if (v instanceof Boolean b) {
      return b;
    }
    final String s = String.valueOf(v).strip().toLowerCase(Locale.ROOT);
    return switch (s) {
      case "yes", "true" -> Boolean.TRUE;
      case "no", "false" -> Boolean.FALSE;
      case "all", "any", "" -> null;
      default -> throw new BadRequestException("filter '" + key + "' must be one of yes|no|all");
    };
  }

  private static RangePair range(final Map<String, Object> filters, final String key) {
    final Object v = filters.get(key);
    if (v == null) {
      return RangePair.EMPTY;
    }
    if (!(v instanceof List<?> list) || list.size() != 2) {
      throw new BadRequestException("filter '" + key + "' must be a two-element [min, max] array");
    }
    final Double min = bound(key, list.get(0));
    final Double max = bound(key, list.get(1));
    if (min != null && max != null && min > max) {
      // Static, input-free message: do not echo the key or the bound values.
      throw new BadRequestException("range filter min must be <= max");
    }
    return new RangePair(min, max);
  }

  private static Double bound(final String key, final Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Number n) {
      final double d = n.doubleValue();
      // Reject NaN / +-Infinity for every range key before the per-key checks: a non-finite bound is
      // meaningless as a min/max and would otherwise flow into HDS/index range clauses. Static message.
      if (!Double.isFinite(d)) {
        throw new BadRequestException("range filter bounds must be finite numbers");
      }
      // versionScore is an integer score; a fractional bound (e.g. 7.9) would be silently
      // truncated to 7 by intValue(), so reject non-integral bounds instead.
      if ("versionScore".equals(key) && d != Math.rint(d)) {
        throw new BadRequestException("filter '" + key + "' range bounds must be whole numbers");
      }
      return d;
    }
    throw new BadRequestException("filter '" + key + "' range bounds must be numeric");
  }

  private record RangePair(Double min, Double max)
  {
    static final RangePair EMPTY = new RangePair(null, null);

    Integer minInt() {
      return min == null ? null : min.intValue();
    }

    Integer maxInt() {
      return max == null ? null : max.intValue();
    }
  }
}
