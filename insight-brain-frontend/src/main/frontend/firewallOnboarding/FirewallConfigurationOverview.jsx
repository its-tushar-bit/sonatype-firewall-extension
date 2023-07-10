/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { NxTile, NxPageTitle, NxP, NxH1, NxH2 } from '@sonatype/react-shared-components';
import ActionsFooter from './ActionsFooter';
import { stepsById } from './firewallOnboardingUtils';
import { selectTotalEnabledRepositoriesByTypeAndProp } from './firewallOnboardingSelectors';
import logo from '../img/inspect_and_complete_page_image.svg';

const currentStep = stepsById.protect;

export default function FirewallConfigurationOverview() {
  const totalEnabledProxyRepositories = useSelector((state) =>
    selectTotalEnabledRepositoriesByTypeAndProp(state, 'proxy', 'quarantineEnabled')
  );
  const totalEnabledHostedRepositories = useSelector((state) =>
    selectTotalEnabledRepositoriesByTypeAndProp(state, 'hosted', 'namespaceConfusionProtectionEnabled')
  );

  return (
    <>
      <NxPageTitle>
        <NxH1 className="firewall-onboarding-page__title">{currentStep.title}</NxH1>
        <NxP className="firewall-onboarding-page__subTitle">Inspect and complete onboarding.</NxP>
      </NxPageTitle>
      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Congratulations, you’re all set!</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          Once you launch Firewall, <b>malicious blocking</b> will be enabled for{' '}
          <b data-testid="proxy-repositories-count">{totalEnabledProxyRepositories}</b> proxy repositories and{' '}
          <b>namespace confusion protection</b> will be enabled for{' '}
          <b data-testid="hosted-repositories-count">{totalEnabledHostedRepositories}</b> hosted repositories.
          <div className="logo-container">
            <img src={logo} alt="Inspect and complete logo" />
          </div>
        </NxTile.Content>
        <ActionsFooter currentStep={currentStep} />
      </NxTile>
    </>
  );
}
