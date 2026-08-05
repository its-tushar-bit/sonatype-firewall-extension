/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneEstateComponentDetail } from 'TestRoot/nosc/components/detail/estate/renderNexusOneEstateComponentDetail';
import {
  getApiV2ComponentDetailsUrl,
  getViolationsListUrl,
} from 'MainRoot/util/CLMLocation';
import { violationDetailHref } from 'MainRoot/nosc/violations/violationDetailHref';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

const COMPONENT_HASH = 'abc123hash';

describe('EstateComponentViolationsTab', () => {
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

  it('POSTs violations/list with componentHash and links rows to violation detail', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getViolationsListUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      expect(body.componentHash).toBe(COMPONENT_HASH);
      expect(body.includeFacets).toBe(false);
      expect(body.page).toBe(0);
      return [
        200,
        {
          violations: [
            {
              policyViolationId: 'pv-1',
              policyName: 'Security-Critical',
              threatLevel: 9,
              applicationName: 'WebGoat',
              state: 'OPEN',
            },
          ],
          total: 1,
          page: 0,
          pageSize: 25,
          hasNextPage: false,
          source: 'index',
        },
      ];
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'violations');

    expect(await screen.findByTestId('nosc-estate-component-violations-table')).toBeInTheDocument();
    const link = screen.getByTestId('nosc-estate-component-violations-row-link');
    expect(link).toHaveAttribute('href', violationDetailHref('pv-1'));
    expect(link).toHaveTextContent('Security-Critical');
  });

  it('requests the next page when pagination Next is clicked', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    const bodies: Array<{ page?: number; componentHash?: string }> = [];
    axiosMock.onPost(getViolationsListUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      bodies.push(body);
      return [
        200,
        {
          violations: [
            {
              policyViolationId: `pv-page-${body.page}`,
              policyName: `Policy ${body.page}`,
              threatLevel: 5,
              state: 'OPEN',
            },
          ],
          total: 40,
          page: body.page,
          pageSize: 25,
          hasNextPage: body.page === 0,
          source: 'index',
        },
      ];
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'violations');
    await screen.findByTestId('nosc-estate-component-violations-table');

    await userEvent.click(
      screen.getByTestId('nosc-estate-component-violations-pagination').querySelector(
        'button[aria-label="Next page"]',
      ) as HTMLElement,
    );

    await waitFor(() => {
      expect(bodies.some((b) => b.page === 1 && b.componentHash === COMPONENT_HASH)).toBe(true);
    });
  });

  it('renders policy name as plain text when policyViolationId is blank', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getViolationsListUrl()).reply(200, {
      violations: [
        {
          policyViolationId: '   ',
          policyName: 'Security-Critical',
          threatLevel: 9,
          state: 'OPEN',
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
      source: 'index',
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'violations');

    expect(await screen.findByTestId('nosc-estate-component-violations-row-label')).toHaveTextContent(
      'Security-Critical',
    );
    expect(screen.queryByTestId('nosc-estate-component-violations-row-link')).not.toBeInTheDocument();
  });

  it('retries from page 0 after a mid-list failure', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    const bodies: Array<{ page?: number }> = [];
    axiosMock.onPost(getViolationsListUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      bodies.push(body);
      if (body.page === 1 && bodies.filter((b) => b.page === 1).length === 1) {
        return [500];
      }
      return [
        200,
        {
          violations: [
            {
              policyViolationId: `pv-page-${body.page}`,
              policyName: `Policy ${body.page}`,
              threatLevel: 5,
              state: 'OPEN',
            },
          ],
          total: 40,
          page: body.page,
          pageSize: 25,
          hasNextPage: body.page === 0,
          source: 'index',
        },
      ];
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'violations');
    await screen.findByTestId('nosc-estate-component-violations-table');

    await userEvent.click(
      screen.getByTestId('nosc-estate-component-violations-pagination').querySelector(
        'button[aria-label="Next page"]',
      ) as HTMLElement,
    );

    const error = await screen.findByTestId('nosc-estate-component-violations-error');
    await userEvent.click(within(error).getByRole('button', { name: 'Retry' }));

    await waitFor(() => {
      expect(bodies.filter((b) => b.page === 0).length).toBeGreaterThanOrEqual(2);
    });
    expect(await screen.findByTestId('nosc-estate-component-violations-table')).toBeInTheDocument();
  });
});
