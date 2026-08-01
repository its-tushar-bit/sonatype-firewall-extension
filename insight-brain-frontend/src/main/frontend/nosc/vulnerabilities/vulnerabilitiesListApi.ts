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
  VulnerabilityRow,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import {
  VULNERABILITY_CVSS_MAX,
  VULNERABILITY_CVSS_MIN,
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
  };
}

export function isDefaultCvssRange(range: VulnerabilityCvssRange): boolean {
  return range[0] === VULNERABILITY_CVSS_MIN && range[1] === VULNERABILITY_CVSS_MAX;
}

export function hasActiveVulnerabilityFilters(filters: VulnerabilitiesFilterState): boolean {
  return (
    filters.severities.size > 0 ||
    filters.ecosystems.size > 0 ||
    filters.organizations.size > 0 ||
    filters.applications.size > 0 ||
    filters.stages.size > 0 ||
    !isDefaultCvssRange(filters.cvssRange)
  );
}

export function severityLabel(id: string): string {
  return SEVERITY_LABELS[id.toLowerCase()] ?? id;
}

export function ecosystemLabel(id: string): string {
  return ECOSYSTEM_LABELS[id.toLowerCase()] ?? id;
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
