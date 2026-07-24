/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import VulnerabilitiesList from 'MainRoot/nosc/vulnerabilities/VulnerabilitiesList';
import { NEXUS_ONE_VULNERABILITIES_STATE_NAME } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';
import { getVulnerabilitiesListUrl } from 'MainRoot/util/CLMLocation';
import { MOCK_VULNERABILITIES_LIST_RESPONSE } from 'TestRoot/nosc/vulnerabilities/mockVulnerabilitiesListResponse';
import type { VulnerabilitiesListResponse } from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';

describe('VulnerabilitiesList', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;
  let user: ReturnType<typeof userEvent.setup>;

  beforeAll(() => {
    _setBaseUrlForTesting('http://localhost');
    installRadixJsdomShims();
  });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    user = userEvent.setup();
  });

  afterEach(() => {
    axiosMock.restore();
  });

  const renderList = (params: Record<string, unknown> = {}) =>
    renderNexusOneRoute(<VulnerabilitiesList />, NEXUS_ONE_VULNERABILITIES_STATE_NAME, params);

  it('fetches and renders the shell with filter rail, cards, and toolbar', async () => {
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, MOCK_VULNERABILITIES_LIST_RESPONSE);
    renderList();

    expect(await screen.findByTestId('vulnerabilities-card-list')).toBeInTheDocument();
    expect(screen.getByTestId('preview-vulnerabilities-page')).toBeInTheDocument();
    expect(screen.getByTestId('vulnerabilities-filter-rail-desktop')).toBeInTheDocument();
    expect(screen.getByTestId('vulnerabilities-toolbar')).toBeInTheDocument();
    expect(screen.getByText('CVE-2024-0001')).toBeInTheDocument();
  });

  it('sends default My Scan Data sort and includeFacets on the first page', async () => {
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, MOCK_VULNERABILITIES_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('vulnerabilities-card-list');
    const body = JSON.parse(axiosMock.history.post[0].data);
    expect(body).toEqual(
      expect.objectContaining({
        tab: 'myScanData',
        page: 0,
        orderBy: '-cvssScore',
        includeFacets: true,
      }),
    );
  });

  it('hydrates tab, search, page, and filters from deep-linked route params', async () => {
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, MOCK_VULNERABILITIES_LIST_RESPONSE);
    renderList({
      tab: 'catalog',
      q: 'log4j',
      page: '2',
      severity: 'critical,high',
      ecosystem: 'maven',
      cvss: '7-10',
    });

    // Prefer the first hydrated POST — a later refetch may omit facets once the cache is warm.
    await waitFor(() => {
      const hydrated = axiosMock.history.post.find((request) => {
        const body = JSON.parse(String(request.data));
        return (
          body.search === 'log4j' &&
          body.page === 1 &&
          body.tab === 'catalog' &&
          Array.isArray(body.severities) &&
          body.severities.includes('critical')
        );
      });
      expect(hydrated).toBeDefined();
      const body = JSON.parse(String(hydrated!.data));
      expect(body).toEqual(
        expect.objectContaining({
          tab: 'catalog',
          search: 'log4j',
          page: 1,
          severities: ['critical', 'high'],
          ecosystems: ['maven'],
          minCvssScore: 7,
          maxCvssScore: 10,
        }),
      );
    });
  });

  it('applies a severity filter, resets to page 0, and writes the URL', async () => {
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, MOCK_VULNERABILITIES_LIST_RESPONSE);
    const { router } = renderList();

    await screen.findByTestId('vulnerabilities-card-list');
    await user.click(screen.getByTestId('vulnerabilities-filter-severity-desktop-option-critical'));

    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.severities).toEqual(['critical']);
      expect(last.page).toBe(0);
      expect(last.includeFacets).toBe(true);
    });

    await waitFor(() => {
      expect(router.stateService.params.severity).toBe('critical');
      expect(router.stateService.params.page).toBeUndefined();
    });
  });

  it('submits search and clears facet cache for a fresh includeFacets request', async () => {
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, MOCK_VULNERABILITIES_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('vulnerabilities-card-list');
    const searchBox = screen.getByTestId('vulnerabilities-toolbar-search');
    await user.type(searchBox, 'log4j{enter}');

    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.search).toBe('log4j');
      expect(last.page).toBe(0);
      expect(last.includeFacets).toBe(true);
    });
  });

  it('omits facets when paging past the first page and keeps rail counts', async () => {
    const paged: VulnerabilitiesListResponse = {
      ...MOCK_VULNERABILITIES_LIST_RESPONSE,
      total: 30,
      hasNextPage: true,
    };
    const pagedNoFacets: VulnerabilitiesListResponse = { ...paged, facets: undefined };
    axiosMock
      .onPost(getVulnerabilitiesListUrl())
      .replyOnce(200, paged)
      .onPost(getVulnerabilitiesListUrl())
      .reply(200, pagedNoFacets);
    renderList();

    await screen.findByTestId('vulnerabilities-card-list');
    expect(JSON.parse(axiosMock.history.post[0].data).includeFacets).toBe(true);

    await user.click(screen.getByRole('button', { name: /next page/i }));

    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.page).toBe(1);
      expect(last.includeFacets).toBe(false);
    });

    const rail = screen.getByTestId('vulnerabilities-filter-severity-desktop');
    expect(within(rail).getByText(/Critical/i)).toBeInTheDocument();
  });

  it('requests facets when deep-linked directly to page 2 with an empty cache', async () => {
    const paged: VulnerabilitiesListResponse = {
      ...MOCK_VULNERABILITIES_LIST_RESPONSE,
      total: 30,
      hasNextPage: true,
    };
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, paged);
    renderList({ page: '2' });

    await screen.findByTestId('vulnerabilities-card-list');
    await waitFor(() => {
      const pageTwo = axiosMock.history.post.find((request) => {
        const body = JSON.parse(String(request.data));
        return body.page === 1 && body.includeFacets === true;
      });
      expect(pageTwo).toBeDefined();
    });
    // Rail must not render count-less on a bookmarked page-2 deep link.
    expect(
      screen.getByTestId('vulnerabilities-filter-severity-desktop-option-critical'),
    ).toBeInTheDocument();
  });

  it('switches to Catalog and clears facets for a new request', async () => {
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, MOCK_VULNERABILITIES_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('vulnerabilities-card-list');
    await user.click(screen.getByTestId('vulnerabilities-tab-catalog'));

    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.tab).toBe('catalog');
      expect(last.page).toBe(0);
      expect(last.includeFacets).toBe(true);
    });
  });

  it('clears facet cache on sort change so includeFacets is true', async () => {
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, MOCK_VULNERABILITIES_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('vulnerabilities-card-list');
    await user.click(screen.getByRole('combobox', { name: /sort/i }));
    await user.click(await screen.findByRole('option', { name: /Lowest CVSS/i }));

    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.orderBy).toBe('cvssScore');
      expect(last.page).toBe(0);
      expect(last.includeFacets).toBe(true);
    });
  });

  it('renders empty and error states with retry', async () => {
    const empty: VulnerabilitiesListResponse = {
      ...MOCK_VULNERABILITIES_LIST_RESPONSE,
      vulnerabilities: [],
      total: 0,
      facets: { totalVulnerabilities: 0, severities: {}, ecosystems: {} },
    };
    axiosMock.onPost(getVulnerabilitiesListUrl()).replyOnce(200, empty);
    renderList();
    expect(await screen.findByTestId('vulnerabilities-list-empty')).toBeInTheDocument();

    axiosMock.reset();
    axiosMock.onPost(getVulnerabilitiesListUrl()).replyOnce(500);
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, MOCK_VULNERABILITIES_LIST_RESPONSE);
    renderList();

    const retry = await screen.findByRole('button', { name: /retry/i });
    expect(screen.getByTestId('vulnerabilities-list-error')).toBeInTheDocument();
    await user.click(retry);
    expect(await screen.findByTestId('vulnerabilities-card-list')).toBeInTheDocument();
  });

  it('restores severity checkbox selection from a bookmarked filter URL', async () => {
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, MOCK_VULNERABILITIES_LIST_RESPONSE);
    renderList({ severity: 'critical' });

    await screen.findByTestId('vulnerabilities-card-list');
    expect(screen.getByTestId('vulnerabilities-filter-severity-desktop-option-critical')).toBeChecked();
    expect(screen.getByTestId('vulnerabilities-filter-severity-desktop-option-high')).not.toBeChecked();
  });

  it('normalizes a junk-carrying deep-link URL to its canonical form', async () => {
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply(200, MOCK_VULNERABILITIES_LIST_RESPONSE);
    // severity=bogus is unsupported and cvss=abc is malformed — both are dropped by the codec, so
    // the container must rewrite the address bar (leaving only the valid severity=critical).
    const { router } = renderList({
      severity: 'critical,bogus',
      cvss: 'abc',
    });

    await screen.findByTestId('vulnerabilities-card-list');
    await waitFor(() => {
      expect(router.urlService.url()).not.toContain('cvss=');
      expect(router.urlService.url()).not.toContain('bogus');
    });
    expect(router.urlService.url()).toContain('severity=critical');
  });

  it('clamps an out-of-range deep-linked page back to the last page with rows', async () => {
    axiosMock.onPost(getVulnerabilitiesListUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      if (body.page > 0) {
        return [
          200,
          {
            ...MOCK_VULNERABILITIES_LIST_RESPONSE,
            total: 25,
            page: body.page,
            vulnerabilities: [],
          },
        ];
      }
      return [200, { ...MOCK_VULNERABILITIES_LIST_RESPONSE, total: 25, page: 0 }];
    });
    const { router } = renderList({ page: '100' });

    await waitFor(() => {
      expect(router.urlService.url()).not.toContain('page=100');
    });
    // total=25 / pageSize=25 → max page is 1, so the page query is omitted (default).
    expect(await screen.findByTestId('vulnerabilities-card-list')).toBeInTheDocument();
  });
});
