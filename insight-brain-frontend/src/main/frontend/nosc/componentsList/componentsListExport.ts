/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ComponentsListFilterState } from 'MainRoot/nosc/componentsList/componentsListFilters';
import { isDefaultComponentsThreatRange } from 'MainRoot/nosc/componentsList/componentsListFilters';

/**
 * Build the Classic dashboard export filter payload for Martha Components CSV (My Scan Data).
 *
 * Uses POST /rest/dashboard/export/componentRisks. The catalog list UI has no Classic sort
 * control after the Ana catalog pivot, so export uses Classic's default
 * {@code APPLICATION_COUNT}.
 *
 * Application, stage, and threat-level selections map into Classic {@code RisksFilterDTO}
 * (CLM-43211 / CLM-43960). Organization and ecosystem selections are friendly names on this
 * rail and stay omitted until a name→id bridge exists; free-text search is index-only and has
 * no Classic equivalent.
 */
export function buildComponentsListExportPayload(
  filters: ComponentsListFilterState,
): Record<string, unknown> {
  const payload: Record<string, unknown> = {
    orderBy: 'APPLICATION_COUNT',
  };
  if (filters.applications.size > 0) {
    payload.applicationIds = Array.from(filters.applications).sort();
  }
  if (filters.stages.size > 0) {
    payload.stageIds = Array.from(filters.stages).sort();
  }
  // Classic export accepts a single policyThreatLevelRange (Applications export parity).
  if (!isDefaultComponentsThreatRange(filters.threatRange)) {
    payload.policyThreatLevelRange = {
      minPolicyThreatLevel: filters.threatRange[0],
      maxPolicyThreatLevel: filters.threatRange[1],
    };
  }
  return payload;
}
