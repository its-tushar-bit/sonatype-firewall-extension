/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useNavigate } from 'react-router';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from '../test-utils';
import { ComponentsSearchPage } from 'GuideRoot/components/ComponentsSearchPage';
import type { ComponentSearchResponse } from '@guide/ui-core/types';

jest.mock('GuideRoot/api/componentsBackend', () => ({
  searchComponents: jest.fn(),
}));

jest.mock('@guide/ui-core', () => {
  const actual = jest.requireActual('@guide/ui-core');
  return {
    ...actual,
    PageLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    FilteredPageLayout: ({ children, header }: { children: React.ReactNode; header: React.ReactNode }) => (
      <>{header}{children}</>
    ),
    ComponentsHeader: ({ total }: { total: number }) => <p>Results: {total}</p>,
    ComponentsResultsList: ({ isPending, components }: { isPending: boolean; components: unknown[] }) =>
      isPending ? <p role="status" aria-label="loading-skeletons" /> : <p>component count: {components.length}</p>,
    Pagination: () => <p>pagination-visible</p>,
    EmptyComponentsResults: () => <p>no results</p>,
  };
});

import { searchComponents } from 'GuideRoot/api/componentsBackend';

const mockSearchComponents = searchComponents as jest.MockedFunction<typeof searchComponents>;

function makeMockResponse(total: number, hitCount = 25): ComponentSearchResponse {
  return {
    hits: Array.from({ length: hitCount }, (_, i) => ({
      format: 'npm',
      originId: `pkg:npm/test-${i}@1.0.0`,
      name: `test-package-${i}`,
      version: '1.0.0',
      registryLink: `https://npmjs.com/package/test-${i}`,
      licenses: [],
    })),
    total,
    offset: 0,
    limit: 25,
    aggregations: { byFormat: { npm: total }, byCategory: {}, bySeverity: { none: total }, byLicense: {} },
  };
}

describe('ComponentsSearchPage', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows loading skeletons on initial render before the fetch resolves', () => {
    mockSearchComponents.mockReturnValue(new Promise(() => {}));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    expect(screen.getByRole('status', { name: /loading-skeletons/i })).toBeInTheDocument();
    expect(screen.queryByText('pagination-visible')).not.toBeInTheDocument();
  });

  it('renders results and pagination after fetch resolves when total > 25', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(30));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    await screen.findByText('Results: 30');
    expect(screen.getByText('pagination-visible')).toBeInTheDocument();
    expect(screen.queryByRole('status', { name: /loading/i })).not.toBeInTheDocument();
  });

  it('hides pagination when total does not exceed 25', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(10, 10));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    await screen.findByText('Results: 10');
    expect(screen.queryByText('pagination-visible')).not.toBeInTheDocument();
  });

  it('re-fetches when URL search params change', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(30));

    function NavHelper() {
      const navigate = useNavigate();
      return <button onClick={() => navigate('/components?query=lodash')}>change-url</button>;
    }

    const user = userEvent.setup();
    render(
      <>
        <NavHelper />
        <ComponentsSearchPage />
      </>,
      { routerOptions: { initialEntries: ['/components'] } }
    );

    await screen.findByText('Results: 30');
    expect(mockSearchComponents).toHaveBeenCalledTimes(1);

    mockSearchComponents.mockResolvedValue(makeMockResponse(5, 5));
    await user.click(screen.getByRole('button', { name: 'change-url' }));

    await waitFor(() => {
      expect(mockSearchComponents).toHaveBeenCalledTimes(2);
    });
    await screen.findByText('Results: 5');
  });

  it('renders an error message when the fetch rejects', async () => {
    mockSearchComponents.mockRejectedValue(new Error('Network error'));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    await screen.findByText(/Error loading components:.*Network error/i);
    expect(screen.queryByText(/Results:/)).not.toBeInTheDocument();
  });

  it('passes query param from URL to searchComponents', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(3, 3));

    render(<ComponentsSearchPage />, {
      routerOptions: { initialEntries: ['/components?query=lodash'] },
    });

    await screen.findByText('Results: 3');

    expect(mockSearchComponents).toHaveBeenCalledWith(
      expect.objectContaining({ query: 'lodash' })
    );
  });

  it('renders empty state when search returns no results', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(0, 0));

    render(<ComponentsSearchPage />, { routerOptions: { initialEntries: ['/components'] } });

    await screen.findByText('no results');
    expect(screen.queryByText('pagination-visible')).not.toBeInTheDocument();
  });

  it('passes offset param from URL to searchComponents', async () => {
    mockSearchComponents.mockResolvedValue(makeMockResponse(30));

    render(<ComponentsSearchPage />, {
      routerOptions: { initialEntries: ['/components?offset=25'] },
    });

    await screen.findByText('Results: 30');

    expect(mockSearchComponents).toHaveBeenCalledWith(
      expect.objectContaining({
        options: expect.objectContaining({ offset: 25 }),
      })
    );
  });
});
