/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import * as scmOnboardingActions from './scmOnboardingActions';

import ScmOnboarding from '../scmOnboarding/ScmOnboarding';

function mapStateToProps({ scmOnboarding, router }) {
  return {
    //permissions
    loadingPermissions: scmOnboarding.permissionsState.loadingPermissions,
    loadingPermissionsError: scmOnboarding.permissionsState.loadingPermissionsError,

    // config
    loadingPage: scmOnboarding.viewState.loadingPage,
    scmProvider: scmOnboarding.configState.scmProvider,
    isRootScmConfigured: scmOnboarding.configState.isRootScmConfigured,

    // compositeSourceControl data
    isScmTokenConfigured: scmOnboarding.configState.isScmTokenConfigured,
    isScmTokenOverridden: scmOnboarding.configState.isScmTokenOverridden,
    validateScmHostUrl: scmOnboarding.viewState.validateScmHostUrl,

    // organizations
    organizations: scmOnboarding.formState.organizations,
    selectedOrganization: scmOnboarding.formState.selectedOrganization,
    isNewOrganizationModalVisible: scmOnboarding.viewState.isNewOrganizationModalVisible,

    // repositories
    loadingRepositories: scmOnboarding.viewState.loadingRepositories,
    repositories: scmOnboarding.formState.repositories,
    totalRepositories: scmOnboarding.formState.totalRepositories,
    selectedRepositoryCount: scmOnboarding.formState.selectedRepositoryCount,
    importedRepositoryCount: scmOnboarding.formState.importedRepositoryCount,
    failedImportCount: scmOnboarding.formState.failedImportCount,
    failedRepos: scmOnboarding.formState.failedRepos,
    newlyImportedRepos: scmOnboarding.formState.newlyImportedRepos,
    isImportStatusDialogVisible: scmOnboarding.viewState.isImportStatusDialogVisible,
    isImporting: scmOnboarding.viewState.isImporting,

    // host URL
    defaultHostUrl: scmOnboarding.formState.defaultHostUrl,
    currentHostUrlState: scmOnboarding.formState.currentHostUrlState,
    isGitHostDialogVisible: scmOnboarding.viewState.isGitHostDialogVisible,
    isGitHostNeeded: scmOnboarding.viewState.isGitHostNeeded,
    isSelectingOrganization: scmOnboarding.viewState.isSelectingOrganization,

    // sorting
    sortConfiguration: scmOnboarding.sortConfiguration,

    // router state
    preselectedOrganizationId: router.currentParams.organizationId,

    // error state
    generalError: scmOnboarding.viewState.generalError,
    loadRepositoriesErrorCode: scmOnboarding.viewState.loadRepositoriesErrorCode,
    addOrganizationError: scmOnboarding.viewState.addOrganizationError,
  };
}

const ScmOnboardingContainer = connect(mapStateToProps, scmOnboardingActions)(ScmOnboarding);
export default ScmOnboardingContainer;
