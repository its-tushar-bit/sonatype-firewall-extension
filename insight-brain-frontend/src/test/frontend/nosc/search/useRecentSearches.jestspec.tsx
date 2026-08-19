/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React, { StrictMode } from 'react';
import { act, renderHook } from '@testing-library/react';
import { configureStore } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
import reducers from 'MainRoot/reduxConfig/reducers';
import {
  clearRecentSearches,
  RECENT_SEARCHES_LIMIT,
  RECENT_SEARCHES_STORAGE_KEY_PREFIX,
  recentSearchesStorageKey,
  useRecentSearches,
} from 'MainRoot/nosc/search/useRecentSearches';

const USER = 'ada';
const OTHER_USER = 'grace';
const USER_KEY = recentSearchesStorageKey(USER) as string;
const OTHER_USER_KEY = recentSearchesStorageKey(OTHER_USER) as string;

function storeForUser(username: string | null) {
  return configureStore({
    reducer: reducers,
    preloadedState: { userSession: { data: username === null ? null : { username }, loading: false, error: null } },
  });
}

/** Renders the hook as `username`, wrapped in the StrictMode double-invoke when asked. */
function renderForUser(username: string | null, { strict = false }: { strict?: boolean } = {}) {
  const store = storeForUser(username);
  return renderHook(() => useRecentSearches(), {
    wrapper: ({ children }) => {
      const provided = <Provider store={store}>{children}</Provider>;
      return strict ? <StrictMode>{provided}</StrictMode> : provided;
    },
  });
}

function storedQueriesFor(key: string): string[] {
  const raw = window.localStorage.getItem(key);
  if (!raw) return [];
  return (JSON.parse(raw) as { q: string }[]).map((entry) => entry.q);
}

function storedQueries(): string[] {
  return storedQueriesFor(USER_KEY);
}

