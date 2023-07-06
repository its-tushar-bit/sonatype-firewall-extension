/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';
import OnboardingSteps from 'MainRoot/firewallOnboarding/OnboardingSteps';
import { steps } from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';

describe('OnboardingSteps', function () {
  const renderComponent = () => render(<OnboardingSteps currentStep={steps[0]} />);

  it('renders all the steps', () => {
    renderComponent();

    steps.forEach((step) => {
      const index = step.index + 1;
      expect(screen.getByRole('listitem', { name: `${index}. ${step.name}` })).toBeVisible();
    });
  });
});
