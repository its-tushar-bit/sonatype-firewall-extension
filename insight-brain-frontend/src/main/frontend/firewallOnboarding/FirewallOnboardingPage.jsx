/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { NxPageMain } from '@sonatype/react-shared-components';

import LoadWrapper from '../react/LoadWrapper';
import OnboardingSteps from './OnboardingSteps';
import ProxyRepositoriesSelector from './ProxyRepositoriesSelector';
import FirewallConfigurationOverview from './FirewallConfigurationOverview';
import { selectCurrentStep } from './firewallOnboardingSelectors';
import { stepsIds } from './firewallOnboardingUtils';

const content = {
  [stepsIds.SELECT]: ProxyRepositoriesSelector,
  [stepsIds.PROTECT]: FirewallConfigurationOverview,
};

export default function FirewallOnboardingPage() {
  const currentStep = useSelector(selectCurrentStep);
  const Content = content[currentStep.id];

  return (
    <NxPageMain id="firewall-onboarding-page" className="firewall-onboarding-page">
      <LoadWrapper loading={false} error={null} retryHandler={() => {}}>
        <aside className="sidebar">
          <OnboardingSteps currentStep={currentStep} isRequired={{}} />
        </aside>
        <div className="content">
          <header className="nx-page-title">
            <h1 className="nx-h1 iq-dependency-tree__title">{currentStep.title}</h1>
          </header>
          <Content />
        </div>
      </LoadWrapper>
    </NxPageMain>
  );
}
