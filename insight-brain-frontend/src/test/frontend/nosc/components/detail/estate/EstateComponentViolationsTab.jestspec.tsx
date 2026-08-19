/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneEstateComponentDetail } from 'TestRoot/nosc/components/detail/estate/renderNexusOneEstateComponentDetail';
import { getApiV2ComponentDetailsUrl, getViolationsListUrl } from 'MainRoot/util/CLMLocation';
import { estateComponentDetailStateNameForTab } from 'MainRoot/nosc/components/detail/estate/estateComponentDetailUtils';
import { violationDetailHref } from 'MainRoot/nosc/violations/violationDetailHref';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

const COMPONENT_HASH = 'abc123hash';
const SECOND_COMPONENT_HASH = 'def456hash';

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

  it('keeps componentHash and facets off when searching violations', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getViolationsListUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      return [
        200,
        {
          violations: [
            {
              policyViolationId: `pv-${body.search || 'initial'}`,
              threatLevel: 7,
              threatCategory: 'security',
              policyName: body.search ? 'Filtered Policy' : 'Initial Policy',
              applicationName: 'WebGoat',
              stage: 'Build',
              state: 'OPEN',
            },
          ],
          total: 1,
          page: body.page,
          pageSize: body.pageSize,
          hasNextPage: false,
          source: 'LOCAL',
        },
      ];
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'violations');
    expect(await screen.findByText('Initial Policy')).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Search policy violations'), 'Filtered');
    await userEvent.click(screen.getByRole('button', { name: 'Search violations' }));

    await waitFor(() => {
      expect(screen.getByText('Filtered Policy')).toBeInTheDocument();
    });

    const bodies = axiosMock.history.post
      .filter((request) => request.url === getViolationsListUrl())
      .map((request) => JSON.parse(request.data as string));
    expect(bodies.at(-1)).toMatchObject({
      componentHash: COMPONENT_HASH,
      includeFacets: false,
      search: 'Filtered',
      orderBy: '-policyThreatLevel',
    });
  });

  it('clears the violations search when the componentHash changes via navigation', async () => {
    const bodies: Array<{ componentHash?: string; search?: string; page?: number }> = [];
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getViolationsListUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      bodies.push(body);
      const searched = body.search ? 'Filtered' : 'Initial';
      return [
        200,
        {
          violations: [
            {
              policyViolationId: `${body.componentHash}-${body.search || 'initial'}`,
              threatLevel: 7,
              policyName: `${searched} Policy for ${body.componentHash}`,
              applicationName: 'WebGoat',
              state: 'OPEN',
            },
          ],
          total: 1,
          page: body.page,
          pageSize: body.pageSize,
          hasNextPage: false,
          source: 'LOCAL',
        },
      ];
    });

    const { router } = renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'violations');
    expect(await screen.findByText(`Initial Policy for ${COMPONENT_HASH}`)).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Search policy violations'), 'Filtered');
    await userEvent.click(screen.getByRole('button', { name: 'Search violations' }));
    expect(await screen.findByText(`Filtered Policy for ${COMPONENT_HASH}`)).toBeInTheDocument();

    await act(async () => {
      await router.stateService.go(estateComponentDetailStateNameForTab('violations'), {
        componentHash: SECOND_COMPONENT_HASH,
      });
    });

    expect(await screen.findByText(`Initial Policy for ${SECOND_COMPONENT_HASH}`)).toBeInTheDocument();
    expect(screen.getByLabelText('Search policy violations')).toHaveValue('');
    expect(bodies.at(-1)).toMatchObject({
      componentHash: SECOND_COMPONENT_HASH,
      page: 0,
    });
    expect(bodies.at(-1)).not.toHaveProperty('search');
  });

  it('resets violations page to 0 when the component hash changes', async () => {
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
              policyViolationId: `pv-${body.componentHash}-${body.page}`,
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

    const { router } = renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'violations');
    await screen.findByTestId('nosc-estate-component-violations-table');

    await userEvent.click(
      screen
        .getByTestId('nosc-estate-component-violations-pagination')
        .querySelector('button[aria-label="Next page"]') as HTMLElement
    );
    await waitFor(() => {
      expect(bodies.some((body) => body.componentHash === COMPONENT_HASH && body.page === 1)).toBe(true);
    });

    await act(async () => {
      await router.stateService.go(estateComponentDetailStateNameForTab('violations'), {
        componentHash: SECOND_COMPONENT_HASH,
      });
    });

    await waitFor(() => {
      expect(bodies.some((body) => body.componentHash === SECOND_COMPONENT_HASH && body.page === 0)).toBe(true);
    });
    expect(bodies.filter((body) => body.componentHash === SECOND_COMPONENT_HASH).at(-1)?.page).toBe(0);
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
      screen
        .getByTestId('nosc-estate-component-violations-pagination')
        .querySelector('button[aria-label="Next page"]') as HTMLElement
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
      'Security-Critical'
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
      screen
        .getByTestId('nosc-estate-component-violations-pagination')
        .querySelector('button[aria-label="Next page"]') as HTMLElement
    );

    const error = await screen.findByTestId('nosc-estate-component-violations-error');
    await userEvent.click(within(error).getByRole('button', { name: 'Retry' }));

    await waitFor(() => {
      expect(bodies.filter((b) => b.page === 0).length).toBeGreaterThanOrEqual(2);
    });
    expect(await screen.findByTestId('nosc-estate-component-violations-table')).toBeInTheDocument();
  });
});
