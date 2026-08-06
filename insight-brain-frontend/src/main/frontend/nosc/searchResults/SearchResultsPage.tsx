/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSelector } from 'react-redux';
import { useCurrentStateAndParams } from '@uirouter/react';
import {
  Box,
  Button,
  Flex,
  Grid,
  IconButton,
  SegmentedControl,
  Text,
  TextField,
  Theme,
  VisuallyHidden,
} from '@radix-ui/themes';
import { Card, PageHeading, SectionHeading } from '@sonatype/nexus-one-components';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { useGlobalSearch } from 'MainRoot/nosc/search/useGlobalSearch';
import {
  FacetBucket,
  RENDERED_ITEM_TYPES,
  ResultsTab,
  SearchEntityType,
  SearchRow,
  SearchSource,
  tabIdForType,
} from 'MainRoot/nosc/search/searchTypes';
import { clickHrefFor } from 'MainRoot/nosc/search/searchClickTargets';
import { FilterBar, FilterInsertRequest } from 'MainRoot/nosc/search/FilterBar';
import { computeFilterInsert, useFocusInputWithCaret } from 'MainRoot/nosc/search/searchFilterInsert';
import { WarningPill } from 'MainRoot/nosc/search/WarningPill';
import { selectIsCatalogFederationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  DEFAULT_SEARCH_SOURCE,
  HIDDEN_TABS_FOR_DATA_SOURCE,
  isTabHiddenForSource,
  parseSearchSource,
} from 'MainRoot/nosc/search/searchDataSource';
import router from 'MainRoot/router/routerInstance';
import { SearchResultsTabs } from 'MainRoot/nosc/searchResults/SearchResultsTabs';
import { SearchResultsFilters, isRenderableFacet } from 'MainRoot/nosc/searchResults/SearchResultsFilters';
import {
  hasThreatLevelField,
  isPredicateToken,
  stripPredicates,
  tokenize,
} from 'MainRoot/nosc/searchResults/facetQuery';
import { computeCountsByType, formatTotalLabel } from 'MainRoot/nosc/searchResults/searchResultsCounts';
import { SearchResultsList } from 'MainRoot/nosc/searchResults/SearchResultsList';

// Radix styles and the Nexus One brand/Radix palette tokens are loaded once
// at the SPA bundle entry (`nexus-one/App.tsx`, via the side-effect import of
// `@sonatype/nexus-one-components`, which ships brand-colors.css + radix-palette.css)
// and the Classic bundle (`ClassicToggleButton.tsx`). They apply app-wide
// regardless of which route mounts first, so leaf pages must not re-import them.

/**
 * P1-F13: Full /search results page mounted by UI-Router at
 * `/search?q=<query>&tab=<itemType>&source=<local|catalog>&page=<n>`.
 *
 * Layout: 250px filter sidebar + 1fr main column (input row + tabs + list).
 *
 * Data model:
 *   - Server pagination: the backend (GET /rest/search/results) filters by tab
 *     server-side and returns a single page of rows (25/page here). The full
 *     tenant is never materialised client-side.
 *   - Pagination is cursor-based: the backend read path is cursor-only, so
 *     every page past page 1 threads the previous page's `nextSearchAfter`
 *     cursor rather than a numeric offset. Page 1 sends no cursor.
 *     `page` is kept in the URL for round-trip/reset semantics only.
 *   - tabCounts populates all six tab badges. The ALL tab gets them free from its
 *     packing pass; an entity tab asks for them with `includeTabCounts`, which the
 *     backend serves with one count-only search per sibling section, so the flag is
 *     only sent when the badges are not already cached. When counts are absent the
 *     active tab's count is merged over the last-seen fallback so other tabs keep
 *     their badge across switches.
 *   - `?source=catalog` targets the shared catalog and hides the IQ-only tabs. It is
 *     clamped to local when CATALOG_FEDERATION is off, so a hand-typed URL cannot
 *     query a corpus the flag withholds.
 */

/** Rows per page. AC: server-paginated at 25/page with next/prev controls. */
const RESULTS_PAGE_SIZE = 25;

