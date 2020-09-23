/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {connect} from 'react-redux';

import * as scmOnboardingActions from '../scmOnboarding/scmOnboardingActions';
import ScmOnboarding from '../scmOnboarding/ScmOnboarding';

function mapStateToProps({scmOnboarding}) {
  return {
    // config
    loadingConfig: scmOnboarding.loadingConfig,
    isManifestScanFeatureEnabled: scmOnboarding.isManifestScanFeatureEnabled,
    defaultHostUrlState: scmOnboarding.defaultHostUrlState,

    // organizations
    loadingOrganizations: scmOnboarding.loadingOrganizations,
    organizations: scmOnboarding.organizations,
    selectedOrganization: scmOnboarding.selectedOrganization,
    setSelectedOrganization: scmOnboarding.setSelectedOrganization,
    loadOrgHostUrl: scmOnboarding.loadOrgHostUrl,

    // repositories
    loadingRepositories: scmOnboarding.loadingRepositories,
    repositories: scmOnboarding.repositories
  };
}

export default connect(mapStateToProps, scmOnboardingActions)(ScmOnboarding);
