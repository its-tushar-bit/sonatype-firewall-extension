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
let renderComponent, firewallOnboardingPreloadedState;

describe('FirewallOnboardingPage', function () {
  beforeEach(function () {
    firewallOnboardingPreloadedState = {
      firewallOnboarding: {
        incompleteConfigurationModal: { showModal: false },
        loading: false,
        currentStep,
        supportedFormats: [],
        repositories: {
          loading: false,
          loadError: null,
          saving: false,
          saveError: null,
          list: [{ id: '1', repositoryType: 'proxy', quarantineEnabled: true }],
        },
        unconfiguredRepoManagers: {
          repoManagers: [
            { id: 'id', instanceId: 'instanceId', userAgent: 'userAgent', configured: false, configureTime: null },
          ],
          loading: false,
          loadError: null,
        },
        protectionRules: {
          supplyChainAttacksProtectionEnabled: false,
          namespaceConfusionProtectionEnabled: false,
        },
        launchFirewall: {
          saving: false,
          saveError: null,
        },
      },
    };
    renderComponent = (preloadedState = firewallOnboardingPreloadedState) =>
      render(<FirewallOnboardingPage />, { preloadedState });
  });

  steps.forEach((currentStep) => {
    it(`renders ${currentStep.name} page with the correct text`, () => {
      firewallOnboardingPreloadedState.firewallOnboarding.currentStep = currentStep;
      renderComponent(firewallOnboardingPreloadedState);

      expect(screen.getByRole('complementary')).toBeVisible();
      expect(screen.getByRole('navigation')).toBeVisible();
    });
  });
});
