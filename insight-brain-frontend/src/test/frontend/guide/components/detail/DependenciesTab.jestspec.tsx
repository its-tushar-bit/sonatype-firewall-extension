/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { Route, Routes } from 'react-router';
import { Tabs } from '@radix-ui/themes';
import { render, screen, waitFor } from '../../test-utils';
import { DependenciesTab } from 'GuideRoot/components/detail/DependenciesTab';
import * as backend from 'GuideRoot/api/componentsBackend';
import { mockComponentDetail, mockDependencies } from 'TestRoot/guide/api/fixtures/componentDetailFixtures';
import { ComponentProvider } from '@guide/ui-core';

jest.mock('GuideRoot/api/componentsBackend', () => ({
  getComponentDependencies: jest.fn(),
}));

jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    MobileFilterWrapper: ({ children }: { children: React.ReactNode }) => <div data-testid="filter-wrapper">{children}</div>,
    ComponentCard: ({ component }: { component: { name: string; version?: string; versionScore?: number } }) => (
      <div data-testid="component-card">
        <span>{component.name}</span>
        {component.version && <span>{component.version}</span>}
        {component.versionScore !== undefined && <span data-testid="dep-score">{component.versionScore}</span>}
      </div>
    ),
  };
});

const mockGetDeps = backend.getComponentDependencies as jest.MockedFunction<typeof backend.getComponentDependencies>;

function renderTab(depsResponse = { hits: mockDependencies, total: 2, offset: 0, limit: 25, aggregations: {} }) {
  mockGetDeps.mockResolvedValue(depsResponse as any);
  return render(
    <Tabs.Root value="dependencies">
      <Routes>
        <Route
          path="/component/:ecosystem/:pkg/:version/dependencies"
          element={
            <ComponentProvider
              component={mockComponentDetail}
              vulnerabilityCount={2}
              versionsCount={3}
              dependencyCount={depsResponse.total}
            >
              <DependenciesTab />
            </ComponentProvider>
          }
        />
      </Routes>
    </Tabs.Root>,
    { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21/dependencies'] } }
  );
}

describe('DependenciesTab', () => {
  it('shows skeleton while loading', () => {
    mockGetDeps.mockImplementation(() => new Promise(() => {}));
    render(
      <Tabs.Root value="dependencies">
        <Routes>
          <Route
            path="/component/:ecosystem/:pkg/:version/dependencies"
            element={
              <ComponentProvider component={mockComponentDetail} vulnerabilityCount={2} versionsCount={3} dependencyCount={2}>
                <DependenciesTab />
              </ComponentProvider>
            }
          />
        </Routes>
      </Tabs.Root>,
      { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21/dependencies'] } }
    );
    expect(screen.getByTestId('tab-skeleton')).toBeInTheDocument();
    expect(screen.queryByTestId('filter-wrapper')).not.toBeInTheDocument();
  });

  it('renders dependency cards with correct data after loading', async () => {
    renderTab();
    await waitFor(() => {
      expect(screen.getByTestId('filter-wrapper')).toBeInTheDocument();
    });

    expect(screen.getAllByTestId('component-card')).toHaveLength(2);

    // Names
    expect(screen.getByText('underscore')).toBeInTheDocument();
    expect(screen.getByText('moment')).toBeInTheDocument();

    // Versions
    expect(screen.getByText('1.13.6')).toBeInTheDocument();
    expect(screen.getByText('2.29.4')).toBeInTheDocument();

    // Trust scores
    const scores = screen.getAllByTestId('dep-score');
    expect(scores[0]).toHaveTextContent('82');
    expect(scores[1]).toHaveTextContent('70');
  });

  it('each dependency card links to its component detail URL', async () => {
    renderTab();
    await waitFor(() => {
      expect(screen.getAllByTestId('component-card')).toHaveLength(2);
    });
    const links = screen.getAllByRole('link');
    expect(links[0]).toHaveAttribute('href', expect.stringContaining('/component/npm/underscore/1.13.6'));
    expect(links[1]).toHaveAttribute('href', expect.stringContaining('/component/npm/moment/2.29.4'));
  });

  it('shows empty state when no dependencies', async () => {
    renderTab({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} });
    await waitFor(() => {
      expect(screen.getByText(/no dependencies found/i)).toBeInTheDocument();
    });
  });
});
