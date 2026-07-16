/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ViolationsFilterState,
  ViolationThreatRange,
  ViolationWaiverType,
  VIOLATION_THREAT_MAX,
  VIOLATION_THREAT_MIN,
} from 'MainRoot/nosc/violations/violationListTypes';
import {
  createDefaultViolationsFilterState,
  DEFAULT_VIOLATION_THREAT_RANGE,
  isDefaultThreatRange,
  STATE_LABELS,
  THREAT_CATEGORY_LABELS,
} from 'MainRoot/nosc/violations/violationsListApi';

/**
 * URL-friendly names for the list-page hash query. Sort is intentionally absent: the Violations list
 * only supports the single {@code -policyThreatLevel} order, so there is nothing to persist.
 */
export interface ViolationsListQueryState {
  readonly search: string;
  /** 0-based page index for the list API. */
  readonly page: number;
  readonly filters: ViolationsFilterState;
}

// Accepted URL tokens are derived from the label maps' keys (single source of truth), so a new
// state/category added there is honored in deep links instead of being silently dropped here.
/** Violation states the backend list validator accepts; unknown tokens in the URL are dropped. */
const SUPPORTED_STATES = new Set<string>(Object.keys(STATE_LABELS));

/** Policy threat categories the facet exposes; unknown tokens in the URL are dropped. */
const SUPPORTED_CATEGORIES = new Set<string>(Object.keys(THREAT_CATEGORY_LABELS));

