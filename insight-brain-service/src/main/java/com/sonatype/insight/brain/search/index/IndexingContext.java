/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.search.ConversionHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.document.Document;

public abstract class IndexingContext
{
  private final OwnerDAO ownerDAO;

  private final Map<String, Owner> ownersById = new ConcurrentHashMap<>();

  private final Map<String, String> vulnDescByVulnId = new ConcurrentHashMap<>();

  private final Map<String, String> licenseNameById = new ConcurrentHashMap<>();

  /**
   * Memoized {@code applicationId} -> its category (tag) names, populated load-on-miss by
   * {@link #getCategoryNamesByApp}. Apps with no categories are absent (so the caller omits the
   * field), but {@link #categoryNamesLoadedApps} records the app ids already loaded so an app with
   * no categories is not re-queried on every call.
   */
  private final Map<String, List<String>> categoryNamesByApplicationId = new ConcurrentHashMap<>();

  /** App ids whose category-name load has already run (present here even when they had no categories). */
  private final Set<String> categoryNamesLoadedApps = ConcurrentHashMap.newKeySet();

  /**
   * Per-app category (tag) names, cached load-on-miss via {@code loader}. On each call the app ids
   * not yet loaded are collected and passed to {@code loader} as a single batch; the loader returns
   * the {@code appId -> category names} map for the apps that have categories (absent = none). The
   * full-reindex path pre-warms with all app ids (one chunked IN-clause query); the incremental
   * per-app path loads each missing app on demand (still a batch DAO call over just the missing
   * ids). Idempotent: an already-loaded app id is never re-queried, even when it has no categories.
   * Returns only the subset for {@code applicationIds} (O(requested)), so a caller cannot
   * accidentally iterate the whole accumulated cache; an app with no categories is absent.
   */
  public Map<String, List<String>> getCategoryNamesByApp(
      final Set<String> applicationIds,
      final Function<Set<String>, Map<String, List<String>>> loader)
  {
    loadMissing(applicationIds, categoryNamesLoadedApps, loader, categoryNamesByApplicationId::putAll);
    return subsetFor(applicationIds, categoryNamesByApplicationId);
  }

  /**
   * Memoized {@code applicationId} -> latest-evaluation epoch-millis, populated load-on-miss by
   * {@link #getLatestEvaluationEpochMsByApp}. Apps with no evaluation are absent (so the caller
   * omits the "never evaluated" field), but a {@link #latestEvaluationLoadedApps} marker records
   * which app ids have been loaded so a genuine "never evaluated" app is not re-queried on every
   * call.
   */
  private final Map<String, Long> latestEvaluationEpochMsByApp = new ConcurrentHashMap<>();

  /** App ids whose latest-evaluation load has already run (present here even when they had no evaluation row). */
  private final Set<String> latestEvaluationLoadedApps = ConcurrentHashMap.newKeySet();

  /**
   * Memoized {@code applicationId} -> its combined {@link ViolationRollup} (the active-only
   * {@code "stage:severity:count"} display tokens plus the denormalized filter/sort aggregates),
   * populated load-on-miss by {@link #getViolationRollupByApp}. Both are computed from ONE widened
   * violations query, so the pills and the aggregates share a single DB round-trip per app batch. Apps
   * with no unfixed violations are absent (so the caller omits the fields), but
   * {@link #violationRollupLoadedApps} records the app ids already loaded so a no-violation app is not
   * re-queried on every call.
   */
  private final Map<String, ViolationRollup> violationRollupByApp = new ConcurrentHashMap<>();

  /** App ids whose violation-rollup load has already run (present here even when they had no violations). */
  private final Set<String> violationRollupLoadedApps = ConcurrentHashMap.newKeySet();

