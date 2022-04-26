/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { within } from '@testing-library/dom';
import AdvancedSearchCriteriaBuilder from 'MainRoot/advancedSearch/AdvancedSearchCriteriaBuilder';

describe('AdvancedSearchCriteriaBuilder', () => {
  let renderComponent, minimalProps;

  beforeEach(() => {
    minimalProps = {
      setCurrentQuery: () => {},
      currentQuery: '',
      showCriteriaBuilder: false,
      setShowCriteriaBuilder: () => {},
      inputFieldId: '',
    };

    renderComponent = (additionalProps = {}) =>
      render(<AdvancedSearchCriteriaBuilder {...minimalProps} {...additionalProps} />);
  });

  it('renders "Component" section with the correct query terms', () => {
    renderComponent({ showCriteriaBuilder: true });
    const componentGroupTags = [
      'componentHash',
      'componentFormat',
      'componentName',
      'componentCoordinateGroupId',
      'componentCoordinateArtifactId',
      'componentCoordinateVersion',
      'componentCoordinateClassifier',
      'componentCoordinateExtension',
      'componentCoordinateName',
      'componentCoordinateQualifier',
      'componentCoordinatePackageId',
      'componentCoordinateArchitecture',
      'componentCoordinatePlatform',
    ];

    // Get "Component" group and search within it for each related tag
    const componentQueryGroup = screen.getByRole('group', { name: 'Component' });
    componentGroupTags.forEach((tagName) => {
      expect(within(componentQueryGroup).getByRole('switch', { name: tagName })).toBeVisible();
    });
  });
});
