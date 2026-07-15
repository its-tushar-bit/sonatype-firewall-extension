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
import ViolationsList from 'MainRoot/nosc/violations/ViolationsList';
import { NEXUS_ONE_VIOLATIONS_STATE_NAME } from 'MainRoot/nosc/violations/violationsRoute';
import { getViolationsListUrl } from 'MainRoot/util/CLMLocation';
import { MOCK_VIOLATIONS_LIST_RESPONSE } from 'MainRoot/nosc/violations/mockViolationsListData';
import { ViolationsListResponse } from 'MainRoot/nosc/violations/violationListTypes';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

describe('ViolationsList', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;
  let user: ReturnType<typeof userEvent.setup>;

  beforeAll(() => {
    _setBaseUrlForTesting('http://localhost');
    // Interactive filter rail uses Radix Slider + ScrollArea (need ResizeObserver / Pointer shims).
    installRadixJsdomShims();
  });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    user = userEvent.setup();
  });

  afterEach(() => {
    axiosMock.restore();
  });

  const renderList = () => renderNexusOneRoute(<ViolationsList />, 'nexusOneViolations');

  it('fetches and renders the two-column shell with filter rail, cards, and content', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    expect(await screen.findByTestId('violation-card-grid')).toBeInTheDocument();
    expect(screen.getByTestId('preview-violations-page')).toBeInTheDocument();
    expect(screen.getByTestId('violations-page-layout')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-rail')).toBeInTheDocument();
    expect(screen.getByTestId('violations-page-content')).toBeInTheDocument();
  });

  it('renders one card per violation row with component names and threat badges', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    const cards = await screen.findAllByTestId('violation-card');
    expect(cards).toHaveLength(MOCK_VIOLATIONS_LIST_RESPONSE.violations.length);
    expect(screen.getByText('log4j-core : 2.14.0')).toBeInTheDocument();
    expect(screen.getByText('lodash : 4.17.15')).toBeInTheDocument();
    expect(screen.getAllByTestId('violation-threat-badge').length).toBe(cards.length);
  });

  it('links each card to the embedded violation detail route at /violations/{id}', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    const link = screen.getByRole('link', {
      name: /open violation for security - critical on log4j-core : 2\.14\.0/i,
    });
    expect(link).toHaveAttribute('href', '#/violations/pv-log4j-critical');
  });

  it('includes state and auto-waiver in the card link accessible name', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    expect(
      screen.getByRole('link', {
        name: /waived violation for quality - standards on busybox : 1\.33 in cherry - platform,.*auto-waived/i,
      }),
    ).toBeInTheDocument();
  });

  it('falls back to a placeholder when componentName is missing or blank', async () => {
    const rowWithoutName = {
      ...MOCK_VIOLATIONS_LIST_RESPONSE.violations[0],
      componentName: '',
      componentVersion: undefined,
      componentIdentifier: undefined,
    };
    const response: ViolationsListResponse = {
      ...MOCK_VIOLATIONS_LIST_RESPONSE,
      violations: [rowWithoutName],
      total: 1,
      facets: { totalViolations: 1 },
    };
    axiosMock.onPost(getViolationsListUrl()).reply(200, response);
    renderList();

    await screen.findByTestId('violation-card-grid');
    expect(screen.getByText('(unknown component)')).toBeInTheDocument();
    expect(
      screen.getByRole('link', {
        name: /open violation for security - critical on \(unknown component\)/i,
      }),
    ).toBeInTheDocument();
  });

  it('shows an auto-waiver pill only for auto-waived rows', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    expect(screen.getAllByTestId('violation-card-auto-waiver')).toHaveLength(1);
  });

  it('renders filter rail sections with friendly labels from facets and row-derived names', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    const rail = screen.getByTestId('violations-filter-rail');
    expect(within(rail).getByTestId('violations-filter-state')).toHaveTextContent('Open');
    expect(within(rail).getByTestId('violations-filter-state')).toHaveTextContent('Waived');
    expect(within(rail).getByTestId('violations-filter-policy-type')).toHaveTextContent('Security');
    expect(within(rail).getByTestId('violations-filter-policy-type')).toHaveTextContent('License');
    // Org/app facet ids resolve to human names via the current page rows.
    expect(within(rail).getByTestId('violations-filter-organizations')).toHaveTextContent('Java-team');
    expect(within(rail).getByTestId('violations-filter-applications')).toHaveTextContent('Apple - Java');
    expect(within(rail).getByTestId('violations-filter-stages')).toHaveTextContent('Build');
  });

  it('reflects the total count in the toolbar', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    expect(screen.getByTestId('violations-toolbar-count')).toHaveTextContent('3 violations');
  });

  it('sends the default threat-desc sort and 0-based first page on initial load', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    const body = JSON.parse(axiosMock.history.post[0].data);
    expect(body.page).toBe(0);
    expect(body.orderBy).toBe('-policyThreatLevel');
    expect(body.includeFacets).toBe(true);
  });

  it('submits a search term to the API and resets to the first page', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    const searchBox = screen.getByTestId('violations-toolbar-search');
    await user.type(searchBox, 'log4j{enter}');

    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.search).toBe('log4j');
      expect(last.page).toBe(0);
    });
  });

  it('applies a state filter, refetching with policyViolationStates and page 0', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    await user.click(screen.getByTestId('violations-filter-state-option-OPEN'));

    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.policyViolationStates).toEqual(['OPEN']);
      expect(last.page).toBe(0);
    });
  });

  it('resets filters back to an unfiltered request', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    await user.click(screen.getByTestId('violations-filter-state-option-OPEN'));
    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.policyViolationStates).toEqual(['OPEN']);
    });

    await user.click(screen.getByTestId('violations-filter-reset'));
    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.policyViolationStates).toBeUndefined();
    });
  });

  it('sends a narrowed threat range once the slider commits', async () => {
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    await screen.findByTestId('violation-card-grid');
    // Focus the first slider thumb and nudge the min up; Radix commits on keyup.
    const slider = screen.getByTestId('violations-filter-threat-slider');
    const thumb = slider.querySelector('[role="slider"]') as HTMLElement;
    thumb.focus();
    await user.keyboard('{ArrowRight}');

    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.policyThreatLevelRange).toBe('1,10');
    });
  });

  it('renders the empty state when the API returns no violations', async () => {
    const empty: ViolationsListResponse = {
      ...MOCK_VIOLATIONS_LIST_RESPONSE,
      violations: [],
      total: 0,
      facets: { totalViolations: 0 },
    };
    axiosMock.onPost(getViolationsListUrl()).reply(200, empty);
    renderList();

    expect(await screen.findByTestId('violations-list-empty')).toBeInTheDocument();
    expect(screen.queryByTestId('violation-card-grid')).not.toBeInTheDocument();
  });

  it('renders an error banner and refetches on retry', async () => {
    axiosMock.onPost(getViolationsListUrl()).replyOnce(500);
    axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
    renderList();

    const retry = await screen.findByRole('button', { name: /retry/i });
    expect(screen.getByTestId('violations-list-error')).toBeInTheDocument();
    await user.click(retry);

    expect(await screen.findByTestId('violation-card-grid')).toBeInTheDocument();
  });

  it('pages forward with a 0-based page index when more results exist', async () => {
    const paged: ViolationsListResponse = { ...MOCK_VIOLATIONS_LIST_RESPONSE, total: 30, hasNextPage: true };
    axiosMock.onPost(getViolationsListUrl()).reply(200, paged);
    renderList();

    await screen.findByTestId('violation-card-grid');
    const next = screen.getByRole('button', { name: /next page/i });
    await user.click(next);

    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.page).toBe(1);
    });
  });

  it('omits facets when paging past the first page and keeps the rail counts', async () => {
    const paged: ViolationsListResponse = { ...MOCK_VIOLATIONS_LIST_RESPONSE, total: 30, hasNextPage: true };
    // Page 2+ responses carry no facets — the rail must fall back to the page-1 counts.
    const pagedNoFacets: ViolationsListResponse = { ...paged, facets: undefined };
    axiosMock
      .onPost(getViolationsListUrl())
      .replyOnce(200, paged)
      .onPost(getViolationsListUrl())
      .reply(200, pagedNoFacets);
    renderList();

    await screen.findByTestId('violation-card-grid');
    expect(JSON.parse(axiosMock.history.post[0].data).includeFacets).toBe(true);

    await userEvent.click(screen.getByRole('button', { name: /next page/i }));

    await waitFor(() => {
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.page).toBe(1);
      expect(last.includeFacets).toBe(false);
    });

    // Cached page-1 facets keep the policy-type counts visible while on page 2.
    const rail = screen.getByTestId('violations-filter-rail');
    expect(within(rail).getByTestId('violations-filter-policy-type')).toHaveTextContent('Security');
  });

  describe('URL state (CLM-42260)', () => {
    it('hydrates search, filters, and page from deep-linked route params', async () => {
      axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
      renderNexusOneRoute(<ViolationsList />, 'nexusOneViolations', {
        q: 'log4j',
        state: 'OPEN',
        page: '2',
        threat: '4-10',
      });

      // The initial transition can fire a pre-hydration request, so look for the request that carries
      // the restored deep-link state rather than assuming it is the first POST.
      await waitFor(() => {
        const hydrated = axiosMock.history.post.find((request) => {
          const body = JSON.parse(String(request.data));
          return body.search === 'log4j' && body.page === 1;
        });
        expect(hydrated).toBeDefined();
        const body = JSON.parse(String(hydrated!.data));
        // List API wire format for threat range is the comma string "min,max". Classic CSV export
        // uses the object form { minPolicyThreatLevel, maxPolicyThreatLevel } — keep those distinct.
        expect(body).toEqual(
          expect.objectContaining({
            search: 'log4j',
            page: 1, // page=2 (1-based) → 0-based index 1
            policyViolationStates: expect.arrayContaining(['OPEN']),
            policyThreatLevelRange: '4,10',
          }),
        );
      });
    });

    it('restores the sidebar checkbox selection from a bookmarked filter URL', async () => {
      axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
      renderNexusOneRoute(<ViolationsList />, 'nexusOneViolations', { state: 'OPEN' });

      await screen.findByTestId('violation-card-grid');
      expect(screen.getByTestId('violations-filter-state-option-OPEN')).toBeChecked();
      expect(screen.getByTestId('violations-filter-state-option-WAIVED')).not.toBeChecked();
    });

    it('shows facet counts in the rail when deep-linked directly to page 2', async () => {
      // page=2 would normally send includeFacets:false, but with an empty cache the container must still
      // request facets so the rail is not count-less on a bookmarked deep link.
      const paged: ViolationsListResponse = { ...MOCK_VIOLATIONS_LIST_RESPONSE, total: 30, hasNextPage: true };
      axiosMock.onPost(getViolationsListUrl()).reply(200, paged);
      renderNexusOneRoute(<ViolationsList />, 'nexusOneViolations', { page: '2' });

      await screen.findByTestId('violation-card-grid');
      expect(JSON.parse(axiosMock.history.post[0].data).includeFacets).toBe(true);
      const rail = screen.getByTestId('violations-filter-rail');
      expect(within(rail).getByTestId('violations-filter-policy-type')).toHaveTextContent('Security');
    });

    it('clamps a deep-linked page beyond the result set back to the last page with rows', async () => {
      // total 30 / pageSize 25 → max UI page 2; a stale ?page=99 bookmark must settle on page=2.
      const paged: ViolationsListResponse = {
        ...MOCK_VIOLATIONS_LIST_RESPONSE,
        total: 30,
        hasNextPage: false,
      };
      axiosMock.onPost(getViolationsListUrl()).reply(200, paged);
      const { router } = renderNexusOneRoute(<ViolationsList />, 'nexusOneViolations', { page: '99' });

      await screen.findByTestId('violation-card-grid');
      await waitFor(() => {
        expect(router.urlService.url()).toContain('page=2');
        expect(router.urlService.url()).not.toContain('page=99');
      });
    });

    it('writes the committed search back to the hash query via the shared router', async () => {
      axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
      const { router } = renderNexusOneRoute(<ViolationsList />, 'nexusOneViolations');

      await screen.findByTestId('violation-card-grid');
      await user.type(screen.getByTestId('violations-toolbar-search'), 'log4j{enter}');

      // Assert a real round-trip on the harness router the component reads from, not a spied singleton.
      await waitFor(() => expect(router.urlService.url()).toContain('q=log4j'));
    });

    it('writes selected filters to the hash query when a filter is toggled', async () => {
      axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
      const { router } = renderNexusOneRoute(<ViolationsList />, 'nexusOneViolations');

      await screen.findByTestId('violation-card-grid');
      await user.click(screen.getByTestId('violations-filter-state-option-OPEN'));

      await waitFor(() => expect(router.urlService.url()).toContain('state=OPEN'));
    });

    it('follows a real back/forward navigation without clobbering it', async () => {
      axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
      const { router } = renderNexusOneRoute(<ViolationsList />, 'nexusOneViolations');
      await screen.findByTestId('violation-card-grid');

      // Drive a real navigation to a populated state (as browser back/forward would).
      await router.stateService.go(NEXUS_ONE_VIOLATIONS_STATE_NAME, { q: 'apple' });

      // The component hydrates to the navigated state...
      await waitFor(() =>
        expect(screen.getByTestId('violations-toolbar-search')).toHaveValue('apple'),
      );
      // ...and does not spuriously navigate back: the URL stays on the navigated state.
      expect(router.urlService.url()).toContain('q=apple');
      const last = JSON.parse(axiosMock.history.post[axiosMock.history.post.length - 1].data);
      expect(last.search).toBe('apple');
    });

    it('normalizes a junk-carrying deep-link URL to its canonical form', async () => {
      axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
      // threat=abc is malformed and state=BOGUS is unsupported — both are dropped by the codec, so the
      // container must rewrite the address bar to drop them (leaving only the valid state=OPEN).
      const { router } = renderNexusOneRoute(<ViolationsList />, 'nexusOneViolations', {
        state: 'OPEN,BOGUS',
        threat: 'abc',
      });

      await screen.findByTestId('violation-card-grid');
      await waitFor(() => {
        expect(router.urlService.url()).not.toContain('threat=');
        expect(router.urlService.url()).not.toContain('BOGUS');
      });
      expect(router.urlService.url()).toContain('state=OPEN');
    });

    it('clears the hash query when filters are reset to default', async () => {
      axiosMock.onPost(getViolationsListUrl()).reply(200, MOCK_VIOLATIONS_LIST_RESPONSE);
      const { router } = renderNexusOneRoute(<ViolationsList />, 'nexusOneViolations', { state: 'OPEN' });

      await screen.findByTestId('violation-card-grid');
      expect(router.urlService.url()).toContain('state=OPEN');

      await user.click(screen.getByTestId('violations-filter-reset'));
      await waitFor(() => expect(router.urlService.url()).not.toContain('state=OPEN'));
    });
  });
});
