/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen } from '@testing-library/react';
import { render, userEvent } from 'TestRoot/SpecUtil';
import { FilteredListLayout, CountNoun, FilteredListLayoutProps } from 'MainRoot/nosc/components/FilteredListLayout';
import { DomainIcons } from 'MainRoot/nosc/icons';

const TestIcon = DomainIcons.Policies;

const defaultCountNoun: CountNoun = { singular: 'item', plural: 'items' };
const vulnerabilityCountNoun: CountNoun = { singular: 'vulnerability', plural: 'vulnerabilities' };

interface TestItem {
  id: string;
  name: string;
}

const defaultItems: TestItem[] = [
  { id: '1', name: 'Item One' },
  { id: '2', name: 'Item Two' },
  { id: '3', name: 'Item Three' },
];

function renderFilteredListLayout(overrides: Partial<FilteredListLayoutProps<TestItem>> = {}) {
  const defaultProps: FilteredListLayoutProps<TestItem> = {
    title: 'Test Page',
    slug: 'test-page',
    description: 'Test description',
    icon: TestIcon,
    countNoun: defaultCountNoun,
    items: defaultItems,
    totalCount: defaultItems.length,
    loading: false,
    error: null,
    renderCardGrid: (items) => (
      <ul>
        {items.map((item) => (
          <li key={item.id} data-testid={`item-${item.id}`}>
            {item.name}
          </li>
        ))}
      </ul>
    ),
    ...overrides,
  };

  return render(<FilteredListLayout {...defaultProps} />);
}

