/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxPageMain, NxTile } from '@sonatype/react-shared-components';

import LoadWrapper from '../react/LoadWrapper';
import OnboardingSteps from './OnboardingSteps';
import ActionsFooter from './ActionsFooter';
import { selectCurrentStep } from './firewallOnboardingSelectors';
import { actions } from './firewallOnboardingSlice';
import { stateGo } from '../reduxUiRouter/routerActions';
import { setShowWelcomeModalToTrueInStore } from '../firewall/firewallWelcomeModalStore';

export default function FirewallOnboardingPage() {
  const dispatch = useDispatch();
  const continueToNextStep = () => dispatch(actions.continueToNextStep());
  const goBackToPreviousStep = () => dispatch(actions.goBackToPreviousStep());
  const currentStep = useSelector(selectCurrentStep);

  const handleLaunch = () => {
    // Do Launch Stuff
    setShowWelcomeModalToTrueInStore();
    dispatch(stateGo('firewall.firewallPage'));
  };

  return (
    <NxPageMain id="firewall-onboarding-page" className="firewall-onboarding-page">
      <LoadWrapper loading={false} error={null} retryHandler={() => {}}>
        <aside className="sidebar">
          <OnboardingSteps currentStep={currentStep} isRequired={{}} />
        </aside>
        <div className="content">
          <header className="nx-page-title">
            <h1 className="nx-h1 iq-dependency-tree__title">Select proxy repositories</h1>
          </header>
          <NxTile>
            <NxTile.Content>content</NxTile.Content>
            <ActionsFooter
              currentStep={currentStep}
              onPrevious={goBackToPreviousStep}
              onNext={continueToNextStep}
              onLaunch={handleLaunch}
            />
          </NxTile>
        </div>
      </LoadWrapper>
    </NxPageMain>
  );
}
