/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { useNavigate } from 'react-router';
import { render, screen, waitFor } from '../test-utils';
import { SearchPage } from 'GuideRoot/search/SearchPage';
import type { SearchResponse, ComponentSearchResponse, VulnerabilitySearchResponse, SecurityEventDocument } from '@guide/ui-core/types';
import type { ApiSearchResponse } from 'GuideRoot/api/securityEventsBackend';

// Mock ResizeObserver (used by @guide/ui-core and @radix-ui components but not available in jsdom)
class MockResizeObserver {
  observe = jest.fn();
  unobserve = jest.fn();
  disconnect = jest.fn();
}
global.ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;

jest.mock('GuideRoot/api/searchBackend', () => ({
  searchAll: jest.fn(),
  fetchGlobalSearchTotals: jest.fn(),
}));

jest.mock('GuideRoot/api/componentsBackend', () => ({
  searchComponents: jest.fn(),
  fetchComponentBrowseAggregations: jest.fn().mockResolvedValue(null),
}));

jest.mock('GuideRoot/api/vulnerabilitiesBackend', () => ({
  searchVulnerabilities: jest.fn(),
  fetchVulnerabilityBrowseAggregations: jest.fn().mockResolvedValue(null),
}));

jest.mock('GuideRoot/api/securityEventsBackend', () => ({
  searchSecurityEvents: jest.fn(),
  fetchSecurityEventBrowseAggregations: jest.fn().mockResolvedValue(null),
}));

type FacetAggregations = Record<string, Record<string, number>>;

jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    PageLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    FilteredPageLayout: ({ children, header, subheader, aggregations }: { children: React.ReactNode; header: React.ReactNode; subheader?: React.ReactNode; aggregations?: FacetAggregations }) => (
      <>
        {header}
        {subheader}
        <ul aria-label="facet-aggregations">
          {Object.entries(aggregations ?? {}).flatMap(([groupKey, buckets]) =>
            Object.entries(buckets ?? {}).map(([bucketKey, count]) => (
              <li key={`${groupKey}.${bucketKey}`}>{`${groupKey}.${bucketKey}=${count}`}</li>
            ))
          )}
        </ul>
        {children}
      </>
    ),
    SearchTabs: ({ activeTab, totalAll, totalComponents, totalVulnerabilities, totalSecurityEvents, showSecurityEventsTab }: {
      activeTab: string; totalAll?: number; totalComponents?: number; totalVulnerabilities?: number;
      totalSecurityEvents?: number; showSecurityEventsTab?: boolean;
    }) => (
      <div data-testid="search-tabs">
        <span>tab:{activeTab}</span>
        <span>all:{totalAll ?? 0}</span>
        <span>cmp:{totalComponents ?? 0}</span>
        <span>vul:{totalVulnerabilities ?? 0}</span>
        <span>sec:{totalSecurityEvents ?? 0}</span>
        {showSecurityEventsTab && <span>se-tab-shown</span>}
      </div>
    ),
    SearchResultsList: ({ results, isPending }: { results: unknown[]; isPending: boolean }) =>
      isPending ? <p role="status" aria-label="loading-skeletons" /> : <p>all-results: {results.length}</p>,
    ComponentsResultsList: ({ components, isPending }: { components: unknown[]; isPending: boolean }) =>
      isPending ? <p role="status" aria-label="loading-skeletons" /> : <p>components-results: {components.length}</p>,
    VulnerabilitiesResultsList: ({ vulnerabilities, isPending }: { vulnerabilities: unknown[]; isPending: boolean }) =>
      isPending ? <p role="status" aria-label="loading-skeletons" /> : <p>vulnerabilities-results: {vulnerabilities.length}</p>,
    SecurityEventResultsList: ({ events, isPending }: { events: unknown[]; isPending: boolean }) =>
      isPending ? <p role="status" aria-label="loading-skeletons" /> : <p>security-events-results: {events.length}</p>,
    Pagination: () => <p>pagination-visible</p>,
    EmptyResultsCard: () => <p>no results</p>,
  };
});