  /**
   * Combined per-application violation rollup surfaced onto APPLICATION docs, computed in one widened
   * violations query.
   * <ul>
   * <li>{@code stageSeverityTokens} — the ACTIVE-only {@code "stage:severity:count"} display pills
   * (never null, possibly empty), unchanged by the wider fetch;</li>
   * <li>{@code maxThreatLevel} — max raw threat level across ACTIVE violations (null when none);</li>
   * <li>{@code stages}/{@code policyTypes} — sets derived from ACTIVE violations only (never null);</li>
   * <li>{@code states} — open/waived/legacy classified over the wider UNFIXED set so waived/legacy
   * surface (never null);</li>
   * <li>{@code stateSortOrdinal} — worst (min) state-sort priority across {@code states} (null when
   * the app has no unfixed violation).</li>
   * </ul>
   */
  public record ViolationRollup(
      List<String> stageSeverityTokens,
      Integer maxThreatLevel,
      Set<String> stages,
      Set<String> policyTypes,
      Set<String> states,
      Integer stateSortOrdinal)
  {
    public ViolationRollup {
      // Defend the Javadoc's "never null" contract and freeze the collections so the cached rollup
      // cannot be mutated after construction (maxThreatLevel/stateSortOrdinal are explicitly nullable).
      stageSeverityTokens = stageSeverityTokens == null ? List.of() : List.copyOf(stageSeverityTokens);
      stages = stages == null ? Set.of() : Set.copyOf(stages);
      policyTypes = policyTypes == null ? Set.of() : Set.copyOf(policyTypes);
      states = states == null ? Set.of() : Set.copyOf(states);
    }
  }

  /**
   * Latest-evaluation epoch-millis per app, cached load-on-miss via {@code loader}. On each call the
   * app ids not yet loaded are collected and passed to {@code loader} as a single batch; the loader
   * returns the {@code appId -> latest epoch-ms} map for the apps that have an evaluation (absent =
   * never evaluated). The full-reindex path pre-warms with all app ids (one batch query); the
   * incremental per-app path loads each missing app on demand (still a batch DAO call over just the
   * missing ids). Idempotent: an already-loaded app id is never re-queried, even when it has no
   * evaluation. Returns only the subset for {@code applicationIds} (O(requested)), so a caller
   * cannot accidentally iterate the whole accumulated cache; absent means "never evaluated".
   */
  public Map<String, Long> getLatestEvaluationEpochMsByApp(
      final Set<String> applicationIds,
      final Function<Set<String>, Map<String, Long>> loader)
  {
    loadMissing(applicationIds, latestEvaluationLoadedApps, loader, latestEvaluationEpochMsByApp::putAll);
    return subsetFor(applicationIds, latestEvaluationEpochMsByApp);
  }

  /**
   * Per-app {@code "stage:severity:count"} rollup tokens, cached load-on-miss via {@code loader}.
   * On each call the app ids not yet loaded are collected and passed to {@code loader} as a single
   * batch; the loader returns the {@code appId -> tokens} map for the apps that have unfixed
   * violations (absent = none). The full-reindex path pre-warms with all app ids (one batch query);
   * the incremental per-app path loads each missing app on demand (still a batch DAO call over just
   * the missing ids). Idempotent: an already-loaded app id is never re-queried, even when it has no
   * violations. Returns only the subset for {@code applicationIds} (O(requested)), so a caller
   * cannot accidentally iterate the whole accumulated cache; an app with no violations is absent.
   */
  public Map<String, ViolationRollup> getViolationRollupByApp(
      final Set<String> applicationIds,
      final Function<Set<String>, Map<String, ViolationRollup>> loader)
  {
    loadMissing(applicationIds, violationRollupLoadedApps, loader, violationRollupByApp::putAll);
    return subsetFor(applicationIds, violationRollupByApp);
  }

  /**
   * Determines which of {@code requestedIds} have not yet been loaded (tracked in {@code loadedIds}),
   * batch-loads only those via {@code loader}, stores the result via {@code cacheStore}, and marks
   * every requested id loaded — so apps with no rows are cached as "loaded, absent" and never
   * re-queried. Synchronized on {@code loadedIds} so a concurrent caller does not issue a duplicate
   * load for the same ids.
   */
  private static <V> void loadMissing(
      final Set<String> requestedIds,
      final Set<String> loadedIds,
      final Function<Set<String>, Map<String, V>> loader,
      final Consumer<Map<String, V>> cacheStore)
  {
    if (CollectionUtils.isEmpty(requestedIds) || loadedIds.containsAll(requestedIds)) {
      return;
    }
    // The loader (a DB batch query) runs under the lock to dedup concurrent loads of the same ids.
    // Reindex builds an IndexingContext single-threaded per run (full-reindex warms once with all
    // ids; the incremental path is one app at a time), so the lock is uncontended in practice.
    synchronized (loadedIds) {
      Set<String> missing = new HashSet<>(requestedIds);
      missing.removeAll(loadedIds);
      if (missing.isEmpty()) {
        return;
      }
      cacheStore.accept(loader.apply(missing));
      loadedIds.addAll(missing);
    }
  }

