/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, act, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';
import ViolationsPage from 'MainRoot/nosc/violations/ViolationsPage';
import { MOCK_VIOLATIONS_LIST_RESPONSE } from 'MainRoot/nosc/violations/mockViolationsListData';
import { createDefaultViolationsFilterState } from 'MainRoot/nosc/violations/violationsListApi';
import { ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

beforeAll(() => {
  // The filter rail renders Radix Slider + ScrollArea, and the mobile drawer a Radix Dialog.
  installRadixJsdomShims();
});

const noop = () => {};

function renderPage(overrides: Partial<React.ComponentProps<typeof ViolationsPage>> = {}) {
  return renderNexusOneRoute(
    <ViolationsPage
      violations={MOCK_VIOLATIONS_LIST_RESPONSE.violations}
      facets={MOCK_VIOLATIONS_LIST_RESPONSE.facets}
      filters={createDefaultViolationsFilterState()}
      onFilterToggle={noop}
      onWaiverTypeChange={noop}
      onThreatRangeChange={noop}
      onResetFilters={noop}
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

/** A filter selection with one active state, so the empty state / reset affordances light up. */
function activeFilters(): ViolationsFilterState {
  return { ...createDefaultViolationsFilterState(), states: new Set(['OPEN']) };
}

describe('ViolationsPage', () => {
  let user: ReturnType<typeof userEvent.setup>;

  beforeEach(() => {
    user = userEvent.setup();
  });

  it('renders the loading skeleton when loading', () => {
    renderPage({ loading: true });
    expect(screen.getByTestId('violations-list-loading')).toBeInTheDocument();
  });

  it('renders an error banner with retry when error is set', async () => {
    const onRetry = jest.fn();
    renderPage({ error: 'Backend unavailable', onRetry });
    expect(screen.getByTestId('violations-list-error')).toBeInTheDocument();
    await user.click(await screen.findByRole('button', { name: /retry/i }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders the empty state when there are no violations', () => {
    renderPage({ violations: [], totalCount: 0, facets: { totalViolations: 0 } });
    expect(screen.getByTestId('violations-list-empty')).toBeInTheDocument();
    expect(screen.getByText('No violations to display.')).toBeInTheDocument();
  });

  it('shows a search-specific empty message when a committed search returns nothing', () => {
    renderPage({ violations: [], totalCount: 0, facets: { totalViolations: 0 }, searchValue: 'log4j' });
    expect(screen.getByTestId('violations-list-empty')).toBeInTheDocument();
    expect(screen.getByText('No violations match your search.')).toBeInTheDocument();
    expect(screen.getByText('Try adjusting or clearing your search.')).toBeInTheDocument();
  });

  it('shows a filter-specific empty message when filters are active', () => {
    renderPage({
      violations: [],
      totalCount: 0,
      facets: { totalViolations: 0 },
      filters: activeFilters(),
    });
    expect(screen.getByText('No violations match your filters.')).toBeInTheDocument();
    expect(screen.getByText('Try adjusting or resetting your filters.')).toBeInTheDocument();
  });

  it('shows a combined empty message when both search and filters are active', () => {
    renderPage({
      violations: [],
      totalCount: 0,
      facets: { totalViolations: 0 },
      searchValue: 'log4j',
      filters: activeFilters(),
    });
    expect(screen.getByText('No violations match your search and filters.')).toBeInTheDocument();
    expect(screen.getByText('Try adjusting or clearing your search and filters.')).toBeInTheDocument();
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
    await user.click(screen.getByTestId('violations-empty-clear-search'));
    expect(onSearchSubmit).toHaveBeenCalledWith('');
  });

  it('offers a "Reset filters" action in the empty state when filters are active', async () => {
    const onResetFilters = jest.fn();
    renderPage({
      violations: [],
      totalCount: 0,
      facets: { totalViolations: 0 },
      filters: activeFilters(),
      onResetFilters,
    });
    expect(screen.getByText('No violations match your filters.')).toBeInTheDocument();
    await user.click(screen.getByTestId('violations-empty-reset-filters'));
    expect(onResetFilters).toHaveBeenCalledTimes(1);
  });

  it('omits the recovery actions when the empty state is not from a search or filter', () => {
    renderPage({ violations: [], totalCount: 0, facets: { totalViolations: 0 } });
    expect(screen.getByText('No violations to display.')).toBeInTheDocument();
    expect(screen.queryByTestId('violations-empty-clear-search')).not.toBeInTheDocument();
    expect(screen.queryByTestId('violations-empty-reset-filters')).not.toBeInTheDocument();
  });

  it('opens the mobile filter drawer with the same filter rail on demand', async () => {
    renderPage();
    expect(screen.queryByTestId('violations-filters-mobile-drawer')).not.toBeInTheDocument();
    await user.click(screen.getByTestId('violations-filters-mobile-trigger'));
    expect(await screen.findByTestId('violations-filters-mobile-drawer')).toBeInTheDocument();
    // The drawer hosts a second rail instance under the mobile id namespace.
    expect(screen.getByTestId('violations-filter-mobile-rail')).toBeInTheDocument();
  });

  it('announces active filters on the mobile trigger via aria-label', () => {
    renderPage({ filters: activeFilters() });
    expect(screen.getByTestId('violations-filters-mobile-trigger')).toHaveAttribute(
      'aria-label',
      'Filters (active)',
    );
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

  describe('Legal list props (CLM-43207)', () => {
    it('passes hide-state / hide-waiver / LTG title through to the filter rail', () => {
      renderPage({
        hideStateFilter: true,
        hideWaiverTypeFilter: true,
        threatCategorySectionTitle: 'License Threat Group',
        threatCategoryUseIdentityLabels: true,
        facets: {
          totalViolations: 1,
          threatCategories: { Copyleft: 1 },
        },
      });
      const rail = screen.getByTestId('violations-filter-rail');
      expect(within(rail).queryByTestId('violations-filter-state')).not.toBeInTheDocument();
      expect(within(rail).queryByTestId('violations-filter-waiver-type')).not.toBeInTheDocument();
      expect(within(rail).getByText('License Threat Group')).toBeInTheDocument();
      expect(within(rail).getByText('Copyleft')).toBeInTheDocument();
    });

    it('uses Legal-branded error title and filter-aware empty noun', () => {
      const { unmount } = renderPage({
        error: 'Backend unavailable',
        errorTitle: 'Failed to load license risk findings',
      });
      // Banner variant concatenates title + message into one text node.
      expect(screen.getByText(/Failed to load license risk findings/i)).toBeInTheDocument();
      unmount();

      renderPage({
        violations: [],
        totalCount: 0,
        facets: { totalViolations: 0 },
        filters: activeFilters(),
        emptyResultNoun: 'license risk findings',
      });
      expect(screen.getByText('No license risk findings match your filters.')).toBeInTheDocument();
    });

    it('accepts filterDrawerDescription for the mobile filters helper text', async () => {
      renderPage({
        filterDrawerDescription:
          'Narrow license risk by license threat group, threat, stage, organization, and application.',
      });
      await user.click(screen.getByTestId('violations-filters-mobile-trigger'));
      const drawer = await screen.findByTestId('violations-filters-mobile-drawer');
      expect(
        within(drawer).getByText(
          'Narrow license risk by license threat group, threat, stage, organization, and application.',
        ),
      ).toBeInTheDocument();
    });
  });
});
