/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import WaiversListPage from 'MainRoot/nosc/waivers/WaiversListPage';
import { getIndexQueryUrl } from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import { MOCK_WAIVERS_INDEX_QUERY_RESPONSE } from 'TestRoot/nosc/waivers/mockWaiversAnaData';
import type { WaiversIndexQueryResponse } from 'MainRoot/nosc/waivers/waiversListApi';

/**
 * Container-level coverage for the Ana Waivers list (CLM-43204). Verifies the router-driven
 * WaiversListPage POSTs to {@code /rest/search/index-query} with the Confluence V1 filter
 * schema, renders the shared Applications-style chrome (filter rail + toolbar + table), and
 * surfaces the empty / 409 / 500 states expected by the design.
 */

describe('WaiversListPage (Ana)', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    installRadixJsdomShims();
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    axiosMock.reset();
  });

  const renderPage = (params: Record<string, unknown> = {}) =>
    renderNexusOneRoute(<WaiversListPage />, 'nexusOneWaivers', params);

  function reply(body: WaiversIndexQueryResponse = MOCK_WAIVERS_INDEX_QUERY_RESPONSE) {
    axiosMock.onPost(getIndexQueryUrl()).reply(200, body);
  }

  it('renders skeleton while loading, then a row per waiver from index-query', async () => {
    reply();

    renderPage();
    // Loading skeleton fires on the very first render before the layoutEffect flips
    // fetchEnabled → true. useTile keeps status='loading' until the request resolves.
    expect(screen.getByTestId('waivers-list-loading')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId('waivers-ana-table')).toBeInTheDocument();
    });
    const table = screen.getByTestId('waivers-ana-table');
    expect(within(table).getAllByTestId('waivers-ana-table-row')).toHaveLength(2);
    // Two-row fixture: one manual + one auto. The Auto badge should appear at least once.
    expect(within(table).getAllByText('Auto').length).toBeGreaterThanOrEqual(1);
  });

  it('renders the applications-style chrome (rail, toolbar, count)', async () => {
    reply();
    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId('waivers-ana-table')).toBeInTheDocument();
    });
    expect(screen.getByTestId('waivers-page-layout')).toBeInTheDocument();
    expect(screen.getByTestId('waivers-filter-rail')).toBeInTheDocument();
    expect(screen.getByTestId('waivers-toolbar')).toBeInTheDocument();
    expect(screen.getByTestId('waivers-toolbar-count')).toHaveTextContent('2 waivers');
  });

  it('POSTs entityType=WAIVER with the expected default request shape', async () => {
    reply();
    renderPage();
    await waitFor(() => expect(axiosMock.history.post.length).toBeGreaterThan(0));

    const body = JSON.parse(String(axiosMock.history.post.at(-1)?.data));
    expect(body).toEqual(
      expect.objectContaining({
        entityType: 'WAIVER',
        page: 1,
        includeFacets: true,
        sort: '-policyWaiverCreatedAt',
      }),
    );
  });

  it('submitting search POSTs a query filter and resets to page 1', async () => {
    reply();
    renderPage();
    await waitFor(() => expect(screen.getByTestId('waivers-toolbar')).toBeInTheDocument());
    // Wait for the initial POST to complete so the second one (with the search) is the last.
    await waitFor(() => expect(axiosMock.history.post.length).toBeGreaterThan(0));

    const user = userEvent.setup();
    const input = screen.getByTestId('waivers-toolbar-search');
    await user.type(input, 'guava{enter}');

    await waitFor(() => {
      const body = JSON.parse(String(axiosMock.history.post.at(-1)?.data));
      expect(body.filters?.query).toBe('guava');
      expect(body.page).toBe(1);
    });
  });

  it('renders a calm empty state when there are zero waivers', async () => {
    reply({ ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE, rows: [], totalEstimate: 0 });
    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId('waivers-list-empty')).toBeInTheDocument();
    });
    expect(screen.getByText(/no waivers in scope/i)).toBeInTheDocument();
  });

  it('renders an informational banner for HTTP 409 (index still building)', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(409, { message: 'index building' });
    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId('waivers-list-not-ready')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('waivers-list-error')).not.toBeInTheDocument();
  });

  it('renders an error state on 500 with a Retry button', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(500, 'oops');
    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId('waivers-list-error')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('rows link to the native Waiver Detail page with the correct owner segments', async () => {
    reply();
    renderPage();
    // findAllBy... because the fixture has two rows and therefore two detail links.
    const links = await screen.findAllByTestId('waivers-ana-table-row-detail-link');
    expect(links).toHaveLength(2);
    const hrefs = links.map((el) => el.getAttribute('href') ?? '');
    expect(hrefs.some((href) => href.includes('/waivers/application/app-internal-1/waiver-1'))).toBe(true);
    expect(hrefs.some((href) => href.includes('/waivers/organization/org-root/waiver-auto-2'))).toBe(true);
  });

  it('carries type=autoWaiver on the auto-waiver row so the detail page fetches the right API (CLM-43502)', async () => {
    reply();
    renderPage();
    const links = await screen.findAllByTestId('waivers-ana-table-row-detail-link');
    expect(links).toHaveLength(2);
    const hrefs = links.map((el) => el.getAttribute('href') ?? '');
    expect(
      hrefs.some(
        (href) => href.includes('/waivers/organization/org-root/waiver-auto-2') && href.includes('type=autoWaiver'),
      ),
    ).toBe(true);
    expect(
      hrefs.some((href) => href.includes('/waivers/application/app-internal-1/waiver-1') && !href.includes('type=')),
    ).toBe(true);
  });

  it('keeps page 2 after Next (URL sync must not stomp the cursor-backed advance)', async () => {
    let cursorSeenOnPageTwo: string | undefined;
    axiosMock.onPost(getIndexQueryUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      if (body.page === 1) {
        return [
          200,
          {
            ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE,
            totalEstimate: 100,
            nextSearchAfter: 'cursor-p2',
          },
        ];
      }
      if (body.page === 2) {
        cursorSeenOnPageTwo = body.searchAfter;
        return [
          200,
          {
            ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE,
            page: 2,
            totalEstimate: 100,
            nextSearchAfter: null,
          },
        ];
      }
      return [200, MOCK_WAIVERS_INDEX_QUERY_RESPONSE];
    });

    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId('waivers-list-pagination')).toBeInTheDocument();
    });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Next page' }));

    await waitFor(() => expect(cursorSeenOnPageTwo).toBe('cursor-p2'));
    await waitFor(() => {
      expect(screen.getByText('Page 2 of 2')).toBeInTheDocument();
    });
    // Final POST must remain page 2 — a stale inbound URL sync used to abort this and snap to 1.
    const lastBody = JSON.parse(String(axiosMock.history.post.at(-1)?.data));
    expect(lastBody.page).toBe(2);
    expect(lastBody.searchAfter).toBe('cursor-p2');
  });
});
