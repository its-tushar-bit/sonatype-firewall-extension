/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * Fair-Semaphore-per-user throttle. Caps the number of concurrent Global Search requests one
 * authenticated user may run so a single client cannot flood the query path and starve other users.
 *
 * <p>
 * Semaphores are created lazily and held in a size-bounded, idle-expiring cache keyed by
 * {@code tenantId + username}. Bounding the cache stops the map from growing without limit for a
 * deployment that sees many distinct principals over its lifetime (e.g. SCIM churn); the tenant
 * component keeps buckets isolated across MTIQ tenants so the same username in two tenants cannot share
 * a permit pool (this limiter is a process-wide singleton). Anonymous requests share a single sentinel
 * bucket so unauthenticated traffic cannot circumvent the limit by rotating principal identities.
 */
public final class PerUserRateLimiter
{
  /** Default per-user concurrent-request cap for Global Search. */
  public static final int DEFAULT_PERMITS_PER_USER = 3;

  /** How long a caller waits for an available permit before falling through. */
  public static final long ACQUIRE_TIMEOUT_MILLIS = 250L;

  /** Upper bound on distinct (tenant, user) buckets held at once. */
  static final long MAX_TRACKED_USERS = 50_000L;

  /** Idle window after which an unused bucket is evicted. */
  static final Duration BUCKET_IDLE_TTL = Duration.ofMinutes(10);

  private static final String ANONYMOUS_KEY = "\u0000anonymous";

  private final int permitsPerUser;

  private final Cache<String, Semaphore> semaphores = CacheBuilder.newBuilder()
      .maximumSize(MAX_TRACKED_USERS)
      .expireAfterAccess(BUCKET_IDLE_TTL)
      .build();

  public PerUserRateLimiter(int permitsPerUser) {
    if (permitsPerUser < 1) {
      throw new IllegalArgumentException("permitsPerUser must be >= 1");
    }
    this.permitsPerUser = permitsPerUser;
  }

  /**
   * Try to acquire a permit for the supplied user in the current tenant. Returns a {@link Permit} that
   * MUST be closed on the request path so the permit is returned to the pool.
   *
   * @throws RateLimitedException when no permit is available within {@link #ACQUIRE_TIMEOUT_MILLIS}
   */
  public Permit acquire(String username) throws RateLimitedException {
    String user = username == null || username.isBlank() ? ANONYMOUS_KEY : username;
    String key = GlobalSearchTenancy.currentTenantId() + "\u0000" + user;
    Semaphore sem;
    try {
      sem = semaphores.get(key, () -> new Semaphore(permitsPerUser, true));
    }
    catch (ExecutionException | UncheckedExecutionException e) {
      // Semaphore construction cannot fail; surface any wrapped runtime issue as a rate-limit fallthrough.
      throw new RateLimitedException("unable to acquire rate-limit permit");
    }
    boolean acquired;
    try {
      acquired = sem.tryAcquire(ACQUIRE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RateLimitedException("interrupted while waiting for rate-limit permit");
    }
    if (!acquired) {
      throw new RateLimitedException("Too many concurrent Global Search requests for this user");
    }
    return new Permit(sem);
  }

  /** Released-once handle. */
  public static final class Permit
      implements AutoCloseable
  {
    private final Semaphore sem;

    private boolean released;

    private Permit(Semaphore sem) {
      this.sem = sem;
    }

    @Override
    public synchronized void close() {
      if (!released) {
        released = true;
        sem.release();
      }
    }
  }
}
