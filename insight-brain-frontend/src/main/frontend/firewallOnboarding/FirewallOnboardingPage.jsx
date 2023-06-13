/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxPageMain } from '@sonatype/react-shared-components';

import LoadWrapper from '../react/LoadWrapper';
import IncompleteConfigurationModal from './IncompleteConfigurationModal';
import WelcomeScreen from './WelcomeScreen';
import OnboardingScreen from './OnboardingScreen';
import { selectShowWelcomeScreen } from './firewallOnboardingSelectors';
import { setLeftNavigationOpen } from '../util/preferenceStore';
import { actions } from './firewallOnboardingSlice';

export default function FirewallOnboardingPage() {
  const dispatch = useDispatch();
  const openIncompleteConfigurationModal = (href) => dispatch(actions.openIncompleteConfigurationModal(href));
  const showWelcomeScreen = useSelector(selectShowWelcomeScreen);

  useEffect(() => {
    setLeftNavigationOpen(false);

    const globalSidebarEl = document.querySelector('.nx-global-sidebar');

    const captureClick = (event) => {
      const anchorEl = event.target.closest('a');
      if (anchorEl) {
        event.preventDefault();
        openIncompleteConfigurationModal(anchorEl.href);
      }
    };

    globalSidebarEl?.addEventListener('click', captureClick);
    return () => globalSidebarEl?.removeEventListener('click', captureClick);
  }, []);

  return (
    <NxPageMain id="firewall-onboarding-page" className="firewall-onboarding-page">
      <LoadWrapper loading={false} error={null} retryHandler={() => {}}>
        <IncompleteConfigurationModal />
        {showWelcomeScreen ? <WelcomeScreen /> : <OnboardingScreen />}
      </LoadWrapper>
    </NxPageMain>
  );
}
