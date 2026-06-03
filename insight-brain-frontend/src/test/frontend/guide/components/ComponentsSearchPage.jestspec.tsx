/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useNavigate } from 'react-router';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from '../test-utils';
import { ComponentsSearchPage } from 'GuideRoot/components/ComponentsSearchPage';
import type { ComponentSearchResponse } from '@guide/ui-core/types';

jest.mock('GuideRoot/api/componentsBackend', () => ({
  searchComponents: jest.fn(),
  fetchComponentBrowseAggregations: jest.fn().mockResolvedValue(null),
}));

type FacetAggregations = Record<string, Record<string, number>>;

jest.mock('GuideRoot/utils/navigation', () => ({
  reloadPage: jest.fn(),
  clearErrorRetries: jest.fn(),
  getErrorRetryCount: jest.fn().mockReturnValue(0),
}));

jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    PageLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    FilteredPageLayout: ({ children, header, aggregations }: { children: React.ReactNode; header: React.ReactNode; aggregations?: FacetAggregations }) => (
      <>
        {header}
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
    ComponentsHeader: ({ total }: { total: number }) => <p>Results: {total}</p>,
    ComponentsResultsList: ({ isPending, components }: { isPending: boolean; components: unknown[] }) =>
      isPending ? <p role="status" aria-label="loading-skeletons" /> : <p>component count: {components.length}</p>,
    Pagination: () => <p>pagination-visible</p>,
    EmptyComponentsResults: () => <p>no results</p>,
  };
});

import { searchComponents, fetchComponentBrowseAggregations } from 'GuideRoot/api/componentsBackend';
import { reloadPage } from 'GuideRoot/utils/navigation';

const mockSearchComponents = searchComponents as jest.MockedFunction<typeof searchComponents>;
const mockFetchComponentBrowseAggregations = fetchComponentBrowseAggregations as jest.MockedFunction<
  typeof fetchComponentBrowseAggregations
>;

function makeMockResponse(total: number, hitCount = 25): ComponentSearchResponse {
  return {
    hits: Array.from({ length: hitCount }, (_, i) => ({
      format: 'npm',
      originId: `pkg:npm/test-${i}@1.0.0`,
      name: `test-package-${i}`,
      version: '1.0.0',
      registryLink: `https://npmjs.com/package/test-${i}`,
      licenses: [],
    })),
    total,
    offset: 0,
    limit: 25,
    aggregations: { byFormat: { npm: total }, byCategory: {}, bySeverity: { none: total }, byLicense: {} },
  };
}

