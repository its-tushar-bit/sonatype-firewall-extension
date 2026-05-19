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
import type { SearchResponse, ComponentSearchResponse, VulnerabilitySearchResponse } from '@guide/ui-core/types';

// Mock ResizeObserver (used by @guide/ui-core and @radix-ui components but not available in jsdom)
class MockResizeObserver {
  observe = jest.fn();
  unobserve = jest.fn();
  disconnect = jest.fn();
}
global.ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;

jest.mock('GuideRoot/api/searchBackend', () => ({
  searchAll: jest.fn(),
  searchComponents: jest.fn(),
  searchVulnerabilities: jest.fn(),
}));

jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    PageLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    FilteredPageLayout: ({ children, header, subheader }: { children: React.ReactNode; header: React.ReactNode; subheader?: React.ReactNode }) => (
      <>{header}{subheader}{children}</>
    ),
    SearchTabs: ({ activeTab, totalAll, totalComponents, totalVulnerabilities }: {
      activeTab: string; totalAll?: number; totalComponents?: number; totalVulnerabilities?: number;
    }) => (
      <div data-testid="search-tabs">
        <span>tab:{activeTab}</span>
        <span>all:{totalAll ?? 0}</span>
        <span>cmp:{totalComponents ?? 0}</span>
        <span>vul:{totalVulnerabilities ?? 0}</span>
      </div>
    ),
    SearchResultsList: ({ results, isPending }: { results: unknown[]; isPending: boolean }) =>
      isPending ? <p role="status" aria-label="loading-skeletons" /> : <p>all-results: {results.length}</p>,
    ComponentsResultsList: ({ components, isPending }: { components: unknown[]; isPending: boolean }) =>
      isPending ? <p role="status" aria-label="loading-skeletons" /> : <p>components-results: {components.length}</p>,
    VulnerabilitiesResultsList: ({ vulnerabilities, isPending }: { vulnerabilities: unknown[]; isPending: boolean }) =>
      isPending ? <p role="status" aria-label="loading-skeletons" /> : <p>vulnerabilities-results: {vulnerabilities.length}</p>,
    Pagination: () => <p>pagination-visible</p>,
    EmptyResultsCard: () => <p>no results</p>,
  };
});

import { searchAll, searchComponents, searchVulnerabilities } from 'GuideRoot/api/searchBackend';

const mockSearchAll = searchAll as jest.MockedFunction<typeof searchAll>;
const mockSearchComponents = searchComponents as jest.MockedFunction<typeof searchComponents>;
const mockSearchVulnerabilities = searchVulnerabilities as jest.MockedFunction<typeof searchVulnerabilities>;

function makeAllResponse(total: number, hitCount = 5): SearchResponse {
  return {
    hits: Array.from({ length: hitCount }, (_, i) => ({
      format: 'npm', originId: `c-${i}`, name: `c-${i}`, version: '1', registryLink: '', licenses: [],
    })) as SearchResponse['hits'],
    total, offset: 0, limit: 25,
    aggregations: { byType: { component: 3, vulnerability: 2 } },
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

describe('SearchPage', () => {
  afterEach(() => { jest.clearAllMocks(); });

  it('defaults to the All tab and renders SearchResultsList with all hits', async () => {
    mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?query=foo'] } });

    await screen.findByText('all-results: 5');
    expect(screen.getByText('tab:all')).toBeInTheDocument();
  });

  it('switches to ComponentsResultsList when ?tab=components', async () => {
    mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));
    mockSearchComponents.mockResolvedValue(makeComponentsResponse(7, 7));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?tab=components&query=foo'] } });

    await screen.findByText('components-results: 7');
    expect(screen.getByText('tab:components')).toBeInTheDocument();
    expect(screen.queryByText(/all-results/)).not.toBeInTheDocument();
  });

  it('switches to VulnerabilitiesResultsList when ?tab=vulnerabilities', async () => {
    mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));
    mockSearchVulnerabilities.mockResolvedValue(makeVulnerabilitiesResponse(3, 3));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?tab=vulnerabilities'] } });

    await screen.findByText('vulnerabilities-results: 3');
    expect(screen.getByText('tab:vulnerabilities')).toBeInTheDocument();
  });

  it('propagates query into the active-tab fetcher', async () => {
    mockSearchAll.mockResolvedValue(makeAllResponse(0, 0));
    mockSearchComponents.mockResolvedValue(makeComponentsResponse(0, 0));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?tab=components&query=axios'] } });

    await waitFor(() => {
      expect(mockSearchComponents).toHaveBeenCalled();
    });
    const callArgs = mockSearchComponents.mock.calls[0]?.[0];
    expect(callArgs?.query).toBe('axios');
  });

  it('renders the generic empty state when results are empty and not pending', async () => {
    mockSearchAll.mockResolvedValue(makeAllResponse(0, 0));
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

  it('always issues a parallel searchAll for tab totals regardless of active tab', async () => {
    mockSearchAll.mockResolvedValue(makeAllResponse(5, 5));
    mockSearchComponents.mockResolvedValue(makeComponentsResponse(7, 7));

    render(<SearchPage />, { routerOptions: { initialEntries: ['/search?tab=components'] } });

    await screen.findByText('components-results: 7');
    await waitFor(() => {
      expect(mockSearchAll).toHaveBeenCalled();
      expect(mockSearchComponents).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(screen.getByText('cmp:3')).toBeInTheDocument();
    });
    expect(screen.getByText('vul:2')).toBeInTheDocument();
  });
});
