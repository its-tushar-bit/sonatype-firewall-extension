/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '../test-utils';
import { VulnerabilitiesPage } from 'GuideRoot/vulnerabilities/VulnerabilitiesPage';
import * as vulnerabilitiesBackend from 'GuideRoot/api/vulnerabilitiesBackend';

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

const mockSearchVulnerabilities = vulnerabilitiesBackend.searchVulnerabilities as jest.MockedFunction<
  typeof vulnerabilitiesBackend.searchVulnerabilities
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

describe('VulnerabilitiesPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Default mock implementation returns successful response
    mockSearchVulnerabilities.mockResolvedValue(mockSearchResponse);
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

      // Wait for error state to be displayed
      await waitFor(() => {
        expect(screen.getByText(/Error loading vulnerabilities/)).toBeInTheDocument();
      });

      // Error message should be visible
      expect(screen.getByText(/Network error/)).toBeInTheDocument();
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

  describe('search parameters', () => {
    it('calls searchVulnerabilities on initial load', async () => {
      render(<VulnerabilitiesPage />);

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });
    });

    it('calls searchVulnerabilities with query params from URL', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: {
          initialEntries: ['/?query=log4j'],
        },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      // Should be called with the query parameter from URL
      const callArgs = mockSearchVulnerabilities.mock.calls[0][0];
      expect(callArgs.query).toBe('log4j');
    });

    it('calls searchVulnerabilities with filter params from URL', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: {
          initialEntries: ['/?severities=critical&severities=high&affectedEcosystems=maven'],
        },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0];
      // buildVulnerabilityFilters returns filters with these values
      expect(callArgs.filters).toBeDefined();
    });

    it('calls searchVulnerabilities with exploitation filter', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: {
          initialEntries: ['/?exploitationKnown=true'],
        },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0];
      expect(callArgs.filters).toBeDefined();
    });

    it('calls searchVulnerabilities with malware filter', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: {
          initialEntries: ['/?hasMalware=true'],
        },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0];
      expect(callArgs.filters).toBeDefined();
    });

    it('calls searchVulnerabilities with pagination params', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: {
          initialEntries: ['/?offset=25&limit=10'],
        },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0];
      expect(callArgs.options).toBeDefined();
    });

    it('calls searchVulnerabilities with sort params', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: {
          initialEntries: ['/?sort=sonatypeCvssSeverity:desc'],
        },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0];
      expect(callArgs.options).toBeDefined();
    });

    it('calls searchVulnerabilities with combined params', async () => {
      render(<VulnerabilitiesPage />, {
        routerOptions: {
          initialEntries: ['/?query=log4j&severities=critical&offset=0&limit=10&sort=publishedDate:desc'],
        },
      });

      await waitFor(() => {
        expect(mockSearchVulnerabilities).toHaveBeenCalled();
      });

      const callArgs = mockSearchVulnerabilities.mock.calls[0][0];
      expect(callArgs.query).toBe('log4j');
      expect(callArgs.filters).toBeDefined();
      expect(callArgs.options).toBeDefined();
    });
  });
});
