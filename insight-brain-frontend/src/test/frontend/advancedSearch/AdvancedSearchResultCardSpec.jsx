/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import AdvancedSearchResultCard from 'MainRoot/advancedSearch/AdvancedSearchResultCard';

describe('AdvancedSearchResultCard', () => {
  let renderComponent, minimalProps;

  beforeEach(() => {
    minimalProps = {
      searchResultItem: {},
      groupIdentifier: '',
      $state: {
        get: jasmine.createSpy('$state.get').and.returnValue({ data: { title: 'some page' } }),
        href: jasmine.createSpy('$state.href').and.returnValue('/noop'),
      },
    };

    renderComponent = (additionalProps = {}) =>
      render(<AdvancedSearchResultCard {...minimalProps} {...additionalProps} />);
  });

  it('only displays component name and report when result does not contain a vulnerability', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      applicationName: 'testApp',
      policyEvaluationStage: 'testStage',
      groupIdentifier: 'testGroupIdentifier',
    };

    renderComponent({ searchResultItem: componentTypeResult });

    // Application and Report rows should exist
    expect(screen.getByRole('row', { name: 'Application testApp' })).toBeVisible();
    expect(screen.getByRole('row', { name: 'Report testStage' })).toBeVisible();

    // All other named cells should not exist
    expect(screen.queryByRole('cell', { name: 'Organization' })).toBeNull();
    expect(screen.queryByRole('cell', { name: 'Application Category' })).toBeNull();
    expect(screen.queryByRole('cell', { name: 'Component Name' })).toBeNull();
    expect(screen.queryByRole('cell', { name: 'Component Label' })).toBeNull();
    expect(screen.queryByRole('cell', { name: 'Security Issue' })).toBeNull();
    expect(screen.queryByRole('cell', { name: 'Vulnerability Description' })).toBeNull();
    expect(screen.queryByRole('cell', { name: 'Policy' })).toBeNull();
  });
});
