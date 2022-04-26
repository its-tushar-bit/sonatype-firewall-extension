/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import AdvancedSearchForm from 'MainRoot/advancedSearch/AdvancedSearchForm';

describe('AdvancedSearchForm', () => {
  let renderComponent, minimalProps;

  beforeEach(() => {
    minimalProps = {
      setCurrentQuery: () => {},
      setShowAllComponentResults: () => {},
      searchFormSubmit: () => {},
      currentQuery: '',
      searchResult: {},
      totalNumberOfHits: 0,
      isShowingAllComponentResults: false,
      isToggleComponentResultsEnabled: false,
      toggleHelp: () => {},
      showHelp: false,
    };

    renderComponent = (additionalProps = {}) => render(<AdvancedSearchForm {...minimalProps} {...additionalProps} />);
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
});
