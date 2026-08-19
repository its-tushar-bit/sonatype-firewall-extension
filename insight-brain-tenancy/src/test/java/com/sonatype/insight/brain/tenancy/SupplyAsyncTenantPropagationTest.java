/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.setTenantWithoutValidation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression test for the {@code TenantAwareSupplier} usage pattern at
 * {@code CompletableFuture.supplyAsync(Supplier, Executor)} call sites that fan work out onto a shared,
 * non-tenant-aware executor. Without the wrapper, the worker thread still holds whatever tenant a previous
 * task left in its {@link ThreadLocal} (potentially invalidated), and any downstream call that reads
 * {@link TenantThreadLocal#getTenant()} throws
 * {@link TenantThreadLocal.InvalidTenantOperationException}. Wrapping the supplier re-binds the caller's tenant
 * on the worker for the duration of the call.
 */
public class SupplyAsyncTenantPropagationTest
    extends MultiTenantTestSupport
{
  private ExecutorService pool;

  @BeforeEach
  public void setUpPool() {
    // Single-worker executor to force worker reuse across submissions -- deterministically reproduces the
    // stale-ThreadLocal condition ForkJoinPool workers hit in production.
    pool = Executors.newSingleThreadExecutor();
  }

  @AfterEach
  public void tearDownPool() {
    pool.shutdownNow();
  }

  // Complements TenantAwareSupplierTest by exercising the wrapper on a pool worker whose ThreadLocal
  // already holds an invalidated Tenant, which is the scenario the unit-level test does not cover.
  @Test
  public void testSupplyAsync_withTenantAwareSupplier_seesCallerTenant_evenAfterPoolWorkerHeldAnInvalidatedTenant() throws Exception {
    // Simulate a prior request that finished and had its tenant torn down, leaving a stale (invalid)
    // Tenant object in the worker's ThreadLocal.
    primePoolWorkerWithInvalidatedTenant(pool);

    Tenant callerTenant = new Tenant("caller-tenant");
    setTenantWithoutValidation(callerTenant);

    // Wrapping the supplier captures the caller's tenant at construction and rebinds it on the worker for
    // the duration of the call.
    AtomicReference<Tenant> observedOnWorker = new AtomicReference<>();
    CompletableFuture<String> future = CompletableFuture.supplyAsync(
        new TenantAwareSupplier<>(() -> {
          observedOnWorker.set(TenantThreadLocal.getTenant());
          return "ok";
        }),
        pool);

    assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("ok");
    assertThat(observedOnWorker.get()).isEqualTo(callerTenant);
  }

  @Test
  public void testSupplyAsync_withoutTenantAwareSupplier_throwsOnPoolWorkerHoldingInvalidatedTenant() throws Exception {
    // Reproduces the leak: worker retains a stale invalidated tenant from a prior request; a raw lambda
    // inherits it because the caller's tenant is not carried across to the worker.
    primePoolWorkerWithInvalidatedTenant(pool);

    setTenantWithoutValidation(new Tenant("caller-tenant"));

    CompletableFuture<Tenant> future = CompletableFuture.supplyAsync(TenantThreadLocal::getTenant, pool);

    assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(TenantThreadLocal.InvalidTenantOperationException.class);
  }

  /**
   * Runs a task on the given pool that sets a tenant on the worker's ThreadLocal, then invalidates it, so the
   * worker retains a stale invalidated Tenant reference -- mirroring what happens when a real request finishes
   * and its tenant lifecycle is torn down but the ForkJoinPool worker itself keeps running.
   *
   * <p>
   * Caller must ensure {@code pool} has exactly one worker; otherwise the primed worker may not be the one
   * that runs subsequent submissions and priming becomes racy.
   * </p>
   */
  private static void primePoolWorkerWithInvalidatedTenant(ExecutorService pool) throws Exception {
    Future<?> primed = pool.submit(() -> {
      Tenant t = new Tenant("prior-request-tenant");
      setTenantWithoutValidation(t);
      t.invalidate();
    });
    primed.get(5, TimeUnit.SECONDS);
  }
}
