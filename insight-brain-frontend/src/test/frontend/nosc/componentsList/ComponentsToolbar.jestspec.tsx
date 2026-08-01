/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import ComponentsToolbar from 'MainRoot/nosc/componentsList/ComponentsToolbar';
import { EMPTY_COMPONENTS_LIST_FILTERS } from 'MainRoot/nosc/componentsList/componentsListFilters';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

beforeAll(() => {
  installRadixJsdomShims();
});

function renderToolbar(overrides: Partial<React.ComponentProps<typeof ComponentsToolbar>> = {}) {
  const props = {
    tab: 'myScanData' as const,
    onTabChange: jest.fn(),
    totalCount: 2,
    searchValue: '',
    onSearchSubmit: jest.fn(),
    filters: EMPTY_COMPONENTS_LIST_FILTERS,
    ...overrides,
  };
  render(
    <Theme>
      <ComponentsToolbar {...props} />
    </Theme>,
  );
  return props;
}

describe('ComponentsToolbar', () => {
  let user: ReturnType<typeof userEvent.setup>;

  beforeEach(() => {
    user = userEvent.setup();
  });

  it('renders My Scan Data and Sonatype Catalog tabs', () => {
    renderToolbar();
    expect(screen.getByTestId('components-tab-my-scan-data')).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByTestId('components-tab-catalog')).toHaveAttribute('aria-selected', 'false');
  });

  it('lifts tab changes', async () => {
    const props = renderToolbar();
    await user.click(screen.getByTestId('components-tab-catalog'));
    expect(props.onTabChange).toHaveBeenCalledWith('catalog');
  });

  it('disables CSV export on the Sonatype Catalog tab', () => {
    renderToolbar({ tab: 'catalog', totalCount: 5 });
    expect(screen.getByTestId('components-toolbar-csv')).toBeDisabled();
    expect(screen.getByText(/Sonatype Catalog export is not available/i)).toBeInTheDocument();
  });

  it('submits search on Enter', async () => {
    const props = renderToolbar();
    const input = screen.getByTestId('components-toolbar-search');
    await user.type(input, 'guava{Enter}');
    expect(props.onSearchSubmit).toHaveBeenCalledWith('guava');
  });

  it('shows the component count for My Scan Data', () => {
    renderToolbar({ totalCount: 1 });
    expect(screen.getByTestId('components-toolbar-count')).toHaveTextContent('1 component');
  });

  it('shows a visible CSV caveat when only organization filters are active', () => {
    renderToolbar({
      totalCount: 3,
      searchValue: '',
      filters: {
        ...EMPTY_COMPONENTS_LIST_FILTERS,
        organizations: new Set(['Java Team']),
      },
    });
    const hint = screen.getByTestId('components-toolbar-csv-filter-hint');
    expect(hint).toHaveTextContent(/organization.*ecosystem filters are not applied/i);
    expect(screen.getByTestId('components-toolbar-csv')).toHaveAttribute(
      'aria-describedby',
      'components-toolbar-csv-filter-hint',
    );
  });

  it('shows Catalog unavailable instead of a zero count', () => {
    renderToolbar({ tab: 'catalog', totalCount: 0, catalogAvailable: false });
    expect(screen.getByTestId('components-toolbar-count')).toHaveTextContent('Catalog unavailable');
  });

  it('formats capped totals with a plus suffix', () => {
    renderToolbar({ totalCount: 10000, exactTotalEstimate: false });
    expect(screen.getByTestId('components-toolbar-count')).toHaveTextContent('10,000+ components');
  });
});
