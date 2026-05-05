/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import AdvancedSearchHelp from 'MainRoot/advancedSearch/AdvancedSearchHelp';

describe('AdvancedSearchHelp', () => {
  const renderComponent = (preloadedState = {}) =>
    render(<AdvancedSearchHelp showHelp={true} toggleHelp={jest.fn()} />, { preloadedState });

  it('renders new policy violation and license example queries', () => {
    renderComponent();

    expect(screen.getByText('policyViolationPolicyName:"License-Copyleft"')).toBeVisible();
    expect(screen.getByText('componentLicenseThreatLevel:[8 TO 10]')).toBeVisible();
    expect(
      screen.getByText('policyViolationThreatCategory:license AND policyViolationWaiverStatus:"Active"')
    ).toBeVisible();
  });

  it('renders help explanation text for new examples', () => {
    renderComponent();

    expect(screen.getByText('Find components violating a specific policy')).toBeVisible();
    expect(screen.getByText('Find components with high-threat licenses')).toBeVisible();
    expect(screen.getByText('Find license policy violations with active waivers')).toBeVisible();
  });

  it('renders new examples in SBOM Manager mode', () => {
    renderComponent({
      router: {
        currentState: { name: 'sbomManager.advancedSearch' },
      },
    });

    expect(screen.getByText('policyViolationPolicyName:"License-Copyleft"')).toBeVisible();
    expect(screen.getByText('componentLicenseThreatLevel:[8 TO 10]')).toBeVisible();
    expect(
      screen.getByText('policyViolationThreatCategory:license AND policyViolationWaiverStatus:"Active"')
    ).toBeVisible();
  });
});
