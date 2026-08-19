/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Badge, Box, Dialog, Flex, IconButton, Text, TextField, Tooltip } from '@radix-ui/themes';
import { useDebounce } from '@react-hook/debounce';
import { tokens } from '@guide/ui-core/utils';
import { ArrowLeft, ChevronRight, Search, X } from 'lucide-react';
import { usePolicyContext } from './PolicyContext';
import { useOwnerAdapter } from './OwnerAdapterProvider';
import { useOnboarding } from 'GuideRoot/onboarding';
import type { AncestorPathEntry, AppSummary, OrgSummary, Owner } from './types';

const S = tokens.space;
const SZ = tokens.sizes;

/** Backend contract limits (GUIDE-3046). */
const TOP_ORGS_LIMIT = 20;
const APPS_LIMIT = 500;
const SEARCH_LIMIT = 10;
/** Backend rejects shorter queries with 400; gate in the UI before fetching. */
const SEARCH_MIN_QUERY_LENGTH = 3;
const SEARCH_DEBOUNCE_MS = 275;

/** Sentinel option id for the "Root Organization" row (which has no owner id of its own). */
const ROOT_OPTION_ID = '__root__';

type ModalView = 'orgs' | 'apps';
type TypeFilter = 'all' | 'org' | 'app';

function breadcrumb(path: AncestorPathEntry[]): string {
  return path.map((p) => p.name).join(' / ');
}

