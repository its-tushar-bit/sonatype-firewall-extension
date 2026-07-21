/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ApplicationsListFilterState } from 'MainRoot/nosc/applications/applicationsListFilters';
import { applicationsListFiltersToRequest } from 'MainRoot/nosc/applications/applicationsListFilters';
import type { ApplicationsListOrderBy } from 'MainRoot/nosc/applications/applicationsListQuery';

/**
 * Classic PostgreSQL application-risk export rejects Martha's {@code lastEvaluationTime} tokens
 * ({@code PostgresApplicationRiskService}). Map to Classic {@code TOTAL_RISK} while preserving
 * direction. Callers should surface this remapping in the export UI (see ApplicationsToolbar).
 */
export function toClassicExportOrderBy(orderBy: ApplicationsListOrderBy): string {
  if (orderBy === 'lastEvaluationTime') {
    return 'TOTAL_RISK';
  }
  if (orderBy === '-lastEvaluationTime') {
    return '-TOTAL_RISK';
  }
  return orderBy;
}

/**
 * Build the Classic dashboard export filter payload for Martha Applications CSV.
 *
 * Uses POST /rest/dashboard/export/applicationRisks (multipart {@code filter} JSON). Sidebar
 * filters map into Classic export fields; free-text {@code search} is index-only and is not
 * supported on the Classic export path. Martha toolbar sort ({@code lastEvaluationTime}) is
 * mapped to Classic {@code TOTAL_RISK} because PostgreSQL-backed export does not support
 * evaluation-time ordering. Pagination fields are omitted — the export resource fetches all
 * matching rows via {@code getApplicationRisks(..., 0, Integer.MAX_VALUE)}.
 *
 * Stage parity caveat: Martha list stage filters are violation-scoped (apps with at least one
 * matching violation in the selected stage). Classic export applies {@code stageIds} through
 * {@code applicationRiskService.getApplicationRisks}, which uses Classic stage matching and may
 * return a different application set for the same sidebar selection.
 */
export function buildApplicationsListExportPayload(
  filters: ApplicationsListFilterState,
  orderBy: ApplicationsListOrderBy,
): Record<string, unknown> {
  const requestFilters = applicationsListFiltersToRequest(filters);
  const payload: Record<string, unknown> = {
    orderBy: toClassicExportOrderBy(orderBy),
  };

  if (requestFilters.organizationIds?.length) {
    payload.organizationIds = requestFilters.organizationIds;
  }
  if (requestFilters.applicationIds?.length) {
    payload.applicationIds = requestFilters.applicationIds;
  }
  if (requestFilters.stageIds?.length) {
    payload.stageIds = requestFilters.stageIds;
  }
  // Classic export accepts a single policyThreatLevelRange; Martha list may emit one range.
  if (requestFilters.policyThreatLevelRanges?.length) {
    payload.policyThreatLevelRange = requestFilters.policyThreatLevelRanges[0];
  }

  return payload;
}
