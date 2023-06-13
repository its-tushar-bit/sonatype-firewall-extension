/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';
import WelcomeScreen from 'MainRoot/firewallOnboarding/WelcomeScreen';

describe('WelcomeScreen', function () {
  const renderComponent = () => render(<WelcomeScreen />);

  it('renders screen with the correct text', () => {
    const DESCRIPTION_TEXT =
      'Protect against 3rd party malicious attacks, dependency confusion and investigate existing threats and risks in your repositories.';

    renderComponent();
    expect(screen.getByText('Welcome to Repository Firewall')).toBeVisible();
    expect(screen.getByText('Start step-by-step configuration')).toBeVisible();
    expect(screen.getByText(DESCRIPTION_TEXT)).toBeVisible();
  });

  it('renders "Get Started" button with the correct text', () => {
    renderComponent();
    expect(screen.getByRole('button')).toHaveTextContent('Get Started');
  });
});
