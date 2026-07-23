/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalRow;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalSearchResponse;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.SearchInputs;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;

/**
 * Concrete Lucene-backed {@link GlobalSearchSuggestIqLocalClient} for Global Search {@code /suggest}.
 * Adapts {@link IqLocalSearchService} (the shared Lucene query executor) to the typeahead SPI shape
 * consumed by {@link SuggestServiceImpl}.
 *
 * <p>
 * Per requested {@link SuggestItemType} it maps to a {@link Tab} + native {@link ItemType} set, runs
 * one first-page relevance query through {@link IqLocalSearchService#search(SearchInputs)} (which
 * applies the caller's READ permission filter via {@code buildPermittedQuery}), and maps each returned
 * {@link SearchResultItemDTO} to a {@link SuggestRow} tagged {@link SearchSource#LOCAL}. No pagination
 * cursor is threaded — suggest fetches only the first page per type.
 *
 * <p>
 * <b>Parallel fan-out.</b> The per-type queries run concurrently on a shared virtual-thread executor so
 * a request for N types costs one query's latency, not N sequential ones. Each task is wrapped in a
 * {@link TenantAwareOneTimeRunnable}, which captures the caller's Shiro Subject and tenant on the
 * calling thread and re-associates them inside the worker; without this the worker would see a null
 * principal and {@code buildPermittedQuery} would fail-close to zero permitted rows (mirrors the
 * ALL-tab {@code AllTabPacker} fan-out). Results are merged back in the requested {@code types} order,
 * so parallel completion order never changes the grouping or best-match outcome. A single type's query
 * failing degrades that type to an empty group rather than failing the whole suggest. Each fan-out
 * permit is owned by {@code fanOut}'s stack frame (an {@link AutoCloseable} handle released in its
 * {@code finally}), so a permit is never leaked if {@code submit} throws before the worker runs, and a
 * slow worker is bounded by {@link #SUGGEST_FAN_OUT_TIMEOUT_MILLIS} rather than blocking the HTTP
 * thread until GC reclaims the future.
 *
 * <p>
 * <b>Authorization.</b> A {@code null} principal short-circuits to an empty list per the SPI contract;
 * a non-null principal relies on the thread-local security context that {@code IqLocalSearchService}
 * already scopes every query against. No href is emitted on any row — suggest rows stay within
 * Lifecycle.
 */
