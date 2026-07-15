/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';
import {
  isDefaultThreatRange,
  toSortedArray,
  VIOLATIONS_DEFAULT_ORDER_BY,
} from 'MainRoot/nosc/violations/violationsListApi';

/**
 * Build the Classic dashboard export filter payload for the Martha Violations CSV.
 *
 * Posts to POST /rest/dashboard/export/newestRisks (multipart {@code filter} JSON), which streams the
 * full filtered result set and emits the canonical 9-column violations CSV
 * ({@code DashboardViolationRiskDTO.getCsvHeader()}) — including Date First Seen, Timestamp First Seen,
 * and Reference (CVE) that the Martha index list API does not carry. The payload deserializes into the
 * Classic {@code RisksFilterDTO}: sidebar filter selections map directly (states as an array of enum
 * names, categories as a comma-delimited string, threat range as a {@code minPolicyThreatLevel /
 * maxPolicyThreatLevel} object, id sets as arrays). Free-text {@code search} is index-only and is not
 * supported on the Classic export path, so it is intentionally omitted. Pagination is omitted — the
 * export resource fetches every matching row.
 */
export function buildViolationsListExportPayload(
  filters: ViolationsFilterState,
): Record<string, unknown> {
  const payload: Record<string, unknown> = { orderBy: VIOLATIONS_DEFAULT_ORDER_BY };

  const states = toSortedArray(filters.states);
  const categories = toSortedArray(filters.threatCategories);
  const stageIds = toSortedArray(filters.stageIds);
  const organizationIds = toSortedArray(filters.organizationIds);
  const applicationIds = toSortedArray(filters.applicationIds);

  if (states) payload.policyViolationStates = states;
  if (categories) payload.policyThreatCategories = categories.join(',');
  if (stageIds) payload.stageIds = stageIds;
  if (organizationIds) payload.organizationIds = organizationIds;
  if (applicationIds) payload.applicationIds = applicationIds;
  if (!isDefaultThreatRange(filters.threatRange)) {
    payload.policyThreatLevelRange = {
      minPolicyThreatLevel: filters.threatRange[0],
      maxPolicyThreatLevel: filters.threatRange[1],
    };
  }

  return payload;
}
