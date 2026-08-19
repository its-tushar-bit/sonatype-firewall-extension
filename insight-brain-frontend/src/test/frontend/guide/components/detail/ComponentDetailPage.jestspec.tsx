/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { Route, Routes, useSearchParams } from 'react-router';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from '../../test-utils';
import { ComponentDetailPage } from 'GuideRoot/components/detail/ComponentDetailPage';
import * as backend from 'GuideRoot/api/componentsBackend';
import { mockComponentDetail } from 'TestRoot/guide/api/fixtures/componentDetailFixtures';

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
  getRecommendations: jest.fn(),
}));

jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    PageLayout: ({ children }: { children: React.ReactNode }) => <div data-testid="page-layout">{children}</div>,
    ComponentTabsLayout: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    ComponentDetailsHeader: ({ component, recommendationsResponse }: { component: { name: string; version: string }; recommendationsResponse: unknown }) => (
      <h1 data-testid="component-header" data-has-recommendations={recommendationsResponse !== null ? 'true' : 'false'}>{component.name} {component.version}</h1>
    ),
    MalwareBanner: () => null,
    ComponentProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    Breadcrumbs: ({ items }: { items: Array<{ label: string }> }) => (
      <nav>{items.map((i) => <span key={i.label}>{i.label}</span>)}</nav>
    ),
    ArtifactPendingProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    useSetArtifactPending: () => jest.fn(),
  };
});

const mockGetComponentDetail = backend.getComponentDetail as jest.MockedFunction<typeof backend.getComponentDetail>;
const mockGetComponentVulnerabilities = backend.getComponentVulnerabilities as jest.MockedFunction<typeof backend.getComponentVulnerabilities>;
const mockGetComponentVersions = backend.getComponentVersions as jest.MockedFunction<typeof backend.getComponentVersions>;
const mockGetComponentDependencies = backend.getComponentDependencies as jest.MockedFunction<typeof backend.getComponentDependencies>;
const mockGetRecommendations = backend.getRecommendations as jest.MockedFunction<typeof backend.getRecommendations>;

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
    mockGetRecommendations.mockResolvedValue(null);
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

  it('passes recommendations to the header when available', async () => {
    const fakeRecommendations = {
      outcome: 'FOUND_RECOMMENDATIONS' as const,
      fromVersion: { version: '4.17.21' },
      toVersions: [],
    };
    mockGetComponentDetail.mockResolvedValue(mockComponentDetail);
    mockGetRecommendations.mockResolvedValue(fakeRecommendations as any);
    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId('component-header')).toBeInTheDocument();
    });

    expect(screen.getByTestId('component-header')).toHaveAttribute('data-has-recommendations', 'true');
  });

  it('passes null recommendations to the header when unavailable (e.g., 404 or backend error)', async () => {
    // getRecommendations swallows all errors and returns null — verified at the function level in
    // componentsBackend.jestspec.ts. This test asserts that the page renders the unavailable state
    // for that null, regardless of whether the underlying cause was a 404, 500, or network failure.
    mockGetComponentDetail.mockResolvedValue(mockComponentDetail);
    mockGetRecommendations.mockResolvedValue(null);
    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId('component-header')).toBeInTheDocument();
    });

    expect(screen.getByTestId('component-header')).toHaveAttribute('data-has-recommendations', 'false');
    expect(screen.queryByRole('heading', { name: /we hit a snag/i })).not.toBeInTheDocument();
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

  it('keeps the previous artifact view when an artifact-only refetch fails', async () => {
    function ArtifactSwitcher() {
      const [, setSearchParams] = useSearchParams();
      return (
        <button onClick={() => setSearchParams({ extension: 'jar' })}>switch-artifact</button>
      );
    }

    mockGetComponentDetail.mockResolvedValueOnce(mockComponentDetail);
    render(
      <Routes>
        <Route
          path="/component/:ecosystem/:pkg/:version"
          element={
            <>
              <ArtifactSwitcher />
              <ComponentDetailPage />
            </>
          }
        />
      </Routes>,
      { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21'] } }
    );

    await waitFor(() => {
      expect(screen.getByTestId('component-header')).toBeInTheDocument();
    });

    // The next fetch — triggered by the artifact-only transition — fails.
    mockGetComponentDetail.mockRejectedValueOnce(new Error('Network error'));
    await userEvent.click(screen.getByRole('button', { name: 'switch-artifact' }));

    await waitFor(() => {
      expect(mockGetComponentDetail).toHaveBeenCalledTimes(2);
    });

    // Previous artifact's header stays on screen; no full-page error.
    expect(screen.getByTestId('component-header')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /we hit a snag/i })).not.toBeInTheDocument();
  });

  it('renders the policy-context picker above the header after load', async () => {
    mockGetComponentDetail.mockResolvedValue(mockComponentDetail);
    renderPage();

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Policy context — open picker/ })).toBeInTheDocument()
    );
  });

  it('forwards extension and classifier from URL search params to all component API calls', async () => {
    mockGetComponentDetail.mockResolvedValue(mockComponentDetail);
    render(
      <Routes>
        <Route path="/component/:ecosystem/:pkg/:version" element={<ComponentDetailPage />} />
      </Routes>,
      { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21?extension=jar&classifier=sources'] } }
    );

    await waitFor(() => {
      expect(screen.getByTestId('component-header')).toBeInTheDocument();
    });

    const artifact = { extension: 'jar', classifier: 'sources' };
    expect(mockGetComponentDetail).toHaveBeenCalledWith('npm', 'lodash', '4.17.21', artifact);
    expect(mockGetComponentVulnerabilities).toHaveBeenCalledWith(
      'npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 1 }, artifact
    );
    expect(mockGetComponentVersions).toHaveBeenCalledWith(
      'npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 1 }, artifact
    );
    expect(mockGetComponentDependencies).toHaveBeenCalledWith(
      'npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 1 }, artifact
    );
    expect(mockGetRecommendations).toHaveBeenCalledWith('npm', 'lodash', '4.17.21', artifact);
  });

  it('passes undefined for extension and classifier when absent from URL search params', async () => {
    mockGetComponentDetail.mockResolvedValue(mockComponentDetail);
    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId('component-header')).toBeInTheDocument();
    });

    const artifact = { extension: undefined, classifier: undefined };
    expect(mockGetComponentDetail).toHaveBeenCalledWith('npm', 'lodash', '4.17.21', artifact);
    expect(mockGetRecommendations).toHaveBeenCalledWith('npm', 'lodash', '4.17.21', artifact);
  });
});
