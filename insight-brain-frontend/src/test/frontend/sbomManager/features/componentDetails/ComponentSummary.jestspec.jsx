/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from 'TestRoot/SpecUtil';
import { screen } from '@testing-library/dom';
import VulnerabilitiesSummary from 'MainRoot/sbomManager/features/componentDetails/VulnerabilitiesSummary';

describe('ComponentDetailsPage', () => {
  let renderPage;

  const mockComponentSummary = {
    vulnerabilitySummary: {
      highestCvssScore: 9,
      verifiedVulnerabilitiesCount: 84,
      unverifiedVulnerabilitiesCount: 21,
    },
    disclosedVulnerabilities: [
      {
        cvssScore: 9,
        issue: 'sonatype-2018-0863',
        analysisStatus: 'resolved',
        justification: 'code_not_present',
        details: 'Unreachable code',
        verified: true,
      },
    ],
    additionalVulnerabilities: [],
  };

  beforeEach(() => {
    renderPage = (props = {}) => render(<VulnerabilitiesSummary {...props} />);
  });

  it('Renders page content', async () => {
    renderPage(mockComponentSummary);
    expect(await screen.findByText('Component Summary')).toBeVisible();
    expect(screen.getByText('Highest CVSS Score')).toBeVisible();
    expect(screen.getByText(mockComponentSummary.vulnerabilitySummary.highestCvssScore)).toBeVisible();

    expect(screen.getByText('Vulnerabilities Verified')).toBeVisible();
    const verifiedContainer = await screen.findByTestId('verified');
    expect(verifiedContainer).toBeInTheDocument();
    expect(verifiedContainer.textContent).toEqual(
      mockComponentSummary.vulnerabilitySummary.verifiedVulnerabilitiesCount + ' Sonatype Verified'
    );

    const unverifiedContainer = await screen.findByTestId('unverified');
    expect(unverifiedContainer).toBeInTheDocument();
    expect(unverifiedContainer.textContent).toEqual(
      mockComponentSummary.vulnerabilitySummary.unverifiedVulnerabilitiesCount + ' Unverified'
    );
  });

  it('Renders page content with category and  website if present', async () => {
    const props = {
      vulnerabilitySummary: {
        highestCvssScore: 9,
        verifiedVulnerabilitiesCount: 4,
        unverifiedVulnerabilitiesCount: 2,
        category: 'Some Category',
        website: 'someURL',
      },
    };
    renderPage(props);

    expect(await screen.findByText('Component Summary')).toBeVisible();

    const categoryContainer = await screen.findByTestId('category');
    expect(categoryContainer).toBeInTheDocument();
    expect(categoryContainer.textContent).toContain(props.vulnerabilitySummary.category);

    const websiteContainer = await screen.findByTestId('website');
    expect(websiteContainer).toBeInTheDocument();
    expect(websiteContainer.textContent).toEqual(props.vulnerabilitySummary.website);
  });
});