  /**
   * A copy of {@code cache} restricted to {@code requestedIds} (only ids actually present in the
   * cache). O(requested), so callers see only the entries they asked for rather than the whole
   * accumulated map.
   */
  private static <V> Map<String, V> subsetFor(final Set<String> requestedIds, final Map<String, V> cache) {
    if (CollectionUtils.isEmpty(requestedIds)) {
      return Map.of();
    }
    Map<String, V> subset = new HashMap<>();
    for (String id : requestedIds) {
      V value = cache.get(id);
      if (value != null) {
        subset.put(id, value);
      }
    }
    return subset;
  }

  /**
   * Memoized {@code org.getId()} -> its full ancestor-org id chain (incl. self), so the
   * {@code walkHierarchy} DB walk runs at most once per org per indexing run rather than per
   * document (the Label/Policy/Tag paths reindex many docs sharing an org).
   */
  private final Map<String, List<String>> ancestorOrgIdsByOrgId = new ConcurrentHashMap<>();

  /**
   * Per-run dedupe for the orphan-application WARN (see DocumentBuilderHelper). Scoped to this
   * context so it resets each reindex run — a recurring orphan re-WARNs on the next run rather
   * than being suppressed for the JVM lifetime, and it cannot grow unbounded across runs.
   */
  private final Set<String> orphanAppWarnedIds = ConcurrentHashMap.newKeySet();

  /** @return true the first time {@code applicationId} is seen this run (caller should WARN), false thereafter. */
  public boolean shouldWarnOrphanApp(final String applicationId) {
    return orphanAppWarnedIds.add(applicationId);
  }

  private final ConversionHelper conversionHelper;

  public IndexingContext(final OwnerDAO ownerDAO, final ConversionHelper conversionHelper) {
    this.ownerDAO = ownerDAO;
    this.conversionHelper = conversionHelper;
  }

  public Map<String, String> getVulnDescByVulnId() {
    return vulnDescByVulnId;
  }

  public Map<String, String> getLicenseNameById() {
    return licenseNameById;
  }

  public void addOwners(final Collection<? extends Owner> owners) {
    owners.forEach(owner -> ownersById.put(owner.getId(), owner));
  }

  public Owner getOwner(final String id) {
    return ownersById.computeIfAbsent(id, ownerDAO::getById);
  }

  /**
   * The org's ancestor-org id chain ({@code org, parent, ..., root}), computed via
   * {@link OwnerDAO#walkHierarchy(Owner)} once per org and cached for the run. Callers apply their
   * own sentinel filtering; this returns the raw ids.
   * <p>
   * The {@code walkHierarchy} DB walk runs under the {@link ConcurrentHashMap#computeIfAbsent} bin
   * lock, but that walk is a bounded hierarchy traversal (org depth) and reindex builds an
   * IndexingContext single-threaded per run, so the lock is uncontended and its latency is not a
   * concern in practice (mirrors {@code loadMissing}).
   */
  public List<String> getAncestorOrgIds(final Organization org) {
    if (org == null) {
      return List.of();
    }
    return ancestorOrgIdsByOrgId.computeIfAbsent(org.getId(), id -> {
      List<String> ids = new java.util.ArrayList<>();
      ownerDAO.walkHierarchy(org).forEach(o -> ids.add(o.getId()));
      return ids;
    });
  }

  public String newQuery(final FieldIdentifier fieldIdentifier, final String fieldValue) {
    return fieldIdentifier.label + ":" + fieldValue;
  }

  public abstract void deleteDocuments(final String query) throws IOException;

  public abstract void addDocuments(final List<Document> documents) throws IOException;

  public void addNonNullDocuments(final List<Document> documents) throws IOException {
    if (CollectionUtils.isEmpty(documents)) {
      return;
    }
    addDocuments(documents.stream().filter(Objects::nonNull).toList());
  }

  public ConversionHelper getConversionHelper() {
    return conversionHelper;
  }
}
