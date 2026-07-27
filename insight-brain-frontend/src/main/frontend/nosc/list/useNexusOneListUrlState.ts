/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';

export interface NexusOneListUrlParsedState<TFilters> {
  readonly search: string;
  /** 0-based page index (API / codec). */
  readonly page: number;
  readonly filters: TFilters;
}

export interface UseNexusOneListUrlStateOptions<TFilters> {
  readonly stateName: string;
  readonly parse: (params: Record<string, unknown>) => NexusOneListUrlParsedState<TFilters>;
  readonly build: (
    state: NexusOneListUrlParsedState<TFilters>,
  ) => Record<string, string | undefined>;
  readonly rawSnapshot: (params: Record<string, unknown>) => string;
  readonly filtersEqual: (left: TFilters, right: TFilters) => boolean;
}

export interface NexusOneListUrlState<TFilters> {
  /** Committed free-text search. */
  readonly search: string;
  /** 1-based page for Pagination UI. */
  readonly page: number;
  readonly filters: TFilters;
  /** True after the first hydrate so the list fetch uses restored deep-link state. */
  readonly fetchEnabled: boolean;
  readonly setSearch: React.Dispatch<React.SetStateAction<string>>;
  readonly setPage: React.Dispatch<React.SetStateAction<number>>;
  readonly setFilters: React.Dispatch<React.SetStateAction<TFilters>>;
  /** Mark the next state change as a user-driven URL write (replace, no transition). */
  readonly requestUrlWrite: () => void;
}

/**
 * Race-safe URL ↔ list state machine shared by Nexus One list containers (Violations, Legal, …).
 *
 * Encodes: fetch-gate deferral, pendingUrlWrite discipline, hydrate / normalize / write-back effects.
 * Page clamping against {@code total} stays in the container (page-size / API-specific).
 */
export function useNexusOneListUrlState<TFilters>(
  options: UseNexusOneListUrlStateOptions<TFilters>,
): NexusOneListUrlState<TFilters> {
  const { stateName, parse, build, rawSnapshot, filtersEqual } = options;
  const router = useRouter();
  const { params } = useCurrentStateAndParams();

  const parsed = useMemo(() => parse(params), [params, parse]);
  const routeKey = useMemo(() => JSON.stringify(build(parsed)), [parsed, build]);
  const rawKey = useMemo(() => rawSnapshot(params), [params, rawSnapshot]);

  const fetchGateOpened = useRef(false);
  const [fetchEnabled, setFetchEnabled] = useState(false);
  const [page, setPage] = useState(() => parsed.page + 1);
  const [search, setSearch] = useState(() => parsed.search);
  const [filters, setFilters] = useState<TFilters>(() => parsed.filters);
  const pendingUrlWrite = useRef(false);

  const requestUrlWrite = useCallback(() => {
    pendingUrlWrite.current = true;
  }, []);

  useLayoutEffect(() => {
    setSearch((current) => (current === parsed.search ? current : parsed.search));
    setPage((current) => (current === parsed.page + 1 ? current : parsed.page + 1));
    setFilters((current) => (filtersEqual(current, parsed.filters) ? current : parsed.filters));
    if (!fetchGateOpened.current) {
      fetchGateOpened.current = true;
      setFetchEnabled(true);
    }
  }, [parsed, filtersEqual]);

  useEffect(() => {
    if (rawKey === routeKey) return;
    router.stateService.go(stateName, build(parsed), {
      notify: false,
      location: 'replace',
    });
  }, [router, rawKey, routeKey, parsed, stateName, build]);

  useEffect(() => {
    if (!pendingUrlWrite.current) return;
    pendingUrlWrite.current = false;
    const nextParams = build({ search, page: page - 1, filters });
    if (JSON.stringify(nextParams) === routeKey) return;
    router.stateService.go(stateName, nextParams, {
      notify: false,
      location: 'replace',
    });
  }, [router, search, page, filters, routeKey, stateName, build]);

  return {
    search,
    page,
    filters,
    fetchEnabled,
    setSearch,
    setPage,
    setFilters,
    requestUrlWrite,
  };
}