import { searchAll, fetchGlobalSearchTotals } from 'GuideRoot/api/searchBackend';
import { searchComponents, fetchComponentBrowseAggregations } from 'GuideRoot/api/componentsBackend';
import { searchVulnerabilities, fetchVulnerabilityBrowseAggregations } from 'GuideRoot/api/vulnerabilitiesBackend';
import { searchSecurityEvents, fetchSecurityEventBrowseAggregations } from 'GuideRoot/api/securityEventsBackend';

const mockSearchAll = searchAll as jest.MockedFunction<typeof searchAll>;
const mockFetchGlobalSearchTotals = fetchGlobalSearchTotals as jest.MockedFunction<typeof fetchGlobalSearchTotals>;
const mockSearchComponents = searchComponents as jest.MockedFunction<typeof searchComponents>;
const mockSearchVulnerabilities = searchVulnerabilities as jest.MockedFunction<typeof searchVulnerabilities>;
const mockFetchComponentBrowseAggregations = fetchComponentBrowseAggregations as jest.MockedFunction<
  typeof fetchComponentBrowseAggregations
>;
const mockFetchVulnerabilityBrowseAggregations =
  fetchVulnerabilityBrowseAggregations as jest.MockedFunction<typeof fetchVulnerabilityBrowseAggregations>;
const mockSearchSecurityEvents = searchSecurityEvents as jest.MockedFunction<typeof searchSecurityEvents>;
const mockFetchSecurityEventBrowseAggregations =
  fetchSecurityEventBrowseAggregations as jest.MockedFunction<typeof fetchSecurityEventBrowseAggregations>;

function makeAllResponse(total: number, hitCount = 5): SearchResponse {
  return {
    hits: Array.from({ length: hitCount }, (_, i) => ({
      format: 'npm', originId: `c-${i}`, name: `c-${i}`, version: '1', registryLink: '', licenses: [],
    })) as SearchResponse['hits'],
    total, offset: 0, limit: 25,
    // Backend returns byType with plural keys (`components`/`vulnerabilities`).
    aggregations: { byType: { components: 3, vulnerabilities: 2 } },
  };
}

function makeComponentsResponse(total: number, hitCount = 5): ComponentSearchResponse {
  return {
    hits: Array.from({ length: hitCount }, (_, i) => ({
      format: 'npm', originId: `c-${i}`, name: `c-${i}`, version: '1', registryLink: '', licenses: [],
    })),
    total, offset: 0, limit: 25,
    aggregations: { byFormat: { npm: total }, byCategory: {}, bySeverity: {}, byLicense: {} },
  };
}

function makeVulnerabilitiesResponse(total: number, hitCount = 5): VulnerabilitySearchResponse {
  return {
    hits: Array.from({ length: hitCount }, (_, i) => ({
      vulnId: `CVE-${i}`, summary: `summary ${i}`, affectedEcosystems: ['npm'],
    })) as VulnerabilitySearchResponse['hits'],
    total, offset: 0, limit: 25,
    aggregations: { byEcosystem: { npm: total }, bySeverity: {} },
  };
}

function makeSecurityEventsResponse(total: number, hitCount = 5): ApiSearchResponse<SecurityEventDocument> {
  return {
    hits: Array.from({ length: hitCount }, (_, i) => ({
      eventId: `SE-${i}`, title: `event ${i}`, overview: `overview ${i}`,
      publishedDate: '2026-01-01', lastUpdatedDate: '2026-01-02',
      eventSeverityCategory: 'HIGH', eventThreatType: 'MALWARE',
    })),
    total, offset: 0, limit: 25,
    aggregations: { byEventSeverityCategory: { HIGH: total }, byEventThreatType: {} },
  };
}