export function PolicyContextModal({ onClose }: { onClose: () => void }) {
  const adapter = useOwnerAdapter();
  const { activeOwner, setActiveOwner, activePath } = usePolicyContext();
  const { open: openOnboarding } = useOnboarding();

  const [view, setView] = useState<ModalView>('orgs');
  const [drillOrg, setDrillOrg] = useState<OrgSummary | null>(null);
  const [query, setQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('all');
  // Roving-tabindex cursor: the id of the single option that is in the Tab order. Arrow keys and
  // hover move it; Tab then enters the listbox once (landing on this row) and the next Tab leaves.
  const [rovingId, setRovingId] = useState<string | null>(null);

  // Browse data — fetched once per modal open (the component mounts fresh each open).
  const [topOrgs, setTopOrgs] = useState<{ orgs: OrgSummary[]; totalOrgCount: number } | null>(null);
  const [topLoading, setTopLoading] = useState(true);
  const [topError, setTopError] = useState(false);

  // Per-org apps, cached for the modal's lifetime so re-drilling the same org doesn't refetch.
  const appsCacheRef = useRef<Map<string, { apps: AppSummary[]; truncated: boolean }>>(new Map());
  const [drillApps, setDrillApps] = useState<{ apps: AppSummary[]; truncated: boolean } | null>(null);
  const [drillLoading, setDrillLoading] = useState(false);
  const [drillError, setDrillError] = useState(false);

  // Global search (Browse view only).
  const [searchResults, setSearchResults] = useState<{ results: Owner[]; truncated: boolean } | null>(null);
  const [searchInFlight, setSearchInFlight] = useState(false);
  const [searchError, setSearchError] = useState(false);

  const searchRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  // Cancels a superseded apps fetch. Owned here (not in the adapter like searchOwners) because a
  // cache hit supersedes an in-flight drill without calling the adapter — so the abort must fire
  // wherever the drilled org changes: drillIntoOrg, goBack, and unmount.
  const appsAbortRef = useRef<AbortController | null>(null);

  const trimmedQuery = query.trim();
  const isGlobalSearch = view === 'orgs' && trimmedQuery.length > 0;
  const isShortQuery = isGlobalSearch && trimmedQuery.length < SEARCH_MIN_QUERY_LENGTH;

  // Debounce the query via @react-hook/debounce (no hand-rolled setTimeout). The settled value
  // drives the fetch effect below; the adapter's AbortController cancels superseded requests.
  const [debouncedQuery, setDebouncedQuery] = useDebounce(trimmedQuery, SEARCH_DEBOUNCE_MS);
  const searchLoading =
    isGlobalSearch && !isShortQuery && (trimmedQuery !== debouncedQuery || searchInFlight);

  // Fetch top orgs on open.
  useEffect(() => {
    let cancelled = false;
    setTopLoading(true);
    setTopError(false);
    adapter
      .getTopOrgs(TOP_ORGS_LIMIT)
      .then((result) => {
        if (!cancelled) {
          setTopOrgs(result);
          setTopLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setTopError(true);
          setTopLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [adapter]);

  // Keep the debounced query following the live query.
  useEffect(() => {
    setDebouncedQuery(trimmedQuery);
  }, [trimmedQuery, setDebouncedQuery]);

  // Run the global search once the debounced query settles (Browse view, >= min length).
  // The adapter aborts any superseded in-flight request; the `active` flag guards against a stale
  // resolve after the effect re-runs (e.g. the view changed) without a new request to abort it.
  useEffect(() => {
    if (view !== 'orgs' || debouncedQuery.length < SEARCH_MIN_QUERY_LENGTH) {
      setSearchResults(null);
      setSearchInFlight(false);
      setSearchError(false);
      return;
    }
    let active = true;
    setSearchInFlight(true);
    setSearchError(false);
    adapter
      .searchOwners(debouncedQuery, 'all', SEARCH_LIMIT)
      .then((result) => {
        if (active) {
          setSearchResults(result);
          setSearchInFlight(false);
        }
      })
      .catch((error: unknown) => {
        // Superseded request aborted by the adapter — a newer search is in flight; ignore.
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        if (active) {
          setSearchError(true);
          setSearchInFlight(false);
        }
      });
    return () => {
      active = false;
      // Abort the in-flight search when this effect is torn down without a successor (unmount, or
      // leaving the search view) so the request doesn't run to completion — symmetric with the apps
      // fetch cancellation. A superseding search still aborts via searchOwners' own controller.
      adapter.cancelSearch();
    };
  }, [adapter, view, debouncedQuery]);

  const selectOwner = useCallback(
    (owner: Owner | null) => {
      setActiveOwner(owner);
      onClose();
    },
    [setActiveOwner, onClose]
  );

  const drillIntoOrg = useCallback(
    (org: OrgSummary) => {
      // Abort any in-flight apps fetch from a prior drill before switching orgs. Done here (not
      // only in the adapter) so a cache-hit supersession also cancels the earlier request, keeping
      // a slow/out-of-order response from landing under the wrong org header.
      appsAbortRef.current?.abort();
      appsAbortRef.current = null;

      setDrillOrg(org);
      setView('apps');
      setQuery('');
      setTypeFilter('all');

      const cached = appsCacheRef.current.get(org.id);
      if (cached) {
        setDrillApps(cached);
        setDrillLoading(false);
        setDrillError(false);
        return;
      }
      setDrillApps(null);
      setDrillLoading(true);
      setDrillError(false);
      const controller = new AbortController();
      appsAbortRef.current = controller;
      adapter
        .getAppsForOrg(org.id, APPS_LIMIT, controller.signal)
        .then((result) => {
          appsCacheRef.current.set(org.id, result);
          setDrillApps(result);
          setDrillLoading(false);
        })
        .catch((error: unknown) => {
          // Superseded drill aborted by a newer drill/back/unmount — ignore.
          if (error instanceof DOMException && error.name === 'AbortError') {
            return;
          }
          setDrillError(true);
          setDrillLoading(false);
        });
    },
    [adapter]
  );

  const goBack = useCallback(() => {
    // Cancel a still-pending apps fetch so a late response can't stomp state after returning to browse.
    appsAbortRef.current?.abort();
    appsAbortRef.current = null;
    setView('orgs');
    setDrillOrg(null);
    setDrillApps(null);
    setQuery('');
    setTypeFilter('all');
  }, []);

  // Abort any in-flight apps fetch on unmount (modal close) to avoid a state update after teardown.
  useEffect(() => () => appsAbortRef.current?.abort(), []);

  // Focus search on mount and whenever the view changes.
  useEffect(() => {
    searchRef.current?.focus();
  }, [view]);

  // Modal-level keyboard shortcuts, handled at the document level so they work regardless of which
  // row currently holds focus. Escape close is delegated here from the dialog (see the picker's
  // onEscapeKeyDown) so a query is cleared first and the modal only closes on a second press.
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (query) {
          setQuery('');
        } else {
          onClose();
        }
        return;
      }
      if (view === 'apps' && query.length === 0 && (e.key === 'ArrowLeft' || e.key === 'Backspace')) {
        e.preventDefault();
        goBack();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [view, query, goBack, onClose]);

  // Apps view: local, client-side filter over the loaded page (no fetch, no tabs).
  const filteredApps = useMemo(() => {
    if (!drillApps) {
      return [];
    }
    const q = trimmedQuery.toLowerCase();
    if (!q) {
      return drillApps.apps;
    }
    return drillApps.apps.filter((app) => app.name.toLowerCase().includes(q));
  }, [drillApps, trimmedQuery]);

  // Search results grouped by type for the All / Orgs / Apps tabs.
  const grouped = useMemo(() => {
    const results = searchResults?.results ?? [];
    return {
      orgs: results.filter((o): o is OrgSummary => o.type === 'org'),
      apps: results.filter((o): o is AppSummary => o.type === 'app'),
    };
  }, [searchResults]);

  // Ordered option ids for the current view, matching render order — the roving cursor walks this.
  const optionIds = useMemo<string[]>(() => {
    if (view === 'apps') {
      return [...(drillOrg ? [drillOrg.id] : []), ...filteredApps.map((a) => a.id)];
    }
    if (isGlobalSearch) {
      const ids: string[] = [];
      if (typeFilter !== 'app') {
        ids.push(...grouped.orgs.map((o) => o.id));
      }
      if (typeFilter !== 'org') {
        ids.push(...grouped.apps.map((a) => a.id));
      }
      return ids;
    }
    return [ROOT_OPTION_ID, ...(topOrgs?.orgs ?? []).map((o) => o.id)];
  }, [view, drillOrg, filteredApps, isGlobalSearch, typeFilter, grouped, topOrgs]);

  // The row actually in the Tab order: the remembered cursor if it still exists, else the first row.
  const effectiveRovingId =
    rovingId && optionIds.includes(rovingId) ? rovingId : (optionIds[0] ?? null);

  // Announced to screen readers on each view/content transition. Count-bearing where a count exists
  // so the message text actually changes as results land (a static string would never re-announce),
  // and reflects the in-flight/short-query states rather than pre-empting them with a result label.
  const searchResultCount = (searchResults?.results.length ?? 0);
  const liveMessage = isGlobalSearch
    ? isShortQuery
      ? `Type at least ${SEARCH_MIN_QUERY_LENGTH} characters to search`
      : searchLoading
        ? 'Searching…'
        : `${searchResultCount} ${searchResultCount === 1 ? 'result' : 'results'}`
    : view === 'apps' && drillOrg
      ? `Showing apps in ${drillOrg.name}`
      : 'Showing all organizations';

  return (
    <Flex direction="column" style={{ height: '100%', minHeight: 0 }}>
      {/* Header */}
      <Flex align="center" gap={S.inline} px={S.item} pt={S.section} pb={S.inline}>
        {view === 'apps' && (
          <IconButton variant="ghost" size={SZ.body.xs} onClick={goBack} aria-label="Back to all organizations">
            <ArrowLeft size={14} />
          </IconButton>
        )}
        <Box style={{ flex: 1, minWidth: 0 }}>
          <Dialog.Title mb="0">
            <Text size={SZ.body.sm} weight="bold" style={{ color: 'var(--gray-12)' }}>
              {view === 'apps' ? 'Policy context / Applications' : 'Policy context'}
            </Text>
          </Dialog.Title>
        </Box>
        <Text asChild size={SZ.body.xs}>
          <button
            type="button"
            onClick={() => {
              openOnboarding(); // set the tour open on the app-wide provider…
              onClose(); // …then close the picker (the two dialogs cannot stack).
            }}
            style={{
              background: 'none',
              border: 'none',
              padding: 0,
              cursor: 'pointer',
              color: 'var(--accent-11)',
              textDecoration: 'underline',
              flexShrink: 0,
            }}
          >
            Need help?
          </button>
        </Text>
      </Flex>

      {/* Currently-selected banner — Browse only */}
      {!isGlobalSearch && view === 'orgs' && activeOwner && (
        <Box px={S.item} pb={S.item}>
          <Flex
            align="center"
            gap={S.item}
            px={S.section}
            py={S.item}
            aria-label="Current policy context"
            style={{
              background: 'var(--accent-3)',
              borderLeft: '3px solid var(--accent-9)',
              borderRadius: 6,
            }}
          >
            <Box style={{ flex: 1, minWidth: 0 }}>
              <Text
                size={SZ.body.xs}
                weight="bold"
                mb={S.tight}
                style={{
                  display: 'block',
                  textTransform: 'uppercase',
                  letterSpacing: '0.08em',
                  fontSize: 10,
                  color: 'var(--gray-11)',
                }}
              >
                Currently selected
              </Text>
              <Text size={SZ.body.sm} weight="bold" style={{ display: 'block', lineHeight: 1.25, color: 'var(--gray-12)' }}>
                {activePath.length > 1 ? (
                  <>
                    <span style={{ color: 'var(--gray-11)', fontWeight: 400 }}>
                      {activePath.slice(0, -1).map((p) => p.name).join(' / ')}
                      {' / '}
                    </span>
                    {activePath[activePath.length - 1].name}
                  </>
                ) : activePath.length === 1 ? (
                  activePath[0].name
                ) : null}
              </Text>
            </Box>
            <Badge color="indigo" variant="solid" size={SZ.body.xs} aria-hidden>
              {activeOwner.type.toUpperCase()}
            </Badge>
          </Flex>
        </Box>
      )}

      {/* Search / filter field */}
      <Box px={S.item} pb={S.inline}>
        <TextField.Root
          ref={searchRef}
          size={SZ.body.sm}
          placeholder={view === 'apps' && drillOrg ? `Search apps in ${drillOrg.name}…` : 'Search all orgs & apps…'}
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setTypeFilter('all');
          }}
          onKeyDown={(e) => {
            if (e.key === 'ArrowDown') {
              e.preventDefault();
              const first = listRef.current?.querySelector<HTMLElement>('[role="option"]');
              first?.focus();
            }
          }}
          aria-label="Search policy context"
        >
          <TextField.Slot>
            <Search size={14} />
          </TextField.Slot>
          {/* Escape handled at the document level (clear-then-close); ArrowDown moves into the list. */}
          {query && (
            <TextField.Slot>
              <IconButton
                size={SZ.body.xs}
                variant="ghost"
                // Restore focus to the search input: clearing the query unmounts this button (the
                // `{query && …}` guard), which would otherwise drop focus to <body> and strand
                // keyboard/screen-reader users.
                onClick={() => {
                  setQuery('');
                  searchRef.current?.focus();
                }}
                aria-label="Clear search"
              >
                <X size={12} />
              </IconButton>
            </TextField.Slot>
          )}
        </TextField.Root>
      </Box>

      {/* Type-filter tabs — global search only. Hidden while a newer search is in flight so the tab
          counts never disagree with the "Searching…" list body below. */}
      {isGlobalSearch && !isShortQuery && !searchLoading && searchResults && (
        <Flex gap={S.tight} px={S.item} pb={S.inline} wrap="wrap">
          {(
            [
              { key: 'all', label: 'All', count: grouped.orgs.length + grouped.apps.length },
              { key: 'org', label: 'Orgs', count: grouped.orgs.length },
              { key: 'app', label: 'Apps', count: grouped.apps.length },
            ] as { key: TypeFilter; label: string; count: number }[]
          ).map((tab) => (
            <Flex key={tab.key} asChild align="center" gap={S.inline} px={S.item} py={S.tight}>
              <button
                onClick={() => setTypeFilter(tab.key)}
                aria-pressed={typeFilter === tab.key}
                style={{
                  borderRadius: 20,
                  fontSize: 11,
                  border: '1px solid',
                  borderColor: typeFilter === tab.key ? 'var(--accent-7)' : 'var(--gray-5)',
                  background: typeFilter === tab.key ? 'var(--accent-2)' : 'transparent',
                  color: typeFilter === tab.key ? 'var(--accent-11)' : 'var(--gray-10)',
                  cursor: 'pointer',
                  fontWeight: typeFilter === tab.key ? 600 : 400,
                }}
              >
                {tab.label}
                <Badge color="gray" variant="soft" size={SZ.body.xs}>
                  {tab.count}
                </Badge>
              </button>
            </Flex>
          ))}
        </Flex>
      )}

      {/* Screen-reader announcement for view transitions */}
      <Box
        role="status"
        aria-live="polite"
        aria-atomic="true"
        style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0,0,0,0)', whiteSpace: 'nowrap' }}
      >
        {liveMessage}
      </Box>

      {/* List */}
      <Box
        ref={listRef}
        px={S.item}
        pb={S.inline}
        style={{ flex: 1, minHeight: 0, overflowY: 'auto' }}
        role="listbox"
        aria-label="Policy context options"
        onKeyDown={(e) => {
          if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') {
            return;
          }
          e.preventDefault();
          const items = Array.from(listRef.current?.querySelectorAll<HTMLElement>('[role="option"]') ?? []);
          const idx = items.indexOf(document.activeElement as HTMLElement);
          if (e.key === 'ArrowDown') {
            items[idx + 1]?.focus();
          } else if (idx <= 0) {
            searchRef.current?.focus();
          } else {
            items[idx - 1]?.focus();
          }
        }}
      >
        {view === 'apps'
          ? renderAppsView({
              drillOrg: drillOrg!,
              drillApps,
              filteredApps,
              loading: drillLoading,
              error: drillError,
              hasFilter: trimmedQuery.length > 0,
              activeOwner,
              selectOwner,
              rovingId: effectiveRovingId,
              onRowFocus: setRovingId,
            })
          : isGlobalSearch
            ? renderSearchView({
                isShortQuery,
                loading: searchLoading,
                error: searchError,
                results: searchResults,
                grouped,
                typeFilter,
                activeOwner,
                selectOwner,
                rovingId: effectiveRovingId,
                onRowFocus: setRovingId,
              })
            : renderBrowseView({
                loading: topLoading,
                error: topError,
                topOrgs,
                activeOwner,
                selectOwner,
                drillIntoOrg,
                rovingId: effectiveRovingId,
                onRowFocus: setRovingId,
              })}
      </Box>
    </Flex>
  );
}

