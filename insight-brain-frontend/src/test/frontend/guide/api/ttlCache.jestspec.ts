/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { makeKeylessTtlCache, makeKeyedTtlCache } from 'GuideRoot/api/ttlCache';

describe('makeKeylessTtlCache', () => {
  it('returns the in-flight promise to concurrent callers', () => {
    const fetchFn = jest.fn().mockReturnValue(new Promise(() => {}));
    const cache = makeKeylessTtlCache(fetchFn, 1000);

    const a = cache.fetch();
    const b = cache.fetch();

    expect(a).toBe(b);
    expect(fetchFn).toHaveBeenCalledTimes(1);
  });

  it('serves the cached value while within the TTL', async () => {
    const fetchFn = jest.fn().mockResolvedValue('value');
    const cache = makeKeylessTtlCache(fetchFn, 1000);

    await cache.fetch();
    await cache.fetch();

    expect(fetchFn).toHaveBeenCalledTimes(1);
  });

  it('refetches once the TTL has expired', async () => {
    const fetchFn = jest.fn().mockResolvedValue('value');
    const realNow = Date.now;
    let now = 0;
    Date.now = () => now;
    try {
      const cache = makeKeylessTtlCache(fetchFn, 1000);

      await cache.fetch();
      now += 999;
      await cache.fetch();
      expect(fetchFn).toHaveBeenCalledTimes(1);

      now += 2;
      await cache.fetch();
      expect(fetchFn).toHaveBeenCalledTimes(2);
    } finally {
      Date.now = realNow;
    }
  });

  it('evicts entries whose resolved value matches evictWhen', async () => {
    const fetchFn = jest.fn().mockResolvedValueOnce(null).mockResolvedValueOnce('ok');
    const cache = makeKeylessTtlCache(fetchFn, 60_000, (result) => result === null);

    const first = await cache.fetch();
    const second = await cache.fetch();

    expect(first).toBeNull();
    expect(second).toBe('ok');
    expect(fetchFn).toHaveBeenCalledTimes(2);
  });

  it('reset() drops the cached entry', async () => {
    const fetchFn = jest.fn().mockResolvedValue('value');
    const cache = makeKeylessTtlCache(fetchFn, 60_000);

    await cache.fetch();
    cache.reset();
    await cache.fetch();

    expect(fetchFn).toHaveBeenCalledTimes(2);
  });
});

describe('makeKeyedTtlCache', () => {
  it('caches per key and replaces the slot on key change', async () => {
    const fetchFn = jest.fn().mockImplementation((k: string) => Promise.resolve(`v:${k}`));
    const cache = makeKeyedTtlCache<string, string>(fetchFn, 60_000);

    expect(await cache.fetch('a')).toBe('v:a');
    expect(await cache.fetch('a')).toBe('v:a');
    expect(fetchFn).toHaveBeenCalledTimes(1);

    expect(await cache.fetch('b')).toBe('v:b');
    expect(fetchFn).toHaveBeenCalledTimes(2);

    // a no longer in the slot — must refetch
    expect(await cache.fetch('a')).toBe('v:a');
    expect(fetchFn).toHaveBeenCalledTimes(3);
  });

  it('evicts on rejection so the next call retries', async () => {
    const fetchFn = jest
      .fn()
      .mockRejectedValueOnce(new Error('boom'))
      .mockResolvedValueOnce('ok');
    const cache = makeKeyedTtlCache<string, string>(fetchFn, 60_000);

    await expect(cache.fetch('a')).rejects.toThrow('boom');
    expect(await cache.fetch('a')).toBe('ok');
    expect(fetchFn).toHaveBeenCalledTimes(2);
  });

  it('honors a custom keyOf', async () => {
    const fetchFn = jest.fn().mockResolvedValue('v');
    const cache = makeKeyedTtlCache<string | undefined, string>(
      fetchFn,
      60_000,
      (k) => k ?? ''
    );

    await cache.fetch(undefined);
    await cache.fetch('');

    expect(fetchFn).toHaveBeenCalledTimes(1);
  });

  it('refetches once the TTL has expired (per key)', async () => {
    const fetchFn = jest.fn().mockResolvedValue('v');
    const realNow = Date.now;
    let now = 0;
    Date.now = () => now;
    try {
      const cache = makeKeyedTtlCache<string, string>(fetchFn, 1000);

      await cache.fetch('a');
      now += 1500;
      await cache.fetch('a');

      expect(fetchFn).toHaveBeenCalledTimes(2);
    } finally {
      Date.now = realNow;
    }
  });

  it('reset() drops the cached entry', async () => {
    const fetchFn = jest.fn().mockResolvedValue('v');
    const cache = makeKeyedTtlCache<string, string>(fetchFn, 60_000);

    await cache.fetch('a');
    cache.reset();
    await cache.fetch('a');

    expect(fetchFn).toHaveBeenCalledTimes(2);
  });
});
