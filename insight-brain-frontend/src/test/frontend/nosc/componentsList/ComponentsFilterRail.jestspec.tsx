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
  applications: [
    { id: 'app-1', label: 'Checkout', count: 7 },
    { id: 'app-2', label: 'app-2', count: 2 },
  ],
  stages: [
    { id: 'build', label: 'Build', count: 9 },
    { id: 'release', label: 'Release', count: 3 },
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

  // Ecosystem facets are Catalog-only: the My Scan Data dashboard list has no ecosystem
  // buckets, so the rail hides the section even when entries are supplied.
  it('renders organizations but not ecosystems on My Scan Data', () => {
    renderRail();
    expect(screen.getByTestId('components-filter-rail')).toBeInTheDocument();
    expect(screen.getByTestId('components-filter-organizations')).toBeInTheDocument();
    expect(screen.queryByTestId('components-filter-ecosystems')).not.toBeInTheDocument();
  });

  it('hides organizations on the Sonatype Catalog tab', () => {
    renderRail({ tab: 'catalog' });
    expect(screen.getByTestId('components-filter-ecosystems')).toBeInTheDocument();
    expect(screen.queryByTestId('components-filter-organizations')).not.toBeInTheDocument();
  });

  it('renders applications and stages on My Scan Data and toggles by id (CLM-43211)', async () => {
    const props = renderRail();

    const applications = screen.getByTestId('components-filter-applications');
    expect(applications).toHaveTextContent('Checkout');
    // An application the backend could not name still renders, labelled by its id.
    expect(applications).toHaveTextContent('app-2');
    expect(screen.getByTestId('components-filter-stages')).toHaveTextContent('Build');

    await user.click(screen.getByTestId('components-filter-applications-option-app-1'));
    expect(props.onToggleFilter).toHaveBeenCalledWith('applications', 'app-1');

    await user.click(screen.getByTestId('components-filter-stages-option-release'));
    expect(props.onToggleFilter).toHaveBeenCalledWith('stages', 'release');
  });

  it('hides applications and stages on the Sonatype Catalog tab', () => {
    renderRail({ tab: 'catalog' });
    expect(screen.queryByTestId('components-filter-applications')).not.toBeInTheDocument();
    expect(screen.queryByTestId('components-filter-stages')).not.toBeInTheDocument();
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

  it('lifts an ecosystem checkbox toggle to onToggleFilter on the Sonatype Catalog tab', async () => {
    const props = renderRail({ tab: 'catalog' });
    await user.click(screen.getByTestId('components-filter-ecosystems-option-maven'));
    expect(props.onToggleFilter).toHaveBeenCalledWith('ecosystems', 'maven');
  });

  it('enables Reset when filters are active and fires onResetFilters', async () => {
    const props = renderRail({
      hasActiveFilters: true,
      filters: {
        ...EMPTY_COMPONENTS_LIST_FILTERS,
        organizations: new Set(['Org 0']),
      },
    });
    const reset = screen.getByTestId('components-filter-reset');
    expect(reset).toBeEnabled();
    await user.click(reset);
    expect(props.onResetFilters).toHaveBeenCalled();
  });
});
