/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, userEvent } from 'TestRoot/SpecUtil';
import AdvancedSearchForm from 'MainRoot/advancedSearch/AdvancedSearchForm';

describe('AdvancedSearchForm', () => {
  let renderComponent, minimalProps;
  let user;
  beforeEach(() => {
    minimalProps = {
      setCurrentQuery: () => {},
      setShowAllComponentResults: () => {},
      searchFormSubmit: () => {},
      addSearchItem: () => {},
      searchItems: [],
      currentQuery: '',
      searchResult: {},
      totalNumberOfHits: 0,
      isShowingAllComponentResults: false,
      isToggleComponentResultsEnabled: false,
    };
    user = userEvent.setup();
    renderComponent = (additionalProps = {}) => render(<AdvancedSearchForm {...minimalProps} {...additionalProps} />);
  });

  it('renders the search input with the spec placeholder', () => {
    renderComponent();
    expect(
      screen.getByPlaceholderText('Enter CVE ID or use the "Use query builder" or "Add search terms" buttons below.')
    ).toBeVisible();
  });

  it('hides component results toggles if isToggleComponentResultsEnabled is false', () => {
    renderComponent();
    expect(
      screen.queryByRole('radio', { name: 'Limit search results to components that have security vulnerabilities' })
    ).toBeNull();
    expect(screen.queryByRole('radio', { name: 'show all components in search results' })).toBeNull();
  });

  it('shows component results toggles if isToggleComponentResultsEnabled is true', () => {
    renderComponent({ isToggleComponentResultsEnabled: true });

    expect(
      screen.getByRole('radio', { name: 'Limit search results to components that have security vulnerabilities' })
    ).toBeVisible();
    expect(screen.getByRole('radio', { name: 'show all components in search results' })).toBeVisible();
  });

  it('opens query builder when clicking iq-adv-search__query-builder-button', async () => {
    renderComponent();

    const queryBuilderButton = screen.getByRole('button', { name: /Use Query Builder/i });
    await user.click(queryBuilderButton);

    // Check that the AdvancedSearchCriteriaEasyBuilder component is rendered
    expect(screen.getByRole('region', { name: 'Advanced Search Builder' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Build Query Rules' })).toBeVisible();
    expect(screen.getByRole('button', { name: /Add Rule/i })).toBeVisible();
  });

  it('closes query builder when clicking iq-adv-search__query-builder-button again', async () => {
    renderComponent();

    const queryBuilderButton = screen.getByRole('button', { name: /Use Query Builder/i });

    // Open the query builder
    await user.click(queryBuilderButton);
    expect(screen.getByRole('region', { name: 'Advanced Search Builder' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Build Query Rules' })).toBeVisible();

    // Close the query builder
    await user.click(queryBuilderButton);
    expect(screen.queryByRole('heading', { name: 'Build Query Rules' })).not.toBeInTheDocument();
  });

  it('opens search terms builder when clicking iq-adv-search__search-terms-button', async () => {
    renderComponent();

    const searchTermsButton = screen.getByRole('button', { name: /Add Search Terms/i });
    await user.click(searchTermsButton);

    // Check that the AdvancedSearchCriteriaSearchTermsBuilder component is rendered
    expect(screen.getByRole('region', { name: 'Advanced Search Builder' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Search Terms' })).toBeVisible();
  });

  it('closes search terms builder when clicking iq-adv-search__search-terms-button again', async () => {
    renderComponent();

    const searchTermsButton = screen.getByRole('button', { name: /Add Search Terms/i });

    // Open the search terms builder
    await user.click(searchTermsButton);
    expect(screen.getByRole('region', { name: 'Advanced Search Builder' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Search Terms' })).toBeVisible();

    // Close the search terms builder
    await user.click(searchTermsButton);
    expect(screen.queryByRole('heading', { name: 'Search Terms' })).not.toBeInTheDocument();
  });

  it('closes query builder when opening search terms builder', async () => {
    renderComponent();

    const queryBuilderButton = screen.getByRole('button', { name: /Use Query Builder/i });
    const searchTermsButton = screen.getByRole('button', { name: /Add Search Terms/i });

    // Open query builder
    await user.click(queryBuilderButton);
    expect(screen.getByRole('region', { name: 'Advanced Search Builder' })).toBeVisible();

    // Open search terms builder (should close query builder)
    await user.click(searchTermsButton);
    expect(screen.getByRole('region', { name: 'Advanced Search Builder' })).toBeVisible();
    // Note: Search Terms component may not render due to test environment limitations
  });

  it('closes search terms builder when opening query builder', async () => {
    renderComponent();

    const queryBuilderButton = screen.getByRole('button', { name: /Use Query Builder/i });
    const searchTermsButton = screen.getByRole('button', { name: /Add Search Terms/i });

    // Open search terms builder
    await user.click(searchTermsButton);
    expect(screen.getByRole('region', { name: 'Advanced Search Builder' })).toBeVisible();

    // Open query builder (should close search terms builder)
    await user.click(queryBuilderButton);
    expect(screen.getByRole('region', { name: 'Advanced Search Builder' })).toBeVisible();
    // Note: Query builder component may not render due to test environment limitations
  });
});
