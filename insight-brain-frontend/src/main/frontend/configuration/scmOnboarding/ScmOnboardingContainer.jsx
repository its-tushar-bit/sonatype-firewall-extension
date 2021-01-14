/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {connect} from 'react-redux';
import * as scmOnboardingActions from './scmOnboardingActions';
import * as PropTypes from 'prop-types';

import ScmOnboarding from '../scmOnboarding/ScmOnboarding';

function mapStateToProps({ scmOnboarding, router }) {
  return {
    // config
    loadingPage: scmOnboarding.viewState.loadingPage,
    isScmOnboardingFeatureEnabled: scmOnboarding.configState.isScmOnboardingFeatureEnabled,
    scmProvider: scmOnboarding.configState.scmProvider,

    // compositeSourceControl data
    isScmTokenConfigured: scmOnboarding.configState.isScmTokenConfigured,
    validateScmHostUrl: scmOnboarding.viewState.validateScmHostUrl,

    // organizations
    organizations: scmOnboarding.formState.organizations,
    selectedOrganization: scmOnboarding.formState.selectedOrganization,

    // repositories
    loadingRepositories: scmOnboarding.viewState.loadingRepositories,
    repositories: scmOnboarding.formState.repositories,
    totalRepositories: scmOnboarding.formState.totalRepositories,
    selectedRepositoryCount: scmOnboarding.formState.selectedRepositoryCount,
    importedRepositoryCount: scmOnboarding.formState.importedRepositoryCount,
    failedImportCount: scmOnboarding.formState.failedImportCount,
    newlyImportedRepos: scmOnboarding.formState.newlyImportedRepos,

    // host URL
    defaultHostUrl: scmOnboarding.formState.defaultHostUrl,
    currentHostUrlState: scmOnboarding.formState.currentHostUrlState,

    // sorting
    sortConfiguration: scmOnboarding.sortConfiguration,

    // actions
    loadPage: scmOnboarding.loadPage,
    onRepositorySelectionChanged: scmOnboarding.onRepositorySelectionChanged,
    importSelectedRepositories: scmOnboarding.importSelectedRepositories,
    setSelectedOrganization: scmOnboarding.setSelectedOrganization,
    setSorting: scmOnboarding.setSorting,
    setSortingParameters: scmOnboarding.setSortingParameters,

    // router state
    preselectedOrganizationId: router.currentParams.organizationId,

    // error state
    lastErrorMessage: scmOnboarding.viewState.lastErrorMessage
  };
}

const ScmOnboardingContainer = connect(mapStateToProps, scmOnboardingActions)(ScmOnboarding);
export default ScmOnboardingContainer;

ScmOnboardingContainer.propTypes = {
  preselectedOrganizationId: PropTypes.string,
  scmOnboardingActions: PropTypes.shape({
    loadPage: PropTypes.func.isRequired,
    loadRepositories: PropTypes.func.isRequired,
    validateScmHostUrl: PropTypes.func.isRequired
  })
};