/* ── View renderers ── */

function renderBrowseView({
  loading,
  error,
  topOrgs,
  activeOwner,
  selectOwner,
  drillIntoOrg,
  rovingId,
  onRowFocus,
}: {
  loading: boolean;
  error: boolean;
  topOrgs: { orgs: OrgSummary[]; totalOrgCount: number } | null;
  activeOwner: Owner | null;
  selectOwner: (o: Owner | null) => void;
  drillIntoOrg: (o: OrgSummary) => void;
  rovingId: string | null;
  onRowFocus: (id: string) => void;
}) {
  return (
    <>
      <OrgRootRow
        active={!activeOwner}
        onSelect={() => selectOwner(null)}
        tabbable={rovingId === ROOT_OPTION_ID}
        onFocusRow={() => onRowFocus(ROOT_OPTION_ID)}
      />
      {loading ? (
        <StatusText>Loading organizations…</StatusText>
      ) : error ? (
        <StatusText>Couldn&apos;t load organizations. Please try again.</StatusText>
      ) : !topOrgs || topOrgs.orgs.length === 0 ? (
        <StatusText>You don&apos;t have policy-evaluation access on any organizations.</StatusText>
      ) : (
        <>
          <SectionHeader label="Organizations" count={topOrgs.totalOrgCount} />
          {topOrgs.orgs.map((org) => (
            <OrgRow
              key={org.id}
              org={org}
              active={activeOwner?.id === org.id}
              onSelect={() => selectOwner(org)}
              onDrill={() => drillIntoOrg(org)}
              tabbable={rovingId === org.id}
              onFocusRow={() => onRowFocus(org.id)}
            />
          ))}
          {topOrgs.totalOrgCount > topOrgs.orgs.length && (
            <HintText>+ {topOrgs.totalOrgCount - topOrgs.orgs.length} more — search to find</HintText>
          )}
        </>
      )}
    </>
  );
}

