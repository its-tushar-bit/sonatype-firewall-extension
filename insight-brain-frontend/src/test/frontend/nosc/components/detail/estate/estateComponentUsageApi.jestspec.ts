/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildComponentUsageRequest,
  COMPONENT_USAGE_PAGE_SIZE,
  fetchComponentUsageApplications,
  fetchComponentUsageOrganizations,
} from 'MainRoot/nosc/components/detail/estate/estateComponentUsageApi';
import {
  getComponentUsageApplicationsUrl,
  getComponentUsageOrganizationsUrl,
} from 'MainRoot/util/CLMLocation';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

describe('estateComponentUsageApi', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterAll(() => {
    setBaseUrl();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('builds a 0-based where-used request', () => {
    expect(buildComponentUsageRequest('abc', 0, 25)).toEqual({
      componentHash: 'abc',
      page: 0,
      pageSize: 25,
    });
    expect(buildComponentUsageRequest('abc', 1).pageSize).toBe(COMPONENT_USAGE_PAGE_SIZE);
  });

  it('POSTs applications usage and normalizes the response', async () => {
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationPublicId: 'webgoat',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    const result = await fetchComponentUsageApplications('deadbeef', 0);
    expect(result.applications).toHaveLength(1);
    expect(result.applications[0].applicationPublicId).toBe('webgoat');
    expect(result.total).toBe(1);

    const history = axiosMock.history.post;
    expect(history).toHaveLength(1);
    expect(JSON.parse(history[0].data)).toEqual({
      componentHash: 'deadbeef',
      page: 0,
      pageSize: COMPONENT_USAGE_PAGE_SIZE,
    });
  });

  it('POSTs organizations usage', async () => {
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply(200, {
      organizations: [{ organizationId: 'org-1', organizationName: 'Engineering', applicationCount: 3 }],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    const result = await fetchComponentUsageOrganizations('deadbeef', 0);
    expect(result.organizations[0].organizationName).toBe('Engineering');
    expect(result.organizations[0].applicationCount).toBe(3);
  });
});
