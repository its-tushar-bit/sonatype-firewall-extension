/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import ComponentsFilterRail from 'MainRoot/nosc/componentsList/ComponentsFilterRail';
import { EMPTY_COMPONENTS_LIST_FILTERS } from 'MainRoot/nosc/componentsList/componentsListFilters';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

beforeAll(() => {
  installRadixJsdomShims();
});

const ORG_FACETS = Array.from({ length: 10 }, (_, index) => ({
  id: `Org ${index}`,
  label: `Org ${index}`,
  count: index + 1,
}));

const FACETS = {
  totalComponents: 10,
  organizations: ORG_FACETS,
  ecosystems: [
    { id: 'maven', label: 'maven', count: 4 },
    { id: 'npm', label: 'npm', count: 6 },
  ],
};

function renderRail(overrides: Partial<React.ComponentProps<typeof ComponentsFilterRail>> = {}) {
  const props = {
    tab: 'myScanData' as const,
    facets: FACETS,
    filters: EMPTY_COMPONENTS_LIST_FILTERS,
    hasActiveFilters: false,
    onToggleFilter: jest.fn(),
    onResetFilters: jest.fn(),
    ...overrides,
  };
  render(
    <Theme>
      <ComponentsFilterRail {...props} />
    </Theme>,
  );
  return props;
}

describe('ComponentsFilterRail', () => {
  let user: ReturnType<typeof userEvent.setup>;

  beforeEach(() => {
    user = userEvent.setup();
  });

  it('renders ecosystems and organizations on My Scan Data', () => {
    renderRail();
    expect(screen.getByTestId('components-filter-rail')).toBeInTheDocument();
    expect(screen.getByTestId('components-filter-ecosystems')).toBeInTheDocument();
    expect(screen.getByTestId('components-filter-organizations')).toBeInTheDocument();
  });

  it('hides organizations on the Sonatype Catalog tab', () => {
    renderRail({ tab: 'catalog' });
    expect(screen.getByTestId('components-filter-ecosystems')).toBeInTheDocument();
    expect(screen.queryByTestId('components-filter-organizations')).not.toBeInTheDocument();
  });

  it('shows 8 organization options plus See more when facets exceed the collapse limit', () => {
    renderRail();
    const orgGroup = screen.getByTestId('components-filter-organizations');
    expect(within(orgGroup).getAllByRole('checkbox')).toHaveLength(8);
    expect(within(orgGroup).getByTestId('components-filter-organizations-see-more')).toBeInTheDocument();
  });

  it('expands and collapses organization options via See more / See less', async () => {
    renderRail();
    const orgGroup = screen.getByTestId('components-filter-organizations');
    await user.click(within(orgGroup).getByTestId('components-filter-organizations-see-more'));
    expect(within(orgGroup).getAllByRole('checkbox')).toHaveLength(10);
    await user.click(within(orgGroup).getByTestId('components-filter-organizations-see-less'));
    expect(within(orgGroup).getAllByRole('checkbox')).toHaveLength(8);
  });

  it('filters organizations by the sidebar search box', async () => {
    renderRail();
    const orgGroup = screen.getByTestId('components-filter-organizations');
    await user.type(screen.getByTestId('components-filter-organizations-search'), 'Org 0');
    expect(within(orgGroup).getByTestId('components-filter-organizations-option-Org 0')).toBeInTheDocument();
    expect(
      within(orgGroup).queryByTestId('components-filter-organizations-option-Org 1'),
    ).not.toBeInTheDocument();
  });

  it('lifts an ecosystem checkbox toggle to onToggleFilter', async () => {
    const props = renderRail();
    await user.click(screen.getByTestId('components-filter-ecosystems-option-maven'));
    expect(props.onToggleFilter).toHaveBeenCalledWith('ecosystems', 'maven');
  });

  it('enables Reset when filters are active and fires onResetFilters', async () => {
    const props = renderRail({
      hasActiveFilters: true,
      filters: {
        organizations: new Set(['Org 0']),
        ecosystems: new Set(),
      },
    });
    const reset = screen.getByTestId('components-filter-reset');
    expect(reset).toBeEnabled();
    await user.click(reset);
    expect(props.onResetFilters).toHaveBeenCalled();
  });
});