function renderAppsView({
  drillOrg,
  drillApps,
  filteredApps,
  loading,
  error,
  hasFilter,
  activeOwner,
  selectOwner,
  rovingId,
  onRowFocus,
}: {
  drillOrg: OrgSummary;
  drillApps: { apps: AppSummary[]; truncated: boolean } | null;
  filteredApps: AppSummary[];
  loading: boolean;
  error: boolean;
  hasFilter: boolean;
  activeOwner: Owner | null;
  selectOwner: (o: Owner | null) => void;
  rovingId: string | null;
  onRowFocus: (id: string) => void;
}) {
  return (
    <>
      <DrillOrgHeroCard
        org={drillOrg}
        active={activeOwner?.id === drillOrg.id}
        appCount={drillApps?.apps.length ?? drillOrg.appCount}
        onSelect={() => selectOwner(drillOrg)}
        tabbable={rovingId === drillOrg.id}
        onFocusRow={() => onRowFocus(drillOrg.id)}
      />
      {/* Count tracks the rendered list: with an active filter it reflects the matches shown, not
          the full loaded page. Equivalent to the loaded total when unfiltered, and 0 while loading. */}
      <SectionHeader label="Or pick a specific application" count={filteredApps.length} />
      {loading ? (
        <StatusText>Loading applications…</StatusText>
      ) : error ? (
        <StatusText>Couldn&apos;t load applications. Please try again.</StatusText>
      ) : filteredApps.length === 0 ? (
        <StatusText>{hasFilter ? 'No matching applications' : 'No applications in this organization'}</StatusText>
      ) : (
        <>
          {filteredApps.map((app) => (
            <AppRow
              key={app.id}
              app={app}
              active={activeOwner?.id === app.id}
              onSelect={() => selectOwner(app)}
              tabbable={rovingId === app.id}
              onFocusRow={() => onRowFocus(app.id)}
            />
          ))}
          {drillApps?.truncated && (
            <HintText>
              Showing the first {drillApps.apps.length} applications — use global search to find others.
            </HintText>
          )}
        </>
      )}
    </>
  );
}

