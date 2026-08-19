/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type {
  VulnerabilitiesFilterState,
  VulnerabilitiesListFacets,
  VulnerabilitiesListOrderBy,
  VulnerabilitiesListResponse,
  VulnerabilityCvssRange,
  VulnerabilityEpssRange,
  VulnerabilityRow,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import {
  VULNERABILITY_CVSS_MAX,
  VULNERABILITY_CVSS_MIN,
  VULNERABILITY_EPSS_MAX,
  VULNERABILITY_EPSS_MIN,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import type { VulnerabilitiesTab } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';
import { DEFAULT_VULNERABILITIES_TAB } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';

/** Default page size — denser vuln cards (locked in DECISIONS). */
export const VULNERABILITIES_PAGE_SIZE = 25;

/** Default sort — highest CVSS first (backend default). */
export const VULNERABILITIES_DEFAULT_ORDER_BY: VulnerabilitiesListOrderBy = '-cvssScore';

export const DEFAULT_VULNERABILITY_CVSS_RANGE: VulnerabilityCvssRange = [
  VULNERABILITY_CVSS_MIN,
  VULNERABILITY_CVSS_MAX,
];

export const DEFAULT_VULNERABILITY_EPSS_RANGE: VulnerabilityEpssRange = [
  VULNERABILITY_EPSS_MIN,
  VULNERABILITY_EPSS_MAX,
];

export const SEVERITY_LABELS: Readonly<Record<string, string>> = {
  critical: 'Critical',
  high: 'High',
  medium: 'Medium',
  low: 'Low',
  none: 'None',
  unknown: 'Unknown',
};

/** Display labels for common ecosystem ids (URL/API stay lowercase). */
const ECOSYSTEM_LABELS: Readonly<Record<string, string>> = {
  maven: 'Maven',
  npm: 'npm',
  pypi: 'PyPI',
  nuget: 'NuGet',
  golang: 'Go',
  cargo: 'Cargo',
  gem: 'RubyGems',
  composer: 'Composer',
  conan: 'Conan',
  cocoapods: 'CocoaPods',
  huggingface: 'Hugging Face',
  docker: 'Docker',
};

export function createDefaultVulnerabilitiesFilterState(): VulnerabilitiesFilterState {
  return {
    severities: new Set(),
    ecosystems: new Set(),
    organizations: new Set(),
    applications: new Set(),
    stages: new Set(),
    cvssRange: DEFAULT_VULNERABILITY_CVSS_RANGE,
    knownExploited: false,
    malware: false,
    epssRange: DEFAULT_VULNERABILITY_EPSS_RANGE,
    publishedWindow: '',
    cwes: new Set(),
  };
}

export function isDefaultCvssRange(range: VulnerabilityCvssRange): boolean {
  return range[0] === VULNERABILITY_CVSS_MIN && range[1] === VULNERABILITY_CVSS_MAX;
}

export function isDefaultEpssRange(range: VulnerabilityEpssRange): boolean {
  return range[0] === VULNERABILITY_EPSS_MIN && range[1] === VULNERABILITY_EPSS_MAX;
}

/**
 * Whether Reset / "active filters" chrome should show. Tab-private filters only count on the
 * tab that can apply them — Catalog richness on {@code catalog}, estate scope on
 * {@code myScanData} — so a stale in-memory flag cannot keep Reset enabled after a tab switch
 * or a cross-tab deep link (parse/build already strip those tokens from the URL).
 */
export function hasActiveVulnerabilityFilters(
  filters: VulnerabilitiesFilterState,
  tab: VulnerabilitiesTab = DEFAULT_VULNERABILITIES_TAB,
): boolean {
  const estateActive =
    tab === 'myScanData' &&
    (filters.organizations.size > 0 ||
      filters.applications.size > 0 ||
      filters.stages.size > 0);
  const catalogActive =
    tab === 'catalog' &&
    (filters.knownExploited ||
      filters.malware ||
      !isDefaultEpssRange(filters.epssRange) ||
      Boolean(filters.publishedWindow) ||
      filters.cwes.size > 0);
  return (
    filters.severities.size > 0 ||
    filters.ecosystems.size > 0 ||
    !isDefaultCvssRange(filters.cvssRange) ||
    estateActive ||
    catalogActive
  );
}

export function severityLabel(id: string): string {
  return SEVERITY_LABELS[id.toLowerCase()] ?? id;
}

export function ecosystemLabel(id: string): string {
  return ECOSYSTEM_LABELS[id.toLowerCase()] ?? id;
}

/** Display formatting for CVSS scores on cards and filter value labels. */
export function formatCvssScore(score: number): string {
  return score.toFixed(1);
}

/** Display formatting for EPSS scores on cards and filter value labels. */
export function formatEpssScore(score: number): string {
  return score.toFixed(2);
}

/**
 * Formats an ISO published timestamp for card chrome. Unparseable values render as an em dash
 * so malformed HDS dates never surface raw on the card.
 */
export function formatPublishedAt(iso: string): string {
  const ms = Date.parse(iso);
  if (!Number.isFinite(ms)) return '—';
  return new Date(ms).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

/**
 * Card CWE chrome: show up to {@code limit} ids, then a {@code +N} remainder so a long CWE list
 * is not visually identical to one that ends exactly at the limit.
 */
export function formatCweList(cwes: ReadonlyArray<string>, limit = 3): string {
  if (cwes.length === 0) return '';
  const shown = cwes.slice(0, limit);
  const remaining = cwes.length - shown.length;
  return remaining > 0 ? `${shown.join(', ')} +${remaining}` : shown.join(', ');
}

/**
 * Scope facet ids are internal ids, so a missing name would render as an opaque hash. Falling
 * back to the id keeps the option selectable rather than hiding it.
 */
export function scopeLabel(
  names: Readonly<Record<string, string>> | undefined,
  id: string,
): string {
  const name = names?.[id];
  return name && name.trim() ? name : id;
}

export type VulnerabilitiesListRequest = {
  readonly tab?: VulnerabilitiesTab;
  readonly page?: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly orderBy?: VulnerabilitiesListOrderBy;
  readonly includeFacets?: boolean;
  readonly severities?: ReadonlyArray<string>;
  readonly minCvssScore?: number;
  readonly maxCvssScore?: number;
  readonly ecosystems?: ReadonlyArray<string>;
  readonly organizationIds?: ReadonlyArray<string>;
  readonly applicationIds?: ReadonlyArray<string>;
  readonly stageIds?: ReadonlyArray<string>;
  readonly knownExploited?: boolean;
  readonly malware?: boolean;
  readonly minEpssScore?: number;
  readonly maxEpssScore?: number;
  readonly cwes?: ReadonlyArray<string>;
  readonly publishedWindow?: string;
};

export function buildVulnerabilitiesListRequest(params: {
  readonly tab?: VulnerabilitiesTab;
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly orderBy?: VulnerabilitiesListOrderBy;
  readonly includeFacets?: boolean;
  readonly filters?: VulnerabilitiesFilterState;
}): VulnerabilitiesListRequest {
  const search = params.search?.trim();
  const filters = params.filters ?? createDefaultVulnerabilitiesFilterState();
  const tab = params.tab ?? DEFAULT_VULNERABILITIES_TAB;
  // Estate scope is My Scan Data only — Catalog/HDS has no org/app/stage dimension.
  const includeEstateScope = tab === 'myScanData';
  const includeCatalogRichness = tab === 'catalog';
  return {
    tab,
    page: params.page,
    pageSize: params.pageSize ?? VULNERABILITIES_PAGE_SIZE,
    includeFacets: params.includeFacets ?? true,
    orderBy: params.orderBy ?? VULNERABILITIES_DEFAULT_ORDER_BY,
    ...(search ? { search } : {}),
    ...(filters.severities.size > 0
      ? { severities: Array.from(filters.severities).sort() }
      : {}),
    ...(!isDefaultCvssRange(filters.cvssRange)
      ? {
          minCvssScore: filters.cvssRange[0],
          maxCvssScore: filters.cvssRange[1],
        }
      : {}),
    ...(filters.ecosystems.size > 0
      ? { ecosystems: Array.from(filters.ecosystems).sort() }
      : {}),
    ...(includeEstateScope && filters.organizations.size > 0
      ? { organizationIds: Array.from(filters.organizations).sort() }
      : {}),
    ...(includeEstateScope && filters.applications.size > 0
      ? { applicationIds: Array.from(filters.applications).sort() }
      : {}),
    ...(includeEstateScope && filters.stages.size > 0
      ? { stageIds: Array.from(filters.stages).sort() }
      : {}),
    ...(includeCatalogRichness && filters.knownExploited ? { knownExploited: true } : {}),
    ...(includeCatalogRichness && filters.malware ? { malware: true } : {}),
    ...(includeCatalogRichness && !isDefaultEpssRange(filters.epssRange)
      ? {
          minEpssScore: filters.epssRange[0],
          maxEpssScore: filters.epssRange[1],
        }
      : {}),
    ...(includeCatalogRichness && filters.cwes.size > 0
      ? { cwes: Array.from(filters.cwes).sort() }
      : {}),
    ...(includeCatalogRichness && filters.publishedWindow
      ? { publishedWindow: filters.publishedWindow }
      : {}),
  };
}

export function mapVulnerabilitiesListResponse(
  response: VulnerabilitiesListResponse | null | undefined,
): {
  readonly vulnerabilities: ReadonlyArray<VulnerabilityRow>;
  readonly facets: VulnerabilitiesListFacets | null;
  readonly total: number;
} {
  return {
    vulnerabilities: response?.vulnerabilities ?? [],
    facets: response?.facets ?? null,
    total: response?.total ?? 0,
  };
}
