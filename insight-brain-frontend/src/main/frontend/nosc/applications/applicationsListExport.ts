/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ApplicationsListFilterState } from 'MainRoot/nosc/applications/applicationsListFilters';
import { applicationsListFiltersToRequest } from 'MainRoot/nosc/applications/applicationsListFilters';
import type { ApplicationsListOrderBy } from 'MainRoot/nosc/applications/applicationsListQuery';

/**
 * Classic PostgreSQL application-risk export rejects Martha's sort tokens
 * ({@code PostgresApplicationRiskService}). Map to Classic {@code TOTAL_RISK} while preserving
 * direction. Callers should surface this remapping in the export UI (see ApplicationsToolbar).
 */
export function toClassicExportOrderBy(orderBy: ApplicationsListOrderBy): string {
  if (orderBy === 'maxPolicyThreatLevel' || orderBy === 'lastEvaluationTime') {
    return 'TOTAL_RISK';
  }
  if (orderBy === '-maxPolicyThreatLevel' || orderBy === '-lastEvaluationTime') {
    return '-TOTAL_RISK';
  }
  return orderBy;
}

/**
 * Build the Classic dashboard export filter payload for Martha Applications CSV.
 *
 * Uses POST /rest/dashboard/export/applicationRisks (multipart {@code filter} JSON). Sidebar
 * filters map into Classic export fields; free-text {@code search} is index-only and is not
 * supported on the Classic export path. Martha toolbar sort tokens are mapped to Classic
 * {@code TOTAL_RISK} because PostgreSQL-backed export does not support the list sort fields.
 * Pagination fields are omitted — the export resource fetches all
 * matching rows via {@code getApplicationRisks(..., 0, Integer.MAX_VALUE)}.
 *
 * Parity caveat: Martha list stage, policy type, and violation state filters are violation-scoped
 * (apps with at least one violation matching all of them at once). Classic export passes the same
 * fields to {@code applicationRiskService.getApplicationRisks}, which applies them through the
 * SQL dashboard path and may return a different application set for the same sidebar selection.
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
  if (requestFilters.policyThreatCategories) {
    payload.policyThreatCategories = requestFilters.policyThreatCategories;
  }
  if (requestFilters.policyViolationStates?.length) {
    payload.policyViolationStates = requestFilters.policyViolationStates;
  }
  // Classic export accepts a single policyThreatLevelRange; Martha list may emit one range.
  if (requestFilters.policyThreatLevelRanges?.length) {
    payload.policyThreatLevelRange = requestFilters.policyThreatLevelRanges[0];
  }

  return payload;
}