function renderSearchView({
  isShortQuery,
  loading,
  error,
  results,
  grouped,
  typeFilter,
  activeOwner,
  selectOwner,
  rovingId,
  onRowFocus,
}: {
  isShortQuery: boolean;
  loading: boolean;
  error: boolean;
  results: { results: Owner[]; truncated: boolean } | null;
  grouped: { orgs: OrgSummary[]; apps: AppSummary[] };
  typeFilter: TypeFilter;
  activeOwner: Owner | null;
  selectOwner: (o: Owner | null) => void;
  rovingId: string | null;
  onRowFocus: (id: string) => void;
}) {
  if (isShortQuery) {
    return <StatusText>Type at least {SEARCH_MIN_QUERY_LENGTH} characters to search.</StatusText>;
  }
  if (loading) {
    return <StatusText>Searching…</StatusText>;
  }
  if (error) {
    return <StatusText>Search failed. Please try again.</StatusText>;
  }
  const showOrgs = typeFilter !== 'app';
  const showApps = typeFilter !== 'org';
  const hasOrgs = showOrgs && grouped.orgs.length > 0;
  const hasApps = showApps && grouped.apps.length > 0;
  if (!hasOrgs && !hasApps) {
    return <StatusText>No results</StatusText>;
  }
  return (
    <>
      {hasOrgs && (
        <>
          <SectionHeader label="Organizations" count={grouped.orgs.length} />
          {grouped.orgs.map((owner) => (
            <SearchResultRow
              key={owner.id}
              owner={owner}
              pathLabel={breadcrumb(owner.ancestorPath)}
              active={activeOwner?.id === owner.id}
              onSelect={() => selectOwner(owner)}
              tabbable={rovingId === owner.id}
              onFocusRow={() => onRowFocus(owner.id)}
            />
          ))}
        </>
      )}
      {hasApps && (
        <>
          <SectionHeader label="Applications" count={grouped.apps.length} />
          {grouped.apps.map((owner) => (
            <SearchResultRow
              key={owner.id}
              owner={owner}
              pathLabel={breadcrumb(owner.ancestorPath)}
              active={activeOwner?.id === owner.id}
              onSelect={() => selectOwner(owner)}
              tabbable={rovingId === owner.id}
              onFocusRow={() => onRowFocus(owner.id)}
            />
          ))}
        </>
      )}
      {results?.truncated && (
        <HintText>More results available — refine your search.</HintText>
      )}
    </>
  );
}

