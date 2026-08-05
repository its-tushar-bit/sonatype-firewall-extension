/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ComponentsListFilterState,
  COMPONENTS_THREAT_MAX,
  COMPONENTS_THREAT_MIN,
  DEFAULT_COMPONENTS_THREAT_RANGE,
  EMPTY_COMPONENTS_LIST_FILTERS,
  isDefaultComponentsThreatRange,
  type ComponentsThreatRange,
} from 'MainRoot/nosc/componentsList/componentsListFilters';
import {
  ComponentsTab,
  DEFAULT_COMPONENTS_TAB,
  componentsSourceToTab,
  componentsTabToSource,
} from 'MainRoot/nosc/componentsList/componentsRoute';
import {
  MAX_DEEP_LINK_PAGE,
  asString,
  parsePageIndex,
  parseThreatRangeParam,
  serializeThreatRangeParam,
} from 'MainRoot/nosc/list/listQueryCodec';

export interface ComponentsListQueryState {
  readonly tab: ComponentsTab;
  readonly search: string;
  /** 0-based page index for the list UI / request builder. */
  readonly page: number;
  readonly filters: ComponentsListFilterState;
}

export { MAX_DEEP_LINK_PAGE };

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

function parseTab(params: Record<string, unknown>): ComponentsTab {
  // Prefer {@code source=local|catalog} (requirements); accept legacy {@code tab} synonyms.
  if (typeof params.source === 'string') {
    return componentsSourceToTab(params.source);
  }
  if (params.tab === 'catalog') return 'catalog';
  if (params.tab === 'myScanData') return 'myScanData';
  return DEFAULT_COMPONENTS_TAB;
}

/**
 * Parse {@code threat=min-max} via the shared list codec (Applications / Violations-compatible).
 * Malformed tokens fall back to the full-domain default.
 */
export function parseComponentsThreatRange(value: string | null | undefined): ComponentsThreatRange {
  return parseThreatRangeParam(value, {
    minDomain: COMPONENTS_THREAT_MIN,
    maxDomain: COMPONENTS_THREAT_MAX,
    defaultRange: DEFAULT_COMPONENTS_THREAT_RANGE,
  });
}

function serializeComponentsThreatRange(range: ComponentsThreatRange): string | undefined {
  return serializeThreatRangeParam(range, isDefaultComponentsThreatRange);
}

/** Parse UI-Router params for the Martha Components list page (CLM-42214 / CLM-43960). */
export function parseComponentsListParams(params: Record<string, unknown>): ComponentsListQueryState {
  const search = typeof params.q === 'string' ? params.q.trim() : '';
  const page = parsePageIndex(params.page);
  const tab = parseTab(params);
  // Threat is My Scan Data only — ignore crafted/legacy {@code threat=} on Catalog URLs so Reset
  // does not appear for a hidden control.
  const threatRange =
    tab === 'myScanData'
      ? parseComponentsThreatRange(typeof params.threat === 'string' ? params.threat : null)
      : DEFAULT_COMPONENTS_THREAT_RANGE;

  return {
    tab,
    search,
    page,
    filters: {
      organizations: new Set(parseCsvParam(params.org)),
      ecosystems: new Set(parseCsvParam(params.ecosystem)),
      applications: new Set(parseCsvParam(params.app)),
      stages: new Set(parseCsvParam(params.stage)),
      threatRange,
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
    threat: myScanData ? serializeComponentsThreatRange(state.filters.threatRange) : undefined,
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
    threat: asString(params.threat),
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
