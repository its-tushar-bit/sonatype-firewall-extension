/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxTile, NxLoadWrapper, NxPageTitle, NxP, NxH1, NxTextLink } from '@sonatype/react-shared-components';

import ActionsFooter from './ActionsFooter';
import { actions } from './firewallOnboardingSlice';
import {
  selectRepositoriesByType,
  selectRepositories,
  selectUnconfiguredRepoManager,
  selectSupportedFormats,
  selectProtectionRules,
} from './firewallOnboardingSelectors';
import { getRepoPageTitleByFormat } from './firewallOnboardingUtils';
import { selectCurrentStep } from './firewallOnboardingSelectors';
import FirewallRepositoryList from 'MainRoot/react/FirewallRepositoryList/FirewallRepositoryList';
import { stepsIds } from './firewallOnboardingUtils';

const NAMESPACE_CONFUSION_PROTECTION_URL = 'http://links.sonatype.com/products/nxiq/doc/preventing-namespace-confusion';
const repositoriesSelectorPropsByStep = {
  [stepsIds.SELECT_PROXY]: {
    formatType: 'proxy',
    subtitle: 'Choose which proxy repositories you would like to apply your protection rules to.',
  },
  [stepsIds.SELECT_HOSTED]: {
    formatType: 'hosted',
    firewallRepositoryListProps: {
      checkItemPropName: 'namespaceConfusionProtectionEnabled',
    },
    subtitle: (
      <>
        Choose which hosted repositories you would like to enable{' '}
        <NxTextLink href={NAMESPACE_CONFUSION_PROTECTION_URL} external>
          namespace confusion protection
        </NxTextLink>{' '}
        on.
      </>
    ),
  },
};

export default function RepositoriesSelector() {
  const currentStep = useSelector(selectCurrentStep);
  const { formatType, firewallRepositoryListProps = {} } = repositoriesSelectorPropsByStep[currentStep.id];
  const { loading, loadError } = useSelector(selectRepositories);
  const unconfiguredRepoManager = useSelector(selectUnconfiguredRepoManager);
  const reposGroupedByFormat = useSelector((state) => selectRepositoriesByType(state, formatType));
  const supportedFormats = useSelector(selectSupportedFormats);
  const { namespaceConfusionProtectionEnabled, supplyChainAttacksProtectionEnabled } = useSelector(
    selectProtectionRules
  );

  const dispatch = useDispatch();
  const loadRepositories = () => dispatch(actions.loadRepositories(unconfiguredRepoManager));
  const configureRepositories = (repository) => dispatch(actions.configureRepositories(repository));

  const renderRepositoriesByFormat = () => {
    const hasRepositories = reposGroupedByFormat.some(
      (repositoriesByFormat) => repositoriesByFormat.repositories.length > 0
    );
    if (!hasRepositories) {
      return `There are no ${formatType} repositories to apply your protection rules.`;
    }
    return reposGroupedByFormat.map((repositoriesByFormat) => (
      <FirewallRepositoryList
        key={repositoriesByFormat.format}
        title={repositoriesByFormat.format}
        repositories={repositoriesByFormat.repositories}
        supportedFormats={supportedFormats}
        onChange={(updatedItems) => {
          configureRepositories(updatedItems);
        }}
        {...firewallRepositoryListProps}
      />
    ));
  };

  const title = getRepoPageTitleByFormat[formatType].title(
    supplyChainAttacksProtectionEnabled,
    namespaceConfusionProtectionEnabled
  );

  const subtitle = getRepoPageTitleByFormat[formatType].subtitle(
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

RepositoriesSelector.propTypes = {
  step: PropTypes.object,
};