/* ── Sub-components ── */

function StatusText({ children }: { children: React.ReactNode }) {
  return (
    // role="presentation": a status/empty message is not a listbox option, so it must not be
    // exposed as one (ARIA listbox may only contain option/group/presentation children).
    <Box role="presentation" px={S.item} py={S.item}>
      <Text size={SZ.body.sm} color="gray">
        {children}
      </Text>
    </Box>
  );
}

/** Small italic hint (e.g. "+ N more", truncation notices) shown under a list section. */
function HintText({ children }: { children: React.ReactNode }) {
  return (
    <Box role="presentation" px={S.item} py={S.inline}>
      <Text size={SZ.body.xs} color="gray" style={{ fontStyle: 'italic' }}>
        {children}
      </Text>
    </Box>
  );
}

function SelectedPill() {
  return (
    <Badge
      color="indigo"
      variant="solid"
      size={SZ.body.xs}
      aria-hidden
      style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: 9, fontWeight: 600, lineHeight: 1, padding: '2px 6px' }}
    >
      Selected
    </Badge>
  );
}

function OrgRootRow({
  active,
  onSelect,
  tabbable,
  onFocusRow,
}: {
  active: boolean;
  onSelect: () => void;
  tabbable: boolean;
  onFocusRow: () => void;
}) {
  const [hovered, setHovered] = useState(false);
  return (
    <Flex asChild align="center" justify="between" px={S.item} py={S.inline} mb={S.tight}>
      <button
        onClick={onSelect}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        onFocus={() => {
          setHovered(true);
          onFocusRow();
        }}
        onBlur={() => setHovered(false)}
        role="option"
        aria-selected={active}
        aria-label="Root Organization — all policies"
        tabIndex={tabbable ? 0 : -1}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onSelect();
          }
        }}
        style={{
          width: '100%',
          background: active ? 'var(--accent-2)' : hovered ? 'var(--gray-2)' : 'transparent',
          border: 'none',
          borderRadius: 6,
          cursor: 'pointer',
        }}
      >
        <Flex align="center" gap={S.inline}>
          <Text size={SZ.body.sm} style={{ color: active ? 'var(--accent-11)' : 'var(--gray-12)' }}>
            Root Organization
          </Text>
          {active && <SelectedPill />}
        </Flex>
        <Text size={SZ.body.xs} color="gray">
          all policies
        </Text>
      </button>
    </Flex>
  );
}

