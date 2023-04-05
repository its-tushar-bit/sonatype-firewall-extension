/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { render, screen } from 'TestRoot/SpecUtil';
import React from 'react';
import FirewallOnboardingPage from 'MainRoot/firewallOnboarding/FirewallOnboardingPage';

describe('FirewallOnboardingPage', function () {
  let renderComponent;

  beforeEach(() => {
    renderComponent = () => render(<FirewallOnboardingPage />);
  });

  it('renders page with the correct text', () => {
    renderComponent();
    expect(screen.getByText('Firewall Onboarding Page')).toBeVisible();
  });
});