function asString(value: unknown): string | undefined {
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

function parseCsvParam(value: unknown): ReadonlyArray<string> {
  const raw = asString(value);
  if (!raw?.trim()) return [];
  return raw.split(',').map((part) => part.trim()).filter(Boolean);
}

function serializeCsvParam(values: ReadonlySet<string>): string | undefined {
  if (values.size === 0) return undefined;
  return Array.from(values).sort().join(',');
}

function parseFilteredSet(value: unknown, allowed: ReadonlySet<string>): ReadonlySet<string> {
  return new Set(parseCsvParam(value).filter((id) => allowed.has(id)));
}

/** Parse a strictly-integer token, or undefined for anything else (e.g. {@code 4abc}, {@code 4.5}). */
function parseIntegerToken(token: string): number | undefined {
  return /^\d+$/.test(token) ? Number(token) : undefined;
}

/**
 * Parse a {@code "min-max"} threat range param, clamped to the [0, 10] domain and forced ascending.
 * Falls back to the full-domain default when the value is missing or malformed. Tokens must be whole
 * integers ({@code Number.parseInt} would accept {@code 4abc}), matching the drop-malformed intent.
 */
function parseThreatRange(value: unknown): ViolationThreatRange {
  const raw = asString(value);
  if (!raw) return DEFAULT_VIOLATION_THREAT_RANGE;
  const parts = raw.split('-');
  if (parts.length !== 2) return DEFAULT_VIOLATION_THREAT_RANGE;
  const min = parseIntegerToken(parts[0].trim());
  const max = parseIntegerToken(parts[1].trim());
  if (min === undefined || max === undefined) return DEFAULT_VIOLATION_THREAT_RANGE;
  const clamp = (n: number): number => Math.min(VIOLATION_THREAT_MAX, Math.max(VIOLATION_THREAT_MIN, n));
  const lo = clamp(min);
  const hi = clamp(max);
  return [Math.min(lo, hi), Math.max(lo, hi)];
}

function serializeThreatRange(range: ViolationThreatRange): string | undefined {
  return isDefaultThreatRange(range) ? undefined : `${range[0]}-${range[1]}`;
}

// URL tokens for the waiver-type radio. Lowercase in the hash for readability; ANY is the default and
// is omitted from the URL entirely. TYPE_TO_URL is derived from URL_TO_TYPE (single source of truth) so
// the two directions cannot drift apart.
const WAIVER_URL_TO_TYPE: Readonly<Record<string, ViolationWaiverType>> = {
  auto: 'AUTO',
  manual: 'MANUAL',
};
const WAIVER_TYPE_TO_URL: Readonly<Record<string, string>> = Object.fromEntries(
  Object.entries(WAIVER_URL_TO_TYPE).map(([url, type]) => [type, url]),
);

function parseWaiverType(value: unknown): ViolationWaiverType {
  const raw = asString(value)?.toLowerCase();
  return (raw && WAIVER_URL_TO_TYPE[raw]) || 'ANY';
}

function serializeWaiverType(waiverType: ViolationWaiverType): string | undefined {
  return WAIVER_TYPE_TO_URL[waiverType];
}

/**
 * Soft ceiling for deep-linked 1-based {@code page} values. Prevents a stale bookmark like
 * {@code ?page=999999} from posting an absurd 0-based index on the first request; the container still
 * response-clamps to the real last page once {@code total} is known.
 */
export const MAX_DEEP_LINK_PAGE = 10_000;

function parsePageIndex(value: unknown): number {
  const pageParam = typeof value === 'string' ? Number.parseInt(value, 10) : 1;
  if (!Number.isFinite(pageParam) || pageParam <= 1) {
    return 0;
  }
  return Math.min(pageParam, MAX_DEEP_LINK_PAGE) - 1;
}

/** Parse UI-Router params for the Martha Violations list page (CLM-42260). */
export function parseViolationsListParams(params: Record<string, unknown>): ViolationsListQueryState {
  const search = typeof params.q === 'string' ? params.q.trim() : '';
  const page = parsePageIndex(params.page);

  return {
    search,
    page,
    filters: {
      ...createDefaultViolationsFilterState(),
      states: parseFilteredSet(params.state, SUPPORTED_STATES),
      threatCategories: parseFilteredSet(params.category, SUPPORTED_CATEGORIES),
      stageIds: new Set(parseCsvParam(params.stage)),
      organizationIds: new Set(parseCsvParam(params.org)),
      applicationIds: new Set(parseCsvParam(params.app)),
      threatRange: parseThreatRange(params.threat),
      waiverType: parseWaiverType(params.waiver),
    },
  };
}

/**
 * Serialize list state to hash-query params. Keys are emitted in a stable order and default/empty
 * values map to {@code undefined} so UI-Router omits them from the URL (keeping bookmarks minimal).
 */
export function buildViolationsListRouteParams(
  state: ViolationsListQueryState,
): Record<string, string | undefined> {
  return {
    q: state.search.trim() || undefined,
    page: state.page > 0 ? String(state.page + 1) : undefined,
    state: serializeCsvParam(state.filters.states),
    category: serializeCsvParam(state.filters.threatCategories),
    stage: serializeCsvParam(state.filters.stageIds),
    org: serializeCsvParam(state.filters.organizationIds),
    app: serializeCsvParam(state.filters.applicationIds),
    threat: serializeThreatRange(state.filters.threatRange),
    waiver: serializeWaiverType(state.filters.waiverType),
  };
}

/**
 * Stable JSON snapshot of the raw list URL params (before parse/normalize), used to detect when the
 * address bar carried tokens that parse dropped/clamped so the container can rewrite it to canonical
 * form. Lives beside the parse/build codec so it shares the same {@link asString} field handling.
 */
export function rawViolationsListParamsSnapshot(params: Record<string, unknown>): string {
  return JSON.stringify({
    q: asString(params.q),
    page: asString(params.page),
    state: asString(params.state),
    category: asString(params.category),
    stage: asString(params.stage),
    org: asString(params.org),
    app: asString(params.app),
    threat: asString(params.threat),
    waiver: asString(params.waiver),
  });
}

/** Structural equality for two filter selections (set membership + threat range). */
export function violationsFiltersEqual(
  left: ViolationsFilterState,
  right: ViolationsFilterState,
): boolean {
  const setFields: (keyof ViolationsFilterState)[] = [
    'states',
    'threatCategories',
    'stageIds',
    'organizationIds',
    'applicationIds',
  ];
  const setsEqual = setFields.every((field) => {
    const leftIds = left[field] as ReadonlySet<string>;
    const rightIds = right[field] as ReadonlySet<string>;
    if (leftIds.size !== rightIds.size) return false;
    return Array.from(leftIds).every((id) => rightIds.has(id));
  });
  return (
    setsEqual &&
    left.threatRange[0] === right.threatRange[0] &&
    left.threatRange[1] === right.threatRange[1] &&
    left.waiverType === right.waiverType
  );
}
