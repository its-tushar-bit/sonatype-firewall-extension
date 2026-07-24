/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type {
  VulnerabilitiesFilterState,
  VulnerabilitiesListOrderBy,
  VulnerabilityCvssRange,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import {
  VULNERABILITY_CVSS_MAX,
  VULNERABILITY_CVSS_MIN,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import {
  createDefaultVulnerabilitiesFilterState,
  DEFAULT_VULNERABILITY_CVSS_RANGE,
  isDefaultCvssRange,
  SEVERITY_LABELS,
  VULNERABILITIES_DEFAULT_ORDER_BY,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';
import type { VulnerabilitiesTab } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';
import { DEFAULT_VULNERABILITIES_TAB } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';

export interface VulnerabilitiesListQueryState {
  readonly tab: VulnerabilitiesTab;
  readonly search: string;
  /** 0-based page index for the list API. */
  readonly page: number;
  readonly orderBy: VulnerabilitiesListOrderBy;
  readonly filters: VulnerabilitiesFilterState;
}

/**
 * URL-friendly sort slugs ↔ backend orderBy tokens.
 * {@code sort} must not collide with the {@code cvss} filter range param.
 */
const SORT_URL_TO_ORDER_BY: Readonly<Record<string, VulnerabilitiesListOrderBy>> = {
  highest: '-cvssScore',
  lowest: 'cvssScore',
};

const ORDER_BY_TO_SORT_URL: Readonly<Record<VulnerabilitiesListOrderBy, string>> = {
  '-cvssScore': 'highest',
  cvssScore: 'lowest',
};

export function vulnerabilitiesListOrderByLabel(orderBy: VulnerabilitiesListOrderBy): string {
  return orderBy === 'cvssScore' ? 'Lowest CVSS' : 'Highest CVSS';
}

const SUPPORTED_SEVERITIES = new Set<string>(Object.keys(SEVERITY_LABELS));

export const MAX_DEEP_LINK_PAGE = 10_000;

function asString(value: unknown): string | undefined {
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

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
      }
      catch {
        return trimmed;
      }
    })
    .filter(Boolean);
}

/** Percent-encode each token so commas inside ecosystem/org names survive URL round-trips. */
function serializeCsvParam(values: ReadonlySet<string>): string | undefined {
  if (values.size === 0) return undefined;
  return Array.from(values)
    .sort()
    .map((value) => encodeURIComponent(value))
    .join(',');
}

function parseFilteredSet(value: unknown, allowed: ReadonlySet<string>): ReadonlySet<string> {
  return new Set(
    parseCsvParam(value)
      .map((id) => id.toLowerCase())
      .filter((id) => allowed.has(id)),
  );
}

function parseDecimalToken(token: string): number | undefined {
  if (!/^\d+(\.\d+)?$/.test(token)) return undefined;
  const value = Number(token);
  return Number.isFinite(value) ? value : undefined;
}

/**
 * Parse a {@code "min-max"} CVSS range param (decimals allowed), clamped to [0, 10] and ascending.
 * Falls back to the full-domain default when missing or malformed.
 */
function parseCvssRange(value: unknown): VulnerabilityCvssRange {
  const raw = asString(value);
  if (!raw) return DEFAULT_VULNERABILITY_CVSS_RANGE;
  // Limit 2: take only the first two hyphen-separated tokens; trailing junk is discarded
  // here (URL normalization rewrites the address bar to the canonical min-max form).
  const parts = raw.split('-', 2);
  if (parts.length !== 2) return DEFAULT_VULNERABILITY_CVSS_RANGE;
  const min = parseDecimalToken(parts[0].trim());
  const max = parseDecimalToken(parts[1].trim());
  if (min === undefined || max === undefined) return DEFAULT_VULNERABILITY_CVSS_RANGE;
  const clamp = (n: number): number =>
    Math.min(VULNERABILITY_CVSS_MAX, Math.max(VULNERABILITY_CVSS_MIN, n));
  const lo = clamp(min);
  const hi = clamp(max);
  return [Math.min(lo, hi), Math.max(lo, hi)];
}

