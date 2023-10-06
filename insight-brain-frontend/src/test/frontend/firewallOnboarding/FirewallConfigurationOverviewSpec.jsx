/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';
import FirewallConfigurationOverview from 'MainRoot/firewallOnboarding/FirewallConfigurationOverview';
import { steps } from '../../../main/frontend/firewallOnboarding/firewallOnboardingUtils';

const currentStep = steps[1];
const firewallOnboardingPreloadedState = {
  firewallOnboarding: {
    loading: false,
    currentStep,
    supportedFormats: ['npm'],
    repositories: {
      loading: false,
      loadError: null,
      saving: false,
      saveError: null,
      list: [
        { id: '1', repositoryType: 'proxy', format: 'npm', quarantineEnabled: true },
        { id: '2', repositoryType: 'proxy', format: 'npm', quarantineEnabled: true },
        { id: '3', repositoryType: 'proxy', format: 'npm', quarantineEnabled: false },
        { id: '4', repositoryType: 'hosted', format: 'npm', namespaceConfusionProtectionEnabled: true },
        { id: '5', repositoryType: 'hosted', format: 'npm', namespaceConfusionProtectionEnabled: true },
        { id: '6', repositoryType: 'hosted', format: 'npm', namespaceConfusionProtectionEnabled: true },
        { id: '7', repositoryType: 'hosted', format: 'npm', namespaceConfusionProtectionEnabled: false },
        { id: '8', repositoryType: 'other', format: 'npm', quarantineEnabled: true },
        { id: '9', repositoryType: 'other', format: 'npm', quarantineEnabled: true },
        { id: '10', repositoryType: 'other', format: 'npm', quarantineEnabled: false },
        { id: '11', repositoryType: 'other', format: 'unsupportedFormat', quarantineEnabled: true },
      ],
    },
    unconfiguredRepoManagers: {
      repoManagers: [
        { id: 'id', instanceId: 'instanceId', userAgent: 'userAgent', configured: false, configureTime: null },
      ],
      loading: false,
      loadError: null,
    },
  },
};

describe('FirewallConfigurationOverview', function () {
  const renderComponent = (preloadedState = firewallOnboardingPreloadedState) =>
    render(<FirewallConfigurationOverview />, { preloadedState });

  it('renders correct number of selected proxy repositories', () => {
    renderComponent();
    const counter = screen.getByTestId('proxy-repositories-count');

    expect(counter).toBeVisible();
    expect(counter).toHaveTextContent('2 out of 3');
  });

  it('renders the namespace confusion protected repositories count', () => {
    renderComponent();
    const counter = screen.getByTestId('hosted-repositories-count');

    expect(counter).toBeVisible();
    expect(counter).toHaveTextContent('3 out of 4');
  });
});
