/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ComponentsListFilterState,
  EMPTY_COMPONENTS_LIST_FILTERS,
} from 'MainRoot/nosc/componentsList/componentsListFilters';
import {
  ComponentsTab,
  DEFAULT_COMPONENTS_TAB,
  componentsSourceToTab,
  componentsTabToSource,
} from 'MainRoot/nosc/componentsList/componentsRoute';

export interface ComponentsListQueryState {
  readonly tab: ComponentsTab;
  readonly search: string;
  /** 0-based page index for the list UI / request builder. */
  readonly page: number;
  readonly filters: ComponentsListFilterState;
}

/**
 * Soft ceiling for deep-linked 1-based {@code page} values. Prevents a stale bookmark like
 * {@code ?page=999999} from posting an absurd index on the first request.
 */
export const MAX_DEEP_LINK_PAGE = 10_000;

function asString(value: unknown): string | undefined {
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

/**
 * Parse a comma-separated hash-query list. Each segment is {@link decodeURIComponent}'d so
 * values that contain commas (e.g. org names like {@code Widgets, Inc.}) round-trip safely.
 */
function parseCsvParam(value: unknown): ReadonlyArray<string> {
  const raw = asString(value);
  if (!raw?.trim()) return [];
  return raw
    .split(',')
    .map((part) => {
      const trimmed = part.trim();
      if (!trimmed) return '';
      try {
        return decodeURIComponent(trimmed);
      } catch {
        return trimmed;
      }
    })
    .filter(Boolean);
}

/** Serialize filter values; encode each token so commas inside names do not split on parse. */
function serializeCsvParam(values: ReadonlySet<string>): string | undefined {
  if (values.size === 0) return undefined;
  return Array.from(values)
    .sort()
    .map((value) => encodeURIComponent(value))
    .join(',');
}

function parsePageIndex(value: unknown): number {
  const pageParam = typeof value === 'string' ? Number.parseInt(value, 10) : 1;
  if (!Number.isFinite(pageParam) || pageParam <= 1) {
    return 0;
  }
  return Math.min(pageParam, MAX_DEEP_LINK_PAGE) - 1;
}

function parseTab(params: Record<string, unknown>): ComponentsTab {
  // Prefer {@code source=local|catalog} (requirements); accept legacy {@code tab} synonyms.
  if (typeof params.source === 'string') {
    return componentsSourceToTab(params.source);
  }
  if (params.tab === 'catalog') return 'catalog';
  if (params.tab === 'myScanData') return 'myScanData';
  return DEFAULT_COMPONENTS_TAB;
}

/** Parse UI-Router params for the Martha Components list page (CLM-42214). */
export function parseComponentsListParams(params: Record<string, unknown>): ComponentsListQueryState {
  const search = typeof params.q === 'string' ? params.q.trim() : '';
  const page = parsePageIndex(params.page);
  const tab = parseTab(params);

  return {
    tab,
    search,
    page,
    filters: {
      organizations: new Set(parseCsvParam(params.org)),
      ecosystems: new Set(parseCsvParam(params.ecosystem)),
      applications: new Set(parseCsvParam(params.app)),
      stages: new Set(parseCsvParam(params.stage)),
    },
  };
}

/**
 * Serialize list state to hash-query params. Defaults/empty values become {@code undefined}
 * so UI-Router omits them (minimal bookmarks).
 */
export function buildComponentsListRouteParams(state: {
  readonly tab: ComponentsTab;
  readonly search: string;
  /** 0-based page index. */
  readonly page: number;
  readonly filters: ComponentsListFilterState;
}): Record<string, string | undefined> {
  const source = componentsTabToSource(state.tab);
  const myScanData = state.tab === 'myScanData';
  return {
    source: source === 'local' ? undefined : source,
    q: state.search.trim() || undefined,
    page: state.page > 0 ? String(state.page + 1) : undefined,
    org: myScanData ? serializeCsvParam(state.filters.organizations) : undefined,
    ecosystem: serializeCsvParam(state.filters.ecosystems),
    app: myScanData ? serializeCsvParam(state.filters.applications) : undefined,
    stage: myScanData ? serializeCsvParam(state.filters.stages) : undefined,
  };
}

/** Raw hash-query snapshot for the list URL fields (before parse/normalize). */
export function rawComponentsListParamsSnapshot(params: Record<string, unknown>): string {
  return JSON.stringify({
    source: asString(params.source),
    tab: asString(params.tab),
    q: asString(params.q),
    page: asString(params.page),
    org: asString(params.org),
    ecosystem: asString(params.ecosystem),
    app: asString(params.app),
    stage: asString(params.stage),
  });
}

export function emptyComponentsListQueryState(): ComponentsListQueryState {
  return {
    tab: DEFAULT_COMPONENTS_TAB,
    search: '',
    page: 0,
    filters: EMPTY_COMPONENTS_LIST_FILTERS,
  };
}
