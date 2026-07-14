/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ViolationRow,
  ViolationsListRequest,
} from 'MainRoot/nosc/violations/violationListTypes';

/** Default page size for the Violations card list. */
export const VIOLATIONS_PAGE_SIZE = 25;

/** Default sort — highest threat first (backend default; see ViolationsListRequestDTO). */
export const VIOLATIONS_DEFAULT_ORDER_BY = '-policyThreatLevel';

/**
 * Build the POST body for a Violations list request. Only validator-safe fields are sent;
 * {@code page} is 0-based to match the backend contract.
 */
export function buildViolationsListRequest(params: {
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly includeFacets?: boolean;
}): ViolationsListRequest {
  const search = params.search?.trim();
  return {
    page: params.page,
    pageSize: params.pageSize ?? VIOLATIONS_PAGE_SIZE,
    includeFacets: params.includeFacets ?? true,
    orderBy: VIOLATIONS_DEFAULT_ORDER_BY,
    ...(search ? { search } : {}),
  };
}

/** Friendly labels for the enum-keyed violation-state facet (OPEN / WAIVED). */
const STATE_LABELS: Readonly<Record<string, string>> = {
  OPEN: 'Open',
  WAIVED: 'Waived',
};

/** Friendly labels for the policy threat-category facet. */
const THREAT_CATEGORY_LABELS: Readonly<Record<string, string>> = {
  security: 'Security',
  license: 'License',
  quality: 'Quality',
  other: 'Other',
};

/**
 * Friendly labels for the licensed pipeline-stage facet, keyed by stage id. The API keys the
 * {@code stages} facet map by stage id (e.g. {@code build}, {@code stage-release}), while a row's
 * {@code stage} is the resolved display name — so an id→name map cannot be derived from the page rows
 * (unlike org/app rows, which carry both id and name). This authoritative map covers the IQ lifecycle
 * stages; any unknown/future id falls back to a Title-Cased id so the sidebar never renders a raw slug.
 */
const STAGE_LABELS: Readonly<Record<string, string>> = {
  proxy: 'Proxy',
  develop: 'Develop',
  source: 'Source',
  build: 'Build',
  'stage-release': 'Stage Release',
  release: 'Release',
  operate: 'Operate',
};

export function violationStateLabel(id: string): string {
  return STATE_LABELS[id] ?? id;
}

export function threatCategoryLabel(id: string): string {
  return THREAT_CATEGORY_LABELS[id] ?? id;
}

/** Title-case a hyphenated stage id (e.g. {@code stage-release} → {@code Stage Release}). */
function titleCaseStageId(id: string): string {
  return id
    .split('-')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

/**
 * Map a stage-facet id to a display name. Uses the authoritative {@link STAGE_LABELS} map and falls
 * back to a Title-Cased id (never the raw slug) for any id not yet in the map.
 */
export function stageLabel(id: string): string {
  return STAGE_LABELS[id] ?? titleCaseStageId(id);
}

/**
 * Derive id→display-name maps for the org / application facets from the current page of rows. Facet
 * maps are keyed by internal id only; org/app rows carry both the id and the human-readable name, so
 * we use them to label the sidebar for entities visible on the page. Ids with no matching row fall
 * back to the raw id (full name resolution lands with the filter sidebar work, CLM-42258). Stage
 * facets are labeled by {@link stageLabel} instead — a row's {@code stage} is a display name, not the
 * id the facet is keyed by, so it cannot seed an id→name map.
 */
export function deriveViolationFacetLabels(rows: ReadonlyArray<ViolationRow>): {
  readonly organizations: Readonly<Record<string, string>>;
  readonly applications: Readonly<Record<string, string>>;
} {
  const organizations: Record<string, string> = {};
  const applications: Record<string, string> = {};
  rows.forEach((row) => {
    if (row.organizationId && row.organizationName) organizations[row.organizationId] = row.organizationName;
    if (row.applicationId && row.applicationName) applications[row.applicationId] = row.applicationName;
  });
  return { organizations, applications };
}