describe('ComponentsSearchPage', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows full-page skeleton on initial load', () => {
    mockSearchComponents.mockReturnValue(new Promise(() => {}));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    expect(screen.getByRole('status', { name: /loading page content/i })).toBeInTheDocument();
    expect(screen.queryByText('pagination-visible')).not.toBeInTheDocument();
  });

  it('uses component card skeletons, not vulnerability card skeletons', () => {
    mockSearchComponents.mockReturnValue(new Promise(() => {}));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    expect(screen.getAllByTestId('skeleton-component').length).toBeGreaterThan(0);
    expect(screen.queryByTestId('skeleton-vulnerability')).not.toBeInTheDocument();
  });

  it('does not show page skeleton after data has loaded', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(3, 3));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    await screen.findByText('Results: 3');
    expect(screen.queryByRole('status', { name: /loading page content/i })).not.toBeInTheDocument();
  });

  it('renders results and pagination after fetch resolves when total > 25', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(30));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    await screen.findByText('Results: 30');
    expect(screen.getByText('pagination-visible')).toBeInTheDocument();
    expect(screen.queryByRole('status', { name: /loading/i })).not.toBeInTheDocument();
  });

  it('hides pagination when total does not exceed 25', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(10, 10));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    await screen.findByText('Results: 10');
    expect(screen.queryByText('pagination-visible')).not.toBeInTheDocument();
  });

  it('re-fetches when URL search params change', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(30));

    function NavHelper() {
      const navigate = useNavigate();
      return <button onClick={() => navigate('/components?query=lodash')}>change-url</button>;
    }

    const user = userEvent.setup();
    render(
      <>
        <NavHelper />
        <ComponentsSearchPage />
      </>,
      { routerOptions: { initialEntries: ['/components'] } }
    );

    await screen.findByText('Results: 30');
    expect(mockSearchComponents).toHaveBeenCalledTimes(1);

    mockSearchComponents.mockResolvedValue(makeMockResponse(5, 5));
    await user.click(screen.getByRole('button', { name: 'change-url' }));

    await waitFor(() => {
      expect(mockSearchComponents).toHaveBeenCalledTimes(2);
    });
    await screen.findByText('Results: 5');
  });

  it('renders an error message when the fetch rejects', async () => {
    mockSearchComponents.mockRejectedValue(new Error('Network error'));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();
    });

    expect(screen.getByText(/please try again/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    // Go back button is hidden when showGoBack=false
    expect(screen.queryByRole('button', { name: /go back/i })).not.toBeInTheDocument();
  });

  it('calls reloadPage when Retry is clicked', async () => {
    const user = userEvent.setup();
    const mockReloadPage = reloadPage as jest.Mock;
    mockSearchComponents.mockRejectedValue(new Error('Network error'));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /retry/i }));

    expect(mockReloadPage).toHaveBeenCalledTimes(1);
  });

  it('passes query param from URL to searchComponents', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(3, 3));

    render(<ComponentsSearchPage />, {
      routerOptions: { initialEntries: ['/components?query=lodash'] },
    });

    await screen.findByText('Results: 3');

    expect(mockSearchComponents).toHaveBeenCalledTimes(1);
    const callArg = mockSearchComponents.mock.calls[0][0] as URLSearchParams;
    expect(callArg.get('query')).toBe('lodash');
  });

  it('renders empty state when search returns no results', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(0, 0));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    await screen.findByText('no results');
    expect(screen.queryByText('pagination-visible')).not.toBeInTheDocument();
  });

  it('passes offset param from URL to searchComponents', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(30));

    render(<ComponentsSearchPage />, {
      routerOptions: { initialEntries: ['/components?offset=25'] },
    });

    await screen.findByText('Results: 30');

    expect(mockSearchComponents).toHaveBeenCalledTimes(1);
    const callArg = mockSearchComponents.mock.calls[0][0] as URLSearchParams;
    expect(callArg.get('offset')).toBe('25');
  });

  it('injects default limit=25 when URL omits ?limit=', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(0, 0));

    render(<ComponentsSearchPage />, {
      routerOptions: { initialEntries: ['/components'] },
    });

    await screen.findByText('no results');

    const callArg = mockSearchComponents.mock.calls[0][0] as URLSearchParams;
    expect(callArg.get('limit')).toBe('25');
  });

  it('honors a URL-supplied limit instead of overriding it', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(0, 0));

    render(<ComponentsSearchPage />, {
      routerOptions: { initialEntries: ['/components?limit=10'] },
    });

    await screen.findByText('no results');

    const callArg = mockSearchComponents.mock.calls[0][0] as URLSearchParams;
    expect(callArg.get('limit')).toBe('10');
  });

  describe('zero-count facets from browse aggregations', () => {
    it('renders facet buckets present in browse cache but missing from search response with count 0', async () => {
      // Search response only knows about npm; user is filtering down to it.
      mockSearchComponents.mockResolvedValue({
        ...makeMockResponse(2, 2),
        aggregations: {
          byFormat: { npm: 2 },
          byCategory: {},
          bySeverity: { none: 2 },
          byLicense: {},
        },
      });
      // Browse cache has the full facet universe (e.g. user has visited the page before).
      mockFetchComponentBrowseAggregations.mockResolvedValue({
        byFormat: { npm: 100, maven: 50, pypi: 7 },
        byCategory: { Security: 12 },
        bySeverity: { critical: 3, none: 100 },
        byLicense: { 'MIT': 80 },
      });

      render(<ComponentsSearchPage />, {
        routerOptions: { initialEntries: ['/components?formats=npm'] },
      });

      await screen.findByText('Results: 2');

      // Search-response counts win for facets that overlap.
      expect(screen.getByText('byFormat.npm=2')).toBeInTheDocument();
      // Facets only in browse cache get zero-filled into the sidebar.
      expect(screen.getByText('byFormat.maven=0')).toBeInTheDocument();
      expect(screen.getByText('byFormat.pypi=0')).toBeInTheDocument();
      expect(screen.getByText('byCategory.Security=0')).toBeInTheDocument();
      expect(screen.getByText('byLicense.MIT=0')).toBeInTheDocument();
    });

    it('falls back to search-only aggregations when the browse fetch fails', async () => {
      mockSearchComponents.mockResolvedValue({
        ...makeMockResponse(2, 2),
        aggregations: {
          byFormat: { npm: 2 },
          byCategory: {},
          bySeverity: { none: 2 },
          byLicense: {},
        },
      });
      mockFetchComponentBrowseAggregations.mockResolvedValue(null);

      render(<ComponentsSearchPage />, {
        routerOptions: { initialEntries: ['/components?formats=npm'] },
      });

      await screen.findByText('Results: 2');

      expect(screen.getByText('byFormat.npm=2')).toBeInTheDocument();
      // No browse cache, so no zero-filled buckets appear in the sidebar.
      expect(screen.queryByText('byFormat.maven=0')).not.toBeInTheDocument();
      expect(screen.queryByText('byFormat.pypi=0')).not.toBeInTheDocument();
    });
  });
});