function serializeCvssRange(range: VulnerabilityCvssRange): string | undefined {
  if (isDefaultCvssRange(range)) return undefined;
  const fmt = (n: number): string => (Number.isInteger(n) ? String(n) : n.toFixed(1));
  return `${fmt(range[0])}-${fmt(range[1])}`;
}

function parsePageIndex(value: unknown): number {
  const pageParam = typeof value === 'string' ? Number.parseInt(value, 10) : 1;
  if (!Number.isFinite(pageParam) || pageParam <= 1) {
    return 0;
  }
  return Math.min(pageParam, MAX_DEEP_LINK_PAGE) - 1;
}

function parseTab(value: unknown): VulnerabilitiesTab {
  return value === 'catalog' ? 'catalog' : DEFAULT_VULNERABILITIES_TAB;
}

function parseOrderBy(value: unknown): VulnerabilitiesListOrderBy {
  const raw = asString(value)?.toLowerCase();
  if (!raw) return VULNERABILITIES_DEFAULT_ORDER_BY;
  return SORT_URL_TO_ORDER_BY[raw] ?? VULNERABILITIES_DEFAULT_ORDER_BY;
}

function serializeOrderBy(orderBy: VulnerabilitiesListOrderBy): string | undefined {
  if (orderBy === VULNERABILITIES_DEFAULT_ORDER_BY) return undefined;
  return ORDER_BY_TO_SORT_URL[orderBy];
}

/** Parse UI-Router params for the Martha Vulnerabilities list page. */
export function parseVulnerabilitiesListParams(
  params: Record<string, unknown>,
): VulnerabilitiesListQueryState {
  return {
    tab: parseTab(params.tab),
    search: typeof params.q === 'string' ? params.q.trim() : '',
    page: parsePageIndex(params.page),
    orderBy: parseOrderBy(params.sort),
    filters: {
      ...createDefaultVulnerabilitiesFilterState(),
      severities: parseFilteredSet(params.severity, SUPPORTED_SEVERITIES),
      ecosystems: new Set(parseCsvParam(params.ecosystem).map((id) => id.toLowerCase())),
      cvssRange: parseCvssRange(params.cvss),
    },
  };
}

/**
 * Serialize list state to hash-query params. Defaults/empty values map to {@code undefined} so
 * UI-Router omits them from the URL.
 */
export function buildVulnerabilitiesListRouteParams(
  state: VulnerabilitiesListQueryState,
): Record<string, string | undefined> {
  return {
    tab: state.tab === DEFAULT_VULNERABILITIES_TAB ? undefined : state.tab,
    q: state.search.trim() || undefined,
    page: state.page > 0 ? String(state.page + 1) : undefined,
    sort: serializeOrderBy(state.orderBy),
    severity: serializeCsvParam(state.filters.severities),
    cvss: serializeCvssRange(state.filters.cvssRange),
    ecosystem: serializeCsvParam(state.filters.ecosystems),
  };
}

/**
 * Stable JSON snapshot of the raw list URL params (before parse/normalize), used to detect when the
 * address bar carried tokens that parse dropped/clamped so the container can rewrite it to canonical
 * form. Lives beside the parse/build codec so it shares the same {@link asString} field handling.
 */
export function rawVulnerabilitiesListParamsSnapshot(
  params: Record<string, unknown>,
): string {
  return JSON.stringify({
    tab: asString(params.tab),
    q: asString(params.q),
    page: asString(params.page),
    sort: asString(params.sort),
    severity: asString(params.severity),
    cvss: asString(params.cvss),
    ecosystem: asString(params.ecosystem),
  });
}

function setsEqual(left: ReadonlySet<string>, right: ReadonlySet<string>): boolean {
  if (left.size !== right.size) return false;
  for (const value of left) {
    if (!right.has(value)) return false;
  }
  return true;
}

export function vulnerabilitiesFiltersEqual(
  left: VulnerabilitiesFilterState,
  right: VulnerabilitiesFilterState,
): boolean {
  return (
    setsEqual(left.severities, right.severities) &&
    setsEqual(left.ecosystems, right.ecosystems) &&
    left.cvssRange[0] === right.cvssRange[0] &&
    left.cvssRange[1] === right.cvssRange[1]
  );
}
