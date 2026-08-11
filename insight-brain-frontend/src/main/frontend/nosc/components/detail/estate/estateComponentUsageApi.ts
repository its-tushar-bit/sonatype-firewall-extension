/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getComponentUsageApplicationsUrl,
  getComponentUsageOrganizationsUrl,
  getComponentUsageReportsUrl,
} from 'MainRoot/util/CLMLocation';

/** Default page size for estate component where-used tables. */
export const COMPONENT_USAGE_PAGE_SIZE = 25;

export type ComponentUsageRequestFilters = {
  readonly nameSearch?: string;
  readonly includeIds?: ReadonlyArray<string>;
  readonly organizationId?: string;
};

export type ComponentUsageRequest = {
  readonly componentHash: string;
  readonly page: number;
  readonly pageSize: number;
} & ComponentUsageRequestFilters;

export type ComponentUsageReportsRequest = ComponentUsageRequest & {
  readonly applicationId: string;
};

export type ComponentUsageApplicationRow = {
  readonly applicationId?: string;
  readonly applicationPublicId?: string;
  readonly applicationName?: string;
  readonly organizationId?: string;
  readonly organizationName?: string;
  readonly stageTypeIds?: ReadonlyArray<string>;
  readonly lastSeenTime?: number;
};

export type ComponentUsageOrganizationRow = {
  readonly organizationId?: string;
  readonly organizationName?: string;
  readonly applicationCount?: number;
  readonly lastSeenTime?: number;
};

export type ComponentUsageReportRow = {
  readonly reportId?: string;
  readonly stageTypeId?: string;
  readonly evaluationTime?: number;
};

export type ComponentUsageApplicationsResponse = {
  readonly applications: ReadonlyArray<ComponentUsageApplicationRow>;
  readonly total: number;
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
};

export type ComponentUsageOrganizationsResponse = {
  readonly organizations: ReadonlyArray<ComponentUsageOrganizationRow>;
  readonly total: number;
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
};

export type ComponentUsageReportsResponse = {
  readonly reports: ReadonlyArray<ComponentUsageReportRow>;
  readonly total: number;
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
};

function normalizeFilters(filters?: ComponentUsageRequestFilters): ComponentUsageRequestFilters {
  if (!filters) {
    return {};
  }
  const nameSearch = filters.nameSearch?.trim();
  const includeIds = filters.includeIds?.map((id) => id.trim()).filter(Boolean);
  const organizationId = filters.organizationId?.trim();
  return {
    ...(nameSearch ? { nameSearch } : {}),
    ...(includeIds?.length ? { includeIds } : {}),
    ...(organizationId ? { organizationId } : {}),
  };
}

export function buildComponentUsageRequest(
  componentHash: string,
  page: number,
  pageSize: number = COMPONENT_USAGE_PAGE_SIZE,
  filters?: ComponentUsageRequestFilters
): ComponentUsageRequest {
  return {
    componentHash,
    page,
    pageSize,
    ...normalizeFilters(filters),
  };
}

export function buildComponentUsageReportsRequest(
  componentHash: string,
  applicationId: string,
  page: number,
  pageSize: number = COMPONENT_USAGE_PAGE_SIZE
): ComponentUsageReportsRequest {
  return {
    componentHash,
    applicationId,
    page,
    pageSize,
  };
}

export async function fetchComponentUsageApplications(
  componentHash: string,
  page: number,
  pageSize: number = COMPONENT_USAGE_PAGE_SIZE,
  signal?: AbortSignal,
  filters?: ComponentUsageRequestFilters
): Promise<ComponentUsageApplicationsResponse> {
  const { data } = await axios.post<ComponentUsageApplicationsResponse>(
    getComponentUsageApplicationsUrl(),
    buildComponentUsageRequest(componentHash, page, pageSize, filters),
    { signal }
  );
  return {
    applications: data.applications ?? [],
    total: data.total ?? 0,
    page: data.page ?? page,
    pageSize: data.pageSize ?? pageSize,
    hasNextPage: Boolean(data.hasNextPage),
  };
}

export async function fetchComponentUsageReports(
  componentHash: string,
  applicationId: string,
  page: number,
  pageSize: number = COMPONENT_USAGE_PAGE_SIZE,
  signal?: AbortSignal
): Promise<ComponentUsageReportsResponse> {
  const { data } = await axios.post<ComponentUsageReportsResponse>(
    getComponentUsageReportsUrl(),
    buildComponentUsageReportsRequest(componentHash, applicationId, page, pageSize),
    { signal }
  );
  return {
    reports: data.reports ?? [],
    total: data.total ?? 0,
    page: data.page ?? page,
    pageSize: data.pageSize ?? pageSize,
    hasNextPage: Boolean(data.hasNextPage),
  };
}

export async function fetchComponentUsageOrganizations(
  componentHash: string,
  page: number,
  pageSize: number = COMPONENT_USAGE_PAGE_SIZE,
  signal?: AbortSignal,
  filters?: ComponentUsageRequestFilters
): Promise<ComponentUsageOrganizationsResponse> {
  const { data } = await axios.post<ComponentUsageOrganizationsResponse>(
    getComponentUsageOrganizationsUrl(),
    buildComponentUsageRequest(componentHash, page, pageSize, filters),
    { signal }
  );
  return {
    organizations: data.organizations ?? [],
    total: data.total ?? 0,
    page: data.page ?? page,
    pageSize: data.pageSize ?? pageSize,
    hasNextPage: Boolean(data.hasNextPage),
  };
}