describe('useRecentSearches', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('starts empty when nothing is persisted', () => {
    const { result } = renderForUser(USER);
    expect(result.current.entries).toEqual([]);
  });

  it('hydrates persisted entries on mount', () => {
    window.localStorage.setItem(
      USER_KEY,
      JSON.stringify([
        { q: 'log4j', ts: 2 },
        { q: 'spring', ts: 1 },
      ])
    );
    const { result } = renderForUser(USER);
    expect(result.current.entries.map((entry) => entry.q)).toEqual(['log4j', 'spring']);
  });

  it('records a query as the newest entry and persists it', () => {
    const { result } = renderForUser(USER);
    act(() => result.current.record('log4j'));
    expect(result.current.entries.map((entry) => entry.q)).toEqual(['log4j']);
    expect(storedQueries()).toEqual(['log4j']);
  });

  it('moves a repeated query to the top instead of duplicating it', () => {
    const { result } = renderForUser(USER);
    act(() => result.current.record('log4j'));
    act(() => result.current.record('spring'));
    act(() => result.current.record('log4j'));
    expect(result.current.entries.map((entry) => entry.q)).toEqual(['log4j', 'spring']);
  });

  it('trims the recorded query and ignores a blank one', () => {
    const { result } = renderForUser(USER);
    act(() => result.current.record('  log4j  '));
    act(() => result.current.record('   '));
    expect(result.current.entries.map((entry) => entry.q)).toEqual(['log4j']);
  });

  it('caps the list at the limit, evicting the oldest', () => {
    const { result } = renderForUser(USER);
    for (let i = 0; i < RECENT_SEARCHES_LIMIT + 3; i++) {
      act(() => result.current.record(`query-${i}`));
    }
    expect(result.current.entries).toHaveLength(RECENT_SEARCHES_LIMIT);
    // Newest first, so the most recent survives and query-0 is long gone.
    expect(result.current.entries[0].q).toBe(`query-${RECENT_SEARCHES_LIMIT + 2}`);
    expect(result.current.entries.map((entry) => entry.q)).not.toContain('query-0');
  });

  it('writes once per record under StrictMode double-invocation', () => {
    const setItem = jest.spyOn(window.localStorage.__proto__, 'setItem');
    const { result } = renderForUser(USER, { strict: true });
    setItem.mockClear();

    act(() => result.current.record('log4j'));

    const recentWrites = setItem.mock.calls.filter(([key]) => key === USER_KEY);
    expect(recentWrites).toHaveLength(1);
    expect(storedQueries()).toEqual(['log4j']);
    setItem.mockRestore();
  });

  it('keeps deduping correctly across consecutive records under StrictMode', () => {
    const { result } = renderForUser(USER, { strict: true });
    act(() => result.current.record('log4j'));
    act(() => result.current.record('spring'));
    act(() => result.current.record('log4j'));
    expect(result.current.entries.map((entry) => entry.q)).toEqual(['log4j', 'spring']);
    expect(storedQueries()).toEqual(['log4j', 'spring']);
  });

  it('degrades to an empty list when the stored value is not valid JSON', () => {
    window.localStorage.setItem(USER_KEY, 'not-json');
    const { result } = renderForUser(USER);
    expect(result.current.entries).toEqual([]);
  });

  it('discards persisted items that do not match the entry shape', () => {
    window.localStorage.setItem(
      USER_KEY,
      JSON.stringify([{ q: 'good', ts: 1 }, { q: 'missing-ts' }, null, 'nope'])
    );
    const { result } = renderForUser(USER);
    expect(result.current.entries.map((entry) => entry.q)).toEqual(['good']);
  });

  describe('per-account isolation', () => {
    it('does not show one account the entries another account recorded', () => {
      const first = renderForUser(USER);
      act(() => first.result.current.record('acme-payments-service'));
      expect(storedQueriesFor(USER_KEY)).toEqual(['acme-payments-service']);

      // A second account signing in on the same browser profile reads its own key.
      const second = renderForUser(OTHER_USER);
      expect(second.result.current.entries).toEqual([]);
    });

    it('files each account under its own key', () => {
      const first = renderForUser(USER);
      act(() => first.result.current.record('CVE-2021-44228'));
      const second = renderForUser(OTHER_USER);
      act(() => second.result.current.record('log4j'));

      expect(storedQueriesFor(USER_KEY)).toEqual(['CVE-2021-44228']);
      expect(storedQueriesFor(OTHER_USER_KEY)).toEqual(['log4j']);
      expect(USER_KEY).not.toEqual(OTHER_USER_KEY);
    });

    it('reads and writes nothing while no user is signed in', () => {
      const { result } = renderForUser(null);
      act(() => result.current.record('should-not-persist'));

      expect(result.current.entries).toEqual([]);
      const keys = Object.keys(window.localStorage).filter((key) =>
        key.startsWith(RECENT_SEARCHES_STORAGE_KEY_PREFIX)
      );
      expect(keys).toEqual([]);
    });

    it('has no key for a signed-out session', () => {
      expect(recentSearchesStorageKey(null)).toBeNull();
      expect(recentSearchesStorageKey('')).toBeNull();
    });
  });

  describe('clearRecentSearches', () => {
    it('removes the entries of every account', () => {
      window.localStorage.setItem(USER_KEY, JSON.stringify([{ q: 'acme-payments', ts: 1 }]));
      window.localStorage.setItem(OTHER_USER_KEY, JSON.stringify([{ q: 'CVE-2021-44228', ts: 1 }]));

      clearRecentSearches();

      expect(window.localStorage.getItem(USER_KEY)).toBeNull();
      expect(window.localStorage.getItem(OTHER_USER_KEY)).toBeNull();
    });

    it('leaves unrelated stored state alone', () => {
      window.localStorage.setItem(USER_KEY, JSON.stringify([{ q: 'log4j', ts: 1 }]));
      window.localStorage.setItem('nexus-one:left-nav:collapsed', 'true');

      clearRecentSearches();

      expect(window.localStorage.getItem(USER_KEY)).toBeNull();
      expect(window.localStorage.getItem('nexus-one:left-nav:collapsed')).toBe('true');
    });

    it('leaves the panel empty for the next session', () => {
      window.localStorage.setItem(USER_KEY, JSON.stringify([{ q: 'acme-payments', ts: 1 }]));
      clearRecentSearches();

      const { result } = renderForUser(USER);
      expect(result.current.entries).toEqual([]);
    });
  });
});
