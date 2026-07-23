/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { Route, Routes, useNavigate, Outlet } from 'react-router';
import userEvent from '@testing-library/user-event';
import { Tabs } from '@radix-ui/themes';
import { render, screen, waitFor } from '../../test-utils';
import { VulnerabilitiesTab } from 'GuideRoot/components/detail/VulnerabilitiesTab';
import * as backend from 'GuideRoot/api/componentsBackend';
import { mockComponentDetail, mockVulnerabilities } from 'TestRoot/guide/api/fixtures/componentDetailFixtures';
import { ComponentProvider } from '@guide/ui-core';
import type { ArtifactOutletContext } from 'GuideRoot/components/detail/ComponentDetailPage';

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
              <Outlet context={{ extension: undefined, classifier: undefined } satisfies ArtifactOutletContext} />
            </ComponentProvider>
          }
        >
          <Route index element={<VulnerabilitiesTab />} />
        </Route>
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
                <Outlet context={{ extension: undefined, classifier: undefined } satisfies ArtifactOutletContext} />
              </ComponentProvider>
            }
          >
            <Route index element={<VulnerabilitiesTab />} />
          </Route>
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

  it('keeps results mounted without a skeleton flash during a refetch', async () => {
    const user = userEvent.setup();
    mockGetVulns
      .mockResolvedValueOnce({ hits: mockVulnerabilities, total: 2, offset: 0, limit: 25, aggregations: {} } as any)
      .mockImplementationOnce(() => new Promise(() => {}));

    function FilterNav() {
      const navigate = useNavigate();
      return (
        <button type="button" onClick={() => navigate('/component/npm/lodash/4.17.21/vulnerabilities?query=CVE')}>
          do-filter
        </button>
      );
    }

    render(
      <Tabs.Root value="vulnerabilities">
        <Routes>
          <Route
            path="/component/:ecosystem/:pkg/:version/vulnerabilities"
            element={
              <ComponentProvider component={mockComponentDetail} vulnerabilityCount={2} versionsCount={3} dependencyCount={2}>
                <FilterNav />
                <Outlet context={{ extension: undefined, classifier: undefined } satisfies ArtifactOutletContext} />
              </ComponentProvider>
            }
          >
            <Route index element={<VulnerabilitiesTab />} />
          </Route>
        </Routes>
      </Tabs.Root>,
      { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21/vulnerabilities'] } }
    );

    await waitFor(() => {
      expect(screen.getByTestId('filter-wrapper')).toBeInTheDocument();
    });
    expect(screen.getByText('CVE-2021-23337')).toBeInTheDocument();

    await user.click(screen.getByText('do-filter'));
    await waitFor(() => {
      expect(mockGetVulns).toHaveBeenCalledTimes(2);
    });

    expect(screen.queryByTestId('tab-skeleton')).not.toBeInTheDocument();
    expect(screen.getByTestId('filter-wrapper')).toBeInTheDocument();
    expect(screen.getByText('CVE-2021-23337')).toBeInTheDocument();
  });
});
