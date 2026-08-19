/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type {
  VulnerabilitiesFilterState,
  VulnerabilitiesListOrderBy,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import {
  buildVulnerabilitiesListRequest,
  createDefaultVulnerabilitiesFilterState,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';
import { DEFAULT_VULNERABILITIES_TAB } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';

/**
 * Build multipart {@code filter} JSON for POST /rest/dashboard/vulnerabilities/export.
 * Honors search + severity/CVSS/ecosystem filters and Martha orderBy tokens. Omits pagination.
 */
export function buildVulnerabilitiesExportPayload(params: {
  readonly search?: string;
  readonly orderBy: VulnerabilitiesListOrderBy;
  readonly filters?: VulnerabilitiesFilterState;
}): Record<string, unknown> {
  const request = buildVulnerabilitiesListRequest({
    tab: DEFAULT_VULNERABILITIES_TAB,
    page: 0,
    search: params.search,
    orderBy: params.orderBy,
    includeFacets: false,
    filters: params.filters ?? createDefaultVulnerabilitiesFilterState(),
  });
  const { page: _page, pageSize: _pageSize, includeFacets: _facets, ...exportFields } = request;
  return exportFields;
}