describe('SearchPage', () => {
  afterEach(() => { jest.clearAllMocks(); });

  it('shows full-page skeleton on initial load', () => {
    mockSearchAll.mockReturnValue(new Promise(() => {}));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?query=foo'] } });

    expect(screen.getByRole('status', { name: /loading page content/i })).toBeInTheDocument();
  });

  it('uses both vulnerability and component card skeletons alternating', () => {
    mockSearchAll.mockReturnValue(new Promise(() => {}));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?query=foo'] } });

    expect(screen.getAllByTestId('skeleton-vulnerability').length).toBeGreaterThan(0);
    expect(screen.getAllByTestId('skeleton-component').length).toBeGreaterThan(0);
  });

  it('does not show page skeleton after data has loaded', async () => {
    mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?query=foo'] } });

    await screen.findByText('all-results: 5');
    expect(screen.queryByRole('status', { name: /loading page content/i })).not.toBeInTheDocument();
  });

  it('renders the policy-context picker at the top of the page', async () => {
    mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?query=foo'] } });

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Policy context — open picker/ })).toBeInTheDocument()
    );
  });

  it('defaults to the All tab and renders SearchResultsList with all hits', async () => {
    mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?query=foo'] } });

    await screen.findByText('all-results: 5');
    expect(screen.getByText('tab:all')).toBeInTheDocument();
  });

  it('switches to ComponentsResultsList when ?tab=components', async () => {
    mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
    mockSearchComponents.mockResolvedValue(makeComponentsResponse(7, 7));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?tab=components&query=foo'] } });

    await screen.findByText('components-results: 7');
    expect(screen.getByText('tab:components')).toBeInTheDocument();
    expect(screen.queryByText(/all-results/)).not.toBeInTheDocument();
  });

  it('switches to VulnerabilitiesResultsList when ?tab=vulnerabilities', async () => {
    mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
    mockSearchVulnerabilities.mockResolvedValue(makeVulnerabilitiesResponse(3, 3));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?tab=vulnerabilities'] } });

    await screen.findByText('vulnerabilities-results: 3');
    expect(screen.getByText('tab:vulnerabilities')).toBeInTheDocument();
  });

  it('propagates query into the active-tab fetcher', async () => {
    mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(0, 0));
    mockSearchComponents.mockResolvedValue(makeComponentsResponse(0, 0));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?tab=components&query=axios'] } });

    await waitFor(() => {
      expect(mockSearchComponents).toHaveBeenCalled();
    });
    const callArg = mockSearchComponents.mock.calls[0]?.[0] as URLSearchParams;
    expect(callArg.get('query')).toBe('axios');
  });

  it('renders the generic empty state when results are empty and not pending', async () => {
    mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(0, 0));
    mockSearchVulnerabilities.mockResolvedValue(makeVulnerabilitiesResponse(0, 0));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?tab=vulnerabilities'] } });

    await screen.findByText('no results');
  });

  it('renders the full-page error when the initial fetch rejects and no data has loaded', async () => {
    mockSearchAll.mockRejectedValue(new Error('boom'));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?query=foo'] } });

    expect(await screen.findByText('Error loading search: boom')).toBeInTheDocument();
    expect(screen.queryByText(/all-results/)).not.toBeInTheDocument();
  });

  it('renders inline Callout error after a prior successful fetch and keeps prior results visible', async () => {
    mockSearchAll
      .mockResolvedValueOnce(makeAllResponse(5, 5))
      .mockRejectedValueOnce(new Error('boom'));

    function TriggerNavigate() {
      const navigate = useNavigate();
      return <button onClick={() => navigate('/search?query=bar')}>nav</button>;
    }

    render(
      <>
        <SearchPage />
        <TriggerNavigate />
      </>,
      { routerOptions: { initialEntries: ['/search?query=foo'] } }
    );

    await screen.findByText('all-results: 5');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'nav' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Error loading search: boom');
    // Prior tab data is preserved alongside the inline error.
    expect(screen.getByText('all-results: 5')).toBeInTheDocument();
  });

  it('Components tab: issues a parallel fetchGlobalSearchTotals for cross-tab badge counts', async () => {
    mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
    mockSearchComponents.mockResolvedValue(makeComponentsResponse(7, 7));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?tab=components'] } });

    await screen.findByText('components-results: 7');
    await waitFor(() => {
      expect(mockFetchGlobalSearchTotals).toHaveBeenCalled();
      expect(mockSearchComponents).toHaveBeenCalled();
    });
    // searchAll is reserved for the All tab — components/vulnerabilities tabs
    // should go through the memoized fetchGlobalSearchTotals helper instead.
    expect(mockSearchAll).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByText('cmp:3')).toBeInTheDocument();
    });
    expect(screen.getByText('vul:2')).toBeInTheDocument();
  });

  it('All tab: calls searchAll with default limit=25 and preserves user filters when no limit is in the URL', async () => {
    mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));

    render(<SearchPage />, {
      routerOptions: {
        initialEntries: ['/search?query=lodash&formats=npm&publishedWindow=30d'],
      },
    });

    await waitFor(() => {
      expect(mockSearchAll).toHaveBeenCalled();
    });
    const callArg = mockSearchAll.mock.calls[0]?.[0] as URLSearchParams;
    expect(callArg).toBeInstanceOf(URLSearchParams);
    expect(callArg.get('query')).toBe('lodash');
    expect(callArg.get('formats')).toBe('npm');
    expect(callArg.get('publishedWindow')).toBe('30d');
    expect(callArg.get('limit')).toBe('25');
  });

  it('Components tab: calls searchComponents with page URLSearchParams and fetchGlobalSearchTotals with the query for cross-tab totals', async () => {
    mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
    mockSearchComponents.mockResolvedValue(makeComponentsResponse(7, 7));

    render(<SearchPage />, {
      routerOptions: { initialEntries: ['/search?tab=components&query=axios'] },
    });

    await waitFor(() => {
      expect(mockSearchComponents).toHaveBeenCalled();
      expect(mockFetchGlobalSearchTotals).toHaveBeenCalled();
    });

    const componentsArg = mockSearchComponents.mock.calls[0]?.[0] as URLSearchParams;
    expect(componentsArg.get('query')).toBe('axios');
    expect(componentsArg.get('tab')).toBe('components');

    expect(mockFetchGlobalSearchTotals).toHaveBeenCalledWith('axios');
  });

  it('reads byType counts from the plural keys (components/vulnerabilities) returned by the backend', async () => {
    mockFetchGlobalSearchTotals.mockResolvedValue({
      hits: [], total: 0, offset: 0, limit: 1,
      aggregations: { byType: { components: 150, vulnerabilities: 2 } },
    } as SearchResponse);
    mockSearchComponents.mockResolvedValue(makeComponentsResponse(150, 0));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?tab=components&query=keycloak'] } });

    await waitFor(() => {
      expect(screen.getByText('cmp:150')).toBeInTheDocument();
      expect(screen.getByText('vul:2')).toBeInTheDocument();
    });
  });

  it('Vulnerabilities tab: calls fetchGlobalSearchTotals with the query for cross-tab totals', async () => {
    mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
    mockSearchVulnerabilities.mockResolvedValue(makeVulnerabilitiesResponse(3, 3));

    render(<SearchPage />, {
      routerOptions: { initialEntries: ['/search?tab=vulnerabilities&query=cve-2024'] },
    });

    await waitFor(() => {
      expect(mockSearchVulnerabilities).toHaveBeenCalled();
      expect(mockFetchGlobalSearchTotals).toHaveBeenCalled();
    });

    expect(mockFetchGlobalSearchTotals).toHaveBeenCalledWith('cve-2024');
  });

  it('does not render stale tab data after a tab switch (regression: @guide/ui-core 1.10 phantom cards)', async () => {
    // Repro setup: user is on the All tab with loaded results, then clicks the
    // Vulnerabilities tab. While the vulnerabilities fetch is in flight, tabData
    // still holds the All-tab response (component-shape hits, no vulnId). If
    // those stale hits are passed straight through to VulnerabilitiesResultsList,
    // every card renders with vulnId=undefined; in @guide/ui-core 1.10 the
    // duplicate `vuln-undefined` keys cause the unmounts to leak as orphan DOM,
    // producing the cards-with-`/vulnerability/undefined`-links bug.
    mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));
    mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
    let resolveVulnerabilities: (response: VulnerabilitySearchResponse) => void = () => {};
    mockSearchVulnerabilities.mockReturnValue(
      new Promise<VulnerabilitySearchResponse>((resolve) => { resolveVulnerabilities = resolve; })
    );

    function TriggerNavigate() {
      const navigate = useNavigate();
      return <button onClick={() => navigate('/search?query=foo&tab=vulnerabilities')}>nav</button>;
    }

    render(
      <>
        <SearchPage />
        <TriggerNavigate />
      </>,
      { routerOptions: { initialEntries: ['/search?query=foo'] } }
    );

    await screen.findByText('all-results: 5');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'nav' }));

    // While the vulnerabilities request is pending, the list must show the
    // loading skeletons — NOT the stale All-tab hits coerced into vulnerability
    // shape. The "5" here matches the All-tab hitCount, so seeing
    // "vulnerabilities-results: 5" would mean stale data leaked through.
    await screen.findByRole('status', { name: /loading-skeletons/i });
    expect(screen.queryByText('vulnerabilities-results: 5')).not.toBeInTheDocument();
    expect(screen.queryByText('all-results: 5')).not.toBeInTheDocument();

    resolveVulnerabilities(makeVulnerabilitiesResponse(3, 3));
    await screen.findByText('vulnerabilities-results: 3');
  });

  it('Components tab: filter changes do not retrigger fetchGlobalSearchTotals (cached by query)', async () => {
    // The point of routing cross-tab totals through the memoized helper is so
    // filter changes — which leave the query untouched — don't repeatedly
    // refetch the same totals payload. With a real cache in place, this is
    // verified at the searchBackend layer; here we just assert the page passes
    // the bare query (no filters) to the helper, which is what enables caching.
    mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
    mockSearchComponents.mockResolvedValue(makeComponentsResponse(2, 2));

    render(<SearchPage />, {
      routerOptions: { initialEntries: ['/search?tab=components&query=axios&formats=npm&severities=critical'] },
    });

    await screen.findByText('components-results: 2');

    // Helper receives only the query — filters are deliberately excluded so the
    // module-scope cache in searchBackend can dedupe across filter changes.
    expect(mockFetchGlobalSearchTotals).toHaveBeenCalledWith('axios');
    const totalsCalls = mockFetchGlobalSearchTotals.mock.calls;
    expect(totalsCalls.every(([arg]) => arg === 'axios')).toBe(true);
  });

  describe('zero-count facets from browse aggregations', () => {
    it('Components tab: zero-fills facet buckets that exist in browse cache but not in the search response', async () => {
      mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
      mockSearchComponents.mockResolvedValue({
        ...makeComponentsResponse(2, 2),
        aggregations: { byFormat: { npm: 2 }, byCategory: {}, bySeverity: {}, byLicense: {} },
      });
      mockFetchComponentBrowseAggregations.mockResolvedValue({
        byFormat: { npm: 100, maven: 50, pypi: 7 },
        byCategory: { Security: 12 },
        bySeverity: {},
        byLicense: { 'MIT': 80 },
      });

      render(<SearchPage />, {
        routerOptions: { initialEntries: ['/search?tab=components&query=foo&formats=npm'] },
      });

      await screen.findByText('byFormat.npm=2');
      // Zero-filled from browse cache.
      expect(screen.getByText('byFormat.maven=0')).toBeInTheDocument();
      expect(screen.getByText('byFormat.pypi=0')).toBeInTheDocument();
      expect(screen.getByText('byCategory.Security=0')).toBeInTheDocument();
      expect(screen.getByText('byLicense.MIT=0')).toBeInTheDocument();
    });

    it('Vulnerabilities tab: zero-fills facet buckets that exist in browse cache but not in the search response', async () => {
      mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
      mockSearchVulnerabilities.mockResolvedValue({
        ...makeVulnerabilitiesResponse(1, 1),
        aggregations: { byEcosystem: { maven: 1 }, bySeverity: { critical: 1 } },
      });
      mockFetchVulnerabilityBrowseAggregations.mockResolvedValue({
        byEcosystem: { maven: 100, npm: 50, pypi: 7 },
        bySeverity: { critical: 10, high: 25, medium: 30, low: 35 },
      });

      render(<SearchPage />, {
        routerOptions: { initialEntries: ['/search?tab=vulnerabilities&query=foo&severities=critical'] },
      });

      await screen.findByText('byEcosystem.maven=1');
      expect(screen.getByText('byEcosystem.npm=0')).toBeInTheDocument();
      expect(screen.getByText('byEcosystem.pypi=0')).toBeInTheDocument();
      expect(screen.getByText('bySeverity.high=0')).toBeInTheDocument();
      expect(screen.getByText('bySeverity.medium=0')).toBeInTheDocument();
      expect(screen.getByText('bySeverity.low=0')).toBeInTheDocument();
    });

    it('Security Events tab: zero-fills facet buckets that exist in browse cache but not in the search response', async () => {
      mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
      mockSearchSecurityEvents.mockResolvedValue({
        ...makeSecurityEventsResponse(1, 1),
        aggregations: { byEventSeverityCategory: { HIGH: 1 }, byEventThreatType: { MALWARE: 1 } },
      });
      mockFetchSecurityEventBrowseAggregations.mockResolvedValue({
        byEventSeverityCategory: { CRITICAL: 20, HIGH: 40, MEDIUM: 10 },
        byEventThreatType: { MALWARE: 30, VULNERABILITY: 25 },
      });

      render(<SearchPage />, {
        routerOptions: { initialEntries: ['/search?tab=securityEvents&query=foo&severities=HIGH'] },
      });

      await screen.findByText('byEventSeverityCategory.HIGH=1');
      expect(screen.getByText('byEventSeverityCategory.CRITICAL=0')).toBeInTheDocument();
      expect(screen.getByText('byEventSeverityCategory.MEDIUM=0')).toBeInTheDocument();
      expect(screen.getByText('byEventThreatType.VULNERABILITY=0')).toBeInTheDocument();
    });

    it('All tab: never fetches browse aggregations (per-type facet universe is intentionally out of scope)', async () => {
      mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));

      render(<SearchPage />, {
        routerOptions: { initialEntries: ['/search?query=foo'] },
      });

      await screen.findByText('all-results: 5');

      expect(mockFetchComponentBrowseAggregations).not.toHaveBeenCalled();
      expect(mockFetchVulnerabilityBrowseAggregations).not.toHaveBeenCalled();
    });

    it('Components tab: degrades gracefully to search-only aggregations when browse fetch fails', async () => {
      mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
      mockSearchComponents.mockResolvedValue({
        ...makeComponentsResponse(2, 2),
        aggregations: { byFormat: { npm: 2 } },
      });
      mockFetchComponentBrowseAggregations.mockResolvedValue(null);

      render(<SearchPage />, {
        routerOptions: { initialEntries: ['/search?tab=components&query=foo&formats=npm'] },
      });

      await screen.findByText('byFormat.npm=2');
      expect(screen.queryByText('byFormat.maven=0')).not.toBeInTheDocument();
    });
  });

  describe('Security Events tab', () => {
    it('always renders the Security Events tab (matching the ungated /security-events nav entry)', async () => {
      mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));

      render(<SearchPage />, { routerOptions: { initialEntries: ['/search?query=foo'] } });

      expect(await screen.findByText('se-tab-shown')).toBeInTheDocument();
    });

    it('queries the /security-events endpoint (not global search) and renders SE results', async () => {
      mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
      mockSearchSecurityEvents.mockResolvedValue(makeSecurityEventsResponse(3, 3));

      render(<SearchPage />, {
        routerOptions: { initialEntries: ['/search?tab=securityEvents&query=log4j'] },
      });

      await waitFor(() => {
        expect(screen.getByText('security-events-results: 3')).toBeInTheDocument();
      });

      expect(mockSearchSecurityEvents).toHaveBeenCalled();
      const seArg = mockSearchSecurityEvents.mock.calls[0]?.[0] as URLSearchParams;
      expect(seArg.get('query')).toBe('log4j');
      expect(seArg.get('tab')).toBe('securityEvents');
      // Other-tab badge counts still come from the shared global-search aggregation.
      expect(mockFetchGlobalSearchTotals).toHaveBeenCalledWith('log4j');
    });

    it('shows the SE tab badge as the fully-filtered SE total, other badges from byType', async () => {
      mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
      mockSearchSecurityEvents.mockResolvedValue(makeSecurityEventsResponse(42, 5));

      render(<SearchPage />, {
        routerOptions: { initialEntries: ['/search?tab=securityEvents&query=foo'] },
      });

      await waitFor(() => {
        expect(screen.getByText('sec:42')).toBeInTheDocument();
        expect(screen.getByText('cmp:3')).toBeInTheDocument();
        expect(screen.getByText('vul:2')).toBeInTheDocument();
      });
    });

    it('renders the empty state when the SE tab has no results', async () => {
      mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(0, 0));
      mockSearchSecurityEvents.mockResolvedValue(makeSecurityEventsResponse(0, 0));

      render(<SearchPage />, {
        routerOptions: { initialEntries: ['/search?tab=securityEvents&query=nope'] },
      });

      expect(await screen.findByText('no results')).toBeInTheDocument();
    });

    it('does not render stale tab data after switching onto the Security Events tab', async () => {
      // Mirrors the Vulnerabilities-tab stale-data regression: on the All tab with loaded
      // component-shape hits, switching to securityEvents while the SE fetch is in flight must
      // show skeletons, not the prior tab's hits coerced into SE shape (which would render as
      // security-event cards with undefined eventId/title).
      mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));
      mockFetchGlobalSearchTotals.mockResolvedValue(makeAllResponse(5, 5));
      let resolveSecurityEvents: (response: ApiSearchResponse<SecurityEventDocument>) => void = () => {};
      mockSearchSecurityEvents.mockReturnValue(
        new Promise<ApiSearchResponse<SecurityEventDocument>>((resolve) => { resolveSecurityEvents = resolve; })
      );

      function TriggerNavigate() {
        const navigate = useNavigate();
        return <button onClick={() => navigate('/search?query=foo&tab=securityEvents')}>nav</button>;
      }

      render(
        <>
          <SearchPage />
          <TriggerNavigate />
        </>,
        { routerOptions: { initialEntries: ['/search?query=foo'] } }
      );

      await screen.findByText('all-results: 5');

      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: 'nav' }));

      // The "5" matches the All-tab hitCount; seeing "security-events-results: 5" would mean the
      // stale All-tab hits leaked into the SE list instead of being treated as pending.
      await screen.findByRole('status', { name: /loading-skeletons/i });
      expect(screen.queryByText('security-events-results: 5')).not.toBeInTheDocument();
      expect(screen.queryByText('all-results: 5')).not.toBeInTheDocument();

      resolveSecurityEvents(makeSecurityEventsResponse(3, 3));
      await screen.findByText('security-events-results: 3');
    });
  });
});
