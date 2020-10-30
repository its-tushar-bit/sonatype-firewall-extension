/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {useEffect} from 'react';

import MaximizedContainer from '../../react/MaximizedContainer';
import * as PropTypes from 'prop-types';
import {NxBackButton, NxErrorAlert, NxFontAwesomeIcon, NxTextInput} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import ResultsTable from './components/ResultsTable';
import {faSitemap} from '@fortawesome/pro-regular-svg-icons';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';

export default function ScmOnboarding(props) {
  const {
    // actions
    loadConfig,
    loadOrganizations,
    loadRepositories,
    loadCompositeSourceControl,
    importSelectedRepositories,
    onRepositorySelectionChanged,
    defaultHostUrl,
    loadOrgHostUrl,

    // configuration state
    loadingConfig,
    isScmOnboardingFeatureEnabled,
    scmTokenConfigured,

    // orgs
    selectedOrganization,

    // repositories state
    repositories,
    loadingRepositories,
    selectedRepositoryCount,
    importedRepositoryCount,

    // from angular URL router
    isAuthorized,
    preselectedOrganizationId,
    $state
  } = props;

  let hostUrl = defaultHostUrl;

  function onDefaultHostUrlChanged(val) {
    hostUrl = val;
  }

  function load() {
    loadConfig();
    // todo use dynamic provider
    loadOrgHostUrl(preselectedOrganizationId, 'github');
    loadOrganizations(preselectedOrganizationId);
    if (preselectedOrganizationId) {
      loadRepositories(preselectedOrganizationId, hostUrl);
      loadCompositeSourceControl('organization', preselectedOrganizationId);
    }
  }

  useEffect(load, []);

  return (
    <MaximizedContainer id="scm-onboarding-container" className="nx-page-content">
      <div className="nx-page-main">
        <NxBackButton
            href={$state.href($state.get('management.view.organization'),
                {organizationId: preselectedOrganizationId})}
            targetPageTitle={$state.get('management.view.organization').data.title} />
        <div className="nx-page-title iq-page-title">
          {selectedOrganization &&
            <h1 className="nx-h1 iq-scmonboarding-title">
              Import Applications from Github to <NxFontAwesomeIcon icon={faSitemap}/> {selectedOrganization.name}
            </h1>
          }
          <div className="nx-page-title__description">
            <p className='nx-p'>Use the filters and checkboxes to select repositories to import</p>
          </div>
        </div>
        <div className="iq-tile">
          <form className='nx-form'>
            <fieldset className="nx-fieldset">
              <legend className="nx-label">Host URL</legend>
              <NxTextInput id='iq-scm-default-host-field'
                           defaultValue={defaultHostUrl}
                           isPristine={defaultHostUrl === hostUrl}
                           onChange={onDefaultHostUrlChanged}
                           value={hostUrl}/>
              <NxButton
                  id="iq-scm-load-button"
                  variant="primary"
                  disabled={loadingRepositories}
                  onClick={() => loadRepositories(preselectedOrganizationId, hostUrl)}>
                Reload Repositories
              </NxButton>
            </fieldset>
          </form>
        </div>
        <div className="iq-tile">
          <LoadWrapper loading={loadingConfig}>
            {isAuthorized && isScmOnboardingFeatureEnabled && scmTokenConfigured &&
              <ResultsTable
                repositories={repositories}
                loadingRepositories={loadingRepositories}
                selectedRepositoryCount={selectedRepositoryCount}
                importedRepositoryCount={importedRepositoryCount}
                onRepositorySelectionChanged={onRepositorySelectionChanged}
                importSelectedRepositories={importSelectedRepositories} />
            }
            {!isAuthorized &&
              <NxErrorAlert id="scm-onboarding-insufficient-permissions-error">
                <strong>Error</strong> It appears you do not have permission to access this page.
                If you believe this to be incorrect please contact your administrator.
              </NxErrorAlert>
            }
            {!isScmOnboardingFeatureEnabled && isAuthorized &&
              <NxErrorAlert id="scm-onboarding-feature-flag-disabled-error">
                <strong>Error</strong> This feature has not been enabled.
                If you believe this to be incorrect please contact your administrator.
              </NxErrorAlert>
            }
            {!scmTokenConfigured && isAuthorized && isScmOnboardingFeatureEnabled &&
              <NxErrorAlert id="scm-onboarding-invalid-token">
                <strong>Error</strong> The selected Organization does not have SCM configured. You can configure it
                <a href={$state.href($state.get('management.edit.organization.edit-source-control'),
                    {organizationId: preselectedOrganizationId})}> here</a>.
              </NxErrorAlert>
            }
          </LoadWrapper>
        </div>
      </div>
    </MaximizedContainer>
  );
}

export const organizationPropType = {
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired
};

export const repositoryPropType = {
  httpCloneUrl: PropTypes.string.isRequired,
  namespace: PropTypes.string,
  project: PropTypes.string,
  description: PropTypes.string,
  isSelected: PropTypes.bool,
  isImported: PropTypes.bool
};

ScmOnboarding.propTypes = {
  // config
  loadConfig: PropTypes.func.isRequired,
  loadingConfig: PropTypes.bool.isRequired,
  isScmOnboardingFeatureEnabled: PropTypes.bool.isRequired,
  $state: PropTypes.object.isRequired,
  scmTokenConfigured: PropTypes.bool.isRequired,
  scmProvider: PropTypes.string.isRequired,

  // organizations
  loadOrganizations: PropTypes.func.isRequired,
  loadingOrganizations: PropTypes.bool.isRequired,
  organizations: PropTypes.arrayOf(PropTypes.shape(organizationPropType)).isRequired,
  setSelectedOrganization: PropTypes.func.isRequired,
  selectedOrganization: PropTypes.shape(organizationPropType),
  loadOrgHostUrl: PropTypes.func.isRequired,

  // repositories
  loadRepositories: PropTypes.func.isRequired,
  loadingRepositories: PropTypes.bool.isRequired,
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)).isRequired,
  selectedRepositoryCount: PropTypes.number.isRequired,
  importedRepositoryCount: PropTypes.number,

  // from angular router
  isAuthorized: PropTypes.bool.isRequired,
  preselectedOrganizationId: PropTypes.string,

  // actions
  importSelectedRepositories: PropTypes.func.isRequired,
  onRepositorySelectionChanged: PropTypes.func.isRequired,
  loadCompositeSourceControl: PropTypes.func.isRequired,

  // base URL
  defaultHostUrl: PropTypes.string
};
