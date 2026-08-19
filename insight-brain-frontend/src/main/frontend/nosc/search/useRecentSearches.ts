/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import { useSelector } from 'react-redux';
import { selectUsername } from 'MainRoot/user/userSessionSelectors';

/**
 * Client-side Recent Searches for the global-search omnibar, persisted to
 * localStorage. There is no backend for search history, so the list is
 * per-browser and capped; a quota or parse failure degrades to an empty list
 * rather than breaking the panel.
 *
 * Queries can name applications, components and vulnerabilities, so on a shared
 * browser profile they must not leak between accounts. Two things keep them
 * apart: the storage key carries the username, so one account never reads
 * another's entries, and `clearRecentSearches` drops every account's entries on
 * logout. The key alone is not enough (entries would sit on disk indefinitely),
 * and clearing on logout alone is not enough (a browser closed without logging
 * out never runs it), so both apply.
 */

/** Maximum entries kept. Newest first; the oldest is evicted past the cap. */
export const RECENT_SEARCHES_LIMIT = 5;

/**
 * Prefix shared by every account's recent-searches key. Namespaced so it cannot
 * collide with other Preview state, and used on logout to find all of them.
 */
export const RECENT_SEARCHES_STORAGE_KEY_PREFIX = 'nexus-one:global-search:recent';

/**
 * localStorage key holding `username`'s recent searches. A signed-out or
 * not-yet-loaded session has no key of its own and reads/writes nothing, so
 * entries are never filed under an anonymous bucket a later user would read.
 */
export function recentSearchesStorageKey(username: string | null): string | null {
  if (!username) return null;
  return `${RECENT_SEARCHES_STORAGE_KEY_PREFIX}:${username}`;
}

/**
 * Drops every account's recent searches. Called from the logout flow so search
 * terms do not outlive the session that produced them.
 */
export function clearRecentSearches(): void {
  if (typeof window === 'undefined') return;
  try {
    const doomed: string[] = [];
    for (let i = 0; i < window.localStorage.length; i++) {
      const key = window.localStorage.key(i);
      if (key !== null && key.startsWith(RECENT_SEARCHES_STORAGE_KEY_PREFIX)) {
        doomed.push(key);
      }
    }
    // Collected first: removing while iterating by index shifts the remaining keys.
    doomed.forEach((key) => window.localStorage.removeItem(key));
  } catch {
    // A storage failure must not block logout; the redirect still happens.
  }
}

/** One stored query. `q` is the raw query string, operators included. */
export interface RecentSearchEntry {
  readonly q: string;
  /** Unix epoch ms of the most recent use, used for ordering. */
  readonly ts: number;
}

function isEntry(value: unknown): value is RecentSearchEntry {
  if (!value || typeof value !== 'object') return false;
  const candidate = value as Partial<RecentSearchEntry>;
  return typeof candidate.q === 'string' && typeof candidate.ts === 'number';
}

function readFromStorage(storageKey: string | null): RecentSearchEntry[] {
  if (typeof window === 'undefined' || storageKey === null) return [];
  try {
    const raw = window.localStorage.getItem(storageKey);
    if (!raw) return [];
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(isEntry).slice(0, RECENT_SEARCHES_LIMIT);
  } catch {
    return [];
  }
}

function writeToStorage(storageKey: string | null, entries: readonly RecentSearchEntry[]): void {
  if (typeof window === 'undefined' || storageKey === null) return;
  try {
    window.localStorage.setItem(storageKey, JSON.stringify(entries.slice(0, RECENT_SEARCHES_LIMIT)));
  } catch {
    // Quota / private-mode failures are non-fatal: the panel keeps working
    // for the session, just without persistence.
  }
}

export interface UseRecentSearches {
  readonly entries: readonly RecentSearchEntry[];
  /** Record a submitted query, moving an existing identical query to the top. */
  readonly record: (query: string) => void;
}

export function useRecentSearches(): UseRecentSearches {
  const username = useSelector(selectUsername) as string | null;
  const storageKey = recentSearchesStorageKey(username);
  const [entries, setEntries] = useState<readonly RecentSearchEntry[]>([]);
  // Mirrors `entries` so `record` can compute the next list without reading state
  // inside the updater, keeping the updater pure under StrictMode double-invoke.
  const entriesRef = useRef<readonly RecentSearchEntry[]>([]);

  const commit = useCallback((next: readonly RecentSearchEntry[]): void => {
    entriesRef.current = next;
    setEntries(next);
  }, []);

  // Read after mount rather than in the initial state so the hook stays safe
  // under SSR / jsdom without a localStorage stub. Re-reads when the account
  // changes so the panel shows the signed-in user's own entries, never the
  // previous user's.
  useEffect(() => {
    commit(readFromStorage(storageKey));
  }, [commit, storageKey]);

  const record = useCallback(
    (query: string): void => {
      const trimmed = query.trim();
      // Without a signed-in account there is nowhere to file the query, and holding it in
      // memory would show it to whoever signs in next on this tab.
      if (!trimmed || storageKey === null) return;
      const withoutDuplicate = entriesRef.current.filter((entry) => entry.q !== trimmed);
      const next = [{ q: trimmed, ts: Date.now() }, ...withoutDuplicate].slice(0, RECENT_SEARCHES_LIMIT);
      writeToStorage(storageKey, next);
      commit(next);
    },
    [commit, storageKey]
  );

  return { entries, record };
}
