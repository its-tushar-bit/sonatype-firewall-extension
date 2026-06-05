/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render as renderBase, screen, waitFor } from '../test-utils';
import { VulnerabilitiesPage } from 'GuideRoot/vulnerabilities/VulnerabilitiesPage';
import * as vulnerabilitiesBackend from 'GuideRoot/api/vulnerabilitiesBackend';
import * as featureFlagsApi from 'GuideRoot/feature-flags/featureFlagsApi';
import { FeatureFlagProvider } from 'GuideRoot/feature-flags/FeatureFlagProvider';
import { reloadPage } from 'GuideRoot/utils/navigation';

// Mock ResizeObserver (used by @guide/ui-core and @radix-ui components but not available in jsdom)
class MockResizeObserver {
  observe = jest.fn();
  unobserve = jest.fn();
  disconnect = jest.fn();
}
global.ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;

// Mock window.scrollTo (not implemented in jsdom)
window.scrollTo = jest.fn();

// Mock the vulnerabilities backend
jest.mock('GuideRoot/api/vulnerabilitiesBackend');
jest.mock('GuideRoot/utils/navigation', () => ({
  reloadPage: jest.fn(),
  clearErrorRetries: jest.fn(),
  getErrorRetryCount: jest.fn().mockReturnValue(0),
}));

type FacetAggregations = Record<string, Record<string, number>>;

// Mock FilteredPageLayout so we can assert on the aggregations prop the page hands
// off to the sidebar. Other ui-core exports stay real so the page's data flow runs.
jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    FilteredPageLayout: ({
      children,
      header,
      aggregations,
      hideSearch,
    }: {
      children: React.ReactNode;
      header: React.ReactNode;
      aggregations?: FacetAggregations;
      hideSearch?: boolean;
    }) => (
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
  };
});

const mockSearchVulnerabilities = vulnerabilitiesBackend.searchVulnerabilities as jest.MockedFunction<
  typeof vulnerabilitiesBackend.searchVulnerabilities
>;
const mockFetchVulnerabilityBrowseAggregations =
  vulnerabilitiesBackend.fetchVulnerabilityBrowseAggregations as jest.MockedFunction<
    typeof vulnerabilitiesBackend.fetchVulnerabilityBrowseAggregations
  >;

// Sample vulnerability data for tests
const mockVulnerabilityHit = {
  vulnId: 'CVE-2021-44228',
  aliases: ['GHSA-jfh8-c2jp-5v3q'],
  summary: 'Apache Log4j2 JNDI features do not protect against attacker controlled LDAP and other JNDI related endpoints.',
  cvssSeverity: 10.0,
  sonatypeCvssSeverity: 10.0,
  cwes: ['CWE-502', 'CWE-917'],
  affectedEcosystems: ['maven'],
  isMalware: false,
  kev: true,
  epss: 0.97,
  source: 'NVD',
  publishedAt: '2021-12-10T00:00:00Z',
  affectedComponentVersionsCount: 1247,
};

const mockSearchResponse = {
  hits: [mockVulnerabilityHit],
  total: 1,
  offset: 0,
  limit: 25,
  aggregations: {
    byEcosystem: { maven: 1 },
    bySeverity: { critical: 1, high: 0, medium: 0, low: 0, none: 0 },
    byKev: { true: 1, false: 0 },
    byMalware: { true: 0, false: 1 },
  },
};

const render = (ui: React.ReactElement, options?: Parameters<typeof renderBase>[1]) =>
  renderBase(<FeatureFlagProvider>{ui}</FeatureFlagProvider>, options);

