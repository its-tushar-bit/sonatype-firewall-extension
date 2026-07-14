/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';
import ViolationsPage from 'MainRoot/nosc/violations/ViolationsPage';
import { MOCK_VIOLATIONS_LIST_RESPONSE } from 'MainRoot/nosc/violations/mockViolationsListData';

const noop = () => {};

function renderPage(overrides: Partial<React.ComponentProps<typeof ViolationsPage>> = {}) {
  return renderNexusOneRoute(
    <ViolationsPage
      violations={MOCK_VIOLATIONS_LIST_RESPONSE.violations}
      facets={MOCK_VIOLATIONS_LIST_RESPONSE.facets}
      totalCount={MOCK_VIOLATIONS_LIST_RESPONSE.total}
      searchValue=""
      onSearchSubmit={noop}
      page={1}
      pageSize={25}
      onPageChange={noop}
      {...overrides}
    />,
    'nexusOneViolations',
  );
}

describe('ViolationsPage (CLM-42257)', () => {
  it('renders the loading skeleton when loading', () => {
    renderPage({ loading: true });
    expect(screen.getByTestId('violations-list-loading')).toBeInTheDocument();
  });

  it('renders an error banner with retry when error is set', async () => {
    const onRetry = jest.fn();
    renderPage({ error: 'Backend unavailable', onRetry });
    expect(screen.getByTestId('violations-list-error')).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('button', { name: /retry/i }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders the empty state when there are no violations', () => {
    renderPage({ violations: [], totalCount: 0, facets: { totalViolations: 0 } });
    expect(screen.getByTestId('violations-list-empty')).toBeInTheDocument();
    expect(screen.getByText('No violations to display.')).toBeInTheDocument();
  });

  it('shows a search-aware empty message when a committed search returns nothing', () => {
    renderPage({ violations: [], totalCount: 0, facets: { totalViolations: 0 }, searchValue: 'log4j' });
    expect(screen.getByTestId('violations-list-empty')).toBeInTheDocument();
    expect(screen.getByText('No violations match your search.')).toBeInTheDocument();
  });

  it('offers an AC4 "Clear search" action in the search-empty state that clears the term', async () => {
    const onSearchSubmit = jest.fn();
    renderPage({
      violations: [],
      totalCount: 0,
      facets: { totalViolations: 0 },
      searchValue: 'log4j',
      onSearchSubmit,
    });
    await userEvent.click(screen.getByTestId('violations-empty-clear-search'));
    expect(onSearchSubmit).toHaveBeenCalledWith('');
  });

  it('omits the "Clear search" action when the empty state is not from a search', () => {
    renderPage({ violations: [], totalCount: 0, facets: { totalViolations: 0 } });
    expect(screen.queryByTestId('violations-empty-clear-search')).not.toBeInTheDocument();
  });

  it('keeps pagination reachable on an out-of-range (empty) page beyond the first', () => {
    // page 2 with an empty result (e.g. shrunk total): the user must still be able to page back.
    renderPage({ violations: [], totalCount: 30, pageSize: 25, page: 2 });
    expect(screen.getByTestId('violations-list-empty')).toBeInTheDocument();
    expect(screen.getByTestId('violations-pagination')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /previous page/i })).toBeEnabled();
  });

  it('hides pagination on a single page and shows it when total exceeds a page', () => {
    const { unmount } = renderPage({ totalCount: 3, pageSize: 25 });
    expect(screen.queryByTestId('violations-pagination')).not.toBeInTheDocument();
    unmount();

    renderPage({ totalCount: 30, pageSize: 25 });
    expect(screen.getByTestId('violations-pagination')).toBeInTheDocument();
  });

  it('reflows the page offset when the LeftNav collapses and expands', async () => {
    window.localStorage.removeItem('nosc.leftnav.collapsed');
    renderPage();
    const pageMain = screen.getByTestId('preview-violations-page') as HTMLElement;
    expect(pageMain.style.left).toBe('256px');

    await act(async () => {
      window.dispatchEvent(
        new CustomEvent('nosc.leftnav.collapsed.change', { detail: { collapsed: true } }),
      );
    });
    expect(pageMain.style.left).toBe('64px');

    await act(async () => {
      window.dispatchEvent(
        new CustomEvent('nosc.leftnav.collapsed.change', { detail: { collapsed: false } }),
      );
    });
    expect(pageMain.style.left).toBe('256px');
  });
});
