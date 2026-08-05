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
  getComponentUsageApplicationsUrl,
} from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

const COMPONENT_HASH = 'usage-hash-1';

describe('EstateComponentApplicationsTab', () => {
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

  it('links application rows to application detail when publicId is present', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationPublicId: 'webgoat-app',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build', 'release'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');

    expect(await screen.findByTestId('nosc-estate-component-applications-table')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-applications-row-link')).toHaveAttribute(
      'href',
      '#/applications/webgoat-app',
    );
    expect(screen.getByText('Engineering')).toBeInTheDocument();
    expect(screen.getByText('build')).toBeInTheDocument();
  });

  it('issues a second POST with page=1 when Next is clicked', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    const bodies: Array<{ page?: number }> = [];
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      bodies.push(body);
      return [
        200,
        {
          applications: [
            {
              applicationPublicId: `app-${body.page}`,
              applicationName: `App ${body.page}`,
            },
          ],
          total: 40,
          page: body.page,
          pageSize: 25,
          hasNextPage: body.page === 0,
        },
      ];
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    await userEvent.click(
      screen.getByTestId('nosc-estate-component-applications-pagination').querySelector(
        'button[aria-label="Next page"]',
      ) as HTMLElement,
    );

    await waitFor(() => {
      expect(bodies.some((b) => b.page === 1)).toBe(true);
    });
  });

  it('shows an honest empty state when no readable applications contain the hash', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [],
      total: 0,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');

    expect(await screen.findByTestId('nosc-estate-component-applications-empty')).toHaveTextContent(
      'This component was not found in any readable applications.',
    );
  });

  it('shows error + Retry when the where-used applications request fails', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(500);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');

    expect(
      await screen.findByTestId('nosc-estate-component-applications-error'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });
});
