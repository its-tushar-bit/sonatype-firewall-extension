/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {useEffect, Fragment, useState} from 'react';

import MaximizedContainer from '../../react/MaximizedContainer';
import * as PropTypes from 'prop-types';
import {NxBackButton, NxFontAwesomeIcon, NxTextInput} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import ResultsTable from './components/ResultsTable';
import {faSitemap} from '@fortawesome/pro-regular-svg-icons';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';
import {NxSuccessAlert, NxErrorAlert, NxInfoAlert} from '@sonatype/react-shared-components/components/NxAlert/NxAlert';
import {validateHostUrl} from './utils/validators';
import {hasValidationErrors} from '../../util/validationUtil';

const permissionsError = `It appears you do not have permission to access this page.
        If you believe this to be incorrect please contact your administrator.`,
    disabledError = `This feature has not been enabled.
        If you believe this to be incorrect please contact your administrator.`;

export default function ScmOnboarding(props) {
  const {
    // sorting
    sortConfiguration,

    // actions
    setSorting,
    setSortingParameters,
    loadConfig,
    loadOrganizations,
    loadRepositories,
    loadCompositeSourceControl,
    validateScmHostUrl,
    importSelectedRepositories,
    onRepositorySelectionChanged,
    loadOrgHostUrl,
    setCurrentHostUrl,

    // configuration state
    loadingConfig,
    isScmOnboardingFeatureEnabled,
    isScmTokenConfigured,
    scmProvider,

    // orgs
    selectedOrganization,

    // repositories state
    repositories,
    loadingRepositories,
    selectedRepositoryCount,
    totalRepositories,
    newlyImportedRepos,
    failedImportCount,

    // host URL
    defaultHostUrl,
    currentHostUrlState,

    // from angular URL router
    isAuthorized,
    preselectedOrganizationId,
    $state,

    // errors
    lastErrorMessage
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
        isScmOnboardingFeatureEnabled === false ? disabledError :
          isScmTokenConfigured === false ? tokenNotConfiguredError :
            lastErrorMessage;

  function load() {
    loadConfig();
    loadOrganizations(preselectedOrganizationId);
    loadCompositeSourceControl('organization', preselectedOrganizationId);
  }

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    if (scmProvider) {
      loadOrgHostUrl(preselectedOrganizationId, scmProvider);
    }
  }, [scmProvider]);

  useEffect(() => {
    if (preselectedOrganizationId && defaultHostUrl) {
      loadRepositories(preselectedOrganizationId, currentHostUrlState.value);
    }
  }, [defaultHostUrl]);

  const [isSuccessMessageOpen, setIsSuccessMessageOpen] = useState(false),
      [isFormErrorOpen, setIsFormErrorOpen] = useState(false),
      [isFormInfoOpen, setIsFormInfoOpen] = useState(false);

  useEffect(() => {
    setIsSuccessMessageOpen(newlyImportedRepos.length > 0 && failedImportCount === 0);
    setIsFormErrorOpen(newlyImportedRepos.length === 0 && failedImportCount > 0);
    setIsFormInfoOpen(newlyImportedRepos.length > 0 && failedImportCount > 0);
  }, [failedImportCount, newlyImportedRepos]);

  function dismissSuccessMessage() {
    setIsSuccessMessageOpen(false);
  }

  function dismissFormError() {
    setIsFormErrorOpen(false);
  }

  function dismissFormInfo() {
    setIsFormInfoOpen(false);
  }

  function handleLoadRepositories(event) {
    event.preventDefault();
    loadRepositories(preselectedOrganizationId, currentHostUrlState.value ? currentHostUrlState.value : defaultHostUrl);
  }

  function validateAndSetCurrentHostUrl(value) {
    setCurrentHostUrl(value);
    if (value && !hasValidationErrors(validateHostUrl(value))) {
      validateScmHostUrl(scmProvider, value);
    }
  }

  return (
    <MaximizedContainer id="scm-onboarding-container" className="nx-page-content">
      <main className="nx-page-main">
        <NxBackButton
            href={$state.href($state.get('management.view.organization'),
                {organizationId: preselectedOrganizationId})}
            targetPageTitle={$state.get('management.view.organization').data.title} />
        {!error &&
          <Fragment>
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
              {isSuccessMessageOpen &&
              <NxSuccessAlert onClose={dismissSuccessMessage}>
                {newlyImportedRepos.length} repositories were successfully imported to IQ Server as applications under
                the {selectedOrganization.name} Organization.
              </NxSuccessAlert>
              }
              {isFormInfoOpen &&
              <NxInfoAlert onClose={dismissFormInfo}>
                {newlyImportedRepos.length} repositories were successfully imported to IQ Server as applications under
                the {selectedOrganization.name} Organization.<br/>
                {failedImportCount} repositories failed to import.
              </NxInfoAlert>
              }
              {isFormErrorOpen &&
              <NxErrorAlert onClose={dismissFormError}>
                {failedImportCount} repositories failed to import.
              </NxErrorAlert>
              }
            </div>
            <section className="nx-tile host-url-tile">
              <form className="nx-form">
                <div className='nx-tile-content'>
                  <div className="nx-form-row">
                    <div className="nx-form-group">
                      <label className="nx-label">
                        <span className="nx-label__text">Host URL</span>
                        <NxTextInput id="iq-scm-default-host-field"
                                     { ...currentHostUrlState }
                                     onChange={validateAndSetCurrentHostUrl}
                                     validatable={true}
                                     placeholder={defaultHostUrl}/>
                      </label>
                    </div>
                    <div className="nx-btn-bar">
                      <NxButton
                          id="iq-scm-load-button"
                          variant="primary"
                          disabled={loadingRepositories || hasValidationErrors(currentHostUrlState.validationErrors) }
                          onClick={handleLoadRepositories}>
                        Reload Repositories
                      </NxButton>
                    </div>
                  </div>
                </div>
              </form>
            </section>
          </Fragment>
        }
        <section className="nx-tile">
          <LoadWrapper loading={loadingConfig} error={error} retryHandler={load}>
            <ResultsTable { ...{
              repositories,
              loadingRepositories,
              selectedRepositoryCount,
              totalRepositories,
              onRepositorySelectionChanged,
              importSelectedRepositories,
              loadRepositories,
              preselectedOrganizationId,
              sortConfiguration,
              setSorting,
              setSortingParameters
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

const textInputPropType = {
  value: PropTypes.string.isRequired,
  trimmedValue: PropTypes.string.isRequired,
  isPristine: PropTypes.bool.isRequired,
  validationErrors: PropTypes.oneOfType([PropTypes.arrayOf(PropTypes.string.isRequired), PropTypes.string])
};

ScmOnboarding.propTypes = {
  // config
  loadingConfig: PropTypes.bool.isRequired,
  isScmOnboardingFeatureEnabled: PropTypes.bool,
  $state: PropTypes.object.isRequired,
  isScmTokenConfigured: PropTypes.bool.isRequired,
  scmProvider: PropTypes.string,

  // organizations
  loadOrganizations: PropTypes.func.isRequired,
  loadingOrganizations: PropTypes.bool.isRequired,
  organizations: PropTypes.arrayOf(PropTypes.shape(organizationPropType)),
  setSelectedOrganization: PropTypes.func.isRequired,
  selectedOrganization: PropTypes.shape(organizationPropType),
  loadOrgHostUrl: PropTypes.func.isRequired,

  // repositories
  loadRepositories: PropTypes.func.isRequired,
  loadingRepositories: PropTypes.bool.isRequired,
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  selectedRepositoryCount: PropTypes.number.isRequired,
  totalRepositories: PropTypes.number,
  importedRepositoryCount: PropTypes.number,
  failedImportCount: PropTypes.number,
  newlyImportedRepos: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)).isRequired,

  // from angular router
  isAuthorized: PropTypes.bool.isRequired,
  preselectedOrganizationId: PropTypes.string,

  // sorting
  sortConfiguration: PropTypes.shape({
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string,
    key: PropTypes.string
  }),

  // actions
  setSorting: PropTypes.func,
  setSortingParameters: PropTypes.func,
  loadConfig: PropTypes.func.isRequired,
  importSelectedRepositories: PropTypes.func.isRequired,
  onRepositorySelectionChanged: PropTypes.func.isRequired,
  setCurrentHostUrl: PropTypes.func.isRequired,
  loadCompositeSourceControl: PropTypes.func.isRequired,
  validateScmHostUrl: PropTypes.func.isRequired,

  // base URL
  defaultHostUrl: PropTypes.string,
  currentHostUrlState: PropTypes.shape(textInputPropType),

  // errors
  lastErrorMessage: PropTypes.string
};
