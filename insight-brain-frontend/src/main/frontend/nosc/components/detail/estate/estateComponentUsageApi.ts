/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getComponentUsageApplicationsUrl,
  getComponentUsageOrganizationsUrl,
} from 'MainRoot/util/CLMLocation';

/** Default page size for estate component where-used tables. */
export const COMPONENT_USAGE_PAGE_SIZE = 25;

export type ComponentUsageRequest = {
  readonly componentHash: string;
  readonly page: number;
  readonly pageSize: number;
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

export function buildComponentUsageRequest(
  componentHash: string,
  page: number,
  pageSize: number = COMPONENT_USAGE_PAGE_SIZE,
): ComponentUsageRequest {
  return {
    componentHash,
    page,
    pageSize,
  };
}

export async function fetchComponentUsageApplications(
  componentHash: string,
  page: number,
  pageSize: number = COMPONENT_USAGE_PAGE_SIZE,
  signal?: AbortSignal,
): Promise<ComponentUsageApplicationsResponse> {
  const { data } = await axios.post<ComponentUsageApplicationsResponse>(
    getComponentUsageApplicationsUrl(),
    buildComponentUsageRequest(componentHash, page, pageSize),
    { signal },
  );
  return {
    applications: data.applications ?? [],
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
): Promise<ComponentUsageOrganizationsResponse> {
  const { data } = await axios.post<ComponentUsageOrganizationsResponse>(
    getComponentUsageOrganizationsUrl(),
    buildComponentUsageRequest(componentHash, page, pageSize),
    { signal },
  );
  return {
    organizations: data.organizations ?? [],
    total: data.total ?? 0,
    page: data.page ?? page,
    pageSize: data.pageSize ?? pageSize,
    hasNextPage: Boolean(data.hasNextPage),
  };
}
