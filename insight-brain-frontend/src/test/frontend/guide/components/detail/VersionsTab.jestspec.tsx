/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { Route, Routes } from 'react-router';
import { Tabs } from '@radix-ui/themes';
import { render, screen, waitFor } from '../../test-utils';
import { VersionsTab } from 'GuideRoot/components/detail/VersionsTab';
import * as backend from 'GuideRoot/api/componentsBackend';
import { mockComponentDetail, mockVersions } from 'GuideRoot/api/mocks/mockComponentDetailData';
import { ComponentProvider } from '@guide/ui-core';

jest.mock('GuideRoot/api/componentsBackend', () => ({
  getComponentVersions: jest.fn(),
}));

jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    MobileFilterWrapper: ({ children }: { children: React.ReactNode }) => <div data-testid="filter-wrapper">{children}</div>,
    VersionsTable: ({ versions }: { versions: { version: string; versionScore?: number; maxCvss?: number; latestStable?: boolean }[] }) => (
      <table>
        <tbody>
          {versions.map((v) => (
            <tr key={v.version}>
              <td>{v.version}</td>
              {v.versionScore !== undefined && <td data-testid="trust-score">{v.versionScore}</td>}
              {v.maxCvss !== undefined && <td data-testid="cvss-score">{v.maxCvss}</td>}
              {v.latestStable && <td>Latest</td>}
            </tr>
          ))}
        </tbody>
      </table>
    ),
  };
});

const mockGetVersions = backend.getComponentVersions as jest.MockedFunction<typeof backend.getComponentVersions>;

function renderTab(versionsResponse = { hits: mockVersions, total: 3, offset: 0, limit: 25, aggregations: {} }) {
  mockGetVersions.mockResolvedValue(versionsResponse as any);
  return render(
    <Tabs.Root value="versions">
      <Routes>
        <Route
          path="/component/:ecosystem/:pkg/:version/versions"
          element={
            <ComponentProvider
              component={mockComponentDetail}
              vulnerabilityCount={2}
              versionsCount={versionsResponse.total}
              dependencyCount={2}
            >
              <VersionsTab />
            </ComponentProvider>
          }
        />
      </Routes>
    </Tabs.Root>,
    { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21/versions'] } }
  );
}

describe('VersionsTab', () => {
  it('shows skeleton while loading', () => {
    mockGetVersions.mockImplementation(() => new Promise(() => {}));
    render(
      <Tabs.Root value="versions">
        <Routes>
          <Route
            path="/component/:ecosystem/:pkg/:version/versions"
            element={
              <ComponentProvider component={mockComponentDetail} vulnerabilityCount={2} versionsCount={3} dependencyCount={2}>
                <VersionsTab />
              </ComponentProvider>
            }
          />
        </Routes>
      </Tabs.Root>,
      { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21/versions'] } }
    );
    expect(screen.getByTestId('tab-skeleton')).toBeInTheDocument();
    expect(screen.queryByTestId('filter-wrapper')).not.toBeInTheDocument();
  });

  it('renders version rows with correct data after loading', async () => {
    renderTab();
    await waitFor(() => {
      expect(screen.getByTestId('filter-wrapper')).toBeInTheDocument();
    });

    // Version strings
    expect(screen.getByText('4.17.21')).toBeInTheDocument();
    expect(screen.getByText('4.17.20')).toBeInTheDocument();

    // Trust scores from mockVersions
    expect(screen.getByText('78')).toBeInTheDocument();
    expect(screen.getByText('55')).toBeInTheDocument();

    // CVSS scores (4.17.19 and 4.17.20 both have 9.8)
    expect(screen.getByText('7.2')).toBeInTheDocument();
    expect(screen.getAllByText('9.8').length).toBeGreaterThanOrEqual(1);

    // Latest badge for stable version
    expect(screen.getByText('Latest')).toBeInTheDocument();
  });

  it('shows pagination when total exceeds limit', async () => {
    renderTab({ hits: mockVersions, total: 30, offset: 0, limit: 25, aggregations: {} });
    await waitFor(() => {
      expect(screen.getByTestId('filter-wrapper')).toBeInTheDocument();
    });
    expect(screen.getByRole('navigation')).toBeInTheDocument();
  });

  it('shows no pagination when total does not exceed limit', async () => {
    renderTab({ hits: mockVersions, total: 3, offset: 0, limit: 25, aggregations: {} });
    await waitFor(() => {
      expect(screen.getByTestId('filter-wrapper')).toBeInTheDocument();
    });
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
  });
});
