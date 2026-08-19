/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.global.catalog.CatalogSuggestRequest;
import com.sonatype.insight.brain.search.global.catalog.CatalogSuggestResult;
import com.sonatype.insight.brain.search.global.catalog.GlobalSearchSuggestCatalogClient;
import com.sonatype.insight.brain.security.CurrentUser;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link SuggestService} implementation. Runs a single source leg per request — the IQ-local
 * leg for {@code source=local}, the catalog leg for {@code source=catalog} — then assembles the rows
 * into the fixed-order response shape.
 *
 * <p>
 * Hard caps applied here:
 *
 * <ul>
 * <li><b>perTypeLimit</b> — {@value #DEFAULT_PER_TYPE_LIMIT}, applied per group. The source leg is
 * asked for {@code perTypeLimit + BEST_MATCH_LOOKAHEAD} rows so an exact-id match sitting just outside
 * the visible window can still be promoted to BEST MATCH before capping.</li>
 * <li><b>Total row cap</b> — {@value #TOTAL_ROW_CAP} rows. BEST MATCH counts toward the cap; group
 * rows fill the remainder in the fixed order defined by {@link SuggestItemType}.</li>
 * </ul>
 *
 * <p>
 * <b>Single-source dispatch.</b> There is no cross-source fall-through: {@code source=catalog} with an
 * unentitled or unavailable catalog returns an empty response with {@code catalogAvailable: false}
 * rather than backfilling with IQ-local rows. {@code source=local} never calls the catalog.
 *
 * <p>
 * <b>Rate limiting.</b> Typeahead is high-frequency, so this endpoint owns its own
 * {@link PerUserRateLimiter} capping a user at {@link PerUserRateLimiter#SUGGEST_PERMITS_PER_USER}
 * concurrent suggest requests. The cap is per endpoint, independent of the {@code /results} cap, so one
 * browser session cannot flood the query path.
 */
@Named
@Singleton
public class SuggestServiceImpl
    implements SuggestService
{
  private static final Logger log = LoggerFactory.getLogger(SuggestServiceImpl.class);

  /** Per-type row cap. Matches the frontend spec's stratified slice contract (5 rows per group). */
  static final int DEFAULT_PER_TYPE_LIMIT = 5;

  static final int TOTAL_ROW_CAP = 10;

  /**
   * Extra rows requested from the source leg on top of {@code perTypeLimit} so an exact-id match
   * sitting just outside the visible window can still be promoted to BEST MATCH before the per-type
   * cap is applied.
   */
  static final int BEST_MATCH_LOOKAHEAD = 5;

  /**
   * Types populated when {@code source=local} (the default) — the tenant's own IQ index carries every
   * entity. Presentation order matches the results-page tabs.
   */
  static final List<SuggestItemType> LOCAL_TYPES = List.of(
      SuggestItemType.VULNERABILITY,
      SuggestItemType.COMPONENT,
      SuggestItemType.APPLICATION,
      SuggestItemType.VIOLATION,
      SuggestItemType.WAIVER);

  /**
   * Types populated when {@code source=catalog} — the shared catalog only carries open-source
   * components and CVEs, not customer-scoped applications / violations / waivers.
   */
  static final List<SuggestItemType> CATALOG_TYPES = List.of(
      SuggestItemType.VULNERABILITY,
      SuggestItemType.COMPONENT);

  private final GlobalSearchSuggestIqLocalClient iqLocalClient;

  private final GlobalSearchSuggestCatalogClient catalogClient;

  private final BestMatchResolver bestMatchResolver;

  private final CurrentUser currentUser;

  private final PerUserRateLimiter rateLimiter;

  @Inject
  public SuggestServiceImpl(
      final GlobalSearchSuggestIqLocalClient iqLocalClient,
      final GlobalSearchSuggestCatalogClient catalogClient,
      final BestMatchResolver bestMatchResolver,
      final CurrentUser currentUser)
  {
    this(iqLocalClient, catalogClient, bestMatchResolver, currentUser,
        new PerUserRateLimiter(PerUserRateLimiter.SUGGEST_PERMITS_PER_USER));
  }

  /** Test-friendly constructor allowing the caller to inject a rate limiter. */
  SuggestServiceImpl(
      final GlobalSearchSuggestIqLocalClient iqLocalClient,
      final GlobalSearchSuggestCatalogClient catalogClient,
      final BestMatchResolver bestMatchResolver,
      final CurrentUser currentUser,
      final PerUserRateLimiter rateLimiter)
  {
    this.iqLocalClient = iqLocalClient;
    this.catalogClient = catalogClient;
    this.bestMatchResolver = bestMatchResolver;
    this.currentUser = currentUser;
    this.rateLimiter = rateLimiter;
  }

  @Override
  public SuggestResponse suggest(final String query, final SearchSource source) {
    final String username = currentUser == null ? null : currentUser.getUsernameOrSystem();
    try (PerUserRateLimiter.Permit ignored = rateLimiter.acquire(username)) {
      return source == SearchSource.CATALOG ? suggestCatalog(query) : suggestLocal(query);
    }
  }

  private SuggestResponse suggestLocal(final String query) {
    final int perTypeLimit = DEFAULT_PER_TYPE_LIMIT;
    final int fetchLimit = perTypeLimit + BEST_MATCH_LOOKAHEAD;
    final List<SuggestRow> rows = safeIqLocal(query, LOCAL_TYPES, fetchLimit, principalForRequest());
    // catalogAvailable is null on the local path: the catalog was never consulted, so the field is
    // omitted from the JSON rather than reported as an availability failure.
    return assemble(query, LOCAL_TYPES, rows, SearchSource.LOCAL, /* catalogAvailable */ null);
  }

  private SuggestResponse suggestCatalog(final String query) {
    if (!catalogClient.isEnabled()) {
      // Unentitled catalog: empty catalog groups, never reach HDS, catalogAvailable=false.
      return assemble(query, CATALOG_TYPES, List.of(), SearchSource.CATALOG, false);
    }
    final CatalogSuggestResult result = safeCatalog(query, DEFAULT_PER_TYPE_LIMIT);
    return assemble(query, CATALOG_TYPES, result.rows(), SearchSource.CATALOG, result.available());
  }

  /**
   * Groups the source-leg rows by type (uncapped so BEST MATCH sees the expanded window), resolves
   * BEST MATCH, then applies the per-type and total row caps.
   */
  private SuggestResponse assemble(
      final String query,
      final List<SuggestItemType> types,
      final List<SuggestRow> rows,
      final SearchSource requestSource,
      final Boolean catalogAvailable)
  {
    // Empty groups report the REQUESTED source, not one derived from availability: a source=catalog
    // request whose catalog is unavailable still labels its empty groups CATALOG (catalogAvailable
    // separately signals the failure), so the UI picks the right empty-state message.
    final Map<SuggestItemType, List<SuggestRow>> grouped = groupByType(rows, types);

    final List<SuggestRow> candidates = new ArrayList<>();
    for (SuggestItemType type : types) {
      candidates.addAll(grouped.getOrDefault(type, List.of()));
    }
    final SuggestRow bestMatch = bestMatchResolver.resolve(query, candidates);

    final List<SuggestGroup> groups = new ArrayList<>(types.size());
    for (SuggestItemType type : types) {
      final List<SuggestRow> capped = capped(grouped.getOrDefault(type, List.of()), DEFAULT_PER_TYPE_LIMIT);
      groups.add(new SuggestGroup(type, sourceForGroup(capped, requestSource), capped));
    }

    final List<SuggestGroup> dedup = bestMatch == null ? groups : withoutBestMatchInGroup(groups, bestMatch);
    final List<SuggestGroup> totalCapped = applyTotalCap(dedup, bestMatch);
    return new SuggestResponse(bestMatch, totalCapped, catalogAvailable);
  }

  private static Map<SuggestItemType, List<SuggestRow>> groupByType(
      final List<SuggestRow> rows,
      final List<SuggestItemType> types)
  {
    final Map<SuggestItemType, List<SuggestRow>> byType = new EnumMap<>(SuggestItemType.class);
    for (SuggestItemType type : types) {
      byType.put(type, new ArrayList<>());
    }
    for (SuggestRow row : rows) {
      final List<SuggestRow> bucket = byType.get(row.type());
      if (bucket != null) {
        bucket.add(row);
      }
    }
    return byType;
  }

  private static SearchSource sourceForGroup(final List<SuggestRow> rows, final SearchSource requestSource) {
    // A populated group reports its own rows' source; an empty group reports the requested source so
    // the UI labels empty sections consistently with what was asked for, even on a degraded catalog.
    return rows.isEmpty() ? requestSource : rows.get(0).source();
  }

  private List<SuggestRow> safeIqLocal(
      final String query,
      final List<SuggestItemType> types,
      final int fetchLimit,
      final UserPrincipal principal)
  {
    try {
      final List<SuggestRow> result = iqLocalClient.suggest(query, types, fetchLimit, principal);
      return result == null ? List.of() : result;
    }
    catch (RuntimeException re) {
      // IQ-local failure shouldn't sink the whole response. Degrade to empty rows. Never log the query.
      // Typeahead is high-frequency, so keep the WARN to a one-line message (no stack) and put the full
      // stack behind DEBUG to avoid flooding the log on a persistent local-index fault.
      log.warn("IQ-local suggest leg failed; degrading to empty IQ rows: {}", re.toString());
      log.debug("IQ-local suggest leg failure detail", re);
      return List.of();
    }
  }

  private CatalogSuggestResult safeCatalog(final String query, final int perTypeLimit) {
    try {
      // Size the single mixed HDS limit to fill BOTH catalog-served groups even on a lopsided response.
      // HDS returns COMPONENT and VULNERABILITY rows interleaved under one limit, so a run of one type
      // could otherwise starve the other before its cap is reached. Request the per-type cap plus the
      // BEST MATCH look-ahead for each type so an exact match just past the visible slice can still
      // promote (matching the local leg), and so a lopsided mix still leaves enough of each type to
      // fill both caps in the common case.
      final int upstreamLimit = Math.max(1, (perTypeLimit + BEST_MATCH_LOOKAHEAD) * CATALOG_TYPES.size());
      final CatalogSuggestResult result = catalogClient.suggest(new CatalogSuggestRequest(query, upstreamLimit));
      return result == null ? CatalogSuggestResult.unavailable() : result;
    }
    catch (RuntimeException re) {
      // Any RuntimeException here is unexpected: the catalog client is contracted to translate every
      // upstream failure (5xx, 429, timeout, malformed payload) into CatalogSuggestResult.unavailable()
      // rather than throw. Reaching this catch means a programmer error in the client (e.g. building an
      // invalid CatalogSuggestRequest), not an outage. Degrade the catalog groups rather than 500.
      log.debug("Catalog client threw unexpectedly (contract violation); degrading catalog groups", re);
      return CatalogSuggestResult.unavailable();
    }
  }

  /**
   * The IQ-local leg scopes every query against the thread-local Shiro security context (via
   * {@code IqLocalSearchService.buildPermittedQuery}), exactly like the results leg, which passes no
   * principal at all. The explicit principal here is only a defensive gate: the client short-circuits
   * to empty on a {@code null} principal so a system/service context (which has no indexable per-user
   * rows) degrades to an empty local section rather than serving unfiltered rows. Such a caller can
   * clear the controller's read-context gate yet get a 200 with empty local groups, which is
   * acceptable and consistent with the results leg's behaviour.
   */
  private UserPrincipal principalForRequest() {
    if (currentUser == null) {
      return null;
    }
    try {
      return currentUser.getUserPrincipal();
    }
    catch (RuntimeException re) {
      log.debug("CurrentUser.getUserPrincipal() failed; passing null to IQ-local (returns empty)", re);
      return null;
    }
  }

  private static List<SuggestRow> capped(final List<SuggestRow> rows, final int perTypeLimit) {
    if (rows.size() <= perTypeLimit) {
      return List.copyOf(rows);
    }
    return new ArrayList<>(rows.subList(0, perTypeLimit));
  }

  private static List<SuggestGroup> withoutBestMatchInGroup(
      final List<SuggestGroup> groups,
      final SuggestRow bestMatch)
  {
    final List<SuggestGroup> out = new ArrayList<>(groups.size());
    for (SuggestGroup g : groups) {
      if (g.type() != bestMatch.type()) {
        out.add(g);
        continue;
      }
      // Identity (==) comparison is safe: bestMatch is the exact SuggestRow instance BestMatchResolver
      // returned from the candidates list, and capped()/the parallel local fan-out both preserve element
      // references (List.copyOf and subList copy references; the fan-out never reconstructs rows). This
      // invariant spans the local mappers too: GlobalSearchSuggestIqLocalClientImpl's per-type mappers
      // must emit each SuggestRow once and never re-wrap an already-produced row. If a future capped() or
      // mapper rebuilt rows, SuggestRow's value-based equals() would make == silently miss the row —
      // switch this to an id+type match then.
      List<SuggestRow> filtered = null;
      for (int i = 0; i < g.results().size(); i++) {
        if (g.results().get(i) == bestMatch) {
          filtered = new ArrayList<>(g.results());
          filtered.remove(i);
          break;
        }
      }
      out.add(filtered == null ? g : new SuggestGroup(g.type(), g.source(), filtered));
    }
    return out;
  }

  /**
   * Drops rows from the tail of the groups so the total (BEST MATCH + sum of group rows) is at most
   * {@link #TOTAL_ROW_CAP}. Group order is preserved; the last group's rows are trimmed first. Empty
   * groups are still emitted so the UI can render the section headers.
   */
  static List<SuggestGroup> applyTotalCap(final List<SuggestGroup> groups, final SuggestRow bestMatch) {
    int budget = TOTAL_ROW_CAP - (bestMatch == null ? 0 : 1);
    if (budget < 0) {
      budget = 0;
    }
    int total = 0;
    for (SuggestGroup g : groups) {
      total += g.results().size();
    }
    if (total <= budget) {
      return groups;
    }
    final List<SuggestGroup> trimmed = new ArrayList<>(groups.size());
    int remaining = budget;
    for (SuggestGroup g : groups) {
      if (remaining <= 0) {
        trimmed.add(new SuggestGroup(g.type(), g.source(), List.of()));
        continue;
      }
      if (g.results().size() <= remaining) {
        trimmed.add(g);
        remaining -= g.results().size();
      }
      else {
        trimmed.add(new SuggestGroup(g.type(), g.source(),
            new ArrayList<>(g.results().subList(0, remaining))));
        remaining = 0;
      }
    }
    return trimmed;
  }
}
