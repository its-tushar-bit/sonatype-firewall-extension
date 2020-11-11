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
    loadingConfig: scmOnboarding.loadingConfig,
    isScmOnboardingFeatureEnabled: scmOnboarding.isScmOnboardingFeatureEnabled,
    scmProvider: scmOnboarding.scmProvider,

    // compositeSourceControl data
    isScmTokenConfigured: scmOnboarding.viewState.isScmTokenConfigured,
    loadingCompositeSourceControl: scmOnboarding.viewState.loadingCompositeSourceControl,

    // organizations
    loadingOrganizations: scmOnboarding.loadingOrganizations,
    organizations: scmOnboarding.organizations,
    selectedOrganization: scmOnboarding.selectedOrganization,

    // repositories
    loadingRepositories: scmOnboarding.loadingRepositories,
    repositories: scmOnboarding.repositories,
    totalRepositories: scmOnboarding.totalRepositories,
    selectedRepositoryCount: scmOnboarding.selectedRepositoryCount,
    importedRepositoryCount: scmOnboarding.importedRepositoryCount,

    // host URL
    defaultHostUrl: scmOnboarding.defaultHostUrl,
    currentHostUrl: scmOnboarding.currentHostUrl,

    // actions
    onRepositorySelectionChanged: scmOnboarding.onRepositorySelectionChanged,
    importSelectedRepositories: scmOnboarding.importSelectedRepositories,
    setSelectedOrganization: scmOnboarding.setSelectedOrganization,
    loadOrgHostUrl: scmOnboarding.loadOrgHostUrl,

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
    loadConfig: PropTypes.func.isRequired,
    loadOrganizations: PropTypes.func.isRequired,
    loadRepositories: PropTypes.func.isRequired
  })
};
