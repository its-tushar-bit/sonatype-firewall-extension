/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, within, fireEvent, axiosMockAdapter } from 'TestRoot/SpecUtil';
import PrioritiesPageRow from 'MainRoot/development/prioritiesPage/PrioritiesPageRow';
import { faker } from '@faker-js/faker';
import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { getVersionGraphUrl } from 'MainRoot/util/CLMLocation';
import { stringifyComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';
import { dependencyTypeMap } from 'MainRoot/development/prioritiesPage/PrioritiesPageRow';

const publicAppId = 'testPublicAppId';
const scanId = 'testScanId';
const stageId = 'build';

const mockData = generateMockData();

describe('PrioritiesPageRow', () => {
  let renderComponent, axiosMock, selectIsDeveloperBulkRecommendationsEnabled;

  const rowClickSpy = jest.fn();

  const defaultPreloadedState = {
    router: {
      currentParams: {
        publicAppId,
        scanId,
      },
      currentState: {
        name: 'prioritiesPageFromDashboard',
      },
    },
    applicationReport: {
      metadata: {
        stageId: 'build',
      },
      recommendations: {},
    },
  };

  const minimalProps = {
    component: mockData,
    onClick: rowClickSpy,
  };

  beforeEach(() => {
    selectIsDeveloperBulkRecommendationsEnabled = jest
      .spyOn(ProductFeaturesSelectors, 'selectIsDeveloperBulkRecommendationsEnabled')
      .mockReturnValue(true);

    axiosMock = axiosMockAdapter();

    renderComponent = (preloadedState) =>
      render(<PrioritiesPageRow {...minimalProps} />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders a clickable row', () => {
    renderComponent();

    const row = screen.getByRole('row');
    expect(row).toBeInTheDocument();

    fireEvent.click(row);
    expect(rowClickSpy).toHaveBeenCalled();
  });

  it('does not make network requests if developerBulkRecommendations feature flag is enabled', () => {
    renderComponent();
    expect(axiosMock.history.get.length).toBe(0);
  });

  it('makes network requests only if developerBulkRecommendations feature flag is disabled', () => {
    selectIsDeveloperBulkRecommendationsEnabled.mockReturnValue(false);

    const requestData = {
      clientType: 'ci',
      ownerType: 'application',
      ownerId: publicAppId,
      matchState: 'exact',
      proprietary: 'false',
      identificationSource: 'Sonatype',
      componentIdentifier: mockData.componentIdentifier
        ? stringifyComponentIdentifier(mockData.componentIdentifier, 'exact')
        : null,
      hash: mockData.componentHash,
      scanId,
      displayName: mockData.displayName,
      stageId,
      dependencyType: dependencyTypeMap[mockData.dependencyType],
    };
    renderComponent();
    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getVersionGraphUrl(requestData));
  });

  it('renders correct data', () => {
    renderComponent();

    const row = screen.getByRole('row');
    expect(row).toBeInTheDocument();

    const cells = within(row).getAllByRole('cell');

    const priorityCell = cells[0];
    expect(priorityCell).toHaveTextContent(mockData.priority);

    const componentCell = cells[1];
    expect(componentCell).toHaveTextContent(mockData.displayName);

    const reasonForPriorityCell = cells[2];
    expect(reasonForPriorityCell).toHaveTextContent(mockData.highestThreat);
    expect(reasonForPriorityCell).toHaveTextContent(mockData.highestThreatPolicyName);

    if (mockData.action !== 'none') {
      expect(reasonForPriorityCell).toHaveTextContent(mockData.action);
    }

    if (mockData.securityReachable) {
      expect(reasonForPriorityCell).toHaveTextContent('Reachable');
    }

    const suggestedFixCell = cells[3];
    expect(suggestedFixCell).toHaveTextContent(`${mockData.remediationVersion}`);
  });
});

function generateMockData() {
  const hasFail = faker.datatype.boolean();
  const componentHash = faker.git.commitSha();

  return {
    displayName: faker.lorem.word(1),
    componentIdentifier: {
      format: faker.datatype.string(),
      coordinates: {
        artifactId: faker.datatype.string(),
        classifier: faker.datatype.string(),
        extension: faker.datatype.string(),
        groupId: faker.datatype.string(),
        version: '0.5',
      },
    },
    componentHash,
    dependencyType: faker.helpers.arrayElement(['Direct', 'Transitive', 'Inner Source']),
    hasFailActionOnComponent: hasFail,
    action: hasFail ? 'fail' : faker.helpers.arrayElement(['none', 'warn']),
    highestThreat: faker.datatype.number({ min: 0, max: 10 }),
    highestThreatPolicyName: faker.lorem.slug(),
    highestThreatPolicyConstraintName: faker.lorem.sentence(),
    priority: 1,
    securityReachable: faker.datatype.boolean(),
    remediationType: 'next-non-failing',
    remediationVersion: '1.0',
  };
}