@Named
@Primary
@Singleton
public class GlobalSearchSuggestIqLocalClientImpl
    implements GlobalSearchSuggestIqLocalClient
{
  private static final Logger log = LoggerFactory.getLogger(GlobalSearchSuggestIqLocalClientImpl.class);

  /** Public suggest type -> the {@link Tab} whose native item types back it. */
  private static final Map<SuggestItemType, Tab> TAB_BY_TYPE;

  /** Public suggest type -> native IQ {@link ItemType} set queried for that type. */
  private static final Map<SuggestItemType, Set<ItemType>> NATIVE_TYPES_BY_TYPE;

  static {
    EnumMap<SuggestItemType, Tab> tabs = new EnumMap<>(SuggestItemType.class);
    tabs.put(SuggestItemType.VULNERABILITY, Tab.VULNERABILITY);
    tabs.put(SuggestItemType.COMPONENT, Tab.COMPONENT);
    tabs.put(SuggestItemType.APPLICATION, Tab.APPLICATION);
    tabs.put(SuggestItemType.VIOLATION, Tab.VIOLATION);
    tabs.put(SuggestItemType.WAIVER, Tab.WAIVER);
    TAB_BY_TYPE = Map.copyOf(tabs);

    EnumMap<SuggestItemType, Set<ItemType>> types = new EnumMap<>(SuggestItemType.class);
    types.put(SuggestItemType.VULNERABILITY, Set.of(ItemType.SECURITY_VULNERABILITY));
    types.put(SuggestItemType.COMPONENT, Set.of(ItemType.NON_VULNERABLE_COMPONENT));
    types.put(SuggestItemType.APPLICATION, Set.of(ItemType.APPLICATION));
    types.put(SuggestItemType.VIOLATION, Set.of(ItemType.POLICY_VIOLATION, ItemType.LEGAL_VIOLATION));
    types.put(SuggestItemType.WAIVER, Set.of(ItemType.POLICY_WAIVER));
    NATIVE_TYPES_BY_TYPE = Map.copyOf(types);
  }

  /**
   * Shared virtual-thread executor for the per-type fan-out. Bounded by the JVM's cheap virtual-thread
   * cost rather than a fixed pool size; {@link #FAN_OUT_SEMAPHORE} is the hard safety ceiling so a
   * pathological burst cannot fan out unbounded tasks.
   *
   * <p>
   * Deliberately left unmanaged (no {@code @PreDestroy} / {@code Environment.lifecycle().manage(...)}):
   * suggest is short-lived typeahead, in-flight tasks are virtual threads the JVM reclaims on exit, and
   * a graceful in-flight drain buys nothing for a 250ms-bounded query. This mirrors the sibling ALL-tab
   * {@code AllTabPacker.FIRST_FETCH_EXECUTOR}, which is also a static-final unmanaged executor; adding a
   * shutdown hook here alone would diverge from that established pattern for no benefit.
   */
  private static final ExecutorService FAN_OUT_EXECUTOR =
      Executors.newThreadPerTaskExecutor(namedVirtualThreadFactory());

  /**
   * Hard ceiling on concurrent in-flight per-type queries across all suggest requests. Sized to match
   * the ALL-tab fan-out ceiling; a type that cannot get a permit runs inline on the calling thread
   * rather than being dropped, so the ceiling degrades to sequential execution instead of losing rows.
   *
   * <p>
   * Relationship to the per-user cap: {@code PerUserRateLimiter.SUGGEST_PERMITS_PER_USER} (10) allows up
   * to 10 concurrent suggest requests per user, and each fans out over up to {@link SuggestItemType}
   * values (currently 5), so a single user can drive up to ~50 concurrent fan-out queries. This global
   * budget (512) bounds the aggregate across all users. Tune the two together.
   */
  static final int FAN_OUT_PERMITS = 512;

  @VisibleForTesting
  static final Semaphore FAN_OUT_SEMAPHORE = new Semaphore(FAN_OUT_PERMITS);

  /**
   * Per-type upper bound on how long the calling thread waits for a fan-out worker before degrading
   * that type to an empty group and cancelling its future. Suggest is typeahead (p95 &lt; 300ms), so a
   * single slow/hung Lucene query must not block the HTTP thread indefinitely; a few hundred ms leaves
   * headroom for the merge and keeps a wedged query from draining the permit pool. Shared as one
   * deadline across all awaited types, mirroring {@code AllTabPacker.SECTION_FETCH_TIMEOUT_MILLIS}.
   */
  @VisibleForTesting
  static final long SUGGEST_FAN_OUT_TIMEOUT_MILLIS = 250L;

  private static ThreadFactory namedVirtualThreadFactory() {
    AtomicLong counter = new AtomicLong();
    return runnable -> Thread.ofVirtual()
        .name("global-search-suggest-", counter.incrementAndGet())
        .unstarted(runnable);
  }

  private final IqLocalSearchService iqLocalSearchService;

  @Inject
  public GlobalSearchSuggestIqLocalClientImpl(final IqLocalSearchService iqLocalSearchService) {
    this.iqLocalSearchService = Objects.requireNonNull(iqLocalSearchService, "iqLocalSearchService");
  }

  @Override
  public List<SuggestRow> suggest(
      final String query,
      final List<SuggestItemType> types,
      final int perTypeLimit,
      final UserPrincipal principal)
  {
    if (principal == null) {
      // Authorization contract: no principal means system-context caller; never serve unfiltered rows.
      return List.of();
    }
    if (types == null || types.isEmpty() || perTypeLimit <= 0) {
      return List.of();
    }
    return fanOut(query, types, perTypeLimit);
  }

  /**
   * Fans the per-type queries out concurrently, then merges results in the requested {@code types}
   * order. Each worker is wrapped in {@link TenantAwareOneTimeRunnable} (captured on this thread) so it
   * runs the permission filter against the caller's real Subject/tenant (fail-closed, never weakened).
   *
   * <p>
   * Permit ownership lives entirely in this stack frame: each acquired permit is held in a
   * {@link Permit} handle that this method releases in its {@code finally}, so a permit is released
   * exactly once regardless of what happens inside {@code submit}, the worker, the await, or a timeout
   * — closing the leak window between {@code tryAcquire} and the worker actually starting. A type whose
   * query fails or times out degrades to an empty group; the rest of the request is unaffected.
   */
  private List<SuggestRow> fanOut(final String query, final List<SuggestItemType> types, final int perTypeLimit) {
    // De-duplicate up front so BOTH the submission and the merge loops iterate a single occurrence of
    // each type; the SPI does not promise a distinct list, and re-iterating raw types in the merge could
    // otherwise add a type's rows once per occurrence. LinkedHashSet preserves the requested order.
    final Set<SuggestItemType> distinctTypes = new LinkedHashSet<>(types);
    final Map<SuggestItemType, Future<List<SuggestRow>>> futures = new EnumMap<>(SuggestItemType.class);
    final Map<SuggestItemType, List<SuggestRow>> inline = new EnumMap<>(SuggestItemType.class);
    final List<Permit> permits = new ArrayList<>(distinctTypes.size());
    try {
      for (SuggestItemType type : distinctTypes) {
        final Permit permit = Permit.tryAcquire(FAN_OUT_SEMAPHORE);
        if (permit == null) {
          // Safety ceiling hit: run this type inline rather than dropping it, so RBAC filtering still
          // runs on the calling thread (which already holds the caller's Subject) and no rows are lost.
          inline.put(type, fetchType(query, type, perTypeLimit));
          continue;
        }
        permits.add(permit);
        // Capture the Subject/tenant on this thread, then submit. Both the wrap and the submit can throw
        // (e.g. RejectedExecutionException on a shut-down executor); the permit's release is owned by
        // this frame's finally, so a throw here never leaks the permit. Degrade the type to inline so no
        // rows are lost silently.
        try {
          futures.put(type, submitWorker(query, type, perTypeLimit));
        }
        catch (RuntimeException submitFailed) {
          log.debug("Suggest fan-out submit failed for type {}; running inline on the calling thread", type,
              submitFailed);
          inline.put(type, fetchType(query, type, perTypeLimit));
        }
      }

      final long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SUGGEST_FAN_OUT_TIMEOUT_MILLIS);
      final List<SuggestRow> out = new ArrayList<>();
      for (SuggestItemType type : distinctTypes) {
        if (inline.containsKey(type)) {
          out.addAll(inline.get(type));
          continue;
        }
        final Future<List<SuggestRow>> future = futures.get(type);
        if (future == null) {
          continue;
        }
        out.addAll(awaitType(type, future, deadlineNanos));
      }
      return out;
    }
    finally {
      // Release every acquired permit exactly once, on all paths (normal, submit throw, await throw,
      // timeout). Permit.close() is idempotent, so this is safe even if a permit was already released.
      //
      // FAN_OUT_PERMITS bounds TRACKED (admission) concurrency, not real thread lifetime: on timeout the
      // permit is released here as soon as we stop awaiting, but future.cancel(true) is best-effort — a
      // non-interruptible Lucene call can keep running briefly after its permit is freed. So under
      // sustained timeouts the REAL count of in-flight queries can momentarily exceed FAN_OUT_PERMITS.
      // That is acceptable and intentional: the 250ms await bound plus best-effort cancel keep the
      // overrun transient, and bounding hold time deterministically (rather than blocking until the
      // worker truly finishes) is what avoids the resource-exhaustion the caller-frame permit design
      // fixed. Holding the permit until real completion would reintroduce that risk.
      for (Permit permit : permits) {
        permit.close();
      }
    }
  }

  /**
   * Submits a per-type fetch wrapped in a {@link TenantAwareOneTimeRunnable} so the worker thread runs
   * the permission filter against the caller's real Subject/tenant (captured on this thread). Never
   * touches the permit — permit ownership is the caller's ({@link #fanOut}) stack frame.
   */
  private Future<List<SuggestRow>> submitWorker(
      final String query,
      final SuggestItemType type,
      final int perTypeLimit)
  {
    final List<List<SuggestRow>> holder = new ArrayList<>(1);
    final Runnable tenantAware =
        new TenantAwareOneTimeRunnable(() -> holder.add(fetchType(query, type, perTypeLimit)));
    return FAN_OUT_EXECUTOR.submit(() -> {
      tenantAware.run();
      return holder.isEmpty() ? List.of() : holder.get(0);
    });
  }

  /**
   * Awaits a single type's fan-out result, bounded by {@code deadlineNanos}. A failed, interrupted, or
   * timed-out query degrades that type to an empty group (logged at debug) rather than failing the
   * whole suggest; this mirrors the sequential path, where a per-type failure only affected that type.
   * On timeout the future is cancelled ({@code cancel(true)}) so a wedged worker is interrupted rather
   * than left holding resources; the permit itself is released by {@link #fanOut}'s {@code finally}.
   */
  private List<SuggestRow> awaitType(
      final SuggestItemType type,
      final Future<List<SuggestRow>> future,
      final long deadlineNanos)
  {
    final long remaining = Math.max(0L, deadlineNanos - System.nanoTime());
    try {
      return future.get(remaining, TimeUnit.NANOSECONDS);
    }
    catch (TimeoutException te) {
      log.debug("Suggest fan-out timed out for type {}; cancelling and degrading to empty group", type);
      // cancel(true) only interrupts; a non-interruptible Lucene call may run to completion, but its late
      // result is discarded and the permit is freed by fanOut's finally regardless.
      future.cancel(true);
      return List.of();
    }
    catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      future.cancel(true);
      log.debug("Suggest fan-out interrupted for type {}; degrading to empty group", type);
      return List.of();
    }
    catch (ExecutionException ee) {
      log.debug("Suggest fan-out failed for type {}; degrading to empty group", type, ee.getCause());
      return List.of();
    }
  }

  /**
   * Released-once handle over a permit acquired from {@link #FAN_OUT_SEMAPHORE}. Its lifetime is the
   * {@link #fanOut} stack frame that acquires it, so the permit is returned to the pool deterministically
   * on every path — mirroring {@code PerUserRateLimiter.Permit}.
   */
  private static final class Permit
      implements AutoCloseable
  {
    private final Semaphore semaphore;

    private boolean released;

    private Permit(final Semaphore semaphore) {
      this.semaphore = semaphore;
    }

    /** Returns a held {@link Permit}, or {@code null} when the ceiling is hit (no permit acquired). */
    static Permit tryAcquire(final Semaphore semaphore) {
      return semaphore.tryAcquire() ? new Permit(semaphore) : null;
    }

    @Override
    public void close() {
      if (!released) {
        released = true;
        semaphore.release();
      }
    }
  }

  private List<SuggestRow> fetchType(final String query, final SuggestItemType type, final int perTypeLimit) {
    final Tab tab = TAB_BY_TYPE.get(type);
    final Set<ItemType> nativeTypes = NATIVE_TYPES_BY_TYPE.get(type);
    if (tab == null || nativeTypes == null) {
      return List.of();
    }
    final SearchInputs inputs = new SearchInputs(
        query,
        tab,
        nativeTypes,
        perTypeLimit,
        GlobalSearchSortAllowlist.RELEVANCE,
        /* cursor */ null);

    final IqLocalSearchResponse response = iqLocalSearchService.search(inputs);
    final Function<SearchResultItemDTO, SuggestRow> mapper = rowMapperFor(type);
    final List<SuggestRow> rows = new ArrayList<>(response.rows().size());
    int dropped = 0;
    for (IqLocalRow raw : response.rows()) {
      final SuggestRow mapped = mapper.apply(raw.row());
      if (mapped == null) {
        dropped++;
        continue;
      }
      rows.add(mapped);
    }
    if (dropped > 0) {
      log.debug("Dropped {} malformed suggest rows for type {} (missing id or title)", dropped, type);
    }
    return rows;
  }

  static Function<SearchResultItemDTO, SuggestRow> rowMapperFor(final SuggestItemType type) {
    return switch (type) {
      case VULNERABILITY -> GlobalSearchSuggestIqLocalClientImpl::mapVulnerability;
      case COMPONENT -> GlobalSearchSuggestIqLocalClientImpl::mapComponent;
      case APPLICATION -> GlobalSearchSuggestIqLocalClientImpl::mapApplication;
      case VIOLATION -> GlobalSearchSuggestIqLocalClientImpl::mapViolation;
      case WAIVER -> GlobalSearchSuggestIqLocalClientImpl::mapWaiver;
    };
  }

  private static SuggestRow mapApplication(final SearchResultItemDTO doc) {
    if (doc.applicationId == null) {
      return null;
    }
    // A blank name falls back to the public id, then the app id, so the row is never dropped for an
    // empty title (SuggestRow rejects only null titles, not blank ones).
    final String title = firstNonBlank(doc.applicationName, doc.applicationPublicId, doc.applicationId);
    if (title == null) {
      return null;
    }
    return new SuggestRow(
        doc.applicationId,
        SuggestItemType.APPLICATION,
        SearchSource.LOCAL,
        title,
        doc.applicationPublicId,
        /* href */ null);
  }

  /** First non-blank value, or {@code null} when all are null/blank. */
  private static String firstNonBlank(final String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private static SuggestRow mapComponent(final SearchResultItemDTO doc) {
    if (doc.componentHash == null && doc.componentName == null && doc.componentIdentifier == null) {
      return null;
    }
    // Prefer a pkg: coordinate as the id so a pasted coordinate can promote a local component to BEST
    // MATCH, matching the catalog leg (which also carries the coordinate as its id). The coordinate is
    // rendered from the indexed componentIdentifier via the shared IQ purl converter, which wraps the
    // same com.github.packageurl builder the catalog leg uses. When no coordinate can be built, fall
    // back to the component hash (then name) so the row is never dropped for lacking a coordinate.
    final String coordinate = coordinateOf(doc);
    final String id = coordinate != null
        ? coordinate
        : (doc.componentHash != null ? doc.componentHash : doc.componentName);
    final String title = doc.componentName != null
        ? doc.componentName
        : (coordinate != null ? coordinate : doc.componentHash);
    // A doc with a componentIdentifier that cannot be rendered to a coordinate (malformed purl) and
    // no hash/name yields no usable id/title. Drop that single row rather than let the SuggestRow
    // compact constructor throw, which would degrade the whole COMPONENT type to an empty group.
    if (id == null || id.isBlank() || title == null || title.isBlank()) {
      return null;
    }
    return new SuggestRow(
        id,
        SuggestItemType.COMPONENT,
        SearchSource.LOCAL,
        title,
        /* subtitle */ null,
        /* href */ null);
  }

  /**
   * Renders a canonical {@code pkg:} coordinate for a local component doc from its indexed
   * {@link SearchResultItemDTO#componentIdentifier} (format + IQ coordinates). Returns {@code null}
   * when the identifier is absent or the purl cannot be built, so the caller falls back to the
   * hash/name id.
   */
  private static String coordinateOf(final SearchResultItemDTO doc) {
    final ApiComponentIdentifierDTOV2 identifier = doc.componentIdentifier;
    if (identifier == null || identifier.getFormat() == null || identifier.getFormat().isBlank()) {
      return null;
    }
    try {
      final ComponentIdentifier componentIdentifier = identifier.toComponentIdentifier();
      final String purl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
      return purl == null || purl.isBlank() ? null : purl;
    }
    catch (RuntimeException e) {
      log.debug("Could not render a coordinate for a local component (format {}); using hash/name id",
          identifier.getFormat());
      return null;
    }
  }

  private static SuggestRow mapVulnerability(final SearchResultItemDTO doc) {
    if (doc.vulnerabilityId == null) {
      return null;
    }
    return new SuggestRow(
        doc.vulnerabilityId,
        SuggestItemType.VULNERABILITY,
        SearchSource.LOCAL,
        doc.vulnerabilityId,
        doc.vulnerabilityDescription,
        /* href */ null);
  }

  private static SuggestRow mapViolation(final SearchResultItemDTO doc) {
    // Merged VIOLATION covers both POLICY_VIOLATION and LEGAL_VIOLATION docs; both carry the same
    // policyViolation* fields, so a single mapper handles both. A blank policy name falls back to the
    // violation id as the title rather than emitting an empty title; drop the row only when neither
    // yields a usable title (mirrors mapWaiver's blank-safe fallback).
    final String id = firstNonBlank(doc.policyViolationId);
    final String title = firstNonBlank(doc.policyViolationPolicyName, doc.policyViolationId);
    if (id == null || title == null) {
      return null;
    }
    return new SuggestRow(
        id,
        SuggestItemType.VIOLATION,
        SearchSource.LOCAL,
        title,
        doc.applicationPublicId,
        /* href */ null);
  }

  private static SuggestRow mapWaiver(final SearchResultItemDTO doc) {
    // Auto-waivers carry a synthetic policyName from indexing; a blank name still keeps the row and
    // falls back to the waiver id as the title rather than dropping it. A blank-string waiver id can
    // be indexed (setPolicyWaiverId only rejects null), so guard the id too: when neither the id nor
    // a policy name yields a usable value, drop this single row rather than let the SuggestRow compact
    // constructor throw, which would degrade the whole WAIVER group to an empty result.
    final String id = firstNonBlank(doc.policyWaiverId);
    final String title = firstNonBlank(doc.policyWaiverPolicyName, doc.policyWaiverId);
    if (id == null || title == null) {
      return null;
    }
    return new SuggestRow(
        id,
        SuggestItemType.WAIVER,
        SearchSource.LOCAL,
        title,
        /* subtitle */ null,
        /* href */ null);
  }
}
