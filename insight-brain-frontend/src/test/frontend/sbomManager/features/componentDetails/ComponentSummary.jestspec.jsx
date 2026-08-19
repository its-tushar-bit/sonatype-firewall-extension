/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from 'TestRoot/SpecUtil';
import { screen } from '@testing-library/dom';
import ComponentSummary from 'MainRoot/sbomManager/features/componentDetails/ComponentSummary';

describe('ComponentDetailsPage', () => {
  let renderPage;

  const mockComponentSummary = {
    isSbomPoliciesSupported: false,
    vulnerabilitySummary: {
      highestCvssScore: 9,
      verifiedVulnerabilitiesCount: 84,
      unverifiedVulnerabilitiesCount: 21,
      sonatypeIdentifiedVulnerabilitiesCount: 0,
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
    policyViolationSummary: {
      severe: 1,
      critical: 3,
    },
  };

  beforeEach(() => {
    renderPage = (props = {}) => render(<ComponentSummary {...props} />);
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

  it('Renders policy violations section when sbomPolicies is enabled', async () => {
    const props = {
      isSbomPoliciesSupported: true,
    };
    renderPage({ ...mockComponentSummary, ...props });
    expect(screen.getByText('Policy Violations')).toBeVisible();
    const severeThreatCounter = await screen.findByTestId('severe-threat-counter');
    expect(severeThreatCounter).toBeInTheDocument();
    expect(severeThreatCounter.textContent).toContain(mockComponentSummary.policyViolationSummary.severe.toString());

    const criticalThreatCounter = await screen.findByTestId('critical-threat-counter');
    expect(criticalThreatCounter).toBeInTheDocument();
    expect(criticalThreatCounter.textContent).toContain(
      mockComponentSummary.policyViolationSummary.critical.toString()
    );
  });
});