/**
 * Stable empty fallback-count cache. Shared rather than a fresh literal so a render
 * that discards a stale cache keeps a constant identity for the counts memo.
 */
const NO_FALLBACK_COUNTS: Record<string, number> = {};

// Re-exported so existing importers of the count helpers keep one entry point.
export { computeCountsByType, formatTotalLabel } from 'MainRoot/nosc/searchResults/searchResultsCounts';

/**
 * Resolve the `?tab=` param to a tab that is actually rendered. A tab that is
 * unknown, or hidden for the active source (catalog hides the IQ-only tabs), falls
 * back to 'all'. Without this the tablist has no tab matching activeTab, so every
 * tab gets tabIndex={-1} (unreachable by keyboard) and the tabpanel's
 * aria-labelledby points at an element that was never rendered.
 */
export function resolveActiveTab(tabParam: string, source: SearchSource): string {
  // Upper-cased first so a hand-written or externally shared `?tab=violation` link
  // resolves to the Violations tab instead of silently falling back to All.
  const tab = tabParam.toUpperCase();
  if (tab === 'ALL') return 'all';
  if (!(RENDERED_ITEM_TYPES as readonly string[]).includes(tab)) return 'all';
  return isTabHiddenForSource(tabIdForType(tab as SearchEntityType), source) ? 'all' : tab;
}

