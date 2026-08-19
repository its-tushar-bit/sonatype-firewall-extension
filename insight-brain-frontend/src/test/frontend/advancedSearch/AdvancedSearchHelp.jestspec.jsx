/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'TestRoot/SpecUtil';
import AdvancedSearchHelp from 'MainRoot/advancedSearch/AdvancedSearchHelp';

describe('AdvancedSearchHelp', () => {
  const renderComponent = (preloadedState = {}) => render(<AdvancedSearchHelp />, { preloadedState });

  it('renders the "Search query examples" collapsible trigger collapsed by default', () => {
    renderComponent();
    expect(screen.getByRole('button', { name: 'Search query examples' })).toBeVisible();
    expect(screen.queryByText('policyViolationPolicyName:"License-Copyleft"')).not.toBeInTheDocument();
  });

  it('renders new policy violation and license example queries when expanded', async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByRole('button', { name: 'Search query examples' }));

    expect(screen.getByText('policyViolationPolicyName:"License-Copyleft"')).toBeVisible();
    expect(screen.getByText('componentLicenseThreatLevel:[8 TO 10]')).toBeVisible();
    expect(
      screen.getByText('policyViolationThreatCategory:license AND policyViolationWaiverStatus:"Active"')
    ).toBeVisible();
  });

  it('renders help explanation text for new examples when expanded', async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByRole('button', { name: 'Search query examples' }));

    expect(screen.getByText('Find components violating a specific policy')).toBeVisible();
    expect(screen.getByText('Find components with high-threat licenses')).toBeVisible();
    expect(screen.getByText('Find license policy violations with active waivers')).toBeVisible();
  });

  it('renders new examples in SBOM Manager mode when expanded', async () => {
    const user = userEvent.setup();
    renderComponent({
      router: {
        currentState: { name: 'sbomManager.advancedSearch' },
      },
    });

    await user.click(screen.getByRole('button', { name: 'Search query examples' }));

    expect(screen.getByText('policyViolationPolicyName:"License-Copyleft"')).toBeVisible();
    expect(screen.getByText('componentLicenseThreatLevel:[8 TO 10]')).toBeVisible();
    expect(
      screen.getByText('policyViolationThreatCategory:license AND policyViolationWaiverStatus:"Active"')
    ).toBeVisible();
  });
});
