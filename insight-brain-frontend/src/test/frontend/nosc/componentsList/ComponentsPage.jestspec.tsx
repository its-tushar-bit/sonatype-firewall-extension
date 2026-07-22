/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import ComponentsPage from 'MainRoot/nosc/componentsList/ComponentsPage';
import { EMPTY_COMPONENTS_LIST_FILTERS } from 'MainRoot/nosc/componentsList/componentsListFilters';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

beforeAll(() => {
  installRadixJsdomShims();
});

const EMPTY_FACETS = {
  totalComponents: 0,
  organizations: [],
  ecosystems: [],
};

function renderPage(overrides: Partial<React.ComponentProps<typeof ComponentsPage>> = {}) {
  const props = {
    tab: 'myScanData' as const,
    onTabChange: jest.fn(),
    components: [],
    facets: EMPTY_FACETS,
    filters: EMPTY_COMPONENTS_LIST_FILTERS,
    hasActiveFilters: false,
    onToggleFilter: jest.fn(),
    onResetFilters: jest.fn(),
    searchValue: '',
    onSearchSubmit: jest.fn(),
    totalCount: 0,
    page: 1,
    pageSize: 50,
    onPageChange: jest.fn(),
    ...overrides,
  };
  render(
    <Theme>
      <ComponentsPage {...props} />
    </Theme>,
  );
  return props;
}

describe('ComponentsPage', () => {
  it('renders the Martha Components page shell with toolbar and empty state', () => {
    renderPage();
    expect(screen.getByTestId('preview-components-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Components' })).toBeInTheDocument();
    expect(screen.getByTestId('components-toolbar')).toBeInTheDocument();
    expect(screen.getByTestId('components-tab-my-scan-data')).toBeInTheDocument();
    expect(screen.getByTestId('components-list-empty')).toBeInTheDocument();
  });

  it('renders component cards when rows are present', () => {
    renderPage({
      totalCount: 1,
      components: [
        {
          id: 'guava',
          name: 'guava',
          subtitle: '31.1',
          ecosystem: 'maven',
          organization: 'Java Team',
          source: 'local',
        },
      ],
    });
    expect(screen.getByTestId('component-card-name')).toHaveTextContent('guava');
    expect(screen.queryByTestId('components-list-empty')).not.toBeInTheDocument();
  });
});