describe('VulnerabilitiesPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Default mock implementation returns successful response
    mockSearchVulnerabilities.mockResolvedValue(mockSearchResponse);
    // Default: no browse cache, so the page degrades to search-only aggregations.
    mockFetchVulnerabilityBrowseAggregations.mockResolvedValue(null);
    // Default: guide-search flag is enabled (tests can override)
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue(['guide-search']);
  });

  describe('loading state', () => {
    it('shows full-page skeleton including sidebar on initial load', () => {
      mockSearchVulnerabilities.mockImplementation(() => new Promise(() => {}));

      render(<VulnerabilitiesPage />);

      expect(screen.getByRole('status', { name: /loading page content/i })).toBeInTheDocument();
    });

    it('uses vulnerability card skeletons, not component card skeletons', () => {
      mockSearchVulnerabilities.mockImplementation(() => new Promise(() => {}));

      render(<VulnerabilitiesPage />);

      expect(screen.getAllByTestId('skeleton-vulnerability').length).toBeGreaterThan(0);
      expect(screen.queryByTestId('skeleton-component')).not.toBeInTheDocument();
    });

    it('does not show page skeleton after data has loaded', async () => {
      render(<VulnerabilitiesPage />);

      await waitFor(() => {
        expect(screen.queryByRole('status', { name: /loading page content/i })).not.toBeInTheDocument();
      });
    });
  });

  describe('data loaded', () => {
    it('renders vulnerability cards after data loads', async () => {
      render(<VulnerabilitiesPage />);

      // Wait for the vulnerability card to appear
      await waitFor(() => {
        expect(screen.getByText('CVE-2021-44228')).toBeInTheDocument();
      });

      // Check that the vulnerability summary is displayed
      expect(screen.getByText(/Apache Log4j2 JNDI/)).toBeInTheDocument();
    });

    it('renders vulnerability links to details pages', async () => {
      render(<VulnerabilitiesPage />);

      await waitFor(() => {
        expect(screen.getByText('CVE-2021-44228')).toBeInTheDocument();
      });

      // The vulnerability card should be wrapped in a link to the details page
      const link = screen.getByRole('link', { name: /CVE-2021-44228/i });
      expect(link).toHaveAttribute('href', '/vulnerability/CVE-2021-44228');
    });

    it('renders pagination controls when total exceeds limit', async () => {
      // Mock response with multiple pages of results
      mockSearchVulnerabilities.mockResolvedValue({
        ...mockSearchResponse,
        total: 50,
        limit: 25,
      });

      render(<VulnerabilitiesPage />);

      await waitFor(() => {
        expect(screen.getByText('CVE-2021-44228')).toBeInTheDocument();
      });

      // Pagination should be visible (checking for pagination buttons)
      // The Pagination component renders Next/Previous buttons
      expect(screen.getByRole('button', { name: /next/i })).toBeInTheDocument();
    });
  });

  describe('error handling', () => {
    it('handles error state gracefully', async () => {
      mockSearchVulnerabilities.mockRejectedValue(new Error('Network error'));

      render(<VulnerabilitiesPage />);

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
      mockSearchVulnerabilities.mockRejectedValue(new Error('Network error'));

      render(<VulnerabilitiesPage />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /retry/i }));

      expect(mockReloadPage).toHaveBeenCalledTimes(1);
    });
  });

  describe('empty state', () => {
    it('renders empty state when no vulnerabilities match filters', async () => {
      mockSearchVulnerabilities.mockResolvedValue({
        ...mockSearchResponse,
        hits: [],
        total: 0,
      });

      render(<VulnerabilitiesPage />);

      await waitFor(() => {
        expect(screen.getByText(/no vulnerabilities found/i)).toBeInTheDocument();
      });

      expect(screen.getByText(/try adjusting your filters/i)).toBeInTheDocument();
    });

    it('renders reset filters button in empty state', async () => {
      mockSearchVulnerabilities.mockResolvedValue({
        ...mockSearchResponse,
        hits: [],
        total: 0,
      });

      render(<VulnerabilitiesPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /reset filters/i })).toBeInTheDocument();
      });
    });
  });

  describe('calls searchVulnerabilities with correct URL params', () => {
    it('calls searchVulnerabilities with default limit=25 when no limit in URL', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: { initialEntries: ['/'] },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0] as URLSearchParams;
      expect(callArgs.get('limit')).toBe('25');
    });

    it('passes query param from URL', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: { initialEntries: ['/?query=log4j'] },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0] as URLSearchParams;
      expect(callArgs.get('query')).toBe('log4j');
    });

    it('passes filter params from URL verbatim', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: {
          initialEntries: ['/?severities=critical&severities=high&affectedEcosystems=maven'],
        },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0] as URLSearchParams;
      expect(callArgs.getAll('severities')).toEqual(['critical', 'high']);
      expect(callArgs.get('affectedEcosystems')).toBe('maven');
    });

    it('passes pagination params from URL', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: { initialEntries: ['/?offset=25&limit=10'] },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0] as URLSearchParams;
      expect(callArgs.get('offset')).toBe('25');
      expect(callArgs.get('limit')).toBe('10');
    });

    it('passes sort params from URL', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: { initialEntries: ['/?sort=sonatypeCvssSeverity:desc'] },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0] as URLSearchParams;
      expect(callArgs.get('sort')).toBe('sonatypeCvssSeverity:desc');
    });

    it('passes combined params from URL', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: {
          initialEntries: ['/?query=log4j&severities=critical&offset=0&limit=10&sort=publishedDate:desc'],
        },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0] as URLSearchParams;
      expect(callArgs.get('query')).toBe('log4j');
      expect(callArgs.getAll('severities')).toEqual(['critical']);
      expect(callArgs.get('offset')).toBe('0');
      expect(callArgs.get('limit')).toBe('10');
      expect(callArgs.get('sort')).toBe('publishedDate:desc');
    });

    it('strips query from API call when GUIDE_SEARCH is disabled', async () => {
      jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue([]);

      render(<VulnerabilitiesPage />, {
        routerOptions: { initialEntries: ['/?query=log4j'] },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0] as URLSearchParams;
      expect(callArgs.get('query')).toBeNull();
    });
  });

  describe('zero-count facets from browse aggregations', () => {
    it('renders facet buckets present in browse cache but missing from search response with count 0', async () => {
      // User filtered down to severities=critical — search returns only the critical bucket.
      mockSearchVulnerabilities.mockResolvedValue({
        ...mockSearchResponse,
        aggregations: {
          byEcosystem: { maven: 1 },
          bySeverity: { critical: 1 },
          byKev: { true: 1 },
          byMalware: { false: 1 },
        },
      });
      mockFetchVulnerabilityBrowseAggregations.mockResolvedValue({
        byEcosystem: { maven: 100, npm: 50, pypi: 7 },
        bySeverity: { critical: 10, high: 25, medium: 30, low: 35 },
        byKev: { true: 5, false: 95 },
        byMalware: { true: 2, false: 98 },
      });

      render(<VulnerabilitiesPage />, {
        routerOptions: { initialEntries: ['/?severities=critical'] },
      });

      await waitFor(() => {
        expect(mockFetchVulnerabilityBrowseAggregations).toHaveBeenCalled();
      });

      // Search-response counts win where they overlap with the browse cache.
      expect(await screen.findByText('byEcosystem.maven=1')).toBeInTheDocument();
      expect(screen.getByText('bySeverity.critical=1')).toBeInTheDocument();
      // Facets only present in the browse cache get zero-filled into the sidebar.
      expect(screen.getByText('byEcosystem.npm=0')).toBeInTheDocument();
      expect(screen.getByText('byEcosystem.pypi=0')).toBeInTheDocument();
      expect(screen.getByText('bySeverity.high=0')).toBeInTheDocument();
      expect(screen.getByText('bySeverity.medium=0')).toBeInTheDocument();
      expect(screen.getByText('bySeverity.low=0')).toBeInTheDocument();
    });

    it('falls back to search-only aggregations when the browse fetch fails (graceful degradation)', async () => {
      mockSearchVulnerabilities.mockResolvedValue({
        ...mockSearchResponse,
        aggregations: {
          byEcosystem: { maven: 1 },
          bySeverity: { critical: 1 },
          byKev: { true: 1 },
          byMalware: { false: 1 },
        },
      });
      mockFetchVulnerabilityBrowseAggregations.mockResolvedValue(null);

      render(<VulnerabilitiesPage />, {
        routerOptions: { initialEntries: ['/?severities=critical'] },
      });

      await waitFor(() => {
        expect(mockFetchVulnerabilityBrowseAggregations).toHaveBeenCalled();
      });

      expect(await screen.findByText('byEcosystem.maven=1')).toBeInTheDocument();
      // Without a browse cache, no extra zero-filled buckets show up.
      expect(screen.queryByText('byEcosystem.npm=0')).not.toBeInTheDocument();
      expect(screen.queryByText('bySeverity.high=0')).not.toBeInTheDocument();
    });
  });
});
