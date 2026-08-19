/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useEffect, useState } from 'react';
import axios, { AxiosError } from 'axios';
import { getGlobalSearchResultsUrl, getGlobalSearchSuggestUrl } from 'MainRoot/util/CLMLocation';
import {
  FacetBucket,
  ResultsResponse,
  ResultsTab,
  SearchRow,
  SearchSource,
  SuggestGroupRows,
  SuggestResponse,
  resultRowToSearchRow,
  suggestRowToSearchRow,
} from 'MainRoot/nosc/search/searchTypes';
import { MIN_QUERY_LENGTH } from 'MainRoot/nosc/search/searchPanelModel';

/**
 * Debounced global-search hook, backed by the dedicated global-search endpoints:
 *
 *   - typeahead mode → GET /rest/search/suggest
 *   - full mode      → GET /rest/search/results
 *
 * A single request per query replaces the former per-entity fan-out: the backend
 * parses the q= string (including field: predicates), ranks across entity types,
 * and returns a best match plus grouped rows (suggest) or a flat paged list
 * (results). No client-side query building.
 *
 * Cancellation uses an AbortController. A single request per query makes this
 * clean — the effect cleanup aborts the in-flight request so a slow earlier
 * response can never overwrite a newer one. Aborted responses are dropped.
 *
 * Graceful degrade: catalogAvailable:false is NOT an error — local groups still
 * render. The hook exposes catalogAvailable/warnings for callers that surface a
 * "catalog unavailable" indicator; the results page wires that up. Only a real
 * network / 5xx error populates loadError.
 */
const DEBOUNCE_MS = 200;

/**
 * totalEstimate is exact below this cap and literally this value at/above it
 * (rendered as "10,000+"). Mirrors GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP on the backend.
 */
const TOTAL_ESTIMATE_CAP = 10000;

export type GlobalSearchMode = 'typeahead' | 'full';

export interface UseGlobalSearchOptions {
  /**
   * `typeahead` — one call to /rest/search/suggest for the omnibar dropdown.
   * `full` — one call to /rest/search/results for the /search results page.
   */
  readonly mode?: GlobalSearchMode;

  /** Row source: tenant IQ index ('local', default) or shared catalog ('catalog'). */
  readonly source?: SearchSource;

  /** Full mode only: which results tab to fetch. Defaults to ALL. */
  readonly tab?: ResultsTab;

  /** Full mode only: 1-indexed page number. Defaults to 1. */
  readonly page?: number;

  /** Full mode only: page size (backend caps at 100). Defaults to the backend default. */
  readonly pageSize?: number;

  /**
   * Full mode only: opaque deep-pagination cursor. When set, the backend seeks
   * past the `search_after` sort key instead of computing a numeric offset, so
   * deep pages (past the ~1000-hit window `page` can address) stay efficient.
   * The caller carries forward `nextSearchAfter` from the previous page.
   */
  readonly searchAfter?: string | null;

  /**
   * Full mode only: when true the backend returns the per-tab `facets` map for a
   * single IQ-local entity tab. The ALL tab / catalog source ignore it (facets
   * come back null), so the caller should only request it on an entity tab.
   */
  readonly includeFacets?: boolean;

  /**
   * Full mode only: when true the backend probes the sibling sections so the response
   * carries every tab's count badge. Each sibling costs one count-only search on top of
   * the caller's own page search, so it is opt-in. The ALL tab gets its counts free from
   * the packing pass and pages after the first reuse the active tab's own total, so the
   * caller should only request it on an entity tab's first page whose sibling counts are
   * not already known.
   */
  readonly includeTabCounts?: boolean;
}

export interface GlobalSearchState {
  readonly loading: boolean;
  readonly loadError: string | null;

  /** Flat rows for the active view (suggest groups flattened, or results page rows). */
  readonly results: readonly SearchRow[];

  /** Typeahead only: the single best-match row promoted above the groups, or null. */
  readonly bestMatch: SearchRow | null;
  /** Typeahead only: per-entity-type sections in fixed presentation order. */
  readonly groups: readonly SuggestGroupRows[];

  /** Full mode: total estimate (exact below 10000, else 10000). Suggest has no total (0). */
  readonly totalEstimate: number;
  /** Full mode: optional per-tab counts when the backend supplies them; else undefined. */
  readonly tabCounts?: Partial<Record<ResultsTab, number>>;
  /** Full mode: opaque cursor for the next page, or null on the last page. */
  readonly nextSearchAfter: string | null;
  /**
   * Full mode: per-tab facet buckets (facet key → buckets) when includeFacets was
   * requested on an entity tab; null/undefined for the ALL tab, catalog source, or
   * an older backend.
   */
  readonly facets: Record<string, FacetBucket[]> | null | undefined;
  /** Parser/compiler warnings from the results endpoint. */
  readonly warnings: readonly string[];

  /**
   * Tri-state catalog signal, passed through from the response: undefined when the
   * catalog was not consulted, true when usable, false when requested but degraded.
   */
  readonly catalogAvailable: boolean | undefined;

  /** True when totalEstimate is an exact count (below the 10000 cap). */
  readonly isExactTotal: boolean;
}

const EMPTY_STATE: GlobalSearchState = {
  loading: false,
  loadError: null,
  results: [],
  bestMatch: null,
  groups: [],
  totalEstimate: 0,
  tabCounts: undefined,
  nextSearchAfter: null,
  facets: undefined,
  warnings: [],
  catalogAvailable: undefined,
  isExactTotal: false,
};

