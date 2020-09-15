/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {useEffect} from 'react';

import MaximizedContainer from '../../react/MaximizedContainer';
import * as PropTypes from 'prop-types';
import {NxErrorAlert} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import ImportApplicationsForm, {textInputPropType} from './components/ImportApplicationsForm';
import ResultsTable from './components/ResultsTable';

export default function ScmOnboarding(props) {
  const {
    // actions
    loadConfig,
    loadOrganizations,
    loadRepositories,
    setSelectedOrganization,

    // configuration state
    loadingConfig,
    isManifestScanFeatureEnabled,

    // organizations state
    organizations,
    loadingOrganizations,
    selectedOrganization,

    // repositories state
    repositories,
    loadingRepositories,

    // external state
    isAuthorized,

    // base URL
    defaultHostUrlState
  } = props;

  useEffect(() => {
    loadOrganizations();
    loadConfig();
  }, []);

  return (
    <MaximizedContainer id="scm-onboarding-container" className="nx-page-content">
      <div id="scm-onboarding-root">
        <LoadWrapper loading={loadingConfig}>
          {isAuthorized && isManifestScanFeatureEnabled &&
          <div className="nx-page-main">
            <div className="iq-tile iq-tile--sys-prefs">
              <ImportApplicationsForm
                  setSelectedOrganization={setSelectedOrganization}
                  loadRepositories={loadRepositories}
                  organizations={organizations}
                  loadingOrganizations={loadingOrganizations}
                  selectedOrganization={selectedOrganization}
                  loadingRepositories={loadingRepositories}
                  defaultHostUrlState={defaultHostUrlState}/>
            </div>
            <div className="iq-tile iq-tile--sys-prefs">
              <ResultsTable
                repositories={repositories}
                loadingRepositories={loadingRepositories} />
            </div>
          </div>
          }
          {!isAuthorized &&
          <NxErrorAlert id="scm-onboarding-insufficient-permissions-error">
            <strong>Error</strong> It appears you do not have permission to access this page.
            If you believe this to be incorrect please contact your administrator.
          </NxErrorAlert>
          }
          {!isManifestScanFeatureEnabled && isAuthorized &&
          <NxErrorAlert id="scm-onboarding-feature-flag-disabled-error">
            <strong>Error</strong> This feature has not been enabled.
            If you believe this to be incorrect please contact your administrator.
          </NxErrorAlert>
          }
        </LoadWrapper>
      </div>
    </MaximizedContainer>
  );
}

export const organizationPropType = PropTypes.shape({
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired
});

export const repositoryPropType = PropTypes.shape({
  project: PropTypes.string.isRequired,
  namespace: PropTypes.string.isRequired,
  description: PropTypes.string
});

ScmOnboarding.propTypes = {
  // config
  loadConfig: PropTypes.func.isRequired,
  loadingConfig: PropTypes.bool.isRequired,
  isManifestScanFeatureEnabled: PropTypes.bool.isRequired,

  // organizations
  loadOrganizations: PropTypes.func.isRequired,
  loadingOrganizations: PropTypes.bool.isRequired,
  organizations: PropTypes.arrayOf(organizationPropType).isRequired,
  setSelectedOrganization: PropTypes.func.isRequired,
  selectedOrganization: PropTypes.object,

  // repositories
  loadRepositories: PropTypes.func.isRequired,
  loadingRepositories: PropTypes.bool.isRequired,
  repositories: PropTypes.arrayOf(repositoryPropType).isRequired,

  // from angular
  isAuthorized: PropTypes.bool.isRequired,

  // base URL
  defaultHostUrlState: textInputPropType.isRequired
};
