/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '../../test-utils';
import { ComponentsImpactedTab } from 'GuideRoot/vulnerabilities/detail/ComponentsImpactedTab';
import * as vulnerabilitiesBackend from 'GuideRoot/api/vulnerabilitiesBackend';

class MockResizeObserver {
  observe = jest.fn();
  unobserve = jest.fn();
  disconnect = jest.fn();
}
global.ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;
window.scrollTo = jest.fn();

jest.mock('GuideRoot/api/vulnerabilitiesBackend');

const mockGetVulnerabilityAffectedComponents =
  vulnerabilitiesBackend.getVulnerabilityAffectedComponents as jest.MockedFunction<
    typeof vulnerabilitiesBackend.getVulnerabilityAffectedComponents
  >;

const ROUTES = [{ path: '/vulnerability/:vulnId/components-impacted' }];

function renderAtPath(path: string) {
  return render(<ComponentsImpactedTab />, {
    routerOptions: {
      initialEntries: [path],
      routes: ROUTES,
    },
  });
}

const mockResponse = {
  hits: [
    {
      ecosystem: 'maven',
      namespace: 'org.apache.logging.log4j',
      packageName: 'log4j-core',
      version: '2.14.1',
      fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1',
    },
    {
      ecosystem: 'maven',
      namespace: 'org.apache.logging.log4j',
      packageName: 'log4j-core',
      version: '2.14.0',
      fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0',
    },
  ],
  total: 2,
  offset: 0,
  limit: 50,
};

describe('ComponentsImpactedTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetVulnerabilityAffectedComponents.mockResolvedValue(mockResponse);
  });

  it('shows loading skeleton on initial load', () => {
    mockGetVulnerabilityAffectedComponents.mockImplementation(() => new Promise(() => {}));

    renderAtPath('/vulnerability/CVE-2021-44228/components-impacted');

    expect(document.querySelector('[aria-busy="true"]')).toBeInTheDocument();
  });

  it('renders the table after fetch succeeds', async () => {
    renderAtPath('/vulnerability/CVE-2021-44228/components-impacted');

    await waitFor(() => {
      expect(screen.getByText('2.14.1')).toBeInTheDocument();
      expect(screen.getByText('2.14.0')).toBeInTheDocument();
    });
    expect(screen.queryByText(/failed to load affected components/i)).not.toBeInTheDocument();
  });

  it('handles empty affected components list without error', async () => {
    mockGetVulnerabilityAffectedComponents.mockResolvedValue({
      hits: [],
      total: 0,
      offset: 0,
      limit: 50,
    });

    renderAtPath('/vulnerability/CVE-9999-9999/components-impacted');

    await waitFor(() => {
      expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalled();
      expect(screen.queryByText(/failed to load affected components/i)).not.toBeInTheDocument();
      expect(document.querySelector('[aria-busy="true"]')).not.toBeInTheDocument();
    });
  });

  it('shows error message when fetch fails', async () => {
    mockGetVulnerabilityAffectedComponents.mockRejectedValue(new Error('Network error'));

    renderAtPath('/vulnerability/CVE-2021-44228/components-impacted');

    await waitFor(() => {
      expect(
        screen.getByText(/failed to load affected components/i)
      ).toBeInTheDocument();
    });
  });

  describe('calls getVulnerabilityAffectedComponents with correct params', () => {
    it('passes the vulnId from URL params and default limit', async () => {
      renderAtPath('/vulnerability/CVE-2021-44228/components-impacted');

      await waitFor(() => {
        expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalled();
      });

      expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalledWith(
        'CVE-2021-44228',
        expect.objectContaining({ limit: 50 })
      );
    });

    it('passes pagination params from URL search params', async () => {
      renderAtPath('/vulnerability/CVE-2021-44228/components-impacted?offset=50&limit=25');

      await waitFor(() => {
        expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalled();
      });

      expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalledWith(
        'CVE-2021-44228',
        expect.objectContaining({ offset: 50, limit: 25 })
      );
    });

    it('clamps invalid offset (non-numeric) to 0', async () => {
      renderAtPath('/vulnerability/CVE-2021-44228/components-impacted?offset=abc');

      await waitFor(() => {
        expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalled();
      });

      expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalledWith(
        'CVE-2021-44228',
        expect.objectContaining({ offset: 0 })
      );
    });

    it('clamps negative offset to 0', async () => {
      renderAtPath('/vulnerability/CVE-2021-44228/components-impacted?offset=-5');

      await waitFor(() => {
        expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalled();
      });

      expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalledWith(
        'CVE-2021-44228',
        expect.objectContaining({ offset: 0 })
      );
    });

    it('passes sort params from URL search params', async () => {
      renderAtPath(
        '/vulnerability/CVE-2021-44228/components-impacted?sortField=version&sortOrder=desc'
      );

      await waitFor(() => {
        expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalled();
      });

      expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalledWith(
        'CVE-2021-44228',
        expect.objectContaining({ sortField: 'version', sortOrder: 'desc' })
      );
    });

    it('clamps an invalid sortField to "packageName"', async () => {
      renderAtPath(
        '/vulnerability/CVE-2021-44228/components-impacted?sortField=invalid&sortOrder=asc'
      );

      await waitFor(() => {
        expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalled();
      });

      expect(mockGetVulnerabilityAffectedComponents).toHaveBeenCalledWith(
        'CVE-2021-44228',
        expect.objectContaining({ sortField: 'packageName' })
      );
    });
  });
});