const LOADING_STATE: GlobalSearchState = {
  ...EMPTY_STATE,
  loading: true,
};

/** Copy shown when the caller lacks permission to search. */
const PERMISSION_MESSAGE = 'You do not have permission to search these results.';

/** Copy shown for a server-side or network failure. */
const UNAVAILABLE_MESSAGE = 'Search is unavailable. Try again in a moment.';

/**
 * User-facing copy for a failed search, chosen from the response status class. Axios
 * populates `error.message` with HTTP plumbing text ("Request failed with status code
 * 500") which is meaningless to a user, so the raw message never reaches this copy —
 * see logSearchFailure for where it is recorded.
 */
function errorMessage(error: unknown): string {
  const status = (error as AxiosError)?.response?.status;
  if (status === 401 || status === 403) return PERMISSION_MESSAGE;
  return UNAVAILABLE_MESSAGE;
}

/** Record the raw axios failure, which the user-facing copy deliberately drops. */
function logSearchFailure(error: unknown): void {
  const status = (error as AxiosError)?.response?.status;
  const raw = error instanceof Error ? error.message : String(error);
  console.error(`Global search request failed${status ? ` (HTTP ${status})` : ''}: ${raw}`);
}

/** True when a rejection is the effect-cleanup abort rather than a real failure. */
function isAbort(controller: AbortController, error: unknown): boolean {
  return controller.signal.aborted || (error as AxiosError)?.code === 'ERR_CANCELED';
}

export function useGlobalSearch(query: string, opts?: UseGlobalSearchOptions): GlobalSearchState {
  const trimmed = query.trim();
  const mode: GlobalSearchMode = opts?.mode ?? 'typeahead';
  const source: SearchSource = opts?.source ?? 'local';
  const tab: ResultsTab = opts?.tab ?? 'ALL';
  const page = opts?.page ?? 1;
  const pageSize = opts?.pageSize;
  const searchAfter = opts?.searchAfter ?? undefined;
  const includeFacets = opts?.includeFacets ?? false;
  const includeTabCounts = opts?.includeTabCounts ?? false;
  const [state, setState] = useState<GlobalSearchState>(EMPTY_STATE);

  useEffect(() => {
    if (trimmed.length < MIN_QUERY_LENGTH) {
      setState(EMPTY_STATE);
      return;
    }

    // Created inside the timer callback so the controller's lifecycle matches the
    // request it actually cancels; the outer clearTimeout handles the
    // debounce-window cancel (before any request fires), and controller.abort()
    // below handles an in-flight request. Held in a closure var so cleanup can
    // reach it after the timer fires.
    let controller: AbortController | null = null;
    const handle = setTimeout(() => {
      const activeController = new AbortController();
      controller = activeController;
      setState(LOADING_STATE);

      if (mode === 'typeahead') {
        axios
          .get<SuggestResponse>(getGlobalSearchSuggestUrl(trimmed, source), {
            signal: activeController.signal,
          })
          .then((response) => {
            if (activeController.signal.aborted) return;
            const data = response.data;
            const bestMatch = data.bestMatch ? suggestRowToSearchRow(data.bestMatch) : null;
            const groups: SuggestGroupRows[] = (data.groups ?? []).map((g) => ({
              type: g.type,
              source: g.source,
              rows: (g.results ?? []).map(suggestRowToSearchRow),
            }));
            const flat: SearchRow[] = [];
            if (bestMatch) flat.push(bestMatch);
            for (const g of groups) flat.push(...g.rows);
            setState({
              ...EMPTY_STATE,
              results: flat,
              bestMatch,
              groups,
              // The suggest endpoint reports no warnings, so warnings stays empty
              // here; only the results endpoint below populates it.
              catalogAvailable: data.catalogAvailable ?? undefined,
            });
          })
          .catch((error: unknown) => {
            if (isAbort(activeController, error)) return;
            logSearchFailure(error);
            setState({ ...EMPTY_STATE, loadError: errorMessage(error) });
          });
        return;
      }

      axios
        .get<ResultsResponse>(
          getGlobalSearchResultsUrl({
            q: trimmed,
            tab,
            page,
            pageSize,
            searchAfter,
            source,
            includeFacets,
            includeTabCounts,
          }),
          { signal: activeController.signal }
        )
        .then((response) => {
          if (activeController.signal.aborted) return;
          const data = response.data;
          const results = (data.results ?? []).map(resultRowToSearchRow);
          const totalEstimate = typeof data.totalEstimate === 'number' ? data.totalEstimate : 0;
          setState({
            ...EMPTY_STATE,
            results,
            totalEstimate,
            tabCounts: data.tabCounts,
            nextSearchAfter: data.nextSearchAfter ?? null,
            facets: data.facets ?? undefined,
            warnings: data.warnings ?? [],
            catalogAvailable: data.catalogAvailable,
            isExactTotal: totalEstimate < TOTAL_ESTIMATE_CAP,
          });
        })
        .catch((error: unknown) => {
          if (isAbort(activeController, error)) return;
          logSearchFailure(error);
          setState({ ...EMPTY_STATE, loadError: errorMessage(error) });
        });
    }, DEBOUNCE_MS);

    return () => {
      controller?.abort();
      clearTimeout(handle);
    };
    // All deps are primitives derived from args, so they have stable identity and the effect
    // re-fetches only when an actual value changes.
  }, [trimmed, mode, source, tab, page, pageSize, searchAfter, includeFacets, includeTabCounts]);

  return state;
}
