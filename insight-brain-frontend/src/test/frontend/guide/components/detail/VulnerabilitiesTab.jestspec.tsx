/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { Route, Routes } from 'react-router';
import { Tabs } from '@radix-ui/themes';
import { render, screen, waitFor } from '../../test-utils';
import { VulnerabilitiesTab } from 'GuideRoot/components/detail/VulnerabilitiesTab';
import * as backend from 'GuideRoot/api/componentsBackend';
import { mockComponentDetail, mockVulnerabilities } from 'GuideRoot/api/mocks/mockComponentDetailData';
import { ComponentProvider } from '@guide/ui-core';

jest.mock('GuideRoot/api/componentsBackend', () => ({
  getComponentVulnerabilities: jest.fn(),
}));

jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    MobileFilterWrapper: ({ children }: { children: React.ReactNode }) => <div data-testid="filter-wrapper">{children}</div>,
  };
});

const mockGetVulns = backend.getComponentVulnerabilities as jest.MockedFunction<typeof backend.getComponentVulnerabilities>;

function renderTab(vulnResponse = { hits: mockVulnerabilities, total: 2, offset: 0, limit: 25, aggregations: {} }) {
  mockGetVulns.mockResolvedValue(vulnResponse as any);
  return render(
    <Tabs.Root value="vulnerabilities">
      <Routes>
        <Route
          path="/component/:ecosystem/:pkg/:version/vulnerabilities"
          element={
            <ComponentProvider
              component={mockComponentDetail}
              vulnerabilityCount={vulnResponse.total}
              versionsCount={3}
              dependencyCount={2}
            >
              <VulnerabilitiesTab />
            </ComponentProvider>
          }
        />
      </Routes>
    </Tabs.Root>,
    { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21/vulnerabilities'] } }
  );
}

describe('VulnerabilitiesTab', () => {
  it('shows skeleton while loading', () => {
    mockGetVulns.mockImplementation(() => new Promise(() => {}));
    render(
      <Tabs.Root value="vulnerabilities">
        <Routes>
          <Route
            path="/component/:ecosystem/:pkg/:version/vulnerabilities"
            element={
              <ComponentProvider component={mockComponentDetail} vulnerabilityCount={2} versionsCount={3} dependencyCount={2}>
                <VulnerabilitiesTab />
              </ComponentProvider>
            }
          />
        </Routes>
      </Tabs.Root>,
      { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21/vulnerabilities'] } }
    );
    expect(screen.getByTestId('tab-skeleton')).toBeInTheDocument();
    expect(screen.queryByTestId('filter-wrapper')).not.toBeInTheDocument();
  });

  it('renders vulnerability cards with correct data after loading', async () => {
    renderTab();
    await waitFor(() => {
      expect(screen.getByTestId('filter-wrapper')).toBeInTheDocument();
    });

    // CVE IDs
    expect(screen.getByText('CVE-2021-23337')).toBeInTheDocument();
    expect(screen.getByText('CVE-2020-8203')).toBeInTheDocument();

    // Summaries
    expect(screen.getByText(/command injection vulnerability in lodash/i)).toBeInTheDocument();
    expect(screen.getByText(/prototype pollution vulnerability in lodash/i)).toBeInTheDocument();

    // CVSS scores (sonatypeCvssSeverity)
    expect(screen.getByText('7.2')).toBeInTheDocument();
    expect(screen.getByText('7.4')).toBeInTheDocument();
  });

  it('shows empty state when no vulnerabilities', async () => {
    renderTab({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} });
    await waitFor(() => {
      expect(screen.getByText(/no known vulnerabilities/i)).toBeInTheDocument();
    });
  });

  it('shows pagination when total exceeds limit', async () => {
    renderTab({ hits: mockVulnerabilities, total: 30, offset: 0, limit: 25, aggregations: {} });
    await waitFor(() => {
      expect(screen.getByTestId('filter-wrapper')).toBeInTheDocument();
    });
    expect(screen.getByRole('navigation')).toBeInTheDocument();
  });
});
