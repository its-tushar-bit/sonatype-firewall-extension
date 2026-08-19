/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type {
  ViolationRow,
  ViolationsFilterState,
  ViolationsListFacets,
} from 'MainRoot/nosc/violations/violationListTypes';
import {
  isDefaultThreatRange,
  waiverTypeToRequestFlag,
  WAIVER_TYPE_AUTO,
  WAIVER_TYPE_MANUAL,
} from 'MainRoot/nosc/violations/violationsListApi';

/**
 * Client-side violation filtering + faceting for the search results Violations tab.
 * The Violations *list page* filters server-side (POST /rest/dashboard/violations/list); the search
 * results tab has no such endpoint, so — like the vulnerabilities cves tab — it narrows the rows
 * in the browser using the same ViolationsFilterState the shared rail produces. CLM-42562.
 */

/** Stage facet id for a row: the display stage lowercased/hyphenated to match STAGE_LABELS keys. */
function stageIdOf(row: ViolationRow): string {
  return (row.stage ?? '').trim().toLowerCase().replace(/\s+/g, '-');
}

/** Apply every violation filter group to a set of rows. */
export function filterViolations(
  rows: ReadonlyArray<ViolationRow>,
  filters: ViolationsFilterState,
): ReadonlyArray<ViolationRow> {
  let out = rows;
  if (filters.states.size > 0) {
    out = out.filter((r) => r.state !== undefined && filters.states.has(r.state));
  }
  if (filters.threatCategories.size > 0) {
    out = out.filter((r) => r.threatCategory !== undefined && filters.threatCategories.has(r.threatCategory));
  }
  if (!isDefaultThreatRange(filters.threatRange)) {
    const [min, max] = filters.threatRange;
    out = out.filter((r) => r.threatLevel !== undefined && r.threatLevel >= min && r.threatLevel <= max);
  }
  if (filters.stageIds.size > 0) {
    out = out.filter((r) => filters.stageIds.has(stageIdOf(r)));
  }
  if (filters.organizationIds.size > 0) {
    out = out.filter((r) => r.organizationId !== undefined && filters.organizationIds.has(r.organizationId));
  }
  if (filters.applicationIds.size > 0) {
    out = out.filter((r) => r.applicationId !== undefined && filters.applicationIds.has(r.applicationId));
  }
  const waiverFlag = waiverTypeToRequestFlag(filters.waiverType);
  if (waiverFlag !== undefined) {
    out = out.filter((r) => r.state === 'WAIVED' && r.waivedWithAutoWaiver === waiverFlag);
  }
  return out;
}

/** Increment a key's tally in a counts map. */
function bump(counts: Record<string, number>, key: string | undefined): void {
  if (key === undefined || key === '') return;
  counts[key] = (counts[key] ?? 0) + 1;
}

/**
 * Build the sidebar facet counts from a set of violation rows (keyed to match ViolationsFilterState:
 * states OPEN/WAIVED, categories, stage ids, owner ids, waiver types AUTO/MANUAL). Counts reflect the
 * full tab result set (not narrowed by the current selection) so the facet totals stay stable.
 */
export function computeViolationFacets(rows: ReadonlyArray<ViolationRow>): ViolationsListFacets {
  const states: Record<string, number> = {};
  const threatCategories: Record<string, number> = {};
  const stages: Record<string, number> = {};
  const organizations: Record<string, number> = {};
  const applications: Record<string, number> = {};
  const waiverTypes: Record<string, number> = {};

  for (const r of rows) {
    bump(states, r.state);
    bump(threatCategories, r.threatCategory);
    bump(stages, stageIdOf(r));
    bump(organizations, r.organizationId);
    bump(applications, r.applicationId);
    if (r.state === 'WAIVED') {
      bump(waiverTypes, r.waivedWithAutoWaiver ? WAIVER_TYPE_AUTO : WAIVER_TYPE_MANUAL);
    }
  }

  return {
    totalViolations: rows.length,
    states,
    threatCategories,
    stages,
    organizations,
    applications,
    waiverTypes,
  };
}
