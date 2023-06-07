/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxH1, NxP, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';

import LoadWrapper from '../react/LoadWrapper';
import OnboardingSteps from './OnboardingSteps';
import IncompleteConfigurationModal from './IncompleteConfigurationModal';
import ProxyRepositoriesSelector from './ProxyRepositoriesSelector';
import FirewallConfigurationOverview from './FirewallConfigurationOverview';

import { selectCurrentStep } from './firewallOnboardingSelectors';
import { actions } from './firewallOnboardingSlice';
import { setLeftNavigationOpen } from '../util/preferenceStore';
import { stepsIds } from './firewallOnboardingUtils';

const content = {
  [stepsIds.SELECT]: ProxyRepositoriesSelector,
  [stepsIds.PROTECT]: FirewallConfigurationOverview,
};

export default function FirewallOnboardingPage() {
  const dispatch = useDispatch();
  const openIncompleteConfigurationModal = (href) => dispatch(actions.openIncompleteConfigurationModal(href));

  const currentStep = useSelector(selectCurrentStep);
  const Content = content[currentStep.id];

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
