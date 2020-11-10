/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {useEffect, Fragment} from 'react';

import MaximizedContainer from '../../react/MaximizedContainer';
import * as PropTypes from 'prop-types';
import {NxBackButton, NxFontAwesomeIcon, NxTextInput} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import ResultsTable from './components/ResultsTable';
import {faSitemap} from '@fortawesome/pro-regular-svg-icons';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';

const permissionsError = `It appears you do not have permission to access this page.
        If you believe this to be incorrect please contact your administrator.`,
    disabledError = `This feature has not been enabled.
        If you believe this to be incorrect please contact your administrator.`;

export default function ScmOnboarding(props) {
  const {
    // actions
    loadConfig,
    loadOrganizations,
    loadRepositories,
    loadCompositeSourceControl,
    importSelectedRepositories,
    onRepositorySelectionChanged,
    loadOrgHostUrl,
    setCurrentHostUrl,

    // configuration state
    loadingConfig,
    isScmOnboardingFeatureEnabled,
    scmTokenConfigured,
    scmProvider,

    // orgs
    selectedOrganization,

    // repositories state
    repositories,
    loadingRepositories,
    selectedRepositoryCount,
    totalRepositories,

    // host URL
    defaultHostUrl,
    currentHostUrl,

    // from angular URL router
    isAuthorized,
    preselectedOrganizationId,
    $state,

    error: errorProp
  } = props;

  const scmConfigurationHref = $state.href($state.get('management.edit.organization.edit-source-control'), {
        organizationId: preselectedOrganizationId
      }),
      tokenNotConfiguredError = (
        <Fragment>
          The selected Organization does not have SCM configured. You can configure it{' '}
          <a href={scmConfigurationHref}>here</a>.
        </Fragment>
      ),
      error = !isAuthorized ? permissionsError :
        !isScmOnboardingFeatureEnabled ? disabledError :
          !scmTokenConfigured ? tokenNotConfiguredError :
            errorProp;

  function load() {
    loadConfig();
    loadOrganizations(preselectedOrganizationId);
    loadCompositeSourceControl('organization', preselectedOrganizationId);
  }

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    loadOrgHostUrl(preselectedOrganizationId, scmProvider);
  }, [scmProvider]);

  useEffect(() => {
    if (preselectedOrganizationId && defaultHostUrl) {
      loadRepositories(preselectedOrganizationId, currentHostUrl);
    }
  }, [defaultHostUrl]);

  function handleLoadRepositories(event) {
    event.preventDefault();
    loadRepositories(preselectedOrganizationId, currentHostUrl);
  }

  return (
    <MaximizedContainer id="scm-onboarding-container" className="nx-page-content">
      <main className="nx-page-main">
        <NxBackButton
            href={$state.href($state.get('management.view.organization'),
                {organizationId: preselectedOrganizationId})}
            targetPageTitle={$state.get('management.view.organization').data.title} />
        <div className="nx-page-title iq-scmonboarding-title">
          { selectedOrganization &&
            <h1 className="nx-h1">
              <span>Import Applications to</span>
              <NxFontAwesomeIcon icon={faSitemap}/>
              <span>{selectedOrganization.name}</span>
            </h1>
          }
          <div className="nx-page-title__description">
            <p className="nx-p">Use the filters and checkboxes to select repositories to import</p>
          </div>
        </div>
        <section className="nx-tile host-url-tile">
          <form className="nx-form">
            <div className="nx-form-row">
              <div className="nx-form-group">
                <label className="nx-label">
                  <span className="nx-label__text">Host URL</span>
                  <NxTextInput id="iq-scm-default-host-field"
                               isPristine={defaultHostUrl === currentHostUrl}
                               onChange={setCurrentHostUrl}
                               value={currentHostUrl}/>
                </label>
              </div>
              <div className="nx-btn-bar">
                <NxButton
                    id="iq-scm-load-button"
                    variant="primary"
                    disabled={loadingRepositories}
                    onClick={handleLoadRepositories}>
                  Reload Repositories
                </NxButton>
              </div>
            </div>
          </form>
        </section>
        <section className="nx-tile">
          <LoadWrapper loading={loadingConfig} error={error} retryHandler={load}>
            <ResultsTable { ...{
              repositories,
              loadingRepositories,
              selectedRepositoryCount,
              totalRepositories,
              onRepositorySelectionChanged,
              importSelectedRepositories,
              loadRepositories
            }} />
          </LoadWrapper>
        </section>
      </main>
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
  totalRepositories: PropTypes.number,

  // from angular router
  isAuthorized: PropTypes.bool.isRequired,
  preselectedOrganizationId: PropTypes.string,

  // actions
  importSelectedRepositories: PropTypes.func.isRequired,
  onRepositorySelectionChanged: PropTypes.func.isRequired,
  setCurrentHostUrl: PropTypes.func.isRequired,
  loadCompositeSourceControl: PropTypes.func.isRequired,

  // base URL
  defaultHostUrl: PropTypes.string,
  currentHostUrl: PropTypes.string,
  error: LoadWrapper.propTypes.error
};
