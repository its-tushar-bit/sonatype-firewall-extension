/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from 'TestRoot/SpecUtil';
import { screen } from '@testing-library/dom';
import ComponentDetailsDependencyTreeTile from 'MainRoot/sbomManager/features/componentDetails/dependecyTree/ComponentDetailsDependencyTreeTile';

describe('ComponentDetailsDependencyTreeTile', () => {
  let renderComponent;
  const mockComponentDetails = {
    name: 'com.fasterxml.jackson.core : jackson-databind : 2.4.1',
    hash: 'f07c773f7b3a03c3801d',
    purl: 'pkg:maven/net.sf.jason/jason-schema@1.2.11',
    dependencyType: 'direct',
    format: 'maven',
    metadata: {
      applicationName: 'sbom',
      organizationName: 'test-org',
      reportTime: 1713279301273,
      reportTitle: 'BOM',
    },
    componentSummary: {
      highestCVSSScore: 9,
      verifiedVulnerabilities: {
        verified: 10,
        unverified: 5,
      },
    },
    disclosedVulnerabilities: [
      {
        cvssScore: 9,
        issue: 'sonatype-2018-0863',
        isVerified: true,
        analysisStatus: 'resolved',
        justification: 'code_not_present',
        details: 'Unreachable code',
      },
    ],
    additionalVulnerabilities: [
      {
        cvssScore: 9,
        issue: 'sonatype-2018-0863',
      },
    ],
  };

  const mockDependencyTreeData = [
    {
      children: null,
      isOpen: true,
      treePath: [0],
      originalTreePath: [0],
      hash: 'b6a0d2d511bec1ca3079',
      policyThreatLevel: 9,
      displayName: 'org.jgrapht : jgrapht-core : 1.4.0',
    },
  ];

  beforeEach(() => {
    const preloadedState = {
      sbomComponentDetailsPage: {
        loading: false,
        loadingDependencyTree: false,
        loadDependencyTreeError: null,
        loadError: null,
        publicAppId: null,
        componentDetails: null,
        dependencyTreeSubset: null,
      },
    };
    renderComponent = (additionalPreloadedState = {}) =>
      render(
        <ComponentDetailsDependencyTreeTile componentDetails={mockComponentDetails} preloadedState={preloadedState} />,
        {
          preloadedState: { ...preloadedState, ...additionalPreloadedState },
        }
      );
  });

  it('Renders tree component', async () => {
    renderComponent({
      componentDetails: mockComponentDetails,
      sbomComponentDetailsPage: {
        dependencyTreeSubset: mockDependencyTreeData,
      },
    });

    expect(screen.getByText('Dependency Tree')).toBeVisible();
    expect(screen.getByText('org.jgrapht : jgrapht-core : 1.4.0')).toBeVisible();
  });

  it('returns null and nothing is rendered when no componentDetails has been specified', async () => {
    render(<ComponentDetailsDependencyTreeTile />, {
      preloadedState: {},
    });

    const dependencyTreeHeaderText = await screen.queryByText('Dependency Tree');
    expect(dependencyTreeHeaderText).toBeNull();
  });

  it('shows error when dependency tree is not available for component', async () => {
    renderComponent({});
    const errorMessage = await screen.findByText('Dependency tree not available');
    expect(errorMessage).toBeVisible();
  });
});
