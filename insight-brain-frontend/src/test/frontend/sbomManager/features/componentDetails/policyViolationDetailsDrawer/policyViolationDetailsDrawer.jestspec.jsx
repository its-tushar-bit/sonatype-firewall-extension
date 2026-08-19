/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, setupPortalContainer, waitFor } from 'TestRoot/SpecUtil';

import PolicyViolationDetailsDrawer from 'MainRoot/sbomManager/features/componentDetails/policyViolationDetailsDrawer/PolicyViolationDetailsDrawer';

describe('PolicyViolationDetailsDrawer', () => {
  let initialState;

  const POLICY_VIOLATION_ID = 'POLICY-VIOLATION-ID';
  const SBOM_VERSION = 'SBOM-VERSION';

  const renderComponent = (preloadedState) => render(<PolicyViolationDetailsDrawer />, { preloadedState });

  const mockPolicy = Object.freeze({
    allViolations: [
      {
        policyViolationId: POLICY_VIOLATION_ID,
        policyName: 'POLICY-NAME',
        policyThreatLevel: 9,
        constraints: [],
      },
    ],
  });

  const mockVulnerabilityDetails = Object.freeze({
    identifier: 'VULNERABILITY-ID',
    description: 'vulnerability-details-mock-description',
  });

  beforeAll(() => setupPortalContainer());

  beforeEach(() => {
    initialState = Object.freeze({
      router: {
        currentParams: {
          versionId: SBOM_VERSION,
        },
      },
      sbomComponentDetailsPage: {
        loadingVulnerabilityDetail: false,
        loadingVulnerabilityDetailError: null,
        policyViolationDetailsDrawer: {
          showDrawer: true,
          policyViolationId: POLICY_VIOLATION_ID,
          violationDetails: {
            policyThreatCategory: 'SECURITY',
            threatLevel: 9,
          },
        },
        sbomPolicyViolations: {
          policy: { ...mockPolicy },
        },
        vulnerabilityDetails: { ...mockVulnerabilityDetails },
      },
    });
  });

  it('renders the correct basic content', async () => {
    renderComponent(initialState);

    await waitFor(() => {
      expect(screen.getByText(/Violation of/)).toBeInTheDocument();
    });

    expect(screen.getByText(/Violation of POLICY-NAME/)).toBeInTheDocument();
    expect(screen.getByText(/Vulnerability Details/)).toBeInTheDocument();
    expect(screen.getByText('vulnerability-details-mock-description')).toBeInTheDocument();
  });
});
