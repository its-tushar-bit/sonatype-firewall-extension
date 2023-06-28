/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { NxH1, NxP, NxPageTitle } from '@sonatype/react-shared-components';

import OnboardingSteps from './OnboardingSteps';
import ProxyRepositoriesSelector from './ProxyRepositoriesSelector';
import HostedRepositoriesSelector from './HostedRepositoriesSelector';
import FirewallConfigurationOverview from './FirewallConfigurationOverview';

import { selectCurrentStep } from './firewallOnboardingSelectors';
import { stepsIds } from './firewallOnboardingUtils';

const content = {
  [stepsIds.SELECT_PROXY]: ProxyRepositoriesSelector,
  [stepsIds.SELECT_HOSTED]: HostedRepositoriesSelector,
  [stepsIds.PROTECT]: FirewallConfigurationOverview,
};

export default function FirewallOnboardingPage() {
  const currentStep = useSelector(selectCurrentStep);
  const Content = content[currentStep.id];

  return (
    <div className="onboarding-screen-wrapper">
      <aside className="sidebar">
        <OnboardingSteps currentStep={currentStep} isRequired={{}} />
      </aside>
      <div className="content">
        <Content />
      </div>
    </div>
  );
}
