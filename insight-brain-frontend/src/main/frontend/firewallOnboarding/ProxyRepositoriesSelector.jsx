/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxTile, NxLoadWrapper } from '@sonatype/react-shared-components';

import ActionsFooter from './ActionsFooter';
import { actions } from './firewallOnboardingSlice';
import {
  selectRepositoriesByType,
  selectRepositories,
  selectUnconfiguredRepoManager,
} from './firewallOnboardingSelectors';
import { steps } from './firewallOnboardingUtils';
import FirewallRepositoryList from 'MainRoot/react/FirewallRepositoryList/FirewallRepositoryList';

const [step] = steps;

export default function ProxyRepositoriesSelector() {
  const { loading, loadError } = useSelector(selectRepositories);
  const unconfiguredRepoManager = useSelector(selectUnconfiguredRepoManager);
  const proxyReposGroupByFormat = useSelector((state) => selectRepositoriesByType(state, 'proxy'));

  const dispatch = useDispatch();
  const loadRepositories = () => dispatch(actions.loadRepositories(unconfiguredRepoManager));
  const configureRepositories = (repository) => dispatch(actions.configureRepositories(repository));

  const renderRepositoriesByFormat = () => {
    if (!proxyReposGroupByFormat.length) {
      return 'There are no proxy repositories to apply your protection rules.';
    }
    return proxyReposGroupByFormat.map((repositoriesByFormat) => (
      <FirewallRepositoryList
        key={repositoriesByFormat.format}
        title={repositoriesByFormat.format}
        repositories={repositoriesByFormat.repositories}
        selectedRepositories={[]}
        onChange={(updatedItems) => {
          configureRepositories(updatedItems);
        }}
      />
    ));
  };

  return (
    <NxTile>
      <NxTile.Content className="select-repositories-container">
        <NxLoadWrapper loading={loading} error={loadError} retryHandler={loadRepositories}>
          {renderRepositoriesByFormat()}
        </NxLoadWrapper>
      </NxTile.Content>
      <ActionsFooter currentStep={step} />
    </NxTile>
  );
}
