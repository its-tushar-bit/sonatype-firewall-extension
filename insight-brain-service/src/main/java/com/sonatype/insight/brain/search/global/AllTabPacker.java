/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packs paginated rows for {@link Tab#ALL} from the per-section streams in the fixed presentation
 * order given by {@link Tab#values()} minus {@link Tab#ALL} (Applications, Components, Vulnerabilities,
 * Violations, Waivers).
 *
 * <p>
 * Pagination crosses entity-type boundaries seamlessly: if Applications has 3 rows and Components has
 * 40, page 1 (pageSize=25) returns 3 Applications + the first 22 Components; page 2 returns the next 18
 * Components + the first 7 Vulnerabilities. There is no per-section cap on the ALL tab.
 *
 * <p>
 * Each section is supplied as a lazy {@code SectionSupplier}: the packer pulls additional rows from a
 * section only when its current rows have been consumed. This keeps the heap bounded — the packer never
 * fully materialises a section it does not need to read.
 *
 * <p>
 * <b>Total-estimate semantics.</b> The packer eagerly probes every section to read its
 * {@link SectionResult#totalEstimate()} (via {@code ensureFirstFetched()}). When the first packed page
 * lands inside an early section, late sections still trigger one upstream fetch each to surface a total
 * estimate. This is intentional for the simple sequential dispatcher — a lazy aggregation rewrite is
 * dominated by the parallel-orchestrator follow-up that wraps the dispatcher.
 *
 * <p>
 * TODO: switch to lazy total aggregation once the parallel orchestrator lands.
 */
public final class AllTabPacker
{
  private static final Logger log = LoggerFactory.getLogger(AllTabPacker.class);

  /**
   * Fixed presentation order. Derived from {@link Tab#values()} (minus {@link Tab#ALL}) so a code change
   * to the enum cannot silently drop a section from the ALL tab.
   */
  public static final List<Tab> SECTION_ORDER = buildSectionOrder();

  private static List<Tab> buildSectionOrder() {
    List<Tab> order = new ArrayList<>();
    for (Tab t : Tab.values()) {
      if (t != Tab.ALL) {
        order.add(t);
      }
    }
    return List.copyOf(order);
  }

  /**
   * Per-section first-fetch timeout on the parallel fan-out. Sections that miss this budget are skipped
   * (empty rows, {@code totalEstimate=0}) and the rest of the packing pass proceeds.
   */
  static final long SECTION_FETCH_TIMEOUT_MILLIS = 500L;

  /**
   * Suffix appended to a tab name when its section is retired by the first-fetch timeout. A single
   * budget covers every section (local Lucene legs and the catalog leg alike); the trade-off for one
   * budget is that a slow local section is dropped silently unless we surface it — hence every timeout
   * skip emits this warning so the caller can distinguish "no rows" from "section unavailable".
   */
  static final String WARNING_SECTION_TIMED_OUT_SUFFIX = " section timed out";

  static final String WARNING_SECTION_UNAVAILABLE_SUFFIX = " section unavailable";

  static String sectionTimedOutWarning(Tab tab) {
    return tab.name() + WARNING_SECTION_TIMED_OUT_SUFFIX;
  }

  static String sectionUnavailableWarning(Tab tab) {
    return tab.name() + WARNING_SECTION_UNAVAILABLE_SUFFIX;
  }

  /**
   * Hard bound on consecutive empty pages (with a non-null next cursor) tolerated per section before
   * that section is treated as exhausted. High enough to absorb transient empties (e.g. a whole page of
   * hits filtered out by permissions) but low enough to terminate quickly on a hostile supplier.
   */
  static final int MAX_CONSECUTIVE_EMPTY_FETCHES = 3;

  /**
   * Total empty-with-continuation fetches allowed across the whole packing pass. Scales with the section
   * count so an early section spending its per-section empty budget cannot starve a later section (e.g.
   * WAIVER) of its own budget. Anything beyond this on a single request is a bug or a hostile supplier.
   */
  static final int MAX_EMPTY_FETCHES_PER_REQUEST = SECTION_ORDER.size() * MAX_CONSECUTIVE_EMPTY_FETCHES;

  /**
   * Shared virtual-thread executor for parallel per-section first-fetch. Bounded by the JVM's cheap
   * virtual-thread cost rather than a fixed pool size.
   *
   * <p>
   * A cancelled-but-still-running section fetch (see {@code parallelFirstFetch}) can briefly hold an
   * upstream connection after the caller returned, because {@code cancel(true)} only interrupts and the
   * catalog HDS call is not interruptible. The real bound on that orphaned work is the catalog client's
   * per-call socket timeout ({@code GlobalSearchCatalogHdsClient.CATALOG_SOCKET_TIMEOUT_MILLIS}); the
   * dedicated catalog connection pool (default 20) and the per-user request cap upstream keep a burst
   * from accumulating unbounded in-flight requests. {@link #FIRST_FETCH_PERMITS} is a hard safety
   * ceiling on top of those bounds so a pathological burst cannot fan out unbounded orphaned tasks.
   */
  private static final ExecutorService FIRST_FETCH_EXECUTOR = Executors.newThreadPerTaskExecutor(
      namedVirtualThreadFactory());

  /**
   * Hard ceiling on concurrent in-flight first-fetch tasks across all ALL-tab requests. Sized generously
   * so it never bites normal load (bounded far below this by PerUserRateLimiter + the catalog socket
   * timeout); it only caps a pathological burst of slow, uninterruptible legs. A section that cannot get
   * a permit is retired as unavailable rather than queued, so the ceiling never serializes normal load.
   */
  static final int FIRST_FETCH_PERMITS = 512;

  @VisibleForTesting
  static final Semaphore FIRST_FETCH_SEMAPHORE = new Semaphore(FIRST_FETCH_PERMITS);

  private static ThreadFactory namedVirtualThreadFactory() {
    AtomicLong counter = new AtomicLong();
    return runnable -> Thread.ofVirtual()
        .name("global-search-alltab-", counter.incrementAndGet())
        .unstarted(runnable);
  }

  /**
   * Lazy supplier for a single section's pages.
   *
   * <p>
   * {@link #nextPage(String)} accepts a cursor returned by the previous call's
   * {@link SectionResult#nextSearchAfter()} (or {@code null} on the first call) and returns the next page.
   */
  @FunctionalInterface
  public interface SectionSupplier
  {
    SectionResult nextPage(String searchAfter);
  }

  private AllTabPacker() {
  }

  /**
   * Packs the requested page across all sections.
   *
   * @param suppliers per-section lazy suppliers, keyed by {@link Tab}
   * @param page 1-indexed page number (only used when {@code resumeFromCursor} is {@code null})
   * @param pageSize page size, bounded by {@link ResultsRequest#MAX_PAGE_SIZE}
   * @param resumeFromCursor optional {@link AllTabCursor}; when present, packing resumes from the saved
   *          per-section cursors instead of replaying earlier pages from scratch
   * @return the packed slice, the {@link AllTabCursor} for the next page (or {@code null} when fully
   *         exhausted), the de-duplicated per-section warnings (including a per-section timeout /
   *         unavailable marker), and {@code catalogAvailable} — {@code false} when any catalog-backed
   *         section reported the catalog source degraded.
   */
  @VisibleForTesting
  static PackResult pack(
      Function<Tab, SectionSupplier> suppliers,
      int page,
      int pageSize,
      AllTabCursor resumeFromCursor)
  {
    return pack(suppliers, page, pageSize, resumeFromCursor, null, SearchSource.DEFAULT);
  }

  /** Overload that pins to the default source. */
  @VisibleForTesting
  static PackResult pack(
      Function<Tab, SectionSupplier> suppliers,
      int page,
      int pageSize,
      AllTabCursor resumeFromCursor,
      String sortKey)
  {
    return pack(suppliers, page, pageSize, resumeFromCursor, sortKey, SearchSource.DEFAULT);
  }

  /**
   * Packs the requested page across all sections, pinning the emitted next-page cursor to the supplied
   * sort key and source so a caller who mutates either between pages sees a stale-cursor 410 rather than
   * silently mispaging.
   */
  public static PackResult pack(
      Function<Tab, SectionSupplier> suppliers,
      int page,
      int pageSize,
      AllTabCursor resumeFromCursor,
      String sortKey,
      SearchSource source)
  {
    if (page < 1) {
      throw new IllegalArgumentException("page must be >= 1");
    }
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize must be >= 1");
    }

    Map<Tab, SectionReader> readers = new EnumMap<>(Tab.class);
    for (Tab section : SECTION_ORDER) {
      SectionSupplier supplier = suppliers.apply(section);
      AllTabCursor.SectionCursor sc = resumeFromCursor == null ? null : resumeFromCursor.cursorFor(section);
      readers.put(section, new SectionReader(section, supplier, sc));
    }

    // Fan out the first-fetch across all sections in parallel so a slow leg cannot serialize the others.
    // Each section has its own per-call timeout; timed-out sections are marked skipped rather than
    // failing the whole request.
    parallelFirstFetch(readers);

    if (resumeFromCursor == null) {
      long toSkip = (long) (page - 1) * (long) pageSize;
      while (toSkip > 0) {
        ResultRow skipped = nextRowAcrossSections(readers);
        if (skipped == null) {
          break;
        }
        toSkip--;
      }
    }

    List<ResultRow> packed = new ArrayList<>(pageSize);
    while (packed.size() < pageSize) {
      if (totalEmptyFetches(readers) > MAX_EMPTY_FETCHES_PER_REQUEST) {
        // Whole-request budget exceeded across all sections — the packer stops here rather than keep
        // pulling from a misbehaving supplier.
        break;
      }
      ResultRow row = nextRowAcrossSections(readers);
      if (row == null) {
        break;
      }
      packed.add(row);
    }

    long totalEstimate = 0L;
    boolean catalogAvailable = true;
    // Per-section raw (uncapped) totals, retained (not just summed) so the caller can surface a count
    // badge per section; the 10000 ceiling is applied downstream by ResultsService. A section that timed
    // out, failed, or came back degraded is left absent from the map rather than recorded as 0, so the
    // caller can distinguish "no hits" from "section unavailable".
    Map<Tab, Long> sectionTotals = new EnumMap<>(Tab.class);
    // Preserve SECTION_ORDER and dedup exact-string matches so ALL-tab warnings surface the same way
    // single-tab responses do (X-Search-Warnings / body.warnings).
    LinkedHashSet<String> warnings = new LinkedHashSet<>();
    for (Tab section : SECTION_ORDER) {
      SectionReader reader = readers.get(section);
      if (reader.timedOut()) {
        // Surface every timeout skip so a slow section reads as "unavailable", not silently empty.
        warnings.add(sectionTimedOutWarning(section));
        continue;
      }
      if (reader.failed()) {
        warnings.add(sectionUnavailableWarning(section));
        continue;
      }
      SectionResult last = reader.lastFetchedResult();
      if (last != null) {
        totalEstimate += last.totalEstimate();
        // A degraded section reports a successful result carrying 0 (catalog HDS failure, or a
        // not-entitled / MTIQ deployment), so its count is not a trustworthy zero and is omitted for
        // the same reason a timed-out or failed section is. The section still contributes to
        // totalEstimate, rows, warnings and the catalogAvailable reduction below.
        if (last.catalogAvailable()) {
          sectionTotals.put(section, last.totalEstimate());
        }
        warnings.addAll(last.warnings());
        // Any catalog-backed section that came back degraded flips the response-level flag so the
        // caller can distinguish "catalog returned nothing" from "catalog was unavailable".
        catalogAvailable &= last.catalogAvailable();
      }
    }

    AllTabCursor nextCursor = buildNextCursor(readers, sortKey, pageSize, source);
    return new PackResult(packed, totalEstimate, sectionTotals, nextCursor, List.copyOf(warnings),
        catalogAvailable);
  }

  /**
   * Count-only fan-out: probes every section's first page in parallel (the same virtual-thread pool +
   * bounded semaphore + per-section timeout {@link #parallelFirstFetch} uses for the ALL-tab pack) and
   * returns each section's reported {@code totalEstimate}, keyed by {@link Tab}. Rows are never
   * materialised beyond the single first-fetch page. A section that timed out, failed, was retired at the
   * permit ceiling, or came back degraded is OMITTED from the returned map — mirroring
   * {@link PackResult#sectionTotals()} so the caller can render a placeholder rather than a misleading
   * {@code 0}. Sections listed in {@code skip} are not probed at all (e.g. the caller-active tab whose
   * total is already known).
   *
   * @param suppliers per-section lazy suppliers, keyed by {@link Tab}
   * @param skip sections to omit from the probe (their totals are supplied by the caller); {@code null} is
   *          treated as an empty set (probe every section)
   * @return per-section {@code totalEstimate}, timed-out/unavailable sections omitted
   */
  public static Map<Tab, Long> countTotals(Function<Tab, SectionSupplier> suppliers, Set<Tab> skip) {
    // A null skip means "probe every section"; normalise here so the membership check below cannot NPE.
    Set<Tab> skipped = skip == null ? Set.of() : skip;
    Map<Tab, SectionReader> readers = new EnumMap<>(Tab.class);
    for (Tab section : SECTION_ORDER) {
      if (skipped.contains(section)) {
        continue;
      }
      readers.put(section, new SectionReader(section, suppliers.apply(section), null));
    }
    parallelFirstFetch(readers);
    Map<Tab, Long> totals = new EnumMap<>(Tab.class);
    for (Map.Entry<Tab, SectionReader> e : readers.entrySet()) {
      SectionReader reader = e.getValue();
      // A timed-out / failed / permit-retired section is omitted, not recorded as 0, so the caller can
      // distinguish "no hits" from "section unavailable" (same semantics as sectionTotals()).
      if (reader.timedOut() || reader.failed()) {
        continue;
      }
      SectionResult last = reader.lastFetchedResult();
      // A degraded section reports a successful 0 it cannot vouch for, so it is omitted for the same
      // reason, keeping these totals consistent with sectionTotals().
      if (last != null && last.catalogAvailable()) {
        totals.put(e.getKey(), last.totalEstimate());
      }
    }
    return totals;
  }

  private static void parallelFirstFetch(Map<Tab, SectionReader> readers) {
    Map<Tab, Future<?>> futures = new EnumMap<>(Tab.class);
    for (Map.Entry<Tab, SectionReader> e : readers.entrySet()) {
      SectionReader reader = e.getValue();
      if (reader.firstFetchDone || reader.exhausted) {
        continue;
      }
      // Each section runs buildPermittedQuery, which reads the caller's Shiro Subject (and tenant) from
      // thread-locals. Shiro 2.x no longer propagates the Subject to child threads, so a raw
      // virtual-thread task would see a null principal, fail-close to zero permitted rows, and return
      // an empty section for every authenticated caller. TenantAwareOneTimeRunnable captures the
      // caller's Subject + tenant here (on the calling thread) and re-associates them inside the task,
      // so the permission filter still runs against the real caller (fail-closed, never weakened).
      if (!FIRST_FETCH_SEMAPHORE.tryAcquire()) {
        // Safety ceiling hit: rather than fan out unbounded orphaned tasks, retire this section as
        // unavailable. Under normal load permits are never exhausted; this fires only on a pathological
        // burst of slow, uninterruptible legs.
        log.warn("ALL-tab first-fetch permit ceiling ({}) reached; retiring section {}", FIRST_FETCH_PERMITS,
            e.getKey());
        reader.retire(SectionReader.SkipReason.FAILED);
        continue;
      }
      Runnable task = new TenantAwareOneTimeRunnable(reader::ensureFirstFetched);
      futures.put(e.getKey(), FIRST_FETCH_EXECUTOR.submit(() -> {
        try {
          task.run();
        }
        finally {
          FIRST_FETCH_SEMAPHORE.release();
        }
      }));
    }
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SECTION_FETCH_TIMEOUT_MILLIS);
    for (Map.Entry<Tab, Future<?>> e : futures.entrySet()) {
      long remaining = deadline - System.nanoTime();
      if (remaining < 0) {
        remaining = 0;
      }
      try {
        e.getValue().get(remaining, TimeUnit.NANOSECONDS);
      }
      catch (TimeoutException te) {
        log.debug("ALL-tab section {} first-fetch timed out; skipping", e.getKey());
        // cancel(true) only interrupts; a non-interruptible HDS/Lucene call keeps running to completion.
        // The reader's skipped-guard drops that late result, and the per-call socket timeout on the
        // catalog leg (GlobalSearchCatalogHdsClient.CATALOG_SOCKET_TIMEOUT_MILLIS) is the real upper
        // bound on the orphaned upstream work; cancellation here is best-effort/advisory only.
        e.getValue().cancel(true);
        readers.get(e.getKey()).retire(SectionReader.SkipReason.TIMED_OUT);
      }
      catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        readers.get(e.getKey()).retire(SectionReader.SkipReason.TIMED_OUT);
      }
      catch (ExecutionException ee) {
        // A stale per-section cursor is a client-visible 410, not a degradable section failure: let it
        // propagate so the caller is told to retry from page 1 instead of silently seeing an empty
        // section. Every other failure degrades the section only.
        if (ee.getCause() instanceof StaleCursorException sce) {
          throw sce;
        }
        log.debug("ALL-tab section {} first-fetch failed; skipping", e.getKey(), ee.getCause());
        readers.get(e.getKey()).retire(SectionReader.SkipReason.FAILED);
      }
    }
  }

  private static int totalEmptyFetches(Map<Tab, SectionReader> readers) {
    int total = 0;
    for (SectionReader reader : readers.values()) {
      total += reader.consecutiveEmptyFetches;
    }
    return total;
  }

  private static ResultRow nextRowAcrossSections(Map<Tab, SectionReader> readers) {
    for (Tab section : SECTION_ORDER) {
      SectionReader reader = readers.get(section);
      if (reader.hasNext()) {
        return reader.next();
      }
    }
    return null;
  }

  private static AllTabCursor buildNextCursor(
      Map<Tab, SectionReader> readers,
      String sortKey,
      int pageSize,
      SearchSource source)
  {
    boolean anyMore = false;
    Map<Tab, AllTabCursor.SectionCursor> cursors = new EnumMap<>(Tab.class);
    for (Tab section : SECTION_ORDER) {
      SectionReader reader = readers.get(section);
      AllTabCursor.SectionCursor sc = reader.savedCursor();
      if (sc == null) {
        continue;
      }
      cursors.put(section, sc);
      if (!sc.exhausted()) {
        anyMore = true;
      }
    }
    // If every recorded entry is an exhausted marker, the result set is fully drained and we should not
    // emit a continuation cursor.
    return anyMore ? new AllTabCursor(sortKey, pageSize, source, cursors) : null;
  }

  /**
   * Lightweight streaming reader for one section: lazily pulls upstream pages and tracks how many rows of
   * the current page have been consumed so a partial page can be resumed in a future packer pass.
   *
   * <p>
   * Invariant: the cursor passed to the supplier to fetch the current page is preserved as
   * {@link #currentPageCursor}. If the packer stops mid-page, {@link #savedCursor()} returns
   * {@code (currentPageCursor, currentIndex)} so the next packer pass re-fetches the same page and skips the
   * consumed rows. If the current page is fully drained, {@code savedCursor()} returns
   * {@code (nextPageCursor, 0)}.
   */
  private static final class SectionReader
      implements Iterator<ResultRow>
  {
    private final Tab tab;

    private final SectionSupplier supplier;

    private List<ResultRow> currentRows = List.of();

    private int currentIndex;

    /** Cursor that produced {@link #currentRows} (i.e. what we passed to {@link #supplier}). */
    private String currentPageCursor;

    /** Cursor to use on the next {@link #fetchPage(String)} call to advance to the next upstream page. */
    private String nextPageCursor;

    private SectionResult lastFetchedResult;

    private boolean exhausted;

    private volatile boolean firstFetchDone;

    enum SkipReason
    {
      TIMED_OUT,
      FAILED
    }

    /**
     * Authoritative once set: a section retired by the first-fetch fan-out (timeout or failure) stays
     * empty. A late, non-interruptible upstream call that completes after the skip MUST NOT publish its
     * rows back into this reader (see {@link #fetchPage(String)}), so a retired section can never
     * resurrect stale partial state.
     */
    private volatile boolean skipped;

    private volatile SkipReason skipReason;

    private int consecutiveEmptyFetches;

    SectionReader(Tab tab, SectionSupplier supplier, AllTabCursor.SectionCursor resume) {
      this.tab = tab;
      this.supplier = supplier;
      if (resume != null) {
        if (resume.exhausted()) {
          // The section was fully drained in a prior packer pass; mark as exhausted so we never re-fetch.
          this.exhausted = true;
          this.firstFetchDone = true;
        }
        else {
          // Resume: the saved cursor IS the cursor we passed last time, and the saved skip is how many rows
          // of that page we already consumed.
          this.nextPageCursor = resume.upstreamCursor();
          this.currentIndex = resume.skipWithinPage();
        }
      }
    }

    boolean timedOut() {
      return skipReason == SkipReason.TIMED_OUT;
    }

    boolean failed() {
      return skipReason == SkipReason.FAILED;
    }

    SectionResult lastFetchedResult() {
      ensureFirstFetched();
      return lastFetchedResult;
    }

    AllTabCursor.SectionCursor savedCursor() {
      if (exhausted) {
        // Sticky exhausted marker so the next packer pass does not re-fetch this section from row 0.
        return new AllTabCursor.SectionCursor(null, 0, true);
      }
      if (!firstFetchDone) {
        // Section was never read in this pass. Carry forward whatever resume marker we had.
        if (currentIndex == 0 && nextPageCursor == null) {
          return null;
        }
        return AllTabCursor.SectionCursor.nonExhausted(nextPageCursor, currentIndex);
      }
      if (currentIndex < currentRows.size()) {
        // Mid-page boundary: re-issue the same upstream cursor, skip the rows already consumed.
        return AllTabCursor.SectionCursor.nonExhausted(currentPageCursor, currentIndex);
      }
      // Current page fully drained.
      if (nextPageCursor != null) {
        return AllTabCursor.SectionCursor.nonExhausted(nextPageCursor, 0);
      }
      // Section drained AND no further upstream pages — mark as exhausted so future cursors propagate it.
      return new AllTabCursor.SectionCursor(null, 0, true);
    }

    @Override
    public boolean hasNext() {
      ensureFirstFetched();
      while (currentIndex >= currentRows.size() && !exhausted) {
        if (nextPageCursor == null) {
          exhausted = true;
          break;
        }
        fetchPage(nextPageCursor);
      }
      return currentIndex < currentRows.size();
    }

    @Override
    public ResultRow next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return currentRows.get(currentIndex++);
    }

    private void ensureFirstFetched() {
      String cursor;
      int skip;
      synchronized (this) {
        if (firstFetchDone) {
          return;
        }
        // Claim the first fetch atomically so at most one thread runs it (the fan-out task or,
        // if the fan-out already retired this section, no one). Snapshot the resume cursor/skip
        // under the lock, then run the blocking supplier call OUTSIDE the monitor below.
        firstFetchDone = true;
        cursor = nextPageCursor;
        skip = currentIndex;
      }
      // fetchPage runs supplier.nextPage() outside the lock and publishes under it, applying the
      // resume skip in the same synchronized block so a concurrent retire() cannot be overwritten.
      fetchPage(cursor, skip);
    }

    /**
     * Called by the parallel fan-out when this section missed its first-fetch budget or failed. Marks
     * the reader as exhausted and firstFetchDone so subsequent packing steps skip it cleanly, and
     * records the reason so the packer can surface an accurate warning.
     *
     * <p>
     * Acquires the monitor promptly: {@link #fetchPage(String, int)} runs the blocking supplier call
     * outside the lock, so this never waits the full upstream query duration to retire the section.
     */
    synchronized void retire(SkipReason reason) {
      skipped = true;
      skipReason = reason;
      firstFetchDone = true;
      exhausted = true;
      currentRows = List.of();
      currentIndex = 0;
      nextPageCursor = null;
    }

    private void fetchPage(String cursor) {
      fetchPage(cursor, 0);
    }

    /**
     * Fetches one upstream page. The blocking {@code supplier.nextPage(cursor)} call runs OUTSIDE the
     * monitor so a slow upstream query never holds the lock; {@link #retire(SkipReason)} can therefore
     * acquire the monitor promptly instead of waiting the full upstream duration (well past
     * {@link #SECTION_FETCH_TIMEOUT_MILLIS}). The catalog leg is additionally bounded by
     * {@code GlobalSearchCatalogHdsClient.CATALOG_SOCKET_TIMEOUT_MILLIS}; IQ-LOCAL legs have no socket
     * timeout here, which is exactly why the lock must not be held across the call. The result is
     * published only inside the {@code synchronized} block below, and dropped if the section was
     * retired meanwhile, so a late result can never resurrect a skipped section.
     *
     * <p>
     * {@code skipWithinPage} is the resume offset for a first fetch (0 for subsequent pages).
     */
    private void fetchPage(String cursor, int skipWithinPage) {
      SectionResult result = supplier.nextPage(cursor);
      synchronized (this) {
        if (skipped) {
          // The first-fetch timeout already retired this section while the (non-interruptible) upstream
          // call was still running. Drop the late result rather than resurrect the skipped section.
          return;
        }
        publishFetch(cursor, result, skipWithinPage);
      }
    }

    private void publishFetch(String cursor, SectionResult result, int skipWithinPage) {
      // Caller holds the monitor; writes are published atomically with the skipped-guard check.
      currentPageCursor = cursor;
      lastFetchedResult = result;
      currentRows = result.rows();
      nextPageCursor = result.nextSearchAfter();
      currentIndex = Math.min(skipWithinPage, currentRows.size());
      if (currentRows.isEmpty()) {
        if (nextPageCursor == null) {
          exhausted = true;
        }
        else {
          // Defence against a misbehaving supplier that keeps returning an empty page with a non-null
          // next cursor: count consecutive empty-with-continuation fetches and treat the section as
          // exhausted once the bound is reached.
          consecutiveEmptyFetches++;
          if (consecutiveEmptyFetches >= MAX_CONSECUTIVE_EMPTY_FETCHES) {
            exhausted = true;
            nextPageCursor = null;
          }
        }
      }
      else {
        consecutiveEmptyFetches = 0;
      }
    }
  }

  /**
   * Result of a single packing pass. {@code warnings} aggregates the distinct per-section warnings;
   * {@code catalogAvailable} is {@code false} when any catalog-backed section reported the catalog
   * source degraded.
   */
  public record PackResult(
      List<ResultRow> rows,
      long totalEstimate,
      Map<Tab, Long> sectionTotals,
      AllTabCursor nextCursor,
      List<String> warnings,
      boolean catalogAvailable)
  {
    public PackResult {
      rows = rows == null ? List.of() : List.copyOf(rows);
      sectionTotals = sectionTotals == null ? Map.of() : Map.copyOf(sectionTotals);
      warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
  }
}
