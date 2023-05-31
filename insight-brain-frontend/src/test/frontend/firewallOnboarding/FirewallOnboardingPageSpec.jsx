/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';
import FirewallOnboardingPage from 'MainRoot/firewallOnboarding/FirewallOnboardingPage';
import { steps } from '../../../main/frontend/firewallOnboarding/firewallOnboardingUtils';

const currentStep = steps[0];
const firewallOnboardingPreloadedState = { firewallOnboarding: { currentStep } };

describe('FirewallOnboardingPage', function () {
  const renderComponent = (preloadedState = firewallOnboardingPreloadedState) =>
    render(<FirewallOnboardingPage />, { preloadedState });

  it('renders page with the correct text', () => {
    renderComponent();
    expect(screen.getByRole('complementary')).toBeVisible();
    expect(screen.getByText(currentStep.title)).toBeVisible();
    expect(screen.getByText('Proxy repositories selector')).toBeVisible();
    expect(screen.getByRole('navigation')).toBeVisible();
  });
});