describe('FilteredListLayout', () => {
  describe('page structure and testids', () => {
    it('renders the page with the correct data-testid', () => {
      renderFilteredListLayout();
      expect(screen.getByTestId('preview-test-page-page')).toBeInTheDocument();
    });

    it('renders title, description, and icon in the header', () => {
      renderFilteredListLayout();
      expect(screen.getByText('Test Page')).toBeInTheDocument();
      expect(screen.getByText('Test description')).toBeInTheDocument();
    });

    it('uses the slug for data-testids', () => {
      renderFilteredListLayout({ slug: 'vulnerabilities' });
      expect(screen.getByTestId('preview-vulnerabilities-page')).toBeInTheDocument();
      expect(screen.getByTestId('vulnerabilities-toolbar')).toBeInTheDocument();
    });

    it('renders the default header when renderHeader is not provided', () => {
      renderFilteredListLayout();
      expect(screen.getByText('Test Page')).toBeInTheDocument();
    });

    it('renders custom header when renderHeader is provided', () => {
      renderFilteredListLayout({
        renderHeader: () => <div data-testid="custom-header">Custom Header</div>,
      });
      expect(screen.getByTestId('custom-header')).toBeInTheDocument();
      expect(screen.queryByText('Test Page')).not.toBeInTheDocument();
    });
  });

  describe('countNoun pluralization', () => {
    it('shows singular noun when count is 1', () => {
      renderFilteredListLayout({ items: [defaultItems[0]], totalCount: 1 });
      expect(screen.getByText('1 item')).toBeInTheDocument();
    });

    it('shows plural noun when count is 0', () => {
      renderFilteredListLayout({ items: [], totalCount: 0 });
      expect(screen.getByText('0 items')).toBeInTheDocument();
    });

    it('shows plural noun when count is greater than 1', () => {
      renderFilteredListLayout({ totalCount: 5 });
      expect(screen.getByText('5 items')).toBeInTheDocument();
    });

    it('handles irregular plurals (vulnerability/vulnerabilities)', () => {
      renderFilteredListLayout({
        countNoun: vulnerabilityCountNoun,
        totalCount: 2,
      });
      expect(screen.getByText('2 vulnerabilities')).toBeInTheDocument();
    });

    it('handles irregular singular (1 vulnerability)', () => {
      renderFilteredListLayout({
        countNoun: vulnerabilityCountNoun,
        items: [defaultItems[0]],
        totalCount: 1,
      });
      expect(screen.getByText('1 vulnerability')).toBeInTheDocument();
    });
  });

  describe('toolbar', () => {
    it('renders the toolbar by default', () => {
      renderFilteredListLayout();
      expect(screen.getByTestId('test-page-toolbar')).toBeInTheDocument();
    });

    it('hides the toolbar when showToolbar is false', () => {
      renderFilteredListLayout({ showToolbar: false });
      expect(screen.queryByTestId('test-page-toolbar')).not.toBeInTheDocument();
    });

    it('renders custom toolbar when renderToolbar is provided', () => {
      renderFilteredListLayout({
        renderToolbar: () => <div data-testid="custom-toolbar">Custom Toolbar</div>,
      });
      expect(screen.getByTestId('custom-toolbar')).toBeInTheDocument();
    });

    it('renders custom count via renderCount', () => {
      renderFilteredListLayout({
        renderCount: () => <span>Showing 1-10 of 100</span>,
      });
      expect(screen.getByText('Showing 1-10 of 100')).toBeInTheDocument();
    });
  });

  describe('search functionality', () => {
    it('renders search input when searchable is true (default)', () => {
      renderFilteredListLayout();
      expect(screen.getByTestId('test-page-toolbar-search')).toBeInTheDocument();
    });

    it('hides search input when searchable is false', () => {
      renderFilteredListLayout({ searchable: false });
      expect(screen.queryByTestId('test-page-toolbar-search')).not.toBeInTheDocument();
    });

    it('calls onSearchSubmit on form submit', async () => {
      const onSearchSubmit = jest.fn();
      const user = userEvent.setup();
      renderFilteredListLayout({ onSearchSubmit, searchValue: '' });

      const searchInput = screen.getByTestId('test-page-toolbar-search');
      await user.type(searchInput, 'test query{enter}');

      expect(onSearchSubmit).toHaveBeenCalledWith('test query');
    });

    it('trims whitespace from search input', async () => {
      const onSearchSubmit = jest.fn();
      const user = userEvent.setup();
      renderFilteredListLayout({ onSearchSubmit, searchValue: '' });

      const searchInput = screen.getByTestId('test-page-toolbar-search');
      await user.type(searchInput, '  trimmed query  {enter}');

      expect(onSearchSubmit).toHaveBeenCalledWith('trimmed query');
    });

    it('displays the search placeholder', () => {
      renderFilteredListLayout({ searchPlaceholder: 'Search items...' });
      expect(screen.getByPlaceholderText('Search items...')).toBeInTheDocument();
    });

    it('syncs draft with external searchValue changes', async () => {
      const { rerender } = renderFilteredListLayout({ searchValue: 'initial' });
      const searchInput = screen.getByTestId('test-page-toolbar-search') as HTMLInputElement;
      expect(searchInput.value).toBe('initial');

      rerender(
        <FilteredListLayout
          title="Test Page"
          slug="test-page"
          description="Test description"
          icon={TestIcon}
          countNoun={defaultCountNoun}
          items={defaultItems}
          totalCount={defaultItems.length}
          loading={false}
          error={null}
          searchValue="updated"
          renderCardGrid={(items) => (
            <ul>
              {items.map((i) => (
                <li key={i.id}>{i.name}</li>
              ))}
            </ul>
          )}
        />
      );

      expect(searchInput.value).toBe('updated');
    });
  });

  describe('loading state', () => {
    it('shows loading state when loading is true', () => {
      renderFilteredListLayout({ loading: true });
      expect(screen.getByTestId('test-page-list-loading')).toBeInTheDocument();
    });

    it('does not show content while loading', () => {
      renderFilteredListLayout({ loading: true });
      expect(screen.queryByTestId('item-1')).not.toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('shows error state when error is provided', () => {
      renderFilteredListLayout({ error: 'Failed to load items' });
      expect(screen.getByTestId('test-page-list-error')).toBeInTheDocument();
    });

    it('shows custom error title', () => {
      renderFilteredListLayout({ error: 'Something went wrong' });
      // Error banner should be visible
      expect(screen.getByTestId('test-page-list-error')).toBeInTheDocument();
    });

    it('calls onRetry when retry button is clicked', async () => {
      const onRetry = jest.fn();
      const user = userEvent.setup();
      renderFilteredListLayout({ error: 'Failed to load', onRetry });

      const retryButton = screen.getByRole('button', { name: /retry/i });
      await user.click(retryButton);

      expect(onRetry).toHaveBeenCalledTimes(1);
    });
  });

  describe('empty state', () => {
    it('shows empty state when items array is empty', () => {
      renderFilteredListLayout({ items: [], totalCount: 0 });
      expect(screen.getByTestId('test-page-list-empty')).toBeInTheDocument();
    });

    it('shows default empty title for no items', () => {
      renderFilteredListLayout({ items: [], totalCount: 0 });
      expect(screen.getByText('No items to display.')).toBeInTheDocument();
    });

    it('shows default empty hint for no items', () => {
      renderFilteredListLayout({ items: [], totalCount: 0 });
      expect(screen.getByText('Test Page will appear here once data is loaded.')).toBeInTheDocument();
    });

    it('shows filter-specific empty message when hasActiveFilters is true', () => {
      renderFilteredListLayout({ items: [], totalCount: 0, hasActiveFilters: true });
      expect(screen.getByText('No results match your filters.')).toBeInTheDocument();
      expect(screen.getByText('Try adjusting your search or filters.')).toBeInTheDocument();
    });

    it('shows search-specific empty message when search is active', () => {
      renderFilteredListLayout({
        items: [],
        totalCount: 0,
        searchValue: 'nonexistent',
      });
      expect(screen.getByText('No results match your filters.')).toBeInTheDocument();
    });

    it('shows custom empty title and hint', () => {
      renderFilteredListLayout({
        items: [],
        totalCount: 0,
        emptyTitle: 'Custom empty title',
        emptyHint: 'Custom empty hint',
      });
      expect(screen.getByText('Custom empty title')).toBeInTheDocument();
      expect(screen.getByText('Custom empty hint')).toBeInTheDocument();
    });

    it('renders custom empty state via renderEmpty', () => {
      renderFilteredListLayout({
        items: [],
        totalCount: 0,
        renderEmpty: () => <div data-testid="custom-empty">Custom empty state</div>,
      });
      expect(screen.getByTestId('custom-empty')).toBeInTheDocument();
    });

    it('shows "Clear search" button when search is active and items are empty', () => {
      renderFilteredListLayout({
        items: [],
        totalCount: 0,
        searchValue: 'test query',
        onSearchSubmit: jest.fn(),
      });
      expect(screen.getByTestId('test-page-empty-clear-search')).toBeInTheDocument();
    });

    it('calls onSearchSubmit with empty string when "Clear search" is clicked', async () => {
      const onSearchSubmit = jest.fn();
      const user = userEvent.setup();
      renderFilteredListLayout({
        items: [],
        totalCount: 0,
        searchValue: 'test query',
        onSearchSubmit,
      });

      const clearButton = screen.getByTestId('test-page-empty-clear-search');
      await user.click(clearButton);

      expect(onSearchSubmit).toHaveBeenCalledWith('');
    });

    it('does not show "Clear search" button when hasActiveFilters is true but hasSearch is false', () => {
      renderFilteredListLayout({
        items: [],
        totalCount: 0,
        hasActiveFilters: true,
        searchValue: '',
      });
      expect(screen.queryByTestId('test-page-empty-clear-search')).not.toBeInTheDocument();
    });
  });

  describe('pagination', () => {
    it('does not show pagination when paginated is false', () => {
      renderFilteredListLayout({
        paginated: false,
        page: 1,
        pageSize: 10,
        onPageChange: jest.fn(),
        totalCount: 50,
      });
      expect(screen.queryByTestId('test-page-pagination')).not.toBeInTheDocument();
    });

    it('shows pagination when total exceeds pageSize', () => {
      renderFilteredListLayout({
        page: 1,
        pageSize: 2,
        onPageChange: jest.fn(),
        totalCount: 10,
      });
      expect(screen.getByTestId('test-page-pagination')).toBeInTheDocument();
    });

    it('shows pagination when page > 1 even if total <= pageSize', () => {
      renderFilteredListLayout({
        page: 2,
        pageSize: 10,
        onPageChange: jest.fn(),
        totalCount: 10,
      });
      expect(screen.getByTestId('test-page-pagination')).toBeInTheDocument();
    });

    it('does not show pagination when onPageChange is not provided', () => {
      renderFilteredListLayout({
        page: 1,
        pageSize: 2,
        totalCount: 10,
      });
      expect(screen.queryByTestId('test-page-pagination')).not.toBeInTheDocument();
    });

    it('calls onPageChange when page changes', async () => {
      const onPageChange = jest.fn();
      const user = userEvent.setup();
      renderFilteredListLayout({
        page: 1,
        pageSize: 1,
        onPageChange,
        totalCount: 5,
      });

      // Pagination renders, click next page
      const nextButton = screen.getByRole('button', { name: /next page/i });
      await user.click(nextButton);

      expect(onPageChange).toHaveBeenCalledWith(2);
    });

    it('keeps pagination visible during a loading refetch (rendered outside AsyncPageState)', () => {
      // Pagination lives outside AsyncPageState so a loading refetch (which swaps the list for the
      // skeleton) does not unmount + remount the pager. If someone moves <Pagination> back inside
      // AsyncPageState, the pager would disappear here and this test would fail.
      renderFilteredListLayout({
        loading: true,
        page: 2,
        pageSize: 10,
        onPageChange: jest.fn(),
        totalCount: 50,
      });
      expect(screen.getByTestId('test-page-list-loading')).toBeInTheDocument();
      expect(screen.getByTestId('test-page-pagination')).toBeInTheDocument();
    });

    it('hides pagination while an error banner is shown', () => {
      renderFilteredListLayout({
        error: 'Boom',
        page: 2,
        pageSize: 10,
        onPageChange: jest.fn(),
        totalCount: 50,
      });
      expect(screen.getByTestId('test-page-list-error')).toBeInTheDocument();
      expect(screen.queryByTestId('test-page-pagination')).not.toBeInTheDocument();
    });
  });

  describe('tabs slot', () => {
    it('renders tabs when renderTabs is provided', () => {
      renderFilteredListLayout({
        renderTabs: () => <div data-testid="custom-tabs">My Tab | Other Tab</div>,
      });
      expect(screen.getByTestId('test-page-tabs')).toBeInTheDocument();
      expect(screen.getByText('My Tab | Other Tab')).toBeInTheDocument();
    });

    it('does not render tabs section when renderTabs is not provided', () => {
      renderFilteredListLayout();
      expect(screen.queryByTestId('test-page-tabs')).not.toBeInTheDocument();
    });
  });

  describe('filter rail', () => {
    it('renders filter rail when renderFilterRail is provided', () => {
      renderFilteredListLayout({
        renderFilterRail: () => <div data-testid="filter-rail">Filter Options</div>,
      });
      expect(screen.getByText('Filter Options')).toBeInTheDocument();
    });

    it('renders mobile filter drawer when renderMobileFilterDrawer is provided', () => {
      renderFilteredListLayout({
        renderFilterRail: () => <div>Desktop Filters</div>,
        renderMobileFilterDrawer: () => <div>Mobile Filters</div>,
      });
      // Mobile trigger button should be rendered
      expect(screen.getByTestId('test-page-filters-mobile-trigger')).toBeInTheDocument();
    });

    it('renders the mobile filter drawer when the toolbar is hidden', () => {
      renderFilteredListLayout({
        showToolbar: false,
        renderFilterRail: () => <div>Desktop Filters</div>,
        renderMobileFilterDrawer: () => <div>Mobile Filters</div>,
      });
      expect(screen.queryByTestId('test-page-toolbar')).not.toBeInTheDocument();
      expect(screen.getByTestId('test-page-filters-mobile-trigger')).toBeInTheDocument();
    });

    it('opens the mobile filter drawer when the toolbar is hidden', async () => {
      renderFilteredListLayout({
        showToolbar: false,
        renderFilterRail: () => <div>Desktop Filters</div>,
        renderMobileFilterDrawer: () => <div>Mobile Filters</div>,
      });
      await userEvent.click(screen.getByTestId('test-page-filters-mobile-trigger'));
      expect(screen.getByTestId('test-page-filters-mobile-drawer')).toBeInTheDocument();
      expect(screen.getByText('Mobile Filters')).toBeInTheDocument();
    });

    it('does not show active filter dot when hasActiveFilters is false', () => {
      renderFilteredListLayout({
        renderFilterRail: () => <div>Filters</div>,
        renderMobileFilterDrawer: () => <div>Mobile Filters</div>,
        hasActiveFilters: false,
      });
      expect(screen.queryByTestId('test-page-filters-mobile-active-dot')).not.toBeInTheDocument();
    });

    it('shows active filter dot when hasActiveFilters is true', () => {
      renderFilteredListLayout({
        renderFilterRail: () => <div>Filters</div>,
        renderMobileFilterDrawer: () => <div>Mobile Filters</div>,
        hasActiveFilters: true,
      });
      expect(screen.getByTestId('test-page-filters-mobile-active-dot')).toBeInTheDocument();
    });
  });

  describe('card grid rendering', () => {
    it('renders items via renderCardGrid', () => {
      renderFilteredListLayout();
      expect(screen.getByTestId('item-1')).toBeInTheDocument();
      expect(screen.getByTestId('item-2')).toBeInTheDocument();
      expect(screen.getByTestId('item-3')).toBeInTheDocument();
    });

    it('passes filtered items to renderCardGrid', () => {
      const singleItem = [defaultItems[0]];
      renderFilteredListLayout({ items: singleItem, totalCount: 1 });
      expect(screen.getByTestId('item-1')).toBeInTheDocument();
      expect(screen.queryByTestId('item-2')).not.toBeInTheDocument();
    });
  });
});
