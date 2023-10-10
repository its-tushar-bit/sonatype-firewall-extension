/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxTile, NxLoadingSpinner, NxPageTitle, NxH1, NxH2, NxLoadError } from '@sonatype/react-shared-components';
import ActionsFooter from './ActionsFooter';
import { stepsById } from './firewallOnboardingUtils';
import {
  selectHostedRepositoriesList,
  selectProxyRepositoriesList,
  selectTotalEnabledRepositoriesByTypeAndProp,
  selectLaunchFirewall,
} from './firewallOnboardingSelectors';
import { actions } from './firewallOnboardingSlice';
import logo from '../img/inspect_and_complete_page_image.svg';

const currentStep = stepsById.protect;

export default function FirewallConfigurationOverview() {
  const dispatch = useDispatch();
  const totalEnabledProxyRepositories = useSelector((state) =>
    selectTotalEnabledRepositoriesByTypeAndProp(state, 'proxy', 'quarantineEnabled')
  );
  const totalProxyRepositories = useSelector(selectProxyRepositoriesList);
  const totalEnabledHostedRepositories = useSelector((state) =>
    selectTotalEnabledRepositoriesByTypeAndProp(state, 'hosted', 'namespaceConfusionProtectionEnabled')
  );
  const totalHostedRepositories = useSelector(selectHostedRepositoriesList);
  const { saving, saveError } = useSelector(selectLaunchFirewall);

  return (
    <>
      <NxPageTitle>
        <NxH1 className="firewall-onboarding-page__title">Inspect and complete configuration</NxH1>
      </NxPageTitle>
      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Congratulations, you’ve reached the final step.</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          After you complete, Firewall configuration will run in the background and populate data related to all your
          enabled repositories.
          <br />
          Time taken to complete this process depends on the number of enabled repositories and size of each individual
          repository.
          <br />
          <br />
          <b>Malicious blocking</b> will be enabled for{' '}
          <b data-testid="proxy-repositories-count">
            {totalEnabledProxyRepositories || 0} out of {totalProxyRepositories.length}
          </b>{' '}
          proxy repositories.
          <br />
          <b>Namespace confusion protection</b> will block selected namespaces from{' '}
          <b data-testid="hosted-repositories-count">
            {totalEnabledHostedRepositories || 0} out of {totalHostedRepositories.length}
          </b>{' '}
          hosted repositories.
          <div className="logo-container">
            <img src={logo} alt="Inspect and complete logo" />
          </div>
        </NxTile.Content>
        {saving && <NxLoadingSpinner />}
        {saveError && (
          <NxLoadError titleMessage={' '} error={saveError} retryHandler={() => dispatch(actions.launchFirewall())} />
        )}
        <ActionsFooter currentStep={currentStep} />
      </NxTile>
    </>
  );
}