function DrillOrgHeroCard({
  org,
  active,
  appCount,
  onSelect,
  tabbable,
  onFocusRow,
}: {
  org: OrgSummary;
  active: boolean;
  appCount: number;
  onSelect: () => void;
  tabbable: boolean;
  onFocusRow: () => void;
}) {
  const [hovered, setHovered] = useState(false);
  return (
    <Flex
      asChild
      align="center"
      gap={S.item}
      px={S.section}
      py={S.item}
      mt={S.tight}
      mb={S.item}
    >
      <button
        onClick={onSelect}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        onFocus={() => {
          setHovered(true);
          onFocusRow();
        }}
        onBlur={() => setHovered(false)}
        role="option"
        aria-selected={active}
        aria-label={`Select ${org.name} as context`}
        tabIndex={tabbable ? 0 : -1}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onSelect();
          }
        }}
        style={{
          width: '100%',
          textAlign: 'left',
          border: `1px solid var(${active ? '--accent-8' : '--accent-6'})`,
          background: 'var(--accent-2)',
          borderRadius: 8,
          cursor: 'pointer',
          boxShadow: hovered ? '0 0 0 2px var(--accent-7)' : 'none',
        }}
      >
        <Box style={{ flex: 1, minWidth: 0 }}>
          <Text size={SZ.body.sm} weight="bold" style={{ display: 'block', color: 'var(--gray-12)' }}>
            Select entire organization
          </Text>
          <Text size={SZ.body.xs} mt={S.tight} style={{ display: 'block', color: 'var(--gray-11)' }}>
            Applies to {org.name} and all {appCount} {appCount === 1 ? 'app' : 'apps'}
          </Text>
        </Box>
        {active ? (
          <SelectedPill />
        ) : (
          <Flex asChild align="center" px={S.item} py={S.tight}>
            <span
              style={{
                background: 'var(--accent-9)',
                color: 'var(--accent-contrast)',
                borderRadius: 6,
                fontSize: 12,
                fontWeight: 600,
                lineHeight: 1.4,
              }}
            >
              Select
            </span>
          </Flex>
        )}
      </button>
    </Flex>
  );
}

function SectionHeader({ label, count }: { label: string; count: number }) {
  return (
    <Flex role="presentation" align="center" gap={S.inline} pt={S.inline} px={S.item} pb={S.tight}>
      <Text size={SZ.body.xs} weight="bold" style={{ textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--gray-9)' }}>
        {label}
      </Text>
      <Badge color="gray" variant="soft" size={SZ.body.xs}>
        {count}
      </Badge>
    </Flex>
  );
}

function OrgRow({
  org,
  active,
  onSelect,
  onDrill,
  tabbable,
  onFocusRow,
}: {
  org: OrgSummary;
  active: boolean;
  onSelect: () => void;
  onDrill: () => void;
  tabbable: boolean;
  onFocusRow: () => void;
}) {
  const [hovered, setHovered] = useState(false);
  // Orgs with no directly-selectable apps are leaves — Enter/Space/click selects them; orgs with
  // apps are drill targets — Enter/Space/ArrowRight/click drills in.
  const isLeaf = org.appCount === 0;
  const primaryAction = isLeaf ? onSelect : onDrill;
  const pathLabel = breadcrumb(org.ancestorPath);
  return (
    <Flex
      align="center"
      justify="between"
      px={S.item}
      py={S.inline}
      tabIndex={tabbable ? 0 : -1}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          primaryAction();
        } else if (e.key === 'ArrowRight' && !isLeaf) {
          e.preventDefault();
          onDrill();
        }
      }}
      style={{
        borderRadius: 6,
        background: active ? 'var(--accent-2)' : hovered ? 'var(--gray-2)' : 'transparent',
        cursor: 'pointer',
      }}
      // Hover updates highlight only — no .focus(). Moving real DOM focus on hover would yank
      // keyboard focus off the search input mid-keystroke (the list sits directly below it). The
      // roving-tabindex cursor is driven by onFocus/arrow keys, not by the mouse.
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocus={() => {
        setHovered(true);
        onFocusRow();
      }}
      onBlur={() => setHovered(false)}
      onClick={primaryAction}
      role="option"
      aria-selected={active}
      aria-label={`${org.name}${pathLabel ? `, ${pathLabel}` : ''}, ${org.appCount} ${org.appCount === 1 ? 'application' : 'applications'}`}
    >
      <Box style={{ flex: 1, minWidth: 0 }}>
        <Flex align="center" gap={S.inline}>
          <Text size={SZ.body.sm} truncate style={{ color: active ? 'var(--accent-11)' : 'var(--gray-12)' }}>
            {org.name}
          </Text>
          {active && <SelectedPill />}
        </Flex>
        {pathLabel && (
          <Text size={SZ.body.xs} color="gray" truncate style={{ display: 'block' }}>
            {pathLabel}
          </Text>
        )}
      </Box>
      <Flex align="center" gap={S.inline} style={{ flexShrink: 0 }}>
        <Tooltip content={`${org.appCount} ${org.appCount === 1 ? 'application' : 'applications'}`}>
          <Badge color="gray" variant="soft" size={SZ.body.xs}>
            {org.appCount}
          </Badge>
        </Tooltip>
        {/* Chevron shown on every org row for consistent alignment (0-app orgs still select directly). */}
        <ChevronRight size={12} color="var(--gray-7)" />
      </Flex>
    </Flex>
  );
}