export function SearchResultsPage(): JSX.Element {
  const { params } = useCurrentStateAndParams();
  const query = typeof params.q === 'string' ? params.q : '';
  const tabParam = typeof params.tab === 'string' && params.tab ? params.tab : 'all';
  // Round-trip the omnibar data source so Enter → /search?source=catalog keeps
  // searching the catalog corpus (and shared links stay honest). Clamped to the
  // local default when CATALOG_FEDERATION is off, the same way the omnibar clamps
  // its own effectiveSource: a bookmarked or hand-typed ?source=catalog URL must
  // not query a corpus the flag intentionally withholds.
  const isCatalogEnabled = useSelector(selectIsCatalogFederationEnabled);
  const source: SearchSource = isCatalogEnabled ? parseSearchSource(params.source) : DEFAULT_SEARCH_SOURCE;
  const activeTab = resolveActiveTab(tabParam, source);

  // 1-indexed page from the URL, clamped to >= 1.
  const pageParam = typeof params.page === 'string' ? parseInt(params.page, 10) : NaN;
  const page = Number.isFinite(pageParam) && pageParam >= 1 ? pageParam : 1;

  const [filtersOpen, setFiltersOpen] = useState(false);
  // Local IQ/Guide toggle on the All tab (client-side, over the returned rows).
  const [allSourceFilter, setAllSourceFilter] = useState<'all' | SearchSource>('all');
  // The toggle is hidden off the All tab and in catalog mode, so a selection left
  // behind would silently narrow the rows on return. Reset it whenever the row set
  // it filters is replaced: a new query, a tab change, or a source change.
  useEffect(() => {
    setAllSourceFilter('all');
  }, [query, activeTab, source]);

  // Editable query bound to the results-page input. Seeded from the URL and
  // re-synced whenever the URL query changes (e.g. back/forward navigation).
  const [inputQuery, setInputQuery] = useState(query);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const focusInputWithCaret = useFocusInputWithCaret();
  useEffect(() => {
    setInputQuery(query);
  }, [query]);

  // See computeCountsByType: tabCounts wins outright; otherwise the active tab's
  // count is merged over the last-seen fallback so other tabs keep their badge.
  //
  // Fallback counts belong to the corpus they were counted against, so the cache
  // carries the (query, source) signature they were learned under: the local index and
  // the shared catalog are different corpora, and switching source without editing the
  // query must not render catalog-derived counts as local badges. Tab is deliberately
  // NOT part of the signature — that is what lets sibling tabs keep their badges while
  // the user switches between them within one query.
  //
  // Invalidated during render rather than in an effect: the memo below reads the cache
  // while rendering and the persist effect writes it back afterwards, so an
  // effect-based clear would be undone by that write in the very same commit.
  const fallbackCountsSignature = `${query}\u0000${source}`;
  const fallbackCountsRef = useRef<{ signature: string; counts: Record<string, number> }>({
    signature: fallbackCountsSignature,
    counts: {},
  });
  const priorFallbackCounts =
    fallbackCountsRef.current.signature === fallbackCountsSignature
      ? fallbackCountsRef.current.counts
      : NO_FALLBACK_COUNTS;

  // Map the render-model tab id to the results endpoint tab (ALL for the "all" pseudo-tab).
  const requestedTab: ResultsTab = activeTab === 'all' ? 'ALL' : tabIdForType(activeTab as SearchEntityType);

  // The cursor walk: `nextSearchAfter` from the previous page is threaded as the
  // `search_after` cursor for EVERY page past page 1. The backend read path is
  // cursor-only (numeric `page` is ignored), so page 1 sends no cursor and each
  // later page seeks from the prior page's `nextSearchAfter`.
  // The map is keyed by page number so Prev can reuse a cursor already fetched.
  const cursorsByPage = useRef<Map<number, string>>(new Map());
  // Identity of the sort-key space the cached cursors belong to. Cursors are only
  // valid for the query/tab/source they were issued for.
  const cursorSignature = `${query}\u0000${requestedTab}\u0000${source}`;
  const cursorSignatureRef = useRef(cursorSignature);
  useEffect(() => {
    cursorsByPage.current = new Map();
    cursorSignatureRef.current = cursorSignature;
  }, [cursorSignature]);

  // Page 1 = no cursor; every later page uses the cursor stored for it (learned
  // from the previous page's nextSearchAfter).
  const searchAfter = page > 1 ? cursorsByPage.current.get(page) ?? null : null;

  // A deep-linked / refreshed ?page=N past page 1 has no cursor for N (the cache
  // is in-memory only). The cursor-only backend read path would return page 1's
  // rows under a "Page N" label, and the cursor learned from that response would
  // be cached against N+1 even though it belongs after page 1. Snap back to page 1
  // instead of rendering mislabelled rows.
  const needsPageReset = page > 1 && searchAfter === null;
  useEffect(() => {
    if (needsPageReset) {
      router.stateService.go(
        'nexusOneSearch',
        {
          q: query || undefined,
          tab: activeTab !== 'all' ? activeTab : undefined,
          source: source !== 'local' ? source : undefined,
          page: undefined,
        },
        { location: 'replace' }
      );
    }
  }, [needsPageReset, query, activeTab, source]);

  // Request per-tab facets only on a single entity tab; the ALL tab has none (the
  // backend returns null facets there anyway, but skipping the flag avoids the
  // extra facet fan-out on the busiest tab).
  const includeFacets = requestedTab !== 'ALL';

  // The tab strip renders a badge for every tab, so the per-tab count map is needed on
  // every tab — but it is only worth paying for where it is not already available:
  //   - the ALL tab gets all six counts free from the packing pass, which ignores this flag;
  //   - pages after the first are served the active tab's own total, so the probe is skipped;
  //   - a tab switch within one (query, source) reuses the counts learned on the first tab,
  //     which is why the fallback cache is keyed without the tab.
  // Requesting it costs one count-only search per sibling section, so it is asked for only
  // when none of the above already supplies the badges.
  //
  // The flag is latched per (query, tab, source) rather than recomputed from the cache on
  // every render: the cache is populated by the very response the flag asks for, so reading
  // it directly would flip the flag from true to false the moment the counts arrived,
  // changing a fetch dependency and re-issuing the same request. Keying the latch on the tab
  // as well means a tab switch re-reads the cache once — finding the counts a sibling tab
  // already learned — while the flag stays fixed for the lifetime of one tab's fetch.
  const tabCountsLatchSignature = `${fallbackCountsSignature}\u0000${requestedTab}\u0000${page}`;
  const tabCountsLatchRef = useRef<{ signature: string; needed: boolean } | null>(null);
  if (tabCountsLatchRef.current?.signature !== tabCountsLatchSignature) {
    tabCountsLatchRef.current = {
      signature: tabCountsLatchSignature,
      needed: Object.keys(priorFallbackCounts).length === 0,
    };
  }
  const includeTabCounts = requestedTab !== 'ALL' && page === 1 && tabCountsLatchRef.current.needed;

  const {
    loading,
    loadError,
    results,
    totalEstimate,
    tabCounts,
    nextSearchAfter,
    facets: fetchedFacets,
    warnings,
    isExactTotal,
  } = useGlobalSearch(needsPageReset ? '' : query, {
    mode: 'full',
    tab: requestedTab,
    source,
    page,
    pageSize: RESULTS_PAGE_SIZE,
    searchAfter,
    includeFacets,
    includeTabCounts,
  });

  // The backend sends facets on the first page only: the map is identical on every
  // page of a query, so later pages omit it rather than rebuilding it. Retain the
  // last-seen map, keyed on the query/tab/source it was fetched for, so the rail
  // keeps its buckets and checked state while paging instead of emptying on page 2.
  // Held as state rather than a ref so the retained map is derived during render without
  // mutating anything: a response that carries facets replaces it, and a change of
  // query/tab/source drops it so the previous query's buckets cannot leak into a new one.
  const [retainedFacets, setRetainedFacets] =
    useState<{ signature: string; facets: Record<string, FacetBucket[]> } | null>(null);
  const facetSignature = `${query}\u0000${requestedTab}\u0000${source}`;
  useEffect(() => {
    if (fetchedFacets) {
      setRetainedFacets({ signature: facetSignature, facets: fetchedFacets });
    }
  }, [fetchedFacets, facetSignature]);
  const retainedForThisQuery = retainedFacets?.signature === facetSignature ? retainedFacets.facets : undefined;
  const facets = fetchedFacets ?? retainedForThisQuery;

  // The facet rail shows on an entity tab when there is SOMETHING to render.
  // Renderability is decided with the same predicate the rail itself uses, so a
  // facet key with no descriptor or an empty bucket list cannot reserve a rail
  // column that then renders nothing but the Reset button. The client-side
  // threat-level slider counts as content on the tabs that have that field.
  const hasBackendFacets = !!facets && Object.keys(facets).some((key) => isRenderableFacet(key, facets[key]));
  const hasClientSection = hasThreatLevelField(activeTab);
  const showFacetRail = activeTab !== 'all' && (hasBackendFacets || hasClientSection);

  // Remember the cursor to the NEXT page so a subsequent Next can seek forward.
  // Stored against page+1 since that's the page it unlocks. The signature guard
  // drops a cursor produced by a previous query/tab/source: the reset effect and
  // this effect are not ordered relative to each other, so without it a late
  // response can write a cursor from the prior sort-key space into the freshly
  // cleared cache.
  useEffect(() => {
    if (nextSearchAfter && cursorSignatureRef.current === cursorSignature) {
      cursorsByPage.current.set(page + 1, nextSearchAfter);
    }
  }, [nextSearchAfter, page, cursorSignature]);

  // Tabs hidden for the active source (catalog hides App/Violation/Waiver).
  const hiddenTabs = HIDDEN_TABS_FOR_DATA_SOURCE[source];
  const visibleTypeTabs = useMemo(() => RENDERED_ITEM_TYPES.filter((t) => !hiddenTabs.includes(t)), [hiddenTabs]);

  const { counts: countsByType, nextFallback } = useMemo(
    () => computeCountsByType(tabCounts, totalEstimate, activeTab, priorFallbackCounts),
    [tabCounts, totalEstimate, activeTab, priorFallbackCounts]
  );

  // Persist the fallback cache outside render so the useMemo stays pure. Stamped with
  // the signature the counts were learned under so a later render against a different
  // query/source discards them instead of merging them in.
  useEffect(() => {
    fallbackCountsRef.current = { signature: fallbackCountsSignature, counts: nextFallback };
  }, [nextFallback, fallbackCountsSignature]);

  const filteredResults = useMemo(() => {
    let out: readonly SearchRow[] = results;
    // Source filter (IQ/Guide) on the All tab only — the flat rows carry a
    // per-row `source`, so this is a pure client-side narrowing of the page.
    if (activeTab === 'all' && allSourceFilter !== 'all') {
      out = out.filter((r) => r.source === allSourceFilter);
    }
    return out;
  }, [results, activeTab, allSourceFilter]);

  const goTo = useCallback(
    (next: { q?: string; tab?: string; source?: SearchSource; page?: number }, replace = false): void => {
      const q = next.q ?? query;
      const tab = next.tab ?? activeTab;
      const nextSource = next.source ?? source;
      const nextPage = next.page ?? 1;
      router.stateService.go(
        'nexusOneSearch',
        {
          q: q || undefined,
          tab: tab !== 'all' ? tab : undefined,
          source: nextSource !== 'local' ? nextSource : undefined,
          page: nextPage > 1 ? String(nextPage) : undefined,
        },
        replace ? { location: 'replace' } : undefined
      );
    },
    [query, activeTab, source]
  );

  // A facet selection narrows the SAME query: it rewrites `q=` (append/remove a
  // `field:value` predicate) and re-navigates to page 1, so the server re-queries.
  const handleFacetQueryChange = useCallback(
    (nextQuery: string): void => {
      goTo({ q: nextQuery, page: 1 });
    },
    [goTo]
  );

  // Reset facet selections: strip structured predicates from the query, keeping any
  // free-text terms. A simple heuristic — drop every `field:value` / `field:[..]`
  // token, keep bare words — mirrors the omnibar's plain-text search.
  const handleFacetReset = useCallback((): void => {
    goTo({ q: stripPredicates(query), page: 1 });
  }, [query, goTo]);

  // Reset drops the query's structured predicates AND returns to page 1, so it is
  // meaningful when either of those would actually change.
  // Read from the URL-committed `query`, not `inputQuery`: the rail and Reset act on
  // the query the displayed results were produced from. Keying them off uncommitted
  // input would let a facet click submit half-typed text the user never ran.
  const resetEnabled = page > 1 || tokenize(query).some(isPredicateToken);

  const totalLabel = formatTotalLabel(totalEstimate, isExactTotal);

  // Switching tab resets to page 1 (a different result set). Arrow-key navigation
  // activates on focus per WAI-ARIA, so it replaces the history entry rather than
  // pushing one per keypress.
  const handleTabChange = (newTab: string, viaKeyboard = false): void => {
    goTo({ tab: newTab, page: 1 }, viaKeyboard);
  };

  const submitQuery = useCallback(
    (nextQuery: string): void => {
      // A new query resets to page 1 and clears the cursor cache implicitly (via
      // the query-change effect above).
      goTo({ q: nextQuery, page: 1 });
    },
    [goTo]
  );

  // Filter insert: shared computeFilterInsert. A "complete" leaf (a full
  // predicate) commits immediately — update the input, push the URL, re-run. An
  // "incomplete" leaf (trailing `:`/`:""`) only places the caret and defers to
  // Enter, exactly like the omnibar.
  const handleFilterInsert = useCallback(
    (request: FilterInsertRequest): void => {
      const { value, caretAt, complete } = computeFilterInsert(inputQuery, request.syntax);
      setInputQuery(value);
      if (complete) {
        submitQuery(value);
      } else {
        focusInputWithCaret(inputRef.current, caretAt);
      }
    },
    [inputQuery, submitQuery, focusInputWithCaret]
  );

  const handleFilterMenuClose = useCallback((): void => {
    inputRef.current?.focus();
  }, []);

  // Row destinations can be Classic-bundle URLs as well as Nexus One routes, so they
  // navigate by href rather than through the router.
  const handleResultClick = (row: SearchRow): void => {
    window.location.assign(clickHrefFor(row));
  };

  // Pagination: the next-page cursor is the only signal for "more available". Both
  // index backends over-fetch by one row before minting it, so it is exact -- absent
  // on the last page even when that page is exactly RESULTS_PAGE_SIZE rows. Also
  // treating a full page as "more" would enable Next into a page with no cursor,
  // which the page-reset guard then bounces back to page 1 with no explanation.
  const hasPrev = page > 1;
  // hasPrev/hasNext drive the buttons' disabled state; showPagination keeps the row
  // mounted across a fetch so it doesn't unmount and remount on every page turn.
  const hasMore = !!nextSearchAfter;
  const hasNext = !loading && hasMore;
  const showPagination = hasPrev || hasMore || loading;

  const goPrev = (): void => {
    if (hasPrev) goTo({ page: page - 1 });
  };
  const goNext = (): void => {
    if (hasNext) goTo({ page: page + 1 });
  };

  const { effectiveTheme } = useNoscTheme();
  const offsets = usePreviewShellOffsets();

  // aria-live announcement text, so a screen-reader user hears the new count after
  // paging / re-running without focus movement. The visible "Page N" text is
  // aria-hidden, so the page number is announced here or not at all.
  const liveAnnouncement = loading
    ? 'Loading results'
    : query
    ? `Page ${page}, ${filteredResults.length.toLocaleString()} results on this page`
    : '';

  return (
    <Theme
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
      hasBackground={false}
      data-testid="nosc-search-results-page"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        pointerEvents: 'auto',
        backgroundColor: 'var(--gray-1)',
      }}
    >
      <Box p="6" style={{ maxWidth: '1400px', margin: '0 auto' }}>
        <Flex direction="column" gap="5">
          <PageHeading as="h1">{query ? `Search results for "${query}"` : 'Search'}</PageHeading>
          {query && totalEstimate > 0 && (
            <Text size="2" color="gray" data-testid="nosc-search-results-hit-summary">
              {/* Rows are server-paginated, so the page's row count and the total
                  estimate describe different scopes. Naming the page keeps the two
                  numbers from reading as a single "X of Y" subset. The count is also
                  qualified on a single page when a client filter hid some rows, so the
                  total never reads as the number of rows on screen. */}
              {hasPrev || hasMore
                ? `${totalLabel} matches \u00b7 page ${page} (${filteredResults.length.toLocaleString()} shown)`
                : filteredResults.length < results.length
                ? `${totalLabel} matches (${filteredResults.length.toLocaleString()} shown)`
                : `${totalLabel} matches`}
            </Text>
          )}

          {/* Input row: editable query + inline filter-bar toggle (right adornment). */}
          <Box>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                submitQuery(inputQuery);
              }}
              data-testid="nosc-search-results-input-form"
            >
              <TextField.Root
                ref={inputRef}
                value={inputQuery}
                onChange={(e) => setInputQuery(e.target.value)}
                placeholder="Search apps, components, CVEs, policies..."
                size="2"
                aria-label="Search apps, components, vulnerabilities, and policies"
                data-testid="nosc-search-results-input"
              >
                <TextField.Slot>
                  <ActionIcons.Search size={16} />
                </TextField.Slot>
                <TextField.Slot side="right">
                  <IconButton
                    type="button"
                    size="1"
                    variant={filtersOpen ? 'solid' : 'ghost'}
                    color="gray"
                    highContrast={filtersOpen}
                    aria-expanded={filtersOpen}
                    aria-controls="nosc-search-results-filter-bar"
                    aria-label={filtersOpen ? 'Hide filters' : 'Show filters'}
                    data-testid="nosc-search-results-filter-toggle"
                    onClick={() => setFiltersOpen((v) => !v)}
                  >
                    <ActionIcons.FilterList size={16} />
                  </IconButton>
                </TextField.Slot>
              </TextField.Root>
            </form>
            {filtersOpen && (
              <Box pt="3">
                {/* compact strips the dropdown-panel padding + trailing separator. */}
                <FilterBar
                  id="nosc-search-results-filter-bar"
                  compact
                  onInsert={handleFilterInsert}
                  onMenuClose={handleFilterMenuClose}
                />
              </Box>
            )}
          </Box>

          {/* Live region: announces the current result count without moving focus. */}
          <VisuallyHidden aria-live="polite" data-testid="nosc-search-results-live">
            {liveAnnouncement}
          </VisuallyHidden>

          {!query && <EmptyQuery />}

          {query && (
            <>
              <WarningPill warnings={warnings} />

              <SearchResultsTabs
                activeTab={activeTab}
                countsByType={countsByType}
                onTabChange={handleTabChange}
                loading={loading}
                visibleTypes={visibleTypeTabs}
                panelId="nosc-search-results-panel"
              />

              {/* IQ/Guide source filter — All tab only, and only when the active
                  source can actually return both kinds of row. In catalog mode every
                  row is a catalog row, so the IQ option would silently empty the list. */}
              {activeTab === 'all' && source === 'local' && (
                <Flex align="center" gap="2" data-testid="nosc-search-results-source-filter">
                  <Text size="1" color="gray">
                    Source
                  </Text>
                  <SegmentedControl.Root
                    size="1"
                    value={allSourceFilter}
                    onValueChange={(v) => setAllSourceFilter(v as 'all' | SearchSource)}
                    aria-label="Filter results by source"
                  >
                    <SegmentedControl.Item value="all" data-testid="nosc-search-source-all">
                      All
                    </SegmentedControl.Item>
                    <SegmentedControl.Item value="local" data-testid="nosc-search-source-local">
                      IQ
                    </SegmentedControl.Item>
                    <SegmentedControl.Item value="catalog" data-testid="nosc-search-source-catalog">
                      Guide
                    </SegmentedControl.Item>
                  </SegmentedControl.Root>
                </Flex>
              )}

              <Grid columns={showFacetRail ? { initial: '1', sm: '250px 1fr' } : '1'} gap="6">
                {showFacetRail && (
                  <Box display={{ initial: 'none', sm: 'block' }}>
                    <SearchResultsFilters
                      tab={requestedTab}
                      facets={facets}
                      query={query}
                      onQueryChange={handleFacetQueryChange}
                      onReset={handleFacetReset}
                      resetEnabled={resetEnabled}
                    />
                  </Box>
                )}
                <Box
                  minWidth="0"
                  id="nosc-search-results-panel"
                  role="tabpanel"
                  aria-labelledby={`nosc-search-results-panel-tab-${activeTab}`}
                >
                  {/* TODO(CLM-42562): the tabs loading skeleton slots in here,
                      replacing the list's own loading state while counts settle. */}
                  <SearchResultsList
                    results={filteredResults}
                    loading={loading}
                    loadError={loadError}
                    query={query}
                    onResultClick={handleResultClick}
                  />

                  {/* Gate on prev/next availability, not on the visible-row count: a client
                      filter (source / ecosystem) can empty the current page's rows while a
                      previous page still exists, and the user must retain a way to page back. */}
                  {!loadError && showPagination && (
                    <Flex align="center" justify="between" mt="4" data-testid="nosc-search-results-pagination">
                      <Button
                        variant="soft"
                        color="gray"
                        size="2"
                        disabled={!hasPrev}
                        onClick={goPrev}
                        aria-label="Previous page"
                        data-testid="nosc-search-results-prev"
                      >
                        <ActionIcons.ChevronLeft size={16} />
                        Previous
                      </Button>
                      <Text size="1" color="gray" aria-hidden="true">
                        Page {page}
                      </Text>
                      <Button
                        variant="soft"
                        color="gray"
                        size="2"
                        disabled={!hasNext}
                        onClick={goNext}
                        aria-label="Next page"
                        data-testid="nosc-search-results-next"
                      >
                        Next
                        <ActionIcons.ChevronRight size={16} />
                      </Button>
                    </Flex>
                  )}
                </Box>
              </Grid>
            </>
          )}
        </Flex>
      </Box>
    </Theme>
  );
}

function EmptyQuery(): JSX.Element {
  return (
    <Card size="4" data-testid="nosc-search-results-empty-query">
      <Flex direction="column" align="center" gap="3" p="4">
        <ActionIcons.Search size={24} color="var(--gray-9)" />
        <SectionHeading as="h2" align="center">
          Search across all of IQ
        </SectionHeading>
        <Text size="2" color="gray" align="center" style={{ maxWidth: '480px' }}>
          Type in the search bar above to find applications, components, vulnerabilities, policies, and more across your
          IQ instance.
        </Text>
      </Flex>
    </Card>
  );
}
