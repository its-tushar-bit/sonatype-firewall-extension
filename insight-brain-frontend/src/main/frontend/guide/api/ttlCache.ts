/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Single-slot, in-memory TTL caches for the Guide API helpers.
 *
 * Two shapes are exposed because the call sites have genuinely different
 * failure semantics:
 *
 *   - keyless: the cached value is the result of a single match-all-style
 *     request whose fetcher pre-catches errors and resolves to a sentinel
 *     (typically `null`). The factory does not see rejections; it only
 *     evicts when the resolved value satisfies `evictWhen`, so the next
 *     caller retries instead of serving the failure for the rest of the
 *     TTL window.
 *
 *   - keyed: the cached value depends on a serializable key (e.g. a query
 *     string). A request with a different key replaces the slot. The
 *     fetcher's rejection propagates to the caller, and the cache entry
 *     is evicted on rejection so the next call retries.
 *
 * Caches are dropped on full page reload, which is correct: stale
 * snapshots should not persist across login boundaries.
 */

interface KeylessEntry<T> {
  promise: Promise<T>;
  expiresAt: number;
}

export function makeKeylessTtlCache<T>(
  fetchFn: () => Promise<T>,
  ttlMs: number,
  evictWhen: (result: T) => boolean = () => false
): { fetch: () => Promise<T>; reset: () => void } {
  let cache: KeylessEntry<T> | null = null;

  return {
    fetch(): Promise<T> {
      const now = Date.now();
      if (cache && cache.expiresAt > now) return cache.promise;
      const entry: KeylessEntry<T> = { promise: fetchFn(), expiresAt: now + ttlMs };
      cache = entry;
      entry.promise.then((result) => {
        if (evictWhen(result) && cache === entry) cache = null;
      });
      return entry.promise;
    },
    reset(): void {
      cache = null;
    },
  };
}

interface KeyedEntry<T> {
  keyStr: string;
  promise: Promise<T>;
  expiresAt: number;
}

export function makeKeyedTtlCache<K, T>(
  fetchFn: (key: K) => Promise<T>,
  ttlMs: number,
  keyOf: (key: K) => string = (k) => String(k)
): { fetch: (key: K) => Promise<T>; reset: () => void } {
  let cache: KeyedEntry<T> | null = null;

  return {
    fetch(key: K): Promise<T> {
      const keyStr = keyOf(key);
      const now = Date.now();
      if (cache && cache.keyStr === keyStr && cache.expiresAt > now) {
        return cache.promise;
      }
      const promise = fetchFn(key);
      const entry: KeyedEntry<T> = { keyStr, promise, expiresAt: now + ttlMs };
      cache = entry;
      promise.catch(() => {
        if (cache === entry) cache = null;
      });
      return promise;
    },
    reset(): void {
      cache = null;
    },
  };
}
