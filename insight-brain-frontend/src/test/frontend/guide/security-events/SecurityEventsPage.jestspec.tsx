/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from '../test-utils';
import { SecurityEventsPage } from 'GuideRoot/security-events/SecurityEventsPage';
import * as securityEventsBackend from 'GuideRoot/api/securityEventsBackend';
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

jest.mock('GuideRoot/api/securityEventsBackend');
jest.mock('GuideRoot/utils/navigation', () => ({
  reloadPage: jest.fn(),
  clearErrorRetries: jest.fn(),
  getErrorRetryCount: jest.fn().mockReturnValue(0),
}));

type FacetAggregations = Record<string, Record<string, number>>;

// Mock FilteredPageLayout so we can assert on the aggregations prop handed to the sidebar.
// Other ui-core exports stay real so the page's data flow runs.
jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    FilteredPageLayout: ({
      children,
      header,
      aggregations,
    }: {
      children: React.ReactNode;
      header: React.ReactNode;
      aggregations?: FacetAggregations;
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

const mockSearch = securityEventsBackend.searchSecurityEvents as jest.MockedFunction<
  typeof securityEventsBackend.searchSecurityEvents
>;
const mockBrowse = securityEventsBackend.fetchSecurityEventBrowseAggregations as jest.MockedFunction<
  typeof securityEventsBackend.fetchSecurityEventBrowseAggregations
>;

const mockEventHit = {
  eventId: 'sonatype-2025-0001',
  title: 'Critical RCE in a popular npm package',
  overview: 'A remote code execution vulnerability affecting many downstream projects.',
  publishedDate: '2025-06-01T00:00:00Z',
  eventSeverityCategory: 'Critical',
  eventThreatType: 'VULNERABLE_OSS',
  isKnownExploited: true,
  affectedEcosystems: ['npm'],
} as unknown as Awaited<ReturnType<typeof securityEventsBackend.searchSecurityEvents>>['hits'][number];

const mockResponse = {
  hits: [mockEventHit],
  total: 1,
  offset: 0,
  limit: 25,
  aggregations: {
    byKnownExploited: { true: 1, false: 0 },
    byAffectedEcosystems: { npm: 1 },
    bySeverityCategory: { Critical: 1 },
    byThreatType: { VULNERABLE_OSS: 1 },
  },
};

describe('SecurityEventsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockSearch.mockResolvedValue(mockResponse);
    mockBrowse.mockResolvedValue(null);
  });

  describe('loading state', () => {
    it('shows the full-page skeleton on initial load', () => {
      mockSearch.mockImplementation(() => new Promise(() => {}));
      render(<SecurityEventsPage />);
      expect(screen.getByRole('status', { name: /loading page content/i })).toBeInTheDocument();
    });

    it('hides the skeleton after data loads', async () => {
      render(<SecurityEventsPage />);
      await waitFor(() => {
        expect(screen.queryByRole('status', { name: /loading page content/i })).not.toBeInTheDocument();
      });
    });
  });

  describe('data loaded', () => {
    it('renders event cards', async () => {
      render(<SecurityEventsPage />);
      await waitFor(() => {
        expect(screen.getByText('Critical RCE in a popular npm package')).toBeInTheDocument();
      });
    });

    it('links each card to the security event detail route', async () => {
      render(<SecurityEventsPage />);
      await waitFor(() => {
        expect(screen.getByText('Critical RCE in a popular npm package')).toBeInTheDocument();
      });
      const link = screen.getByRole('link', { name: /Critical RCE in a popular npm package/i });
      expect(link).toHaveAttribute('href', '/security-event/sonatype-2025-0001');
    });

    it('renders pagination when total exceeds limit', async () => {
      mockSearch.mockResolvedValue({ ...mockResponse, total: 50, limit: 25 });
      render(<SecurityEventsPage />);
      await waitFor(() => {
        expect(screen.getByText('Critical RCE in a popular npm package')).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: /next/i })).toBeInTheDocument();
    });
  });

  describe('empty state', () => {
    it('renders the empty state when no events match', async () => {
      mockSearch.mockResolvedValue({ ...mockResponse, hits: [], total: 0 });
      render(<SecurityEventsPage />);
      await waitFor(() => {
        expect(screen.getByText(/no security events found/i)).toBeInTheDocument();
      });
      expect(screen.getByText(/try adjusting your filters/i)).toBeInTheDocument();
    });
  });

  describe('error handling (air-gapped / unreachable search server)', () => {
    it('renders the error page instead of crashing', async () => {
      mockSearch.mockRejectedValue(new Error('Network error'));
      render(<SecurityEventsPage />);
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /go back/i })).not.toBeInTheDocument();
    });

    it('calls reloadPage when Retry is clicked', async () => {
      const user = userEvent.setup();
      mockSearch.mockRejectedValue(new Error('Network error'));
      render(<SecurityEventsPage />);
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();
      });
      await user.click(screen.getByRole('button', { name: /retry/i }));
      expect(reloadPage as jest.Mock).toHaveBeenCalledTimes(1);
    });
  });

  describe('URL params forwarded to searchSecurityEvents', () => {
    it('defaults limit=25 and publishedDate:desc sort', async () => {
      render(<SecurityEventsPage />, { routerOptions: { initialEntries: ['/'] } });
      await waitFor(() => {
        expect(mockSearch).toHaveBeenCalled();
      });
      const params = mockSearch.mock.calls[0][0] as URLSearchParams;
      expect(params.get('limit')).toBe('25');
      expect(params.get('sortField')).toBe('publishedDate');
      expect(params.get('sortOrder')).toBe('desc');
    });

    it('preserves an explicit "Oldest" sort (sortOrder=asc) from the URL', async () => {
      render(<SecurityEventsPage />, {
        routerOptions: { initialEntries: ['/?sortField=publishedDate&sortOrder=asc'] },
      });
      await waitFor(() => {
        expect(mockSearch).toHaveBeenCalled();
      });
      const params = mockSearch.mock.calls[0][0] as URLSearchParams;
      expect(params.get('sortField')).toBe('publishedDate');
      expect(params.get('sortOrder')).toBe('asc');
    });

    it('forwards filter params verbatim', async () => {
      render(<SecurityEventsPage />, {
        routerOptions: {
          initialEntries: [
            '/?severities=Critical&severities=High&threatTypes=MALICIOUS_OSS&knownExploited=true&affectedEcosystems=npm',
          ],
        },
      });
      await waitFor(() => {
        expect(mockSearch).toHaveBeenCalled();
      });
      const params = mockSearch.mock.calls[0][0] as URLSearchParams;
      expect(params.getAll('severities')).toEqual(['Critical', 'High']);
      expect(params.get('threatTypes')).toBe('MALICIOUS_OSS');
      expect(params.get('knownExploited')).toBe('true');
      expect(params.get('affectedEcosystems')).toBe('npm');
    });

    it('forwards pagination params', async () => {
      render(<SecurityEventsPage />, { routerOptions: { initialEntries: ['/?offset=25&limit=10'] } });
      await waitFor(() => {
        expect(mockSearch).toHaveBeenCalled();
      });
      const params = mockSearch.mock.calls[0][0] as URLSearchParams;
      expect(params.get('offset')).toBe('25');
      expect(params.get('limit')).toBe('10');
    });

    it('forwards the free-text query param', async () => {
      render(<SecurityEventsPage />, { routerOptions: { initialEntries: ['/?query=log4j'] } });
      await waitFor(() => {
        expect(mockSearch).toHaveBeenCalled();
      });
      const params = mockSearch.mock.calls[0][0] as URLSearchParams;
      expect(params.get('query')).toBe('log4j');
    });
  });

  describe('browse aggregations merge', () => {
    it('zero-fills facet buckets present only in the browse cache', async () => {
      mockSearch.mockResolvedValue({
        ...mockResponse,
        aggregations: { byAffectedEcosystems: { npm: 1 }, bySeverityCategory: { Critical: 1 } },
      });
      mockBrowse.mockResolvedValue({
        byAffectedEcosystems: { npm: 100, maven: 50, pypi: 7 },
        bySeverityCategory: { Critical: 10, High: 25, Medium: 30, Low: 35 },
      });
      render(<SecurityEventsPage />, { routerOptions: { initialEntries: ['/?severities=Critical'] } });
      await waitFor(() => {
        expect(mockBrowse).toHaveBeenCalled();
      });
      expect(await screen.findByText('byAffectedEcosystems.npm=1')).toBeInTheDocument();
      expect(screen.getByText('byAffectedEcosystems.maven=0')).toBeInTheDocument();
      expect(screen.getByText('bySeverityCategory.High=0')).toBeInTheDocument();
    });

    it('falls back to search-only aggregations when browse fetch returns null', async () => {
      mockSearch.mockResolvedValue({
        ...mockResponse,
        aggregations: { byAffectedEcosystems: { npm: 1 } },
      });
      mockBrowse.mockResolvedValue(null);
      render(<SecurityEventsPage />, { routerOptions: { initialEntries: ['/?severities=Critical'] } });
      await waitFor(() => {
        expect(mockBrowse).toHaveBeenCalled();
      });
      expect(await screen.findByText('byAffectedEcosystems.npm=1')).toBeInTheDocument();
      expect(screen.queryByText('byAffectedEcosystems.maven=0')).not.toBeInTheDocument();
    });
  });
});
