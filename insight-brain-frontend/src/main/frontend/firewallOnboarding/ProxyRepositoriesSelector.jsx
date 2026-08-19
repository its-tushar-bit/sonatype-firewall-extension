/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxTile, NxLoadWrapper, NxPageTitle, NxP, NxH1 } from '@sonatype/react-shared-components';
import ActionsFooter from './ActionsFooter';
import { actions } from './firewallOnboardingSlice';
import {
  selectRepositoriesByType,
  selectRepositories,
  selectUnconfiguredRepoManager,
  selectSupportedFormats,
  selectProtectionRules,
} from './firewallOnboardingSelectors';
import { stepsById, getRepoPageTitleByFormat } from './firewallOnboardingUtils';
import FirewallRepositoryList from 'MainRoot/react/FirewallRepositoryList/FirewallRepositoryList';

const currentStep = stepsById.selectProxy;

export default function ProxyRepositoriesSelector() {
  const { loading, loadError } = useSelector(selectRepositories);
  const unconfiguredRepoManager = useSelector(selectUnconfiguredRepoManager);
  const proxyReposGroupByFormat = useSelector((state) => selectRepositoriesByType(state, 'proxy'));
  const supportedFormats = useSelector(selectSupportedFormats);
  const { namespaceConfusionProtectionEnabled, supplyChainAttacksProtectionEnabled } = useSelector(
    selectProtectionRules
  );

  const dispatch = useDispatch();
  const loadRepositories = () => dispatch(actions.loadRepositories(unconfiguredRepoManager));
  const configureRepositories = (repository) => dispatch(actions.configureRepositories(repository));

  const renderRepositoriesByFormat = () => {
    const hasRepositories = proxyReposGroupByFormat.some(
      (repositoriesByFormat) => repositoriesByFormat.repositories.length > 0
    );
    if (!hasRepositories) {
      return 'There are no proxy repositories to apply your protection rules.';
    }

    return proxyReposGroupByFormat.map((repositoriesByFormat) => (
      <FirewallRepositoryList
        key={repositoriesByFormat.format}
        title={repositoriesByFormat.format}
        repositories={repositoriesByFormat.repositories}
        supportedFormats={supportedFormats}
        onChange={(updatedItems) => {
          configureRepositories(updatedItems);
        }}
      />
    ));
  };

  const title = getRepoPageTitleByFormat.proxy.title(
    supplyChainAttacksProtectionEnabled,
    namespaceConfusionProtectionEnabled
  );

  const subtitle = getRepoPageTitleByFormat.proxy.subtitle(
    supplyChainAttacksProtectionEnabled,
    namespaceConfusionProtectionEnabled
  );

  return (
    <>
      <NxPageTitle>
        <NxH1 className="firewall-onboarding-page__title">{title}</NxH1>
        <NxP className="firewall-onboarding-page__subTitle">{subtitle}</NxP>
      </NxPageTitle>
      <NxTile>
        <NxTile.Content className="select-repositories-container">
          <NxLoadWrapper loading={loading} error={loadError} retryHandler={loadRepositories}>
            {renderRepositoriesByFormat()}
          </NxLoadWrapper>
        </NxTile.Content>
        <ActionsFooter currentStep={currentStep} />
      </NxTile>
    </>
  );
}
