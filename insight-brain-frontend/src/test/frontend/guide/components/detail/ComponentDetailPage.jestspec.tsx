/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { Route, Routes } from 'react-router';
import { render, screen, waitFor } from '../../test-utils';
import { ComponentDetailPage } from 'GuideRoot/components/detail/ComponentDetailPage';
import * as backend from 'GuideRoot/api/componentsBackend';
import { mockComponentDetail } from 'GuideRoot/api/mocks/mockComponentDetailData';

jest.mock('GuideRoot/utils/navigation', () => ({
  reloadPage: jest.fn(),
  clearErrorRetries: jest.fn(),
  getErrorRetryCount: jest.fn().mockReturnValue(0),
}));

jest.mock('GuideRoot/api/componentsBackend', () => ({
  getComponentDetail: jest.fn(),
  getComponentVulnerabilities: jest.fn(),
  getComponentVersions: jest.fn(),
  getComponentDependencies: jest.fn(),
}));

jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    PageLayout: ({ children }: { children: React.ReactNode }) => <div data-testid="page-layout">{children}</div>,
    ComponentTabsLayout: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    ComponentDetailsHeader: ({ component }: { component: { name: string; version: string } }) => (
      <h1 data-testid="component-header">{component.name} {component.version}</h1>
    ),
    MalwareBanner: () => null,
    ComponentProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    Breadcrumbs: ({ items }: { items: Array<{ label: string }> }) => (
      <nav>{items.map((i) => <span key={i.label}>{i.label}</span>)}</nav>
    ),
  };
});

const mockGetComponentDetail = backend.getComponentDetail as jest.MockedFunction<typeof backend.getComponentDetail>;
const mockGetComponentVulnerabilities = backend.getComponentVulnerabilities as jest.MockedFunction<typeof backend.getComponentVulnerabilities>;
const mockGetComponentVersions = backend.getComponentVersions as jest.MockedFunction<typeof backend.getComponentVersions>;
const mockGetComponentDependencies = backend.getComponentDependencies as jest.MockedFunction<typeof backend.getComponentDependencies>;

const emptyCountResponse = { hits: [], total: 2, offset: 0, limit: 1, aggregations: {} };

function renderPage() {
  return render(
    <Routes>
      <Route path="/component/:ecosystem/:pkg/:version" element={<ComponentDetailPage />} />
    </Routes>,
    { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21'] } }
  );
}

describe('ComponentDetailPage', () => {
  beforeEach(() => {
    mockGetComponentVulnerabilities.mockResolvedValue(emptyCountResponse as any);
    mockGetComponentVersions.mockResolvedValue(emptyCountResponse as any);
    mockGetComponentDependencies.mockResolvedValue(emptyCountResponse as any);
  });

  it('shows skeleton while loading', () => {
    mockGetComponentDetail.mockImplementation(() => new Promise(() => {})); // never resolves
    renderPage();
    expect(screen.queryByTestId('component-header')).not.toBeInTheDocument();
  });

  it('renders header after successful load', async () => {
    mockGetComponentDetail.mockResolvedValue(mockComponentDetail);
    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId('component-header')).toBeInTheDocument();
    });

    expect(screen.getByTestId('component-header')).toHaveTextContent('lodash 4.17.21');
  });

  it('shows not-found state when component is null', async () => {
    mockGetComponentDetail.mockResolvedValue(null as any);
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /component not found/i })).toBeInTheDocument();
    });

    expect(screen.getByText(/please check the url and try again/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /go to home/i })).toHaveAttribute('href', '/');
  });

  it('shows error state when fetch rejects', async () => {
    mockGetComponentDetail.mockRejectedValue(new Error('Network error'));
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /we hit a snag/i })).toBeInTheDocument();
    });

    expect(screen.getByText(/please try again/i)).toBeInTheDocument();
    // Retry reloads the page — not a link
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /retry/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /go back/i })).toBeInTheDocument();
  });
});
