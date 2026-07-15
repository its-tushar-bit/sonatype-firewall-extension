/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ApplicationsListFilterState } from 'MainRoot/nosc/applications/applicationsListFilters';
import { applicationsListFiltersToRequest } from 'MainRoot/nosc/applications/applicationsListFilters';
import type { ApplicationsListOrderBy } from 'MainRoot/nosc/applications/applicationsListQuery';

function toClassicExportThreatRange(
  ranges: NonNullable<
    ReturnType<typeof applicationsListFiltersToRequest>['policyThreatLevelRanges']
  >,
): { minPolicyThreatLevel: number; maxPolicyThreatLevel: number } | undefined {
  if (ranges.length === 0) {
    return undefined;
  }
  if (ranges.length === 1) {
    return ranges[0];
  }
  // Classic export accepts a single policyThreatLevelRange; collapse OR-selected buckets to an envelope.
  let min = ranges[0].minPolicyThreatLevel;
  let max = ranges[0].maxPolicyThreatLevel;
  for (let i = 1; i < ranges.length; i += 1) {
    min = Math.min(min, ranges[i].minPolicyThreatLevel);
    max = Math.max(max, ranges[i].maxPolicyThreatLevel);
  }
  return { minPolicyThreatLevel: min, maxPolicyThreatLevel: max };
}

/**
 * Build the Classic dashboard export filter payload for Martha Applications CSV.
 *
 * Uses POST /rest/dashboard/export/applicationRisks (multipart {@code filter} JSON). Sidebar
 * filters map into Classic export fields; free-text {@code search} is index-only and is not
 * supported on the Classic export path. Sort mirrors the toolbar selection via
 * {@link ApplicationRiskScoreDTOComparator} tokens ({@code lastEvaluationTime} /
 * {@code -lastEvaluationTime}). Pagination fields are omitted — the export resource fetches all
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
    orderBy,
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
  if (requestFilters.policyThreatLevelRanges?.length) {
    const threatRange = toClassicExportThreatRange(requestFilters.policyThreatLevelRanges);
    if (threatRange) {
      payload.policyThreatLevelRange = threatRange;
    }
  }

  return payload;
}
