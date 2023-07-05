/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxTile, NxLoadWrapper, NxPageTitle, NxP, NxH1, NxTextLink } from '@sonatype/react-shared-components';

import ActionsFooter from './ActionsFooter';
import { actions } from './firewallOnboardingSlice';
import {
  selectRepositoriesByType,
  selectRepositories,
  selectUnconfiguredRepoManager,
  selectSupportedFormats,
} from './firewallOnboardingSelectors';
import { steps } from './firewallOnboardingUtils';
import FirewallRepositoryList from 'MainRoot/react/FirewallRepositoryList/FirewallRepositoryList';

const currentStep = steps[1];

const NAMESPACE_CONFUSION_PROTECTION_URL = 'http://links.sonatype.com/products/nxiq/doc/preventing-namespace-confusion';

export default function HostedRepositoriesSelector() {
  const { loading, loadError } = useSelector(selectRepositories);
  const unconfiguredRepoManager = useSelector(selectUnconfiguredRepoManager);
  const hostedReposGroupByFormat = useSelector((state) => selectRepositoriesByType(state, 'hosted'));
  const supportedFormats = useSelector(selectSupportedFormats);

  const dispatch = useDispatch();
  const loadRepositories = () => dispatch(actions.loadRepositories(unconfiguredRepoManager));
  const configureRepositories = (repository) => dispatch(actions.configureRepositories(repository));

  const renderRepositoriesByFormat = () => {
    const hasRepositories = hostedReposGroupByFormat.some(
      (repositoriesByFormat) => repositoriesByFormat.repositories.length > 0
    );
    if (!hasRepositories) {
      return 'There are no hosted repositories to apply your protection rules.';
    }
    return hostedReposGroupByFormat.map((repositoriesByFormat) => (
      <FirewallRepositoryList
        key={repositoriesByFormat.format}
        title={repositoriesByFormat.format}
        repositories={repositoriesByFormat.repositories}
        supportedFormats={supportedFormats}
        onChange={(updatedItems) => {
          configureRepositories(updatedItems);
        }}
        checkItemPropName="namespaceConfusionProtectionEnabled"
      />
    ));
  };

  return (
    <>
      <NxPageTitle>
        <NxH1 className="firewall-onboarding-page__title">{currentStep.title}</NxH1>
        <NxP className="firewall-onboarding-page__subTitle">
          Choose which hosted repositories you would like to enable{' '}
          <NxTextLink href={NAMESPACE_CONFUSION_PROTECTION_URL} external>
            namespace confusion protection
          </NxTextLink>{' '}
          on.
        </NxP>
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
