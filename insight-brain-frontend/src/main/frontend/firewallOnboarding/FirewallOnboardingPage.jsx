/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { NxH1, NxP, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';

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
          <NxPageTitle>
            <NxH1 className="firewall-onboarding-page__title">{currentStep.title}</NxH1>
            {currentStep.subTitle && <NxP className="firewall-onboarding-page__subTitle">{currentStep.subTitle}</NxP>}
          </NxPageTitle>
          <Content />
        </div>
      </LoadWrapper>
    </NxPageMain>
  );
}
