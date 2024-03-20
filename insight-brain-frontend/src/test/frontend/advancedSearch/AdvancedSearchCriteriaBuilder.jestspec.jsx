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

  const initialState = {
    advancedSearch: {
      viewState: {
        loading: false,
      },
      configurationState: {
        isEnabled: true,
      },
      formState: {
        searchResult: {
          groupingByDTOS: [],
        },
      },
    },
  };

  const sharedCriteriaGroups = {
    Organization: ['organizationId', 'organizationName'],
    Application: ['applicationId', 'applicationName'],
    Component: [
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
    ],
    'Security Vulnerability': ['vulnerabilityId', 'vulnerabilitySeverity', 'vulnerabilityDescription'],
  };

  const lifecycleCriteriaGroups = {
    Application: ['applicationPublicId'],
    'Application Category': [
      'applicationCategoryId',
      'applicationCategoryName',
      'applicationCategoryColor',
      'applicationCategoryDescription',
    ],
    'Component Label': ['componentLabelId', 'componentLabelName', 'componentLabelColor', 'componentLabelDescription'],
    Policy: ['policyId', 'policyName', 'policyThreatCategory', 'policyThreatLevel'],
    'Security Vulnerability': ['reportId', 'policyEvaluationStage', 'vulnerabilityStatus'],
    Other: ['itemType'],
  };

  const sbomCriteriaGroups = {
    Application: ['applicationVersion', 'sbomFormat'],
  };

  const checkCriteriaGroupsAreRendered = (criteriaGroups) => {
    // Get criteria group and search within it for each related tag
    Object.entries(criteriaGroups).forEach(([criteriaName, componentGroupTags]) => {
      const componentQueryGroup = screen.getByRole('group', { name: criteriaName });
      componentGroupTags.forEach((tagName) => {
        expect(within(componentQueryGroup).getByRole('switch', { name: tagName })).toBeVisible();
      });
    });
  };

  const checkCriteriaGroupTagsAreNotRendered = (criteriaGroups) => {
    Object.values(criteriaGroups).forEach((componentGroupTags) => {
      componentGroupTags.forEach((tagName) => {
        expect(screen.queryByText(tagName)).not.toBeInTheDocument();
      });
    });
  };

  beforeEach(() => {
    minimalProps = {
      setCurrentQuery: () => {},
      currentQuery: '',
      showCriteriaBuilder: false,
      setShowCriteriaBuilder: () => {},
      inputFieldId: '',
    };

    renderComponent = (additionalProps = {}, preloadedState = {}) =>
      render(<AdvancedSearchCriteriaBuilder {...minimalProps} {...additionalProps} />, { preloadedState });
  });

  it('renders the sections with the correct query terms', () => {
    renderComponent({ showCriteriaBuilder: true }, initialState);

    checkCriteriaGroupsAreRendered(sharedCriteriaGroups);
    checkCriteriaGroupsAreRendered(lifecycleCriteriaGroups);
    checkCriteriaGroupTagsAreNotRendered(sbomCriteriaGroups);
  });

  it('renders the sections with the correct query terms when in SBOM Manager', () => {
    const sbomManagerState = {
      ...initialState,
      router: {
        currentState: { name: 'sbomManager.advancedSearch' },
      },
    };
    renderComponent({ showCriteriaBuilder: true }, sbomManagerState);

    // Get criteria group and search within it for each related tag
    checkCriteriaGroupsAreRendered(sharedCriteriaGroups);
    checkCriteriaGroupsAreRendered(sbomCriteriaGroups);
    checkCriteriaGroupTagsAreNotRendered(lifecycleCriteriaGroups);
  });

  it('Does not render the sections when showCriteriaBuilder is false', () => {
    renderComponent({ showCriteriaBuilder: false }, initialState);
    // The organizationId switch is rendered for both SBom and non SBom components
    const organizationId = screen.queryByText('organizationId');
    expect(organizationId).not.toBeInTheDocument();
  });
});
