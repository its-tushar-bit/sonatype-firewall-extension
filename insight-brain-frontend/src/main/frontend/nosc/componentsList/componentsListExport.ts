/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ComponentsListFilterState } from 'MainRoot/nosc/componentsList/componentsListFilters';

/**
 * Build the Classic dashboard export filter payload for Martha Components CSV (My Scan Data).
 *
 * Uses POST /rest/dashboard/export/componentRisks. The catalog list UI has no Classic sort
 * control after the Ana catalog pivot, so export uses Classic's default
 * {@code APPLICATION_COUNT}. Organization/ecosystem/search filters are catalog/index-only and
 * are omitted until a name→id bridge exists for Classic {@code RisksFilterDTO}.
 */
export function buildComponentsListExportPayload(
  filters: ComponentsListFilterState,
): Record<string, unknown> {
  void filters;
  return {
    orderBy: 'APPLICATION_COUNT',
  };
}
