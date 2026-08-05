/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneEstateComponentDetail } from 'TestRoot/nosc/components/detail/estate/renderNexusOneEstateComponentDetail';
import {
  getApiV2ComponentDetailsUrl,
  getComponentUsageOrganizationsUrl,
} from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

const COMPONENT_HASH = 'usage-org-hash-1';

describe('EstateComponentOrganizationsTab', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

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

  it('renders organization rows from the where-used API', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply(200, {
      organizations: [
        {
          organizationId: 'org-1',
          organizationName: 'Scale - Mobile - SRE',
          applicationCount: 3,
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'organizations');

    expect(
      await screen.findByTestId('nosc-estate-component-organizations-table'),
    ).toBeInTheDocument();
    expect(screen.getByText('Scale - Mobile - SRE')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('issues a second POST with page=1 when Next is clicked', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    const bodies: Array<{ page?: number }> = [];
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      bodies.push(body);
      return [
        200,
        {
          organizations: [
            {
              organizationId: `org-${body.page}`,
              organizationName: `Org ${body.page}`,
              applicationCount: 1,
            },
          ],
          total: 40,
          page: body.page,
          pageSize: 25,
          hasNextPage: body.page === 0,
        },
      ];
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'organizations');
    await screen.findByTestId('nosc-estate-component-organizations-table');

    await userEvent.click(
      screen.getByTestId('nosc-estate-component-organizations-pagination').querySelector(
        'button[aria-label="Next page"]',
      ) as HTMLElement,
    );

    await waitFor(() => {
      expect(bodies.some((b) => b.page === 1)).toBe(true);
    });
  });

  it('shows an honest empty state for readable organizations', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply(200, {
      organizations: [],
      total: 0,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'organizations');

    expect(
      await screen.findByTestId('nosc-estate-component-organizations-empty'),
    ).toHaveTextContent('This component was not found in any readable organizations.');
  });

  it('shows error + Retry when the where-used organizations request fails', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply(500);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'organizations');

    expect(
      await screen.findByTestId('nosc-estate-component-organizations-error'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });
});
