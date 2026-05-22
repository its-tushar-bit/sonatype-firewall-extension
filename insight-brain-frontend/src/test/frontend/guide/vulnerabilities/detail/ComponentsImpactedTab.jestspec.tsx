/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router';
import { Theme } from '@radix-ui/themes';
import { NavigationProvider } from '@guide/ui-core';
import { useReactRouterAdapter } from 'GuideRoot/reactRouterAdapter';
import { ComponentsImpactedTab } from 'GuideRoot/vulnerabilities/detail/ComponentsImpactedTab';
import * as vulnerabilitiesBackend from 'GuideRoot/api/vulnerabilitiesBackend';

jest.mock('GuideRoot/api/vulnerabilitiesBackend');

jest.mock('GuideRoot/auth/loginApi', () => ({
  fetchSession: jest.fn().mockResolvedValue({
    authenticated: true,
    user: { username: 'test', displayName: 'Test', groups: [] },
    sessionTimeoutMs: 1800000,
    ssoConfig: null,
  }),
  submitLogin: jest.fn(),
}));

function AdapterBridge({ children }: { children: React.ReactNode }) {
  const adapter = useReactRouterAdapter();
  return (
    <NavigationProvider adapter={adapter}>
      {children}
    </NavigationProvider>
  );
}

function renderWithRouter(ui: React.ReactElement, initialEntries: string[] = ['/']) {
  return require('@testing-library/react').render(ui, {
    wrapper: ({ children }) => (
      <MemoryRouter initialEntries={initialEntries}>
        <Theme appearance="dark" accentColor="indigo" panelBackground="solid">
          <AdapterBridge>
            <Routes>
              <Route path="/vulnerability/:vulnId/components-impacted" element={children} />
            </Routes>
          </AdapterBridge>
        </Theme>
      </MemoryRouter>
    ),
  });
}

const mockAffectedComponents = {
  hits: [
    { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.14.1', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1' },
    { ecosystem: 'maven', namespace: 'org.apache.logging.log4j', packageName: 'log4j-core', version: '2.14.0', fullPackageName: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0' },
  ],
  total: 2,
  offset: 0,
  limit: 50,
};

describe('ComponentsImpactedTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches and displays affected components', async () => {
    (vulnerabilitiesBackend.getVulnerabilityAffectedComponents as jest.Mock).mockResolvedValue(
      mockAffectedComponents
    );

    renderWithRouter(<ComponentsImpactedTab />, ['/vulnerability/CVE-2021-44228/components-impacted']);

    await waitFor(() => {
      // Verify API was called with correct params
      expect(vulnerabilitiesBackend.getVulnerabilityAffectedComponents).toHaveBeenCalledWith(
        'CVE-2021-44228',
        expect.objectContaining({
          offset: 0,
          sortField: 'packageName',
          sortOrder: 'asc',
        })
      );
      // Verify data actually rendered in the DOM (not just that API was called)
      expect(screen.getByText('2.14.1')).toBeInTheDocument();
      expect(screen.getByText('2.14.0')).toBeInTheDocument();
    });
  });

  it('handles empty affected components list', async () => {
    (vulnerabilitiesBackend.getVulnerabilityAffectedComponents as jest.Mock).mockResolvedValue({
      hits: [],
      total: 0,
      offset: 0,
      limit: 50,
    });

    renderWithRouter(<ComponentsImpactedTab />, ['/vulnerability/CVE-9999-9999/components-impacted']);

    await waitFor(() => {
      expect(vulnerabilitiesBackend.getVulnerabilityAffectedComponents).toHaveBeenCalled();
      // Verify the component handled empty data without crashing or showing an error
      expect(screen.queryByText(/Failed to load/i)).not.toBeInTheDocument();
      expect(document.querySelector('[aria-busy="true"]')).not.toBeInTheDocument();
    });
  });

  it('parses pagination offset from URL', async () => {
    (vulnerabilitiesBackend.getVulnerabilityAffectedComponents as jest.Mock).mockResolvedValue(
      mockAffectedComponents
    );

    renderWithRouter(<ComponentsImpactedTab />, ['/vulnerability/CVE-2021-44228/components-impacted?offset=10']);

    await waitFor(() => {
      expect(vulnerabilitiesBackend.getVulnerabilityAffectedComponents).toHaveBeenCalledWith(
        'CVE-2021-44228',
        expect.objectContaining({
          offset: 10,
        })
      );
    });
  });

  it('handles invalid offset parameter gracefully', async () => {
    (vulnerabilitiesBackend.getVulnerabilityAffectedComponents as jest.Mock).mockResolvedValue(
      mockAffectedComponents
    );

    renderWithRouter(<ComponentsImpactedTab />, ['/vulnerability/CVE-2021-44228/components-impacted?offset=abc']);

    await waitFor(() => {
      expect(vulnerabilitiesBackend.getVulnerabilityAffectedComponents).toHaveBeenCalledWith(
        'CVE-2021-44228',
        expect.objectContaining({
          offset: 0,
        })
      );
    });
  });

  it('clamps negative offset to 0', async () => {
    (vulnerabilitiesBackend.getVulnerabilityAffectedComponents as jest.Mock).mockResolvedValue(
      mockAffectedComponents
    );

    renderWithRouter(<ComponentsImpactedTab />, ['/vulnerability/CVE-2021-44228/components-impacted?offset=-5']);

    await waitFor(() => {
      expect(vulnerabilitiesBackend.getVulnerabilityAffectedComponents).toHaveBeenCalledWith(
        'CVE-2021-44228',
        expect.objectContaining({
          offset: 0,
        })
      );
    });
  });

  it('shows loading skeleton immediately on mount before API resolves', () => {
    (vulnerabilitiesBackend.getVulnerabilityAffectedComponents as jest.Mock).mockReturnValue(new Promise(() => {}));

    renderWithRouter(<ComponentsImpactedTab />, ['/vulnerability/CVE-2021-44228/components-impacted']);

    expect(document.querySelector('[aria-busy="true"]')).toBeInTheDocument();
  });

  it('handles API errors gracefully', async () => {
    (vulnerabilitiesBackend.getVulnerabilityAffectedComponents as jest.Mock).mockRejectedValue(
      new Error('Network error')
    );

    renderWithRouter(<ComponentsImpactedTab />, ['/vulnerability/CVE-2021-44228/components-impacted']);

    await waitFor(() => {
      expect(screen.getByText(/Failed to load affected components/i)).toBeInTheDocument();
    });
  });
});
