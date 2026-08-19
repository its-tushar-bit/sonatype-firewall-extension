/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import FirewallOnboardingPage from 'MainRoot/firewallOnboarding/FirewallOnboardingPage';
import { steps } from '../../../main/frontend/firewallOnboarding/firewallOnboardingUtils';

const firewallOnboardingPreloadedState = {
  firewallOnboarding: {
    showWelcomeScreen: true,
    currentStep: steps[0],
    incompleteConfigurationModal: {
      showModal: false,
    },
    repositories: {
      loading: false,
      loadError: null,
    },
    unconfiguredRepoManagers: {
      repoManagers: [],
      loading: false,
      loadError: null,
    },
    protectionRules: {
      supplyChainAttacksProtectionEnabled: false,
      namespaceConfusionProtectionEnabled: false,
      configuring: false,
      configureError: null,
    },
    launchFirewall: {
      saving: false,
      saveError: null,
    },
  },
};
const FIRST_STEP_TITLE = 'Enable Repository Firewall features';
const WELCOME_SCREEN_TITLE = 'Welcome to Repository Firewall';

describe('FirewallOnboardingPage', function () {
  const renderComponent = (preloadedState = firewallOnboardingPreloadedState) =>
    render(<FirewallOnboardingPage />, { preloadedState });

  it('renders the welcome screen when a user arrives on the page', () => {
    renderComponent();

    expect(screen.getByText(WELCOME_SCREEN_TITLE)).toBeVisible();
    expect(screen.queryByText(FIRST_STEP_TITLE)).not.toBeInTheDocument();
  });

  it('renders the onboarding screen after the user clicks the "Get Started" button', () => {
    renderComponent();
    const getStartedBtn = screen.getByRole('button', { name: 'Get Started' });

    fireEvent.click(getStartedBtn);
    expect(screen.getByText(FIRST_STEP_TITLE)).toBeVisible();
    expect(screen.queryByText(WELCOME_SCREEN_TITLE)).not.toBeInTheDocument();
  });
});
