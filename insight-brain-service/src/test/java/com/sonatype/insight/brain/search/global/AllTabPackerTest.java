/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.lang.util.LifecycleUtils;
import org.apache.shiro.util.ThreadContext;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class AllTabPackerTest
{
  private static ResultRow row(Tab tab, int idx) {
    return ResultRow.builder()
        .type(tab.name())
        .source(SearchSource.LOCAL.value())
        .id(tab.name() + "-" + idx)
        .title(tab.name() + " row " + idx)
        .build();
  }

  /** Supplier that returns its entire fixture as one upstream page (no pagination from upstream). */
  private static AllTabPacker.SectionSupplier singlePageSupplier(Tab tab, int count) {
    return cursor -> {
      List<ResultRow> rows = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        rows.add(row(tab, i));
      }
      return new SectionResult(tab, rows, count, null, true);
    };
  }

  private static AllTabPacker.SectionSupplier empty(Tab tab) {
    return cursor -> SectionResult.empty(tab);
  }

  private static Function<Tab, AllTabPacker.SectionSupplier> suppliersFromMap(
      Map<Tab, AllTabPacker.SectionSupplier> map)
  {
    return t -> map.getOrDefault(t, empty(t));
  }

  @Test
  public void components3_vulnerabilities40_pageSize25_page1() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.COMPONENT, singlePageSupplier(Tab.COMPONENT, 3));
    map.put(Tab.VULNERABILITY, singlePageSupplier(Tab.VULNERABILITY, 40));

    AllTabPacker.PackResult page1 = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    assertThat(page1.rows()).hasSize(25);
    long componentRows = page1.rows().stream().filter(r -> r.getType().equals(Tab.COMPONENT.name())).count();
    long vulnRows = page1.rows().stream().filter(r -> r.getType().equals(Tab.VULNERABILITY.name())).count();
    assertThat(componentRows).isEqualTo(3);
    assertThat(vulnRows).isEqualTo(22);
    assertThat(page1.nextCursor()).isNotNull();
  }

  @Test
  public void components3_vulnerabilities40_apps10_pageSize25_page2() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.COMPONENT, singlePageSupplier(Tab.COMPONENT, 3));
    map.put(Tab.VULNERABILITY, singlePageSupplier(Tab.VULNERABILITY, 40));
    map.put(Tab.APPLICATION, singlePageSupplier(Tab.APPLICATION, 10));

    AllTabPacker.PackResult page1 = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);
    assertThat(page1.rows()).hasSize(25);

    AllTabPacker.PackResult page2 = AllTabPacker.pack(suppliersFromMap(map), 2, 25, page1.nextCursor());

    // SECTION_ORDER packs APPLICATION (10) then COMPONENT (3) then VULNERABILITY. Page 1 holds all 10
    // apps + 3 components + 12 vulnerabilities; page 2 is the next 25 vulnerabilities.
    long vulnRows = page2.rows().stream().filter(r -> r.getType().equals(Tab.VULNERABILITY.name())).count();
    long appRows = page2.rows().stream().filter(r -> r.getType().equals(Tab.APPLICATION.name())).count();
    assertThat(page2.rows()).hasSize(25);
    assertThat(vulnRows).isEqualTo(25);
    assertThat(appRows).isEqualTo(0);
    assertThat(page2.nextCursor()).isNotNull();
  }

  @Test
  public void emptyAndSparseSections_areSkippedCleanly() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.COMPONENT, empty(Tab.COMPONENT));
    map.put(Tab.VULNERABILITY, empty(Tab.VULNERABILITY));
    map.put(Tab.APPLICATION, singlePageSupplier(Tab.APPLICATION, 4));

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    assertThat(result.rows()).hasSize(4);
    assertThat(result.rows())
        .allMatch(r -> r.getType().equals(Tab.APPLICATION.name()));
    assertThat(result.nextCursor()).isNull();
  }

  @Test
  public void singleSection_fitsPage1_andNextCursorIsNull() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.COMPONENT, singlePageSupplier(Tab.COMPONENT, 4));

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    assertThat(result.rows()).hasSize(4);
    assertThat(result.nextCursor()).isNull();
  }

  @Test
  public void presentationOrder_followsSectionOrder() {
    // Five rows in every section; pageSize=100 grabs everything across sections.
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    for (Tab t : AllTabPacker.SECTION_ORDER) {
      map.put(t, singlePageSupplier(t, 5));
    }

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 100, null);

    assertThat(result.rows()).hasSize(AllTabPacker.SECTION_ORDER.size() * 5);
    // Each section's rows are packed contiguously in SECTION_ORDER order.
    for (int sectionIdx = 0; sectionIdx < AllTabPacker.SECTION_ORDER.size(); sectionIdx++) {
      Tab expected = AllTabPacker.SECTION_ORDER.get(sectionIdx);
      for (int rowIdx = 0; rowIdx < 5; rowIdx++) {
        int packedIdx = sectionIdx * 5 + rowIdx;
        assertThat(result.rows().get(packedIdx).getType()).isEqualTo(expected.name());
      }
    }
  }

  @Test
  public void totalEstimate_isSumOfPerSectionTotalsObserved() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.COMPONENT, cursor -> new SectionResult(Tab.COMPONENT, List.of(row(Tab.COMPONENT, 0)), 100, null, true));
    map.put(Tab.VULNERABILITY,
        cursor -> new SectionResult(Tab.VULNERABILITY, List.of(row(Tab.VULNERABILITY, 0)), 250, null, true));

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    assertThat(result.totalEstimate()).isEqualTo(350L);
  }

  @Test
  public void sectionTotals_retainsEachSectionTotalObserved() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.COMPONENT, cursor -> new SectionResult(Tab.COMPONENT, List.of(row(Tab.COMPONENT, 0)), 100, null, true));
    map.put(Tab.VULNERABILITY,
        cursor -> new SectionResult(Tab.VULNERABILITY, List.of(row(Tab.VULNERABILITY, 0)), 250, null, true));

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    assertThat(result.sectionTotals()).containsEntry(Tab.COMPONENT, 100L);
    assertThat(result.sectionTotals()).containsEntry(Tab.VULNERABILITY, 250L);
    // Sections with no fixture report an empty section (total 0) and are still recorded.
    assertThat(result.sectionTotals()).containsEntry(Tab.APPLICATION, 0L);
    assertThat(result.sectionTotals()).doesNotContainKey(Tab.ALL);
  }

  @Test
  public void sectionTotals_omitsRetiredSection_distinguishesUnavailableFromZero() {
    // Drain the first-fetch permits so every section retires as unavailable (no sleep needed). A retired
    // section must be ABSENT from sectionTotals so the caller can tell "unavailable" from "0 hits".
    int drained = AllTabPacker.FIRST_FETCH_SEMAPHORE.drainPermits();
    try {
      Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
      map.put(Tab.APPLICATION, singlePageSupplier(Tab.APPLICATION, 5));
      map.put(Tab.COMPONENT, singlePageSupplier(Tab.COMPONENT, 5));

      AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

      assertThat(result.sectionTotals()).doesNotContainKey(Tab.APPLICATION);
      assertThat(result.sectionTotals()).doesNotContainKey(Tab.COMPONENT);
    }
    finally {
      AllTabPacker.FIRST_FETCH_SEMAPHORE.release(drained);
    }
  }

  @Test
  public void sectionTotals_omitsDegradedSection_ratherThanRecordingItsZero() {
    // A degraded catalog section returns a SUCCESSFUL result carrying 0 with catalogAvailable=false
    // (HDS 5xx/429/timeout, or a not-entitled / MTIQ deployment): it never times out and never throws,
    // so it must be filtered on the catalogAvailable flag or its untrustworthy 0 would be recorded as a
    // real count. ALL then also renders as 0 despite nothing having been counted.
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.COMPONENT,
        cursor -> new SectionResult(Tab.COMPONENT, List.of(), 0L, null, false, List.of("catalog unavailable")));
    map.put(Tab.VULNERABILITY,
        cursor -> new SectionResult(Tab.VULNERABILITY, List.of(), 0L, null, false, List.of("catalog unavailable")));
    map.put(Tab.APPLICATION, cursor -> new SectionResult(
        Tab.APPLICATION, List.of(row(Tab.APPLICATION, 0)), 7L, null, true));

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    assertThat(result.sectionTotals()).doesNotContainKey(Tab.COMPONENT);
    assertThat(result.sectionTotals()).doesNotContainKey(Tab.VULNERABILITY);
    // An available section still records its count, including a genuine zero.
    assertThat(result.sectionTotals()).containsEntry(Tab.APPLICATION, 7L);
    // The degraded sections still flip the response-level flag and still feed totalEstimate/warnings.
    assertThat(result.catalogAvailable()).isFalse();
    assertThat(result.warnings()).contains("catalog unavailable");
  }

  @Test
  public void countTotals_omitsDegradedSection_ratherThanRecordingItsZero() {
    // Same omit rule on the count-only fan-out, so single-tab badge counts agree with sectionTotals().
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.COMPONENT, cursor -> new SectionResult(Tab.COMPONENT, List.of(), 0L, null, false));
    map.put(Tab.APPLICATION, cursor -> new SectionResult(
        Tab.APPLICATION, List.of(row(Tab.APPLICATION, 0)), 7L, null, true));

    Map<Tab, Long> totals = AllTabPacker.countTotals(suppliersFromMap(map), null);

    assertThat(totals).doesNotContainKey(Tab.COMPONENT);
    assertThat(totals).containsEntry(Tab.APPLICATION, 7L);
  }

  @Test
  public void warnings_aggregatedAcrossSections_deduplicatedInSectionOrder() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.APPLICATION, cursor -> new SectionResult(
        Tab.APPLICATION, List.of(row(Tab.APPLICATION, 0)), 1, null, true, List.of("shared warning", "app warning")));
    map.put(Tab.COMPONENT, cursor -> new SectionResult(
        Tab.COMPONENT, List.of(row(Tab.COMPONENT, 0)), 1, null, true, List.of("shared warning", "component warning")));

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    // Distinct warnings, deduplicated, in SECTION_ORDER (APPLICATION before COMPONENT).
    assertThat(result.warnings()).containsExactly("shared warning", "app warning", "component warning");
  }

  @Test
  public void packerTerminatesWhenSupplierLoops_emptyRowsWithNonNullCursor() {
    // A misbehaving supplier returns an empty page with a non-null next cursor every time. The packer
    // must bound the number of consecutive empty fetches per section and treat the section as
    // exhausted rather than spin forever.
    final int[] callCount = {0};
    AllTabPacker.SectionSupplier looper = cursor -> {
      callCount[0]++;
      return new SectionResult(Tab.APPLICATION, List.of(), 0L, "forever", true);
    };
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.APPLICATION, looper);

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    assertThat(result.rows()).isEmpty();
    // MAX_CONSECUTIVE_EMPTY_FETCHES per section * SECTION_ORDER.size() is the upper bound. The fixture
    // only registers APPLICATION but the packer probes every section once for its first fetch, so we
    // bound by MAX_CONSECUTIVE_EMPTY_FETCHES * SECTION_COUNT to catch runaway loops.
    assertThat(callCount[0]).isLessThanOrEqualTo(
        AllTabPacker.MAX_CONSECUTIVE_EMPTY_FETCHES * AllTabPacker.SECTION_ORDER.size());
  }

  @Test
  public void emptyWithCursorEarlySections_doNotStarveLaterWaiverSection() {
    // Every section EXCEPT WAIVER (last in SECTION_ORDER) returns empty-with-continuation pages (e.g.
    // whole pages filtered out by RBAC) until it drains at MAX_CONSECUTIVE_EMPTY_FETCHES. Summed across
    // the four early sections that is 4 * 3 = 12 empty fetches. WAIVER has real rows. With a
    // whole-request empty-fetch budget that scales with section count, the packer must still reach
    // WAIVER and pack its rows rather than breaking on the accumulated empties from earlier sections.
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    for (Tab t : AllTabPacker.SECTION_ORDER) {
      if (t == Tab.WAIVER) {
        map.put(t, singlePageSupplier(Tab.WAIVER, 3));
      }
      else {
        map.put(t, cursor -> new SectionResult(t, List.of(), 0L, "more", true));
      }
    }

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    assertThat(result.rows()).hasSize(3);
    assertThat(result.rows()).allMatch(r -> r.getType().equals(Tab.WAIVER.name()));
  }

  @Test
  public void emptyFetchBudget_scalesWithSectionCount() {
    assertThat(AllTabPacker.MAX_EMPTY_FETCHES_PER_REQUEST)
        .isEqualTo(AllTabPacker.SECTION_ORDER.size() * AllTabPacker.MAX_CONSECUTIVE_EMPTY_FETCHES);
  }

  @Test
  public void sectionOrder_matchesTabValuesMinusAll() {
    List<Tab> expected = Arrays.stream(Tab.values()).filter(t -> t != Tab.ALL).toList();
    assertThat(AllTabPacker.SECTION_ORDER).containsExactlyElementsOf(expected);
  }

  @Test
  public void parallelFirstFetch_slowSectionTimesOut_othersReturn() throws Exception {
    // Supplier that sleeps well past SECTION_FETCH_TIMEOUT_MILLIS on its first call. The packer must
    // skip that section but return rows from the fast section.
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.APPLICATION, cursor -> {
      try {
        // Well beyond the 500ms timeout — test uses parkNanos rather than Thread.sleep to avoid
        // holding the platform-thread lock on interruption.
        java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.SECONDS.toNanos(5));
      }
      catch (Exception ignored) {
      }
      return new SectionResult(Tab.APPLICATION, List.of(row(Tab.APPLICATION, 0)), 1L, null, true);
    });
    map.put(Tab.COMPONENT, singlePageSupplier(Tab.COMPONENT, 2));

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    // COMPONENT rows are returned; APPLICATION was skipped by timeout so its rows are absent.
    long componentRows = result.rows().stream().filter(r -> r.getType().equals(Tab.COMPONENT.name())).count();
    assertThat(componentRows).isEqualTo(2);
    assertThat(result.rows()).noneMatch(r -> r.getType().equals(Tab.APPLICATION.name()));
  }

  @Test
  public void parallelFirstFetch_allSectionsRunConcurrently() {
    // Register a supplier for every section. Each supplier increments a counter and sleeps 200ms; the
    // 500ms timeout budget is enough for all seven to complete when they run in parallel but far too
    // little if they ran sequentially (7 * 200ms = 1.4s).
    AtomicInteger calls = new AtomicInteger();
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    for (Tab t : AllTabPacker.SECTION_ORDER) {
      map.put(t, cursor -> {
        calls.incrementAndGet();
        java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(200));
        return new SectionResult(t, List.of(row(t, 0)), 1L, null, true);
      });
    }

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 100, null);

    assertThat(calls.get()).isEqualTo(AllTabPacker.SECTION_ORDER.size());
    // Every section ran and contributed one row within the 500ms fan-out budget. Because a purely
    // sequential fan-out would need SECTION_ORDER.size() * 200ms (far past the budget), every section returning a
    // row proves the first fetches ran concurrently, without a brittle wall-clock timing assertion.
    assertThat(result.rows()).hasSize(AllTabPacker.SECTION_ORDER.size());
  }

  @Test
  public void parallelFirstFetch_propagatesCallerShiroSubjectToSectionWorkerThreads() {
    // Regression for the ALL-tab "0 rows for every authenticated user" bug: the per-section first
    // fetches run on virtual worker threads, and Shiro 2.x does NOT propagate the caller's Subject to
    // child threads. Without explicit propagation buildPermittedQuery reads a null principal on the
    // worker, fail-closes to zero permitted rows, and every section returns empty. The packer now
    // wraps each task in TenantAwareOneTimeRunnable, which re-associates the caller's Subject on the
    // worker. This test binds a Subject with a known principal on the calling thread and asserts each
    // section supplier sees that SAME principal on its worker thread (never null), and that the
    // sections are populated.
    String callerPrincipal = "user-42";
    DefaultSecurityManager sm = new DefaultSecurityManager();
    Subject caller = new Subject.Builder(sm)
        .principals(new SimplePrincipalCollection(callerPrincipal, "test-realm"))
        .buildSubject();
    ThreadContext.bind(sm);
    ThreadContext.bind(caller);
    try {
      Map<Tab, Object> seenPrincipalByTab = new ConcurrentHashMap<>();
      Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
      for (Tab t : AllTabPacker.SECTION_ORDER) {
        map.put(t, cursor -> {
          // Captured on the WORKER thread. If the Subject were not propagated this would be null.
          Object principal = org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
          seenPrincipalByTab.put(t, principal == null ? "<null>" : principal);
          return new SectionResult(t, List.of(row(t, 0)), 1L, null, true);
        });
      }

      AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 100, null);

      // Every section ran with the caller's real principal (not null / anonymous).
      assertThat(seenPrincipalByTab).isNotEmpty();
      assertThat(seenPrincipalByTab.values()).allMatch(callerPrincipal::equals);
      assertThat(seenPrincipalByTab.keySet()).containsExactlyInAnyOrderElementsOf(AllTabPacker.SECTION_ORDER);
      // Sections are populated (the fix does not weaken filtering, it restores the principal so the
      // filter passes rows through).
      assertThat(result.rows()).hasSize(AllTabPacker.SECTION_ORDER.size());
    }
    finally {
      ThreadContext.unbindSubject();
      ThreadContext.unbindSecurityManager();
      LifecycleUtils.destroy(sm);
    }
  }

  @Test
  public void packerResumesViaCursor_acrossThreePages() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.COMPONENT, singlePageSupplier(Tab.COMPONENT, 7));
    map.put(Tab.VULNERABILITY, singlePageSupplier(Tab.VULNERABILITY, 7));
    map.put(Tab.APPLICATION, singlePageSupplier(Tab.APPLICATION, 7));

    AllTabPacker.PackResult p1 = AllTabPacker.pack(suppliersFromMap(map), 1, 10, null);
    AllTabPacker.PackResult p2 = AllTabPacker.pack(suppliersFromMap(map), 2, 10, p1.nextCursor());
    AllTabPacker.PackResult p3 = AllTabPacker.pack(suppliersFromMap(map), 3, 10, p2.nextCursor());

    assertThat(p1.rows()).hasSize(10);
    assertThat(p2.rows()).hasSize(10);
    assertThat(p3.rows()).hasSize(1);

    // Rows in SECTION_ORDER: APPLICATION 0..6 (7), COMPONENT 0..6 (7), VULNERABILITY 0..6 (7) = 21 total.
    List<ResultRow> combined = new ArrayList<>();
    combined.addAll(p1.rows());
    combined.addAll(p2.rows());
    combined.addAll(p3.rows());
    assertThat(combined).hasSize(21);
    assertThat(combined.get(0).getId()).isEqualTo("APPLICATION-0");
    assertThat(combined.get(6).getId()).isEqualTo("APPLICATION-6");
    assertThat(combined.get(7).getId()).isEqualTo("COMPONENT-0");
    assertThat(combined.get(13).getId()).isEqualTo("COMPONENT-6");
    assertThat(combined.get(14).getId()).isEqualTo("VULNERABILITY-0");
    assertThat(combined.get(20).getId()).isEqualTo("VULNERABILITY-6");
  }

  @Test
  public void slowSection_pastTimeout_surfacesTimeoutWarning_andStaysEmpty() throws Exception {
    // A section whose first fetch blocks well past SECTION_FETCH_TIMEOUT_MILLIS is retired: it must not
    // contribute rows, and it must surface a timeout warning (never silently serve 0 rows).
    java.util.concurrent.CountDownLatch released = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.atomic.AtomicBoolean latePublished = new java.util.concurrent.atomic.AtomicBoolean(false);

    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.APPLICATION, singlePageSupplier(Tab.APPLICATION, 3));
    map.put(Tab.COMPONENT, cursor -> {
      try {
        // Block past the fan-out budget so the packer times this section out, then complete anyway to
        // simulate a non-interruptible upstream call finishing after the skip.
        released.await(5, java.util.concurrent.TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      latePublished.set(true);
      return new SectionResult(Tab.COMPONENT, List.of(row(Tab.COMPONENT, 0), row(Tab.COMPONENT, 1)), 2L, null, true);
    });

    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

    // COMPONENT was retired by timeout: no COMPONENT rows, and a timeout warning is present.
    assertThat(result.rows()).noneMatch(r -> r.getType().equals(Tab.COMPONENT.name()));
    assertThat(result.warnings()).contains(AllTabPacker.sectionTimedOutWarning(Tab.COMPONENT));
    // The fast section still packed.
    assertThat(result.rows().stream().filter(r -> r.getType().equals(Tab.APPLICATION.name())).count())
        .isEqualTo(3);

    // Let the slow fetch complete AFTER the pack returned; its late result must not resurrect the
    // retired section (the reader dropped it under the skipped-guard). The already-returned PackResult
    // is immutable, so re-assert it is unaffected.
    released.countDown();
    // Wait for the exact event — the late fetch running its (dropped) publish — instead of a fixed
    // sleep, so the assertion is deterministic under CI load.
    await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).untilTrue(latePublished);
    assertThat(result.rows()).noneMatch(r -> r.getType().equals(Tab.COMPONENT.name()));
  }

  @Test
  public void retire_notBlockedBySlowSupplier_packReturnsWhileUpstreamStillRunning() throws Exception {
    // Regression for the lock-contention finding: the blocking supplier.nextPage() runs OUTSIDE the
    // reader monitor, so retire() (called on the request thread when the fan-out deadline fires) can
    // set skipped and return promptly instead of waiting the full upstream query duration. This test
    // proves the request thread does NOT wait for the slow supplier: pack() returns while the slow
    // supplier is STILL blocked (never released before pack returns) and well before its 30s ceiling.
    java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.atomic.AtomicBoolean release = new java.util.concurrent.atomic.AtomicBoolean(false);
    java.util.concurrent.atomic.AtomicBoolean supplierCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);

    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.APPLICATION, singlePageSupplier(Tab.APPLICATION, 2));
    map.put(Tab.COMPONENT, cursor -> {
      entered.countDown();
      // Non-interruptible block, modelling a real HDS/Lucene/OpenSearch call that cancel(true) cannot
      // stop: park in a loop that ignores interrupts until the test explicitly releases it AFTER
      // pack() has returned. If retire() were blocked on this reader's monitor the whole pack() would
      // stall here for the 30s ceiling.
      long ceiling = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
      while (!release.get() && System.nanoTime() < ceiling) {
        java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(10));
      }
      supplierCompleted.set(true);
      return new SectionResult(Tab.COMPONENT, List.of(row(Tab.COMPONENT, 0)), 1L, null, true);
    });

    long start = System.nanoTime();
    AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);
    long elapsedMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    // The slow supplier actually entered its blocking call, and pack() returned while it was still
    // blocked (not yet released, not yet completed) — the request thread never waited on it.
    assertThat(entered.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    assertThat(supplierCompleted.get()).isFalse();
    // Generous upper bound: pack() must finish shortly after the 500ms fan-out deadline, nowhere near
    // the 30s block. Kept loose to avoid CI flakiness while still failing if retire() serialized on
    // the slow query.
    assertThat(elapsedMillis).isLessThan(10_000L);
    // The slow section contributed nothing; the fast section packed.
    assertThat(result.rows()).noneMatch(r -> r.getType().equals(Tab.COMPONENT.name()));
    assertThat(result.rows().stream().filter(r -> r.getType().equals(Tab.APPLICATION.name())).count())
        .isEqualTo(2);
    assertThat(result.warnings()).contains(AllTabPacker.sectionTimedOutWarning(Tab.COMPONENT));

    // Release the still-running upstream call; its late result must be dropped, not resurrect the
    // retired section (skipped-guard). Wait on the exact completion event rather than sleeping.
    release.set(true);
    await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).untilTrue(supplierCompleted);
    assertThat(result.rows()).noneMatch(r -> r.getType().equals(Tab.COMPONENT.name()));
  }

  @Test
  public void firstFetchPermitCeiling_retiresSectionsAsUnavailable() {
    // Drain the global first-fetch permits to simulate a pathological burst that has hit the ceiling.
    // Every section must then retire as unavailable rather than fanning out unbounded orphaned tasks.
    int drained = AllTabPacker.FIRST_FETCH_SEMAPHORE.drainPermits();
    try {
      Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
      map.put(Tab.APPLICATION, singlePageSupplier(Tab.APPLICATION, 5));
      map.put(Tab.COMPONENT, singlePageSupplier(Tab.COMPONENT, 5));

      AllTabPacker.PackResult result = AllTabPacker.pack(suppliersFromMap(map), 1, 25, null);

      assertThat(result.rows()).isEmpty();
      assertThat(result.warnings())
          .contains(AllTabPacker.sectionUnavailableWarning(Tab.APPLICATION))
          .contains(AllTabPacker.sectionUnavailableWarning(Tab.COMPONENT));
    }
    finally {
      AllTabPacker.FIRST_FETCH_SEMAPHORE.release(drained);
    }
  }

  @Test
  public void countTotals_nullSkip_probesEverySection() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.APPLICATION, singlePageSupplier(Tab.APPLICATION, 3));
    map.put(Tab.COMPONENT, singlePageSupplier(Tab.COMPONENT, 7));

    Map<Tab, Long> totals = AllTabPacker.countTotals(suppliersFromMap(map), null);

    assertThat(totals).containsEntry(Tab.APPLICATION, 3L).containsEntry(Tab.COMPONENT, 7L);
    assertThat(totals.keySet()).containsExactlyInAnyOrderElementsOf(AllTabPacker.SECTION_ORDER);
  }

  @Test
  public void countTotals_skippedSection_isNotProbed() {
    Map<Tab, AllTabPacker.SectionSupplier> map = new EnumMap<>(Tab.class);
    map.put(Tab.APPLICATION, singlePageSupplier(Tab.APPLICATION, 3));
    map.put(Tab.COMPONENT, singlePageSupplier(Tab.COMPONENT, 7));

    Map<Tab, Long> totals =
        AllTabPacker.countTotals(suppliersFromMap(map), EnumSet.of(Tab.APPLICATION));

    assertThat(totals).doesNotContainKey(Tab.APPLICATION);
    assertThat(totals).containsEntry(Tab.COMPONENT, 7L);
  }
}