function AppRow({
  app,
  active,
  onSelect,
  tabbable,
  onFocusRow,
}: {
  app: AppSummary;
  active: boolean;
  onSelect: () => void;
  tabbable: boolean;
  onFocusRow: () => void;
}) {
  const [hovered, setHovered] = useState(false);
  return (
    <Flex
      align="center"
      justify="between"
      px={S.item}
      py={S.inline}
      tabIndex={tabbable ? 0 : -1}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onSelect();
        }
      }}
      style={{
        borderRadius: 6,
        background: active ? 'var(--accent-2)' : hovered ? 'var(--gray-2)' : 'transparent',
        cursor: 'pointer',
      }}
      // Hover updates highlight only — no .focus(). Moving real DOM focus on hover would yank
      // keyboard focus off the search input mid-keystroke (the list sits directly below it). The
      // roving-tabindex cursor is driven by onFocus/arrow keys, not by the mouse.
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocus={() => {
        setHovered(true);
        onFocusRow();
      }}
      onBlur={() => setHovered(false)}
      onClick={onSelect}
      role="option"
      aria-selected={active}
    >
      <Flex align="center" gap={S.inline} style={{ minWidth: 0 }}>
        <Text size={SZ.body.sm} style={{ color: active ? 'var(--accent-11)' : 'var(--gray-12)' }}>
          {app.name}
        </Text>
        {active && <SelectedPill />}
      </Flex>
    </Flex>
  );
}

function SearchResultRow({
  owner,
  pathLabel,
  active,
  onSelect,
  tabbable,
  onFocusRow,
}: {
  owner: Owner;
  pathLabel: string;
  active: boolean;
  onSelect: () => void;
  tabbable: boolean;
  onFocusRow: () => void;
}) {
  const [hovered, setHovered] = useState(false);
  return (
    <Flex
      align="center"
      justify="between"
      px={S.item}
      py={S.inline}
      tabIndex={tabbable ? 0 : -1}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onSelect();
        }
      }}
      style={{
        borderRadius: 6,
        background: active ? 'var(--accent-2)' : hovered ? 'var(--gray-2)' : 'transparent',
        cursor: 'pointer',
      }}
      // Hover updates highlight only — no .focus(). Moving real DOM focus on hover would yank
      // keyboard focus off the search input mid-keystroke (the list sits directly below it). The
      // roving-tabindex cursor is driven by onFocus/arrow keys, not by the mouse.
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocus={() => {
        setHovered(true);
        onFocusRow();
      }}
      onBlur={() => setHovered(false)}
      onClick={onSelect}
      role="option"
      aria-selected={active}
    >
      <Box style={{ minWidth: 0, flex: 1 }}>
        <Flex align="center" gap={S.inline}>
          <Text size={SZ.body.sm} truncate style={{ color: active ? 'var(--accent-11)' : 'var(--gray-12)' }}>
            {owner.name}
          </Text>
          {active && <SelectedPill />}
        </Flex>
        {pathLabel && (
          <Text size={SZ.body.xs} color="gray" truncate style={{ display: 'block' }}>
            {pathLabel}
          </Text>
        )}
      </Box>
    </Flex>
  );
}
