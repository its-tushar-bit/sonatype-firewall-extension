/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { NxPageMain } from '@sonatype/react-shared-components';

import LoadWrapper from '../react/LoadWrapper';
import WelcomeScreen from './WelcomeScreen';
import OnboardingScreen from './OnboardingScreen';
import { selectShowWelcomeScreen } from './firewallOnboardingSelectors';
import { setLeftNavigationOpen, isLeftNavigationOpen } from '../util/preferenceStore';

export default function FirewallOnboardingPage() {
  const showWelcomeScreen = useSelector(selectShowWelcomeScreen);

  useEffect(() => {
    if (isLeftNavigationOpen()) {
      setLeftNavigationOpen(false);
    }
  }, []);

  return (
    <NxPageMain id="firewall-onboarding-page" className="firewall-onboarding-page">
      <LoadWrapper loading={false} error={null} retryHandler={() => {}}>
        {showWelcomeScreen ? <WelcomeScreen /> : <OnboardingScreen />}
      </LoadWrapper>
    </NxPageMain>
  );
}
